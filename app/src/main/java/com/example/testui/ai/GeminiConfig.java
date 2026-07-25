package com.example.testui.ai;

public class GeminiConfig {
    public static final String API_KEY = "";

    // Using gemini-3.5-flash-lite because it's the only model that we can call reliably during testing
    public static final String MODEL_NAME = "gemini-3.5-flash-lite";

    // Gemini API endpoint base URL
    public static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
}
