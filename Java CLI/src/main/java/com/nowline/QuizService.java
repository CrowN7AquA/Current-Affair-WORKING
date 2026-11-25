
package com.nowline;

import java.sql.*;
import java.util.*;

public class QuizService {

    private static final int QUESTION_LIMIT = 5;

    private final NewsService newsService = new NewsService();

    private final Map<Integer, String> categories =
            new LinkedHashMap<>();

    public QuizService() {

        categories.put(1, "national");
        categories.put(2, "international");
        categories.put(3, "politics");
        categories.put(4, "sports");
        categories.put(5, "awards");
        categories.put(6, "finance");
        categories.put(7, "science");
        categories.put(8, "mixed");
    }

    private static class Question {

        private final String question;
        private final List<String> options;
        private final String correctAnswer;

        public Question(
                String question,
                List<String> options,
                String correctAnswer) {

            this.question = question;
            this.options = new ArrayList<>(options);
            this.correctAnswer = correctAnswer;

            Collections.shuffle(this.options);
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getOptions() {
            return options;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public boolean isCorrect(int index) {

            return index >= 0
                    && index < options.size()
                    && options.get(index).equals(correctAnswer);
        }
    }

    public void start(int userId, Scanner sc) {

        System.out.println(
                "\n========== CURRENT AFFAIRS QUIZ =========="
        );

        System.out.println("1. National");
        System.out.println("2. International");
        System.out.println("3. Politics");
        System.out.println("4. Sports");
        System.out.println("5. Awards & Achievements");
        System.out.println("6. Business & Finance");
        System.out.println("7. Science & Technology");
        System.out.println("8. Mixed Quiz");
        System.out.println("0. Return to Dashboard");

        System.out.print("Choose category: ");

        if (!sc.hasNextLine()) return;

        int choice;

        try {

            choice = Integer.parseInt(sc.nextLine().trim());

        } catch (NumberFormatException e) {

            System.out.println("Invalid category.");
            return;
        }

        if (choice == 0) return;

        String category = categories.get(choice);

        if (category == null) {

            System.out.println("Invalid category.");
            return;
        }

        System.out.println("\nFetching latest news...");

        List<Article> articles =
                newsService.fetchLiveArticles(category);

        if (articles.size() < 4) {

            System.out.println(
                    "Not enough live articles to generate a quiz."
            );

            return;
        }

        List<Question> quiz = generateQuestions(articles);

        if (quiz.isEmpty()) {

            System.out.println(
                    "Unable to generate questions from available news."
            );

            return;
        }

        System.out.println("Quiz generated successfully!");
        System.out.println("Category: " + category);
        System.out.println("Total questions: " + quiz.size());
        System.out.println("Enter A, B, C or D.");

        int score = 0;
        int questionNumber = 1;

        for (Question q : quiz) {

            System.out.println(
                    "\nQuestion " + questionNumber
                    + "/" + quiz.size()
            );

            System.out.println(q.getQuestion());

            List<String> options = q.getOptions();

            for (int i = 0; i < options.size(); i++) {

                char letter = (char) ('A' + i);

                System.out.println(
                        letter + ". " + options.get(i)
                );
            }

            String answer;

            while (true) {

                System.out.print("Your answer: ");

                if (!sc.hasNextLine()) {

                    System.out.println("Quiz cancelled.");
                    return;
                }

                answer = sc.nextLine()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                if (answer.matches("[ABCD]")) {
                    break;
                }

                System.out.println(
                        "Invalid answer. Enter A, B, C or D."
                );
            }

            int answerIndex = answer.charAt(0) - 'A';

            if (q.isCorrect(answerIndex)) {

                score++;

                System.out.println("Correct!");

            } else {

                System.out.println("Incorrect.");

                System.out.println(
                        "Correct answer: " + q.getCorrectAnswer()
                );
            }

            questionNumber++;
        }

        double percentage =
                (score * 100.0) / quiz.size();

        System.out.println(
                "\n========== QUIZ RESULT =========="
        );

        System.out.println("Category: " + category);

        System.out.println(
                "Your score: " + score + "/" + quiz.size()
        );

        System.out.printf(
                "Percentage: %.2f%%%n", percentage
        );

        if (percentage >= 80) {

            System.out.println("Excellent performance!");

        } else if (percentage >= 50) {

            System.out.println("Good attempt!");

        } else {

            System.out.println(
                    "Keep reading the latest news and try again."
            );
        }

        saveAttempt(
                userId,
                category,
                score,
                quiz.size()
        );
    }

    private List<Question> generateQuestions(
            List<Article> articles) {

        List<Question> generated = new ArrayList<>();

        List<Article> shuffledArticles =
                new ArrayList<>(articles);

        Collections.shuffle(shuffledArticles);

        Set<String> sources = new LinkedHashSet<>();

        for (Article article : articles) {

            String source = article.getSource();

            if (source != null
                    && !source.isBlank()
                    && !source.equalsIgnoreCase("Unknown")) {

                sources.add(source);
            }
        }

        for (Article article : shuffledArticles) {

            if (generated.size() >= QUESTION_LIMIT) {
                break;
            }

            Question question = null;

            if (generated.size() % 2 == 0) {

                question = createHeadlineQuestion(
                        article, articles
                );

                if (question == null) {

                    question = createSourceQuestion(
                            article, sources
                    );
                }

            } else {

                question = createSourceQuestion(
                        article, sources
                );

                if (question == null) {

                    question = createHeadlineQuestion(
                            article, articles
                    );
                }
            }

            if (question != null) {
                generated.add(question);
            }
        }

        Collections.shuffle(generated);

        return generated;
    }

    private Question createSourceQuestion(
            Article article,
            Set<String> sources) {

        String correctAnswer = article.getSource();

        if (sources.size() < 4
                || !sources.contains(correctAnswer)) {

            return null;
        }

        List<String> alternatives =
                new ArrayList<>(sources);

        alternatives.remove(correctAnswer);

        Collections.shuffle(alternatives);

        List<String> options = new ArrayList<>();

        options.add(correctAnswer);

        for (int i = 0; i < 3; i++) {

            options.add(alternatives.get(i));
        }

        String question =
                "Which news source published this headline?\n"
                + "\"" + article.getTitle() + "\"";

        return new Question(
                question,
                options,
                correctAnswer
        );
    }

    private Question createHeadlineQuestion(
            Article article,
            List<Article> articles) {

        String description = article.getDescription();

        if (description == null
                || description.isBlank()
                || description.length() < 30
                || description.equalsIgnoreCase(
                        "No description available.")) {

            return null;
        }

        String correctAnswer = article.getTitle();

        Set<String> uniqueTitles = new LinkedHashSet<>();

        for (Article other : articles) {

            String title = other.getTitle();

            if (title != null
                    && !title.isBlank()
                    && !title.equalsIgnoreCase(correctAnswer)) {

                uniqueTitles.add(title);
            }
        }

        if (uniqueTitles.size() < 3) {
            return null;
        }

        List<String> alternatives =
                new ArrayList<>(uniqueTitles);

        Collections.shuffle(alternatives);

        List<String> options = new ArrayList<>();

        options.add(correctAnswer);

        for (int i = 0; i < 3; i++) {

            options.add(alternatives.get(i));
        }

        String question =
                "Which headline matches this news description?\n"
                + "\"" + description + "\"";

        return new Question(
                question,
                options,
                correctAnswer
        );
    }

    private void saveAttempt(
            int userId,
            String category,
            int score,
            int total) {

        String sql =
                "INSERT INTO quiz_attempts"
                + "(user_id,category,score,total_questions) "
                + "VALUES(?,?,?,?)";

        try (PreparedStatement ps =
                DB.getConnection().prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setInt(3, score);
            ps.setInt(4, total);

            ps.executeUpdate();

            System.out.println(
                    "Quiz result saved successfully."
            );

        } catch (SQLException e) {

            System.out.println(
                    "Quiz result could not be saved: "
                    + e.getMessage()
            );
        }
    }
}
