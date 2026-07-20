package com.example.testui.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserEntity.class, QuestionEntity.class, QuizEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract QuestionDao questionDao();
    public abstract QuizDao quizDao();

    public static Context appContext;

    public static AppDatabase getDatabase(final Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "eduai_database")
                            .allowMainThreadQueries() // Cho phép chạy trên Main Thread để tương thích ngược 100% với giao diện cũ
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
