package com.nowline;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class AuthService {

    public static final String ADMIN_EMAIL = "bhavinahuja333@gmail.com";

    public void register(Scanner sc) {
        try {
            String name;

            while (true) {
                System.out.print("Name: ");
                name = sc.nextLine().trim();

                if (!name.isEmpty()) break;

                System.out.println("Name cannot be empty.");
            }

            String email;

            while (true) {
                System.out.print("Email: ");
                email = sc.nextLine().trim()
                        .toLowerCase(Locale.ROOT);

                if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    System.out.println("Invalid email address.");
                    continue;
                }

                if (email.equals(ADMIN_EMAIL)) {
                    System.out.println("This email is reserved.");
                    continue;
                }

                if (emailExists(email)) {
                    System.out.println("Email already registered. Enter another email.");
                    continue;
                }

                break;
            }

            String password;

            while (true) {
                password = readPassword("Password: ");

                if (password.length() < 6) {
                    System.out.println("Password must contain at least 6 characters.");
                    continue;
                }

                String confirm = readPassword("Confirm Password: ");

                if (!password.equals(confirm)) {
                    System.out.println("Passwords do not match. Try again.");
                    continue;
                }

                break;
            }

            String sql = "INSERT INTO users(name, email, password_hash) VALUES(?,?,?)";

            try (PreparedStatement ps =
                    DB.getConnection().prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hash(password));

                ps.executeUpdate();
            }

            System.out.println("Registration successful.");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Registration rejected. Email may already exist.");

        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    public User login(Scanner sc) {
        try {
            System.out.print("Email: ");
            String email = sc.nextLine().trim()
                    .toLowerCase(Locale.ROOT);

            String password = readPassword("Password: ");

            if (email.equals(ADMIN_EMAIL)) {
                String adminPassword =
                        System.getenv("NOWLINE_ADMIN_PASSWORD");

                if (adminPassword == null || adminPassword.isBlank()) {
                    System.out.println("Admin password is not configured.");
                    return null;
                }

                if (MessageDigest.isEqual(
                        password.getBytes(StandardCharsets.UTF_8),
                        adminPassword.getBytes(StandardCharsets.UTF_8))) {

                    System.out.println("Admin login successful!");

                    return new User(-1, "Administrator", ADMIN_EMAIL);
                }

                System.out.println("Incorrect admin password.");
                return null;
            }

            String sql = "SELECT user_id, name, email FROM users "
                    + "WHERE email=? AND password_hash=?";

            try (PreparedStatement ps =
                    DB.getConnection().prepareStatement(sql)) {

                ps.setString(1, email);
                ps.setString(2, hash(password));

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        System.out.println("Login successful!");

                        return new User(
                                rs.getInt("user_id"),
                                rs.getString("name"),
                                rs.getString("email")
                        );
                    }
                }
            }

            System.out.println("Incorrect email or password.");

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        return null;
    }

    public boolean emailExists(String email) throws SQLException {

        String sql = "SELECT user_id FROM users WHERE email=?";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String readPassword(String prompt) {

        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException(
                    "Run Java in an integrated terminal."
            );
        }

        char[] password = console.readPassword("%s", prompt);

        if (password == null) {
            throw new IllegalStateException("Password input cancelled.");
        }

        String value = new String(password);

        Arrays.fill(password, '\0');

        return value;
    }

    private String hash(String value) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] bytes = md.digest(
                value.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}