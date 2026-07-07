package com.example.testui.models;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private long timestamp;
    private AIResponse aiResponse;
    private boolean isThinking;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.isThinking = false;
    }

    public ChatMessage(AIResponse aiResponse, boolean isUser) {
        this.aiResponse = aiResponse;
        this.text = aiResponse != null ? aiResponse.getSimplifiedExplanation() : "";
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.isThinking = false;
    }

    public ChatMessage(boolean isThinking) {
        this.isUser = false;
        this.isThinking = isThinking;
        this.text = "";
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public long getTimestamp() { return timestamp; }

    public AIResponse getAiResponse() { return aiResponse; }
    public void setAiResponse(AIResponse aiResponse) { this.aiResponse = aiResponse; }

    public boolean isThinking() { return isThinking; }
    public void setThinking(boolean thinking) { isThinking = thinking; }
}
