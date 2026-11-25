
package com.nowline;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class NewsService {

    private final Map<Integer, Article> currentArticles =
            new HashMap<>();

    private final String apiKey =
            System.getenv("NEWS_API_KEY");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void showNews(String category, int userId, Scanner sc) {

        List<Article> articles =
                new NewsProcessor().process(fetch(category));

        for (Article article : articles) {
            saveArticle(article);
        }

        if (articles.isEmpty()) {
            System.out.println(
                    "No articles available. Check internet/API settings."
            );
            return;
        }

        currentArticles.clear();

        int number = 1;

        for (Article article : articles) {

            currentArticles.put(number, article);

            System.out.println("\n" + number + ". ["
                    + article.getImportance() + "] "
                    + article.getTitle());

            System.out.println(article.getDescription());

            System.out.println("Category: "
                    + article.getCategory()
                    + " | Source: "
                    + article.getSource());

            System.out.println("Published: " + article.getDate());

            System.out.println("Read more: " + article.getUrl());

            number++;
        }

        while (true) {

            System.out.print(
                    "\nEnter article number to bookmark (0 to return): "
            );

            if (!sc.hasNextLine()) {
                return;
            }

            try {

                int selected =
                        Integer.parseInt(sc.nextLine().trim());

                if (selected == 0) {
                    return;
                }

                if (currentArticles.containsKey(selected)) {

                    new BookmarkService().save(
                            userId,
                            currentArticles.get(selected)
                    );

                    return;

                } else {
                    System.out.println("Invalid article number.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    public void search(int userId, Scanner sc) {

        System.out.print("Search keyword: ");

        if (!sc.hasNextLine()) return;

        String keyword = sc.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        showNews("search:" + keyword, userId, sc);
    }

    public void dailyBriefing(int userId) {

        List<Article> articles =
                new NewsProcessor().process(fetch("top"));

        System.out.println(
                "\n========== DAILY BRIEFING =========="
        );

        if (articles.isEmpty()) {
            System.out.println("No current headlines available.");
            return;
        }

        Map<String, Integer> counts = new TreeMap<>();

        int number = 1;

        for (Article article : articles) {

            saveArticle(article);

            counts.merge(
                    article.getImportance(), 1, Integer::sum
            );

            System.out.println("\n" + number + ". ["
                    + article.getImportance() + "] "
                    + article.getTitle());

            System.out.println(article.getDescription());

            System.out.println("Source: " + article.getSource());

            number++;
        }

        System.out.println("\nBriefing summary:");

        counts.forEach((importance, count) ->
                System.out.println(importance + ": " + count));

        System.out.println("Total articles: " + articles.size());
    }

    public List<Article> fetchLiveArticles(String category) {

        List<Article> result = new ArrayList<>();

        if (apiKey == null || apiKey.isBlank()) {

            System.out.println("NEWS_API_KEY not configured.");

            return result;
        }

        try {

            String term;
            String sort = "publishedAt";

            switch (category) {

                case "top" -> term = "India";

                case "national" -> {
                    term = "India";
                    sort = "relevancy";
                }

                case "international" ->
                        term = "world OR international";

                case "politics" ->
                        term = "politics OR election OR government";

                case "sports" ->
                        term = "sports OR cricket OR football";

                case "awards" ->
                        term = "award OR achievement OR prize";

                case "finance" ->
                        term = "business OR finance OR economy";

                case "science" ->
                        term = "science OR technology";

                case "mixed" ->
                        term = "India OR world OR sports OR technology";

                default -> {

                    if (category.startsWith("search:")) {
                        term = category.substring(7);
                    } else {
                        term = category;
                    }
                }
            }

            String encodedTerm = URLEncoder.encode(
                    term, StandardCharsets.UTF_8
            );

            String query =
                    "https://newsapi.org/v2/everything?q="
                    + encodedTerm
                    + "&language=en"
                    + "&sortBy=" + sort
                    + "&pageSize=10";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(query))
                    .header("X-Api-Key", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JSONObject data = new JSONObject(response.body());

            if (!"ok".equals(data.optString("status"))) {

                System.out.println("News API: "
                        + data.optString(
                                "message", "Request failed"
                        ));

                return result;
            }

            JSONArray list = data.getJSONArray("articles");

            Set<String> uniqueTitles = new HashSet<>();

            for (int i = 0; i < list.length(); i++) {

                JSONObject item = list.getJSONObject(i);

                String title = item.optString("title", "").trim();

                if (title.isEmpty()
                        || title.equalsIgnoreCase("[Removed]")
                        || !uniqueTitles.add(
                                title.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                String description = item.optString(
                        "description",
                        "No description available."
                );

                JSONObject sourceObject =
                        item.optJSONObject("source");

                String source = sourceObject == null
                        ? "Unknown"
                        : sourceObject.optString("name", "Unknown");

                String url = item.optString("url", "");

                String date = item.optString(
                        "publishedAt", "Unknown"
                );

                Article article = new Article(
                        title,
                        description,
                        category,
                        source,
                        date,
                        url,
                        classify(title)
                );

                result.add(article);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println("News request interrupted.");

        } catch (Exception e) {

            System.out.println(
                    "Could not fetch live news: " + e.getMessage()
            );
        }

        return result;
    }

    private List<Article> fetch(String category) {

        List<Article> articles = fetchLiveArticles(category);

        if (articles.isEmpty()
                && !Thread.currentThread().isInterrupted()) {

            System.out.println(
                    "Live news unavailable. Showing demo articles."
            );

            return demoArticles(category);
        }

        return articles;
    }

    private String classify(String title) {

        String t = title.toLowerCase(Locale.ROOT);

        if (t.contains("breaking")
                || t.contains("crisis")
                || t.contains("attack")) {

            return "BREAKING";
        }

        if (t.contains("government")
                || t.contains("election")
                || t.contains("court")
                || t.contains("summit")) {

            return "HIGH PRIORITY";
        }

        return "GENERAL";
    }

    private void saveArticle(Article a) {

        String sql =
                "INSERT INTO articles"
                + "(title,description,category,source,"
                + "published_at,url,importance) "
                + "VALUES(?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE title=VALUES(title)";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql)) {

            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getCategory());
            ps.setString(4, a.getSource());
            ps.setString(5, a.getDate());
            ps.setString(6, a.getUrl());
            ps.setString(7, a.getImportance());

            ps.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Article cache not saved: " + e.getMessage()
            );
        }
    }

    private List<Article> demoArticles(String category) {

        String label = category.equals("top")
                ? "National" : category;

        return List.of(

            new Article(
                "Demo: Latest developments in " + label,
                "Sample article shown when live news is unavailable.",
                label,
                "NOWLINE Demo",
                "Today",
                "https://example.com/demo/"
                    + URLEncoder.encode(
                            label, StandardCharsets.UTF_8),
                "HIGH PRIORITY"
            ),

            new Article(
                "Demo: Updates and important stories",
                "Live news is currently unavailable.",
                label,
                "NOWLINE Demo",
                "Today",
                "https://example.org/demo/"
                    + URLEncoder.encode(
                            label, StandardCharsets.UTF_8),
                "GENERAL"
            )
        );
    }
}
