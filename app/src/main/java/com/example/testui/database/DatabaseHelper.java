package com.example.testui.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.UserProfile;

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
}
