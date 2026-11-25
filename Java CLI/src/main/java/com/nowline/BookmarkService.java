package com.nowline;

import java.sql.*;

public class BookmarkService {
    public void menu(int userId, java.util.Scanner scanner) {
        show(userId);
        System.out.print("Enter bookmark article ID to remove (0 to return): ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            if (id == 0) return;
            try (PreparedStatement statement = DB.getConnection().prepareStatement(
                    "DELETE FROM bookmarks WHERE user_id=? AND article_id=?")) {
                statement.setInt(1, userId);
                statement.setInt(2, id);
                System.out.println(statement.executeUpdate() == 1 ? "Bookmark removed." : "Bookmark not found.");
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Could not remove bookmark: " + e.getMessage());
        }
    }
    public void save(int userId, Article article) {
        String find = "SELECT article_id FROM articles WHERE url=?";
        String insert = "INSERT IGNORE INTO bookmarks(user_id, article_id) VALUES(?,?)";
        try {
            int articleId;
            try (PreparedStatement ps = DB.getConnection().prepareStatement(find)) {
                ps.setString(1, article.getUrl());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Article is not stored yet. Refresh live news first.");
                        return;
                    }
                    articleId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = DB.getConnection().prepareStatement(insert)) {
                ps.setInt(1, userId);
                ps.setInt(2, articleId);
                ps.executeUpdate();
            }
            System.out.println("Article bookmarked!");
        } catch (SQLException e) {
            System.out.println("Bookmark failed: " + e.getMessage());
        }
    }

    public void show(int userId) {
        String sql = "SELECT a.article_id, a.title, a.source, a.url FROM articles a JOIN bookmarks b ON a.article_id=b.article_id WHERE b.user_id=?";
        try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n========== YOUR BOOKMARKS ==========");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println(rs.getInt("article_id") + ". " + rs.getString("title"));
                    System.out.println("  Source: " + rs.getString("source"));
                    System.out.println("  " + rs.getString("url"));
                }
                if (!found) System.out.println("No bookmarks saved yet.");
            }
        } catch (SQLException e) {
            System.out.println("Could not load bookmarks: " + e.getMessage());
        }
    }
}
