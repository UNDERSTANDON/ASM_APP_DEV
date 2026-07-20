package com.example.testui.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quizzes")
public class QuizEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long questionId; // Liên kết với id của QuestionEntity
    private String quizQuestionsJson; // Lưu list câu hỏi dạng JSON String
    private int score;
    private String date;
    private int status; // 1 = Đã làm, 0 = Chưa làm

    public QuizEntity(long questionId, String quizQuestionsJson, int score, String date, int status) {
        this.questionId = questionId;
        this.quizQuestionsJson = quizQuestionsJson;
        this.score = score;
        this.date = date;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public String getQuizQuestionsJson() { return quizQuestionsJson; }
    public void setQuizQuestionsJson(String quizQuestionsJson) { this.quizQuestionsJson = quizQuestionsJson; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
