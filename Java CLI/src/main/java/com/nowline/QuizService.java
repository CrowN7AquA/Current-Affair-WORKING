package com.nowline;

import java.sql.*;
import java.util.*;

public class QuizService {
    private final Map<String, String[][]> questions = new HashMap<>();

    public QuizService() {
        questions.put("national", new String[][]{
            {"Which is the capital of India?", "Mumbai", "New Delhi", "Kolkata", "B", "New Delhi"},
            {"Which body conducts India's general elections?", "RBI", "UPSC", "Election Commission", "C", "Election Commission"}
        });
        questions.put("international", new String[][]{
            {"Which organisation is known as the United Nations?", "UN", "WHO", "WTO", "A", "UN"},
            {"Where is the UN headquarters?", "London", "New York", "Paris", "B", "New York"}
        });
        questions.put("politics", new String[][]{
            {"What is the minimum voting age in India?", "16", "18", "21", "B", "18"}
        });
        questions.put("sports", new String[][]{
            {"How many players are on a cricket team on the field?", "9", "10", "11", "C", "11"}
        });
        questions.put("finance", new String[][]{
            {"Which institution is India's central bank?", "SBI", "RBI", "SEBI", "B", "RBI"}
        });
        questions.put("science", new String[][]{
            {"Which planet is known as the Red Planet?", "Venus", "Mars", "Jupiter", "B", "Mars"}
        });
    }

    public void start(int userId, Scanner sc) {
        System.out.println("\nQuiz categories: national, international, politics, sports, finance, science, mixed");
        System.out.print("Choose category: ");
        String category = sc.nextLine().trim().toLowerCase();
        List<String[]> quiz = new ArrayList<>();
        if (category.equals("mixed")) questions.values().forEach(list -> quiz.addAll(Arrays.asList(list)));
        else if (questions.containsKey(category)) quiz.addAll(Arrays.asList(questions.get(category)));
        else { System.out.println("Unknown category."); return; }

        int score = 0;
        for (String[] q : quiz) {
            System.out.println("\n" + q[0]);
            System.out.println("A. " + q[1] + "   B. " + q[2] + "   C. " + q[3]);
            System.out.print("Answer: ");
            if (sc.nextLine().trim().equalsIgnoreCase(q[4])) {
                score++;
                System.out.println("Correct!");
            } else System.out.println("Correct answer: " + q[5]);
        }
        System.out.println("Your score: " + score + "/" + quiz.size());
        saveAttempt(userId, category, score, quiz.size());
    }

    private void saveAttempt(int userId, String category, int score, int total) {
        String sql = "INSERT INTO quiz_attempts(user_id,category,score,total_questions) VALUES(?,?,?,?)";
        try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, category);
            ps.setInt(3, score); ps.setInt(4, total); ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Quiz result could not be saved: " + e.getMessage());
        }
    }
}
