import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class NowlineServer {
    record User(long id, String name, String email) {}
    static class InvalidInput extends Exception { InvalidInput(String m){super(m);} }
    static final Map<String,User> sessions = new HashMap<>();
    static final ExecutorService workers = Executors.newFixedThreadPool(4);
    static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/nowline", System.getenv().getOrDefault("DB_USER","root"), System.getenv("DB_PASSWORD"));
    }
    static String hash(String password, byte[] salt) throws Exception {
        return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(),salt,210000,256)).getEncoded());
    }
    static String encodePassword(String password) throws Exception {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt)+":"+hash(password,salt);
    }
    static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
    static String json(User u){return "{\"uid\":"+q(Long.toString(u.id()))+",\"displayName\":"+q(u.name())+",\"email\":"+q(u.email())+"}";}
    static Map<String,String> form(HttpExchange e) throws Exception {
        Map<String,String> m=new HashMap<>(); String body=new String(e.getRequestBody().readNBytes(16385),StandardCharsets.UTF_8);
        if(body.length()>16384) throw new InvalidInput("Request too large");
        for(String p:body.split("&")){String[] a=p.split("=",2);if(a.length==2)m.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),URLDecoder.decode(a[1],StandardCharsets.UTF_8));}return m;
    }
    static String email(Map<String,String> f) throws InvalidInput {String s=f.getOrDefault("email","").trim().toLowerCase(Locale.ROOT);if(!s.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")||s.length()>254)throw new InvalidInput("Invalid email");return s;}
    static User authenticate(Map<String,String> f, boolean register) throws Exception {
        String email=email(f), password=f.getOrDefault("password",""), name=f.getOrDefault("name","").trim();
        try(Connection c=connect()){
            if(register){if(password.length()<8||password.length()>256||name.isBlank()||name.length()>100)throw new InvalidInput("Name and password of at least 8 characters required");
                try(PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,password_hash) VALUES(?,?,?)")){p.setString(1,name);p.setString(2,email);p.setString(3,encodePassword(password));p.executeUpdate();}}
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE email=?")){p.setString(1,email);try(ResultSet r=p.executeQuery()){if(r.next()){String[] h=r.getString("password_hash").split(":");if(MessageDigest.isEqual(h[1].getBytes(StandardCharsets.UTF_8),hash(password,Base64.getDecoder().decode(h[0])).getBytes(StandardCharsets.UTF_8)))return new User(r.getLong("id"),r.getString("name"),email);}}}
        }throw new InvalidInput("Email or password is incorrect");
    }
    static synchronized User session(String token){return sessions.get(token);}
    static synchronized String login(User u){String t=UUID.randomUUID().toString();sessions.put(t,u);return t;}
    static synchronized void logout(String t){sessions.remove(t);}
    static String poll(User u) throws SQLException {
        int yes=0,no=0;boolean voted=false;
        try(Connection c=connect();PreparedStatement p=c.prepareStatement("SELECT user_id,choice FROM votes");ResultSet r=p.executeQuery()){while(r.next()){if(r.getString(2).equals("yes"))yes++;else no++;if(r.getLong(1)==u.id())voted=true;}}
        return "{\"yes\":"+yes+",\"no\":"+no+",\"voted\":"+voted+"}";
    }
    static synchronized void donate(User u,Map<String,String> f) throws Exception {
        BigDecimal a=new BigDecimal(f.getOrDefault("amount","0"));String freq=f.getOrDefault("frequency","");if(a.signum()<=0||a.compareTo(new BigDecimal("1000000"))>0||a.scale()>2||!Set.of("monthly","onetime").contains(freq))throw new InvalidInput("Invalid donation");
        try(Connection c=connect()){c.setAutoCommit(false);try(PreparedStatement p=c.prepareStatement("INSERT INTO donations(user_id,amount,frequency) VALUES(?,?,?)");PreparedStatement t=c.prepareStatement("UPDATE donation_total SET total=total+? WHERE id=1")){p.setLong(1,u.id());p.setBigDecimal(2,a);p.setString(3,freq);p.executeUpdate();t.setBigDecimal(1,a);if(t.executeUpdate()!=1)throw new SQLException("Missing total row");c.commit();}catch(Exception ex){c.rollback();throw ex;}}
    }
    static void handle(HttpExchange e) throws java.io.IOException {
        int status=200;String out;try{
            String path=e.getRequestURI().getPath();String token=e.getRequestHeaders().getFirst("X-Session");User u=session(token);Map<String,String> f=form(e);
            if(path.equals("/api/login")||path.equals("/api/register")){u=authenticate(f,path.endsWith("register"));out="{\"token\":"+q(login(u))+",\"user\":"+json(u)+"}";}
            else if(path.equals("/api/newsletter")){try(Connection c=connect();PreparedStatement p=c.prepareStatement("INSERT INTO subscribers(email) VALUES(?)")){p.setString(1,email(f));p.executeUpdate();}out="{}";}
            else if(u==null){status=401;out="{\"error\":\"Sign in required\"}";}
            else if(path.equals("/api/me"))out=json(u);
            else if(path.equals("/api/logout")){logout(token);out="{}";}
            else if(path.equals("/api/poll"))out=poll(u);
            else if(path.equals("/api/vote")){String choice=f.get("choice");if(!Set.of("yes","no").contains(choice))throw new InvalidInput("Invalid vote");try(Connection c=connect();PreparedStatement p=c.prepareStatement("INSERT INTO votes VALUES(?,?)")){p.setLong(1,u.id());p.setString(2,choice);p.executeUpdate();}out=poll(u);}
            else if(path.equals("/api/donate")){donate(u,f);out="{}";}
            else {status=404;out="{\"error\":\"Not found\"}";}
        }catch(SQLIntegrityConstraintViolationException ex){status=409;out="{\"error\":\"Record already exists or violates a constraint\"}";}catch(InvalidInput|NumberFormatException ex){status=400;out="{\"error\":"+q(ex.getMessage())+"}";}catch(Exception ex){ex.printStackTrace();status=500;out="{\"error\":\"Server or database error\"}";}
        byte[] bytes=out.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.sendResponseHeaders(status,bytes.length);try(var stream=e.getResponseBody()){stream.write(bytes);}
    }
    public static void main(String[] args) throws Exception {try(Connection c=connect()){System.out.println("MySQL connected");}HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",8080),0);s.createContext("/api/",NowlineServer::handle);s.setExecutor(workers);s.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{s.stop(1);workers.shutdown();}));System.out.println("NOWLINE API on 8080");}
}
