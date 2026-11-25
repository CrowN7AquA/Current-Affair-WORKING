package com.nowline;

import java.util.*;
import java.util.concurrent.*;

public class NewsProcessor {
    public List<Article> process(List<Article> articles) {
        NewsQueue queue = new NewsQueue();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        Runnable producer = () -> {
            try {
                for (Article article : articles) queue.add(article);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Callable<List<Article>> consumer = () -> {
            List<Article> result = new ArrayList<>();
            Set<String> urls = new HashSet<>();
            for (int i = 0; i < articles.size(); i++) {
                Article article = queue.take();
                if (urls.add(article.getUrl())) result.add(article);
            }
            result.sort(Comparator.comparing(Article::getTitle));
            return result;
        };
        try {
            Future<?> producerTask = workers.submit(producer);
            Future<List<Article>> consumerTask = workers.submit(consumer);
            producerTask.get(15, TimeUnit.SECONDS);
            return consumerTask.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return articles;
        } catch (ExecutionException | TimeoutException e) {
            System.out.println("News processing failed: " + e.getMessage());
            return articles;
        } finally {
            workers.shutdownNow();
            try { workers.awaitTermination(2, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public void processDemo() {
        Thread demonstration = new Thread(() -> {
            List<Article> sample = List.of(new Article("Sample headline", "Queue demo", "national",
                    "NOWLINE Demo", "Sample", "https://example.com/queue", "GENERAL"));
            Iterator<Article> iterator = process(sample).iterator();
            while (iterator.hasNext()) System.out.println("Processed: " + iterator.next().getTitle());
        });
        demonstration.start();
        try { demonstration.join(); }
        catch (InterruptedException e) { demonstration.interrupt(); Thread.currentThread().interrupt(); }
    }
}
