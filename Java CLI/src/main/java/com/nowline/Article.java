package com.nowline;

public class Article {
    private final String title, description, category, source, date, url, importance;

    public Article(String title, String description, String category,
                   String source, String date, String url, String importance) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.source = source;
        this.date = date;
        this.url = url;
        this.importance = importance;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSource() { return source; }
    public String getDate() { return date; }
    public String getUrl() { return url; }
    public String getImportance() { return importance; }
}
