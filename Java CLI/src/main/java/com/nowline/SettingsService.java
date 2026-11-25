package com.nowline;

import java.sql.*;
import java.util.*;

public class SettingsService {
    private static final String[] CATEGORIES =
            {"national", "international", "politics", "sports", "awards", "finance", "science"};

    public void menu(int userId, Scanner sc) {
        while (true) {
            System.out.println("\n========== SETTINGS ==========");
            System.out.println("1. Personalize news interests");
            System.out.println("2. View my preferences");
            System.out.println("3. Back");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine();
            if (choice.equals("3")) return;
            if (choice.equals("1")) update(userId, sc);
            else if (choice.equals("2")) view(userId);
            else System.out.println("Invalid choice.");
        }
    }

    private void update(int userId, Scanner sc) {
        System.out.println("Available: " + String.join(", ", CATEGORIES));
        System.out.print("Enter preferred categories separated by commas: ");
        String[] selected = sc.nextLine().toLowerCase().split(",");
        String delete = "DELETE FROM user_preferences WHERE user_id=?";
        String insert = "INSERT IGNORE INTO user_preferences(user_id,category) VALUES(?,?)";
        Connection con = null;
        try {
            con = DB.getConnection();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(delete)) {
                ps.setInt(1, userId); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                for (String item : selected) {
                    String category = item.trim();
                    if (Arrays.asList(CATEGORIES).contains(category)) {
                        ps.setInt(1, userId); ps.setString(2, category); ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            con.commit();
            System.out.println("Preferences updated.");
        } catch (SQLException e) {
            if (con != null) { try { con.rollback(); } catch (SQLException rollbackError) { System.out.println(rollbackError.getMessage()); } }
            System.out.println("Could not update preferences: " + e.getMessage());
        } finally {
            if (con != null) { try { con.setAutoCommit(true); } catch (SQLException e) { DB.close(); } }
        }
    }

    private void view(int userId) {
        String sql = "SELECT category FROM user_preferences WHERE user_id=?";
        try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("Your interests:");
                boolean found = false;
                while (rs.next()) { found = true; System.out.println("- " + rs.getString(1)); }
                if (!found) System.out.println("No preferences selected.");
            }
        } catch (SQLException e) {
            System.out.println("Could not load preferences: " + e.getMessage());
        }
    }
}
