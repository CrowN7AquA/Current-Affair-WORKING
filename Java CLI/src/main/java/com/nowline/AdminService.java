package com.nowline;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Scanner;

public class AdminService {

    private final AuthService auth = new AuthService();

    public void menu(Scanner sc) {

        while (true) {

            System.out.println("\n========== NOWLINE ADMIN ==========");
            System.out.println("Welcome, Administrator!");
            System.out.println("1. View all registered users");
            System.out.println("2. Search user by email");
            System.out.println("3. Add new user");
            System.out.println("4. Remove user by email");
            System.out.println("5. Total registered users");
            System.out.println("6. Update user name");
            System.out.println("7. Logout");
            System.out.print("Enter choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewUsers();
                case "2" -> searchUser(sc);
                case "3" -> auth.register(sc);
                case "4" -> removeUser(sc);
                case "5" -> countUsers();
                case "6" -> updateUser(sc);
                case "7" -> {
                    System.out.println("Admin logged out.");
                    return;
                }

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewUsers() {

        String sql = "SELECT user_id, name, email FROM users ORDER BY user_id";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== REGISTERED USERS ==========");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println(
                        rs.getInt("user_id") + " | "
                        + rs.getString("name") + " | "
                        + rs.getString("email")
                );
            }

            if (!found) {
                System.out.println("No registered users.");
            }

        } catch (SQLException e) {
            System.out.println("Unable to display users: " + e.getMessage());
        }
    }

    private void searchUser(Scanner sc) {

        System.out.print("Enter user email: ");

        String email = sc.nextLine().trim()
                .toLowerCase(Locale.ROOT);

        String sql = "SELECT user_id, name, email FROM users WHERE email=?";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    System.out.println("User ID: " + rs.getInt("user_id"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Email: " + rs.getString("email"));

                } else {
                    System.out.println("User not found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }
    }

    private void removeUser(Scanner sc) {

        System.out.print("Enter email of user to remove: ");

        String email = sc.nextLine().trim()
                .toLowerCase(Locale.ROOT);

        if (email.equals(AuthService.ADMIN_EMAIL)) {
            System.out.println("Administrator cannot be removed.");
            return;
        }

        try {

            if (!auth.emailExists(email)) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Permanently delete " + email + "? (yes/no): ");

            if (!sc.nextLine().trim().equalsIgnoreCase("yes")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            String sql = "DELETE FROM users WHERE email=?";

            try (PreparedStatement ps =
                    DB.getConnection().prepareStatement(sql)) {

                ps.setString(1, email);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("User removed successfully.");
                } else {
                    System.out.println("User not found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Unable to remove user: " + e.getMessage());
        }
    }

    private void countUsers() {

        String sql = "SELECT COUNT(*) AS total FROM users";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                System.out.println("Total registered users: " + rs.getInt("total"));
            }

        } catch (SQLException e) {
            System.out.println("Unable to count users: " + e.getMessage());
        }
    }

    private void updateUser(Scanner sc) {

        System.out.print("Enter user email: ");

        String email = sc.nextLine().trim()
                .toLowerCase(Locale.ROOT);

        try {

            if (!auth.emailExists(email)) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Enter new name: ");

            String name = sc.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("Name cannot be empty.");
                return;
            }

            String sql = "UPDATE users SET name=? WHERE email=?";

            try (PreparedStatement ps =
                    DB.getConnection().prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("User updated successfully.");
                } else {
                    System.out.println("User not found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }
}