package com.example.testui.ai;

import android.os.Handler;
import android.os.Looper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.QuizQuestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AIObj {

    private Map<String, Object> config;
    private static AIObj instance;

    public interface AICallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private AIObj() {}

    public static synchronized AIObj getInstance() {
        if (instance == null) {
            instance = new AIObj();
        }
        return instance;
    }

    /**
     * Flexible initialization for the AI Specialist to use.
     * @param config Map containing education level, subjects, API keys, system prompts, etc.
     */
    public void initialize(Map<String, Object> config) {
        this.config = config;
        // AI Specialist will add initialization logic here (e.g., setting up Gemini/OpenAI client)
    }

    /**
     * Main method to ask a question.
     */
    public void askQuestion(String question, AICallback<AIResponse> callback) {
        // Mocking an asynchronous AI call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AIResponse mockResponse = generateMockAnswer(question);
            callback.onSuccess(mockResponse);
        }, 1500); // 1.5s delay for realism
    }

    /**
     * Method to generate a quiz based on a specific question/answer.
     */
    public void generateQuiz(int questionId, AICallback<List<QuizQuestion>> callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<QuizQuestion> mockQuiz = new ArrayList<>();
            mockQuiz.add(new QuizQuestion(
                "What is the discriminant of the equation 2x² - 5x + 3 = 0?",
                Arrays.asList("1", "4", "25", "49"),
                0,
                "Correct! b² - 4ac = (-5)² - 4(2)(3) = 25 - 24 = 1."
            ));
            callback.onSuccess(mockQuiz);
        }, 1000);
    }

    private AIResponse generateMockAnswer(String question) {
        AIResponse response = new AIResponse(
            "Mathematics",
            "1. Identify coefficients: a=2, b=-5, c=3.\n2. Calculate Δ = b² - 4ac = 25 - 24 = 1.\n3. Use quadratic formula: x = (-b ± √Δ) / 2a.",
            "x₁ = 1.5, x₂ = 1",
            "To solve 2x² - 5x + 3 = 0, we use the quadratic formula. The roots are 1.5 and 1."
        );
        response.setFormulas(Arrays.asList("x = (-b ± √Δ) / 2a", "Δ = b² - 4ac"));
        response.setCommonMistakes(Arrays.asList("Forgetting to divide by 2a", "Sign error in -b"));
        return response;
    }
}
