package com.nowline;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class NewsService {
    private final Map<Integer, Article> currentArticles = new HashMap<>();
    private final String apiKey = System.getenv("NEWS_API_KEY");

    public void showNews(String category, int userId, Scanner sc) {
        List<Article> articles = new NewsProcessor().process(fetch(category));
        for (Article article : articles) saveArticle(article);
        if (articles.isEmpty()) {
            System.out.println("No articles available. Check internet/API settings.");
            return;
        }
        currentArticles.clear();
        int number = 1;
        for (Article article : articles) {
            currentArticles.put(number, article);
            System.out.println("\n" + number + ". [" + article.getImportance() + "] " + article.getTitle());
            System.out.println(article.getDescription());
            System.out.println("Category: " + article.getCategory() + " | Source: " + article.getSource());
            System.out.println("Published: " + article.getDate());
            System.out.println("Read more: " + article.getUrl());
            number++;
        }
        System.out.print("\nEnter article number to bookmark (0 to return): ");
        try {
            int selected = Integer.parseInt(sc.nextLine());
            if (currentArticles.containsKey(selected))
                new BookmarkService().save(userId, currentArticles.get(selected));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }

    public void search(int userId, Scanner sc) {
        System.out.print("Search keyword: ");
        String keyword = sc.nextLine().trim();
        if (keyword.isEmpty()) return;
        showNews("search:" + keyword, userId, sc);
    }

    public void dailyBriefing(int userId) {
        List<Article> articles = fetch("top");
        System.out.println("\n========== DAILY BRIEFING ==========");
        if (articles.isEmpty()) {
            System.out.println("No current headlines available.");
            return;
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (Article a : articles) {
            counts.put(a.getCategory(), counts.getOrDefault(a.getCategory(), 0) + 1);
            System.out.println("[" + a.getImportance() + "] " + a.getTitle());
        }
        System.out.println("\nToday's article count:");
        counts.forEach((category, count) -> System.out.println(category + ": " + count));
    }

    private List<Article> fetch(String category) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("NEWS_API_KEY not set. Showing demo articles.");
            return demoArticles(category);
        }
        try {
            String query;
            if (category.equals("top"))
                query = "https://newsapi.org/v2/everything?q=India&language=en&sortBy=publishedAt&pageSize=10&apiKey=" + apiKey;

            else if (category.equals("national"))
                query = "https://newsapi.org/v2/everything?q=India&language=en&sortBy=relevancy&pageSize=10&apiKey=" + apiKey;
            else {
                String term = category.startsWith("search:") ? category.substring(7) : category;
                if (term.equals("international")) term = "world";
                if (term.equals("awards")) term = "awards";
                if (term.equals("science")) term = "science technology";
                if (term.equals("finance")) term = "business finance";
                query = "https://newsapi.org/v2/everything?q=" + java.net.URLEncoder.encode(term, java.nio.charset.StandardCharsets.UTF_8)
                        + "&sortBy=publishedAt&pageSize=10&apiKey=" + apiKey;
            }
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(query)).timeout(java.time.Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject data = new JSONObject(response.body());
            if (!"ok".equals(data.optString("status"))) {
                System.out.println("News API: " + data.optString("message", "Request failed"));
                return demoArticles(category);
            }
            JSONArray list = data.getJSONArray("articles");
            List<Article> result = new ArrayList<>();
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                String title = item.optString("title", "Untitled");
                String desc = item.optString("description", "No description available.");
                String source = item.optJSONObject("source") == null ? "Unknown" :
                        item.getJSONObject("source").optString("name", "Unknown");
                String url = item.optString("url", "");
                String date = item.optString("publishedAt", "Unknown");
                String importance = classify(title);
                Article article = new Article(title, desc, category, source, date, url, importance);
                result.add(article);
                // Articles are cached after queue processing.
            }
            return result;
        } catch (Exception e) {
            System.out.println("Could not fetch live news: " + e.getMessage());
            return demoArticles(category);
        }
    }

    private String classify(String title) {
        String t = title.toLowerCase();
        if (t.contains("breaking") || t.contains("crisis") || t.contains("attack")) return "BREAKING";
        if (t.contains("government") || t.contains("election") || t.contains("court") || t.contains("summit"))
            return "HIGH PRIORITY";
        return "GENERAL";
    }

    private void saveArticle(Article a) {
        String sql = "INSERT INTO articles(title,description,category,source,published_at,url,importance) " +
                     "VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title)";
        try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
            ps.setString(1, a.getTitle()); ps.setString(2, a.getDescription());
            ps.setString(3, a.getCategory()); ps.setString(4, a.getSource());
            ps.setString(5, a.getDate()); ps.setString(6, a.getUrl());
            ps.setString(7, a.getImportance()); ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Article cache not saved: " + e.getMessage());
        }
    }

    private List<Article> demoArticles(String category) {
        String label = category.equals("top") ? "National" : category;
        return List.of(
            new Article("Demo: Latest developments in " + label, "Sample article shown when live news is unavailable.",
                    label, "NOWLINE Demo", "Today", "https://example.com/demo/" + java.net.URLEncoder.encode(label, java.nio.charset.StandardCharsets.UTF_8), "HIGH PRIORITY"),
            new Article("Demo: Updates and important stories", "Replace demo data by configuring a News API key.",
                    label, "NOWLINE Demo", "Today", "https://example.org/demo/" + java.net.URLEncoder.encode(label, java.nio.charset.StandardCharsets.UTF_8), "GENERAL")
        );
    }
}
