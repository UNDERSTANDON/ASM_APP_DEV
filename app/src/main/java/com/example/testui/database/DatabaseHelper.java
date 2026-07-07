package com.example.testui.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.UserProfile;
import com.example.testui.models.ChatMessage;
import com.example.testui.models.Question;
import java.util.List;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BrightPathLearning.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_USER = "USER";
    public static final String TABLE_SUBJECT = "SUBJECT";
    public static final String TABLE_QUESTION = "QUESTION";
    public static final String TABLE_AI_RESPONSE = "AI_RESPONSE";

    // Common column names
    public static final String KEY_ID = "id";

    // USER Table - column names
    public static final String KEY_USER_EMAIL = "email";
    public static final String KEY_USER_PASSWORD_HASH = "password_hash";
    public static final String KEY_USER_EDUCATION_LEVEL = "education_level";

    // QUESTION Table - column names
    public static final String KEY_QUESTION_USER_ID = "user_id";
    public static final String KEY_QUESTION_CONTENT = "content_text";
    public static final String KEY_QUESTION_SUBJECT_ID = "subject_id";

    // AI_RESPONSE Table - column names
    public static final String KEY_RESPONSE_QUESTION_ID = "question_id";
    public static final String KEY_RESPONSE_LOGICAL_STEPS = "logical_steps";
    public static final String KEY_RESPONSE_FINAL_ANSWER = "final_answer";
    public static final String KEY_RESPONSE_SIMPLIFIED_EXPLANATION = "simplified_explanation";

    // Table Create Statements
    private static final String CREATE_TABLE_USER = "CREATE TABLE " + TABLE_USER + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_USER_EMAIL + " TEXT,"
            + KEY_USER_PASSWORD_HASH + " TEXT," + KEY_USER_EDUCATION_LEVEL + " TEXT" + ")";

    private static final String CREATE_TABLE_QUESTION = "CREATE TABLE " + TABLE_QUESTION + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_QUESTION_USER_ID + " INTEGER,"
            + KEY_QUESTION_SUBJECT_ID + " INTEGER," + KEY_QUESTION_CONTENT + " TEXT" + ")";

    private static final String CREATE_TABLE_AI_RESPONSE = "CREATE TABLE " + TABLE_AI_RESPONSE + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_RESPONSE_QUESTION_ID + " INTEGER,"
            + KEY_RESPONSE_LOGICAL_STEPS + " TEXT," + KEY_RESPONSE_FINAL_ANSWER + " TEXT,"
            + KEY_RESPONSE_SIMPLIFIED_EXPLANATION + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_QUESTION);
        db.execSQL(CREATE_TABLE_AI_RESPONSE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AI_RESPONSE);
        onCreate(db);
    }

    // --- Helper Methods ---

    public long saveQuestion(String content, int userId, int subjectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_QUESTION_CONTENT, content);
        values.put(KEY_QUESTION_USER_ID, userId);
        values.put(KEY_QUESTION_SUBJECT_ID, subjectId);
        return db.insert(TABLE_QUESTION, null, values);
    }

    public void saveAIResponse(long questionId, AIResponse response) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_RESPONSE_QUESTION_ID, questionId);
        values.put(KEY_RESPONSE_LOGICAL_STEPS, response.getLogicalSteps());
        values.put(KEY_RESPONSE_FINAL_ANSWER, response.getFinalAnswer());
        values.put(KEY_RESPONSE_SIMPLIFIED_EXPLANATION, response.getSimplifiedExplanation());
        db.insert(TABLE_AI_RESPONSE, null, values);
    }

    public AIResponse getCachedResponse(long questionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_AI_RESPONSE + " WHERE " + KEY_RESPONSE_QUESTION_ID + " = " + questionId;
        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null && c.moveToFirst()) {
            AIResponse response = new AIResponse(
                    "Unknown", // Subject isn't stored in this table directly in this simple version
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_LOGICAL_STEPS)),
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_FINAL_ANSWER)),
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_SIMPLIFIED_EXPLANATION))
            );
            c.close();
            return response;
        }
        if (c != null) c.close();
        return null;
    }

    // --- Auth & Profile Methods ---

    public long registerUser(String email, String passwordHash, String educationLevel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_EMAIL, email);
        values.put(KEY_USER_PASSWORD_HASH, passwordHash);
        values.put(KEY_USER_EDUCATION_LEVEL, educationLevel);
        return db.insert(TABLE_USER, null, values);
    }

    public boolean authenticateUser(String email, String passwordHash) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = KEY_USER_EMAIL + " = ? AND " + KEY_USER_PASSWORD_HASH + " = ?";
        String[] selectionArgs = {email, passwordHash};
        Cursor cursor = db.query(TABLE_USER, null, selection, selectionArgs, null, null, null);
        boolean authenticated = cursor.getCount() > 0;
        cursor.close();
        return authenticated;
    }

    public UserProfile getUserProfile(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = KEY_USER_EMAIL + " = ?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_USER, null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            UserProfile profile = new UserProfile(
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EMAIL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_EDUCATION_LEVEL))
            );
            cursor.close();
            return profile;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public int updateUserProfile(UserProfile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_EDUCATION_LEVEL, profile.getEducationLevel());
        // Add more fields as UserProfile expands (e.g., aiTone, xpPoints)
        return db.update(TABLE_USER, values, KEY_USER_EMAIL + " = ?", new String[]{profile.getEmail()});
    }

    public List<Integer> getUniqueSessionIds(int userId) {
        List<Integer> sessionIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT DISTINCT " + KEY_QUESTION_SUBJECT_ID + " FROM " + TABLE_QUESTION 
                + " WHERE " + KEY_QUESTION_USER_ID + " = ? ORDER BY " + KEY_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sessionIds.add(cursor.getInt(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sessionIds;
    }

    public List<ChatMessage> getChatHistoryForSession(int userId, int sessionId) {
        List<ChatMessage> chatHistory = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT q." + KEY_ID + ", q." + KEY_QUESTION_CONTENT + ", "
                + "r." + KEY_RESPONSE_LOGICAL_STEPS + ", r." + KEY_RESPONSE_FINAL_ANSWER + ", r." + KEY_RESPONSE_SIMPLIFIED_EXPLANATION
                + " FROM " + TABLE_QUESTION + " q"
                + " LEFT JOIN " + TABLE_AI_RESPONSE + " r ON q." + KEY_ID + " = r." + KEY_RESPONSE_QUESTION_ID
                + " WHERE q." + KEY_QUESTION_USER_ID + " = ? AND q." + KEY_QUESTION_SUBJECT_ID + " = ?"
                + " ORDER BY q." + KEY_ID + " ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(sessionId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String questionText = cursor.getString(1);
                String logicalSteps = cursor.getString(2);
                String finalAnswer = cursor.getString(3);
                String simplifiedExplanation = cursor.getString(4);
                
                chatHistory.add(new ChatMessage(questionText, true));
                
                if (simplifiedExplanation != null || finalAnswer != null || logicalSteps != null) {
                    AIResponse response = new AIResponse("Unknown", logicalSteps, finalAnswer, simplifiedExplanation);
                    chatHistory.add(new ChatMessage(response, false));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return chatHistory;
    }

    public String getSessionTitle(int userId, int sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + KEY_QUESTION_CONTENT + " FROM " + TABLE_QUESTION 
                + " WHERE " + KEY_QUESTION_USER_ID + " = ? AND " + KEY_QUESTION_SUBJECT_ID + " = ? ORDER BY " + KEY_ID + " ASC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(sessionId)});
        String title = "Đoạn chat mới";
        if (cursor != null && cursor.moveToFirst()) {
            String firstQuestion = cursor.getString(0);
            if (firstQuestion != null && !firstQuestion.isEmpty()) {
                if (firstQuestion.length() > 30) {
                    title = firstQuestion.substring(0, 27) + "...";
                } else {
                    title = firstQuestion;
                }
            }
            cursor.close();
        }
        return title;
    }

    public int getSessionMessageCount(int userId, int sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_QUESTION + " WHERE " + KEY_QUESTION_USER_ID + " = ? AND " + KEY_QUESTION_SUBJECT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(sessionId)});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public void deleteChatSession(int userId, int sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT " + KEY_ID + " FROM " + TABLE_QUESTION + " WHERE " + KEY_QUESTION_USER_ID + " = ? AND " + KEY_QUESTION_SUBJECT_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(userId), String.valueOf(sessionId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long qId = cursor.getLong(0);
                db.delete(TABLE_AI_RESPONSE, KEY_RESPONSE_QUESTION_ID + " = ?", new String[]{String.valueOf(qId)});
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.delete(TABLE_QUESTION, KEY_QUESTION_USER_ID + " = ? AND " + KEY_QUESTION_SUBJECT_ID + " = ?", new String[]{String.valueOf(userId), String.valueOf(sessionId)});
    }

    public void clearAllChatHistory(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT " + KEY_ID + " FROM " + TABLE_QUESTION + " WHERE " + KEY_QUESTION_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long qId = cursor.getLong(0);
                db.delete(TABLE_AI_RESPONSE, KEY_RESPONSE_QUESTION_ID + " = ?", new String[]{String.valueOf(qId)});
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.delete(TABLE_QUESTION, KEY_QUESTION_USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public AIResponse getCachedResponseForQuestion(String questionContent) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT r." + KEY_RESPONSE_LOGICAL_STEPS + ", r." + KEY_RESPONSE_FINAL_ANSWER + ", r." + KEY_RESPONSE_SIMPLIFIED_EXPLANATION
                + " FROM " + TABLE_AI_RESPONSE + " r"
                + " JOIN " + TABLE_QUESTION + " q ON r." + KEY_RESPONSE_QUESTION_ID + " = q." + KEY_ID
                + " WHERE q." + KEY_QUESTION_CONTENT + " = ?";
        Cursor c = db.rawQuery(query, new String[]{questionContent});
        if (c != null && c.moveToFirst()) {
            AIResponse response = new AIResponse(
                    "Unknown",
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_LOGICAL_STEPS)),
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_FINAL_ANSWER)),
                    c.getString(c.getColumnIndexOrThrow(KEY_RESPONSE_SIMPLIFIED_EXPLANATION))
            );
            c.close();
            return response;
        }
        if (c != null) c.close();
        return null;
    }

    public List<Question> getSavedQuestions(int userId) {
        List<Question> savedQuestions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_QUESTION + " WHERE " + KEY_QUESTION_USER_ID + " = ? ORDER BY " + KEY_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUESTION_USER_ID));
                int sId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_QUESTION_SUBJECT_ID));
                String contentText = cursor.getString(cursor.getColumnIndexOrThrow(KEY_QUESTION_CONTENT));
                savedQuestions.add(new Question(id, uId, sId, contentText));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return savedQuestions;
    }
}
