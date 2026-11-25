package com.nowline;

import java.util.Arrays;
import java.util.Scanner;

public class Main {
    static String databasePassword = "";
    private static final Scanner sc = new Scanner(System.in);
    private static final AuthService auth = new AuthService();
    private static final NewsService news = new NewsService();
    private static final QuizService quiz = new QuizService();
    private static final BookmarkService bookmarks = new BookmarkService();
    private static final SettingsService settings = new SettingsService();

    public static void main(String[] args) {
    databasePassword = System.getenv("NOWLINE_DB_PASSWORD");

    if (databasePassword == null) {
        if (System.console() != null) {
            char[] password = System.console().readPassword(
                "Enter MySQL password: "
            );

            databasePassword = new String(password);
            Arrays.fill(password, '\0');
        } else {
            System.out.println("Database password not configured.");
            return;
        }
    }
        try { DB.getConnection(); System.out.println("MySQL connected."); }
        catch (java.sql.SQLException e) { System.out.println("Database connection failed: " + e.getMessage()); return; }
        while (true) {
            System.out.println("\n========== NOWLINE ==========");
            System.out.println("YOUR WORLD. YOUR NEWS.");
            System.out.println("1. Start\n2. Exit");
            int choice = readChoice();
            if (choice == 2) break;
            if (choice == 1) accountMenu();
            else System.out.println("Please choose 1 or 2.");
        }
        System.out.println("Thank you for using NOWLINE!");
        DB.close();
    }

    private static void accountMenu() {
        while (true) {
            System.out.println("\n========== ACCOUNT ==========");
            System.out.println("1. Login\n2. Register\n3. Exit");
            int choice = readChoice();
            if (choice == 3) return;
            if (choice == 1) {
                User user = auth.login(sc);
                if (user != null) dashboard(user);
            } else if (choice == 2) {
                auth.register(sc);
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private static void dashboard(User user) {
        while (true) {
            System.out.println("\n========== NOWLINE DASHBOARD ==========");
            System.out.println("Welcome, " + user.getName());
            System.out.println("1. Today's Headlines");
            System.out.println("2. National News");
            System.out.println("3. International News");
            System.out.println("4. Politics");
            System.out.println("5. Sports");
            System.out.println("6. Awards & Achievements");
            System.out.println("7. Business & Finance");
            System.out.println("8. Science & Technology");
            System.out.println("9. Search News");
            System.out.println("10. Daily Briefing");
            System.out.println("11. Current Affairs Quiz");
            System.out.println("12. Bookmarks");
            System.out.println("13. Settings");
            System.out.println("14. Logout");
            System.out.println("15. News processing demonstration");

            int choice = readChoice();
            if (choice == 14) return;
            switch (choice) {
                case 1 -> news.showNews("top", user.getId(), sc);
                case 2 -> news.showNews("national", user.getId(), sc);
                case 3 -> news.showNews("international", user.getId(), sc);
                case 4 -> news.showNews("politics", user.getId(), sc);
                case 5 -> news.showNews("sports", user.getId(), sc);
                case 6 -> news.showNews("awards", user.getId(), sc);
                case 7 -> news.showNews("finance", user.getId(), sc);
                case 8 -> news.showNews("science", user.getId(), sc);
                case 9 -> news.search(user.getId(), sc);
                case 10 -> news.dailyBriefing(user.getId());
                case 11 -> quiz.start(user.getId(), sc);
                case 12 -> bookmarks.menu(user.getId(), sc);
                case 13 -> settings.menu(user.getId(), sc);
                case 15 -> new NewsProcessor().processDemo();
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static int readChoice() {
        System.out.print("Enter choice: ");
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}
