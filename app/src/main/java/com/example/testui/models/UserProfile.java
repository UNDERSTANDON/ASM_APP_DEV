package com.example.testui.models;

public class UserProfile {
    private String email;
    private String educationLevel; // "Tiểu học", "THCS", "THPT", "Đại học"
    private String aiCustomization;
    private String aiTone;
    private int xpPoints;

    public UserProfile(String email, String educationLevel) {
        this.email = email;
        this.educationLevel = educationLevel;
        this.aiTone = "Friendly"; // Default
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getAiCustomization() { return aiCustomization; }
    public void setAiCustomization(String aiCustomization) { this.aiCustomization = aiCustomization; }

    public String getAiTone() { return aiTone; }
    public void setAiTone(String aiTone) { this.aiTone = aiTone; }

    public int getXpPoints() { return xpPoints; }
    public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }
}
