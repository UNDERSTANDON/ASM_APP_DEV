package com.example.testui.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String email;
    private String password;
    private String educationLevel;
    private String favoriteSubject;
    private String aiTone;
    private int xpPoints;

    public UserEntity(@NonNull String email, String password, String educationLevel, String favoriteSubject, String aiTone, int xpPoints) {
        this.email = email;
        this.password = password;
        this.educationLevel = educationLevel;
        this.favoriteSubject = favoriteSubject;
        this.aiTone = aiTone;
        this.xpPoints = xpPoints;
    }

    @NonNull
    public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getFavoriteSubject() { return favoriteSubject; }
    public void setFavoriteSubject(String favoriteSubject) { this.favoriteSubject = favoriteSubject; }

    public String getAiTone() { return aiTone; }
    public void setAiTone(String aiTone) { this.aiTone = aiTone; }

    public int getXpPoints() { return xpPoints; }
    public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }
}
