package com.nowline;

import java.util.ArrayDeque;
import java.util.Queue;

public class NewsQueue {
    private final Queue<Article> queue = new ArrayDeque<>();
    private final int capacity = 30;

    public synchronized void add(Article article) throws InterruptedException {
        while (queue.size() == capacity) wait();
        queue.add(article);
        notifyAll();
    }

    public synchronized Article take() throws InterruptedException {
        while (queue.isEmpty()) wait();
        Article article = queue.remove();
        notifyAll();
        return article;
    }

    public synchronized int size() { return queue.size(); }
}
