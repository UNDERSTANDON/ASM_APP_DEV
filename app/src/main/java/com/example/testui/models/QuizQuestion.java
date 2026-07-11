package com.example.testui.models;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private String feedback;

    public QuizQuestion(String questionText, List<String> options, int correctOptionIndex, String feedback) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.feedback = feedback;
    }

    // Getters
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public String getFeedback() { return feedback; }
}
