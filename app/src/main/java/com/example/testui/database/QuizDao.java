package com.example.testui.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY id DESC")
    List<QuizEntity> getAllQuizzes();

    @Query("SELECT * FROM quizzes WHERE questionId = :questionId LIMIT 1")
    QuizEntity getQuizByQuestionId(long questionId);

    @Insert
    long insertQuiz(QuizEntity quiz);

    @Update
    void updateQuiz(QuizEntity quiz);
}
