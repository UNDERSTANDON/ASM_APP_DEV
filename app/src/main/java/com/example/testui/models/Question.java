package com.example.testui.models;

public class Question {
    private int id;
    private int userId;
    private int subjectId;
    private String contentText;

    public Question(int id, int userId, int subjectId, String contentText) {
        this.id = id;
        this.userId = userId;
        this.subjectId = subjectId;
        this.contentText = contentText;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getSubjectId() { return subjectId; }
    public String getContentText() { return contentText; }

    public String getSubjectName() {
        switch (subjectId) {
            case 1: return "Toán học";
            case 2: return "Vật lý";
            case 3: return "Hóa học";
            case 4: return "Lập trình";
            case 5: return "Lịch sử";
            default: return "Chung";
        }
    }
}
