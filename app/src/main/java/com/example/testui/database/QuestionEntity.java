package com.example.testui.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "questions")
public class QuestionEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String email;
    private int subjectId;
    private String date;
    private String questionText;
    private String sessionId; // Dùng để map liên kết câu trắc nghiệm

    public QuestionEntity(String email, int subjectId, String date, String questionText, String sessionId) {
        this.email = email;
        this.subjectId = subjectId;
        this.date = date;
        this.questionText = questionText;
        this.sessionId = sessionId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
