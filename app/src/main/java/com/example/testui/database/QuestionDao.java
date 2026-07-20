package com.example.testui.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id DESC")
    List<QuestionEntity> getAllQuestions();

    @Query("SELECT * FROM questions WHERE email = :email ORDER BY id DESC")
    List<QuestionEntity> getQuestionsByEmail(String email);

    @Insert
    long insertQuestion(QuestionEntity question);

    @Update
    void updateQuestion(QuestionEntity question);

    @Query("DELETE FROM questions WHERE email = :email")
    void deleteQuestionsByEmail(String email);
}
