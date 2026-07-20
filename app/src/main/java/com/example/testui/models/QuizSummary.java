package com.example.testui.models;

import java.util.concurrent.TimeUnit;

/**
 * Model đại diện cho một quiz entry trên màn hình Quiz Lab.
 * Chứa thông tin tóm tắt: subject, tiêu đề, số câu hỏi, ngày tạo.
 */
public class QuizSummary {

    private long quizId;
    private long questionId;
    private int subjectId;
    private String title;
    private int questionCount;
    private long createdAt; // UNIX timestamp ms (0 = dữ liệu cũ)
    private boolean isCompleted;
    private int score; // -1 nếu chưa làm bài

    public QuizSummary(long quizId, long questionId, int subjectId,
                       String title, int questionCount, long createdAt,
                       boolean isCompleted, int score) {
        this.quizId        = quizId;
        this.questionId    = questionId;
        this.subjectId     = subjectId;
        this.title         = title;
        this.questionCount = questionCount;
        this.createdAt     = createdAt;
        this.isCompleted   = isCompleted;
        this.score         = score;
    }

    // --- Getters ---
    public long getQuizId()        { return quizId; }
    public long getQuestionId()    { return questionId; }
    public int  getSubjectId()     { return subjectId; }
    public String getTitle()       { return title; }
    public int  getQuestionCount() { return questionCount; }
    public long getCreatedAt()     { return createdAt; }
    public boolean isCompleted()   { return isCompleted; }
    public int getScore()          { return score; }

    /** Trả về tên môn học theo subjectId. */
    public String getSubjectName() {
        switch (subjectId) {
            case 1:  return "Toán học";
            case 2:  return "Vật lý";
            case 3:  return "Hóa học";
            case 4:  return "Lập trình";
            case 5:  return "Lịch sử";
            default: return "Chung";
        }
    }

    /**
     * Trả về ngày tạo tương đối: "Hôm nay", "Hôm qua", "N Ngày Trước", "Trước đây".
     * Dữ liệu cũ (createdAt == 0) hiển thị "Trước đây".
     */
    public String getRelativeDate() {
        if (createdAt == 0) return "Trước đây";
        long now      = System.currentTimeMillis();
        long diffMs   = now - createdAt;
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);

        if (diffDays == 0)  return "Hôm nay";
        if (diffDays == 1)  return "Hôm qua";
        if (diffDays < 30)  return diffDays + " Ngày Trước";
        return "Trước đây";
    }
}
