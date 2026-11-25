package com.nowline;

import java.sql.*;

public class DB {
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = System.getenv().getOrDefault("NOWLINE_DB_URL",
                    "jdbc:mysql://localhost:3306/nowline_db?serverTimezone=UTC");
            String user = System.getenv().getOrDefault("NOWLINE_DB_USER", "root");
            String password = System.getenv().getOrDefault("NOWLINE_DB_PASSWORD", "");
            if (password.isEmpty()) password = Main.databasePassword;
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.out.println("Could not close database connection: " + e.getMessage());
        }
    }
}
