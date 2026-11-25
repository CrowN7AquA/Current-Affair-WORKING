package com.nowline;

import java.io.Console;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.Scanner;

public class AuthService {

    public void register(Scanner sc) {
        try {
            System.out.print("Name: ");
            String name = sc.nextLine().trim();

            System.out.print("Email: ");
            String email = sc.nextLine().trim().toLowerCase();

            String password = readPassword("Password: ");

            if (name.isEmpty() || !email.contains("@") 
                    || password.length() < 6)
                throw new IllegalArgumentException(
                    "Enter a name, valid email and password of at least 6 characters."
                );

            String sql = "INSERT INTO users(name, email, password_hash) VALUES(?,?,?)";

            try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hash(password));
                ps.executeUpdate();
            }

            System.out.println("Registration successful. You can now log in.");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("This email is already registered.");

        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    public User login(Scanner sc) {
        try {
            System.out.print("Email: ");
            String email = sc.nextLine().trim().toLowerCase();

            String password = readPassword("Password: ");

            String sql = "SELECT user_id, name, email FROM users WHERE email=? AND password_hash=?";

            try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
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

    private String readPassword(String prompt) {
        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException(
                "Secure password input unavailable. Run in an integrated terminal."
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
            value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte b : bytes)
            result.append(String.format("%02x", b));

        return result.toString();
    }
}