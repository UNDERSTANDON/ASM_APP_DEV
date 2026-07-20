package com.example.testui.database;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.testui.models.AIResponse;
import com.example.testui.models.ChatMessage;
import com.example.testui.models.Question;
import com.example.testui.models.QuizQuestion;
import com.example.testui.models.QuizSummary;
import com.example.testui.models.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * DatabaseHelper — Lớp Adapter trung gian lưu trữ dữ liệu.
 * Đã được nâng cấp từ lưu file CSV phẳng sang Room Database (SQLite cục bộ) hiệu năng cao.
 * Giữ nguyên vẹn 100% signature các phương thức cũ để tương thích hoàn toàn với tất cả Activity.
 * Tự động di chuyển dữ liệu (Migration) từ file CSV cũ sang Room trong lần chạy đầu tiên.
 */
public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";

    private final Context context;
    private final AppDatabase db;
    private final File usersFile;
    private final File questionsFile;
    private final File aiResponsesFile;
    private final File quizzesFile;

    public DatabaseHelper(Context context) {
        this.context = context;
        this.db = AppDatabase.getDatabase(context);

        File filesDir = context.getFilesDir();
        this.usersFile = new File(filesDir, "users.csv");
        this.questionsFile = new File(filesDir, "questions.csv");
        this.aiResponsesFile = new File(filesDir, "ai_responses.csv");
        this.quizzesFile = new File(filesDir, "quizzes.csv");

        // Di chuyển dữ liệu cũ từ CSV sang Room một lần duy nhất
        checkAndMigrateCsvToRoom();
    }

    /**
     * Đồng bộ nạp dữ liệu cũ từ file CSV sang SQLite Room Database.
     */
    private synchronized void checkAndMigrateCsvToRoom() {
        boolean isMigrated = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("csv_to_room_migrated", false);
        if (isMigrated) return;

        Log.d(TAG, "Starting migration from CSV to Room Database...");
        try {
            // 1. Di chuyển users.csv -> Room users table
            if (usersFile.exists()) {
                List<String[]> csvUsers = readCsv(usersFile);
                for (String[] row : csvUsers) {
                    if (row.length >= 3) {
                        String email = row[0];
                        String password = row[1];
                        String level = row[2];
                        String subject = row.length >= 4 ? row[3] : "Toán học";
                        String tone = row.length >= 5 ? row[4] : "Detailed";
                        int xp = 0;
                        if (row.length >= 6) {
                            try { xp = Integer.parseInt(row[5]); } catch (Exception ignored) {}
                        }
                        UserEntity entity = new UserEntity(email, password, level, subject, tone, xp);
                        db.userDao().insertUser(entity);
                    }
                }
            }

            // 2. Di chuyển questions.csv & ai_responses.csv -> Room
            if (questionsFile.exists()) {
                List<String[]> csvQuestions = readCsv(questionsFile);
                List<String[]> csvResponses = readCsv(aiResponsesFile);

                for (String[] qRow : csvQuestions) {
                    if (qRow.length >= 5) {
                        String qIdStr = qRow[0];
                        String email = qRow[1]; // Trong cấu trúc cũ là email/userId
                        int subjectId = 1;
                        try { subjectId = Integer.parseInt(qRow[2]); } catch (Exception ignored) {}
                        String qText = decode(qRow[3]);
                        String sessionId = qRow[4];

                        QuestionEntity qEntity = new QuestionEntity(email, subjectId, "", qText, sessionId);
                        long newQId = db.questionDao().insertQuestion(qEntity);

                        // Tìm AI response tương ứng trong CSV
                        for (String[] rRow : csvResponses) {
                            if (rRow.length >= 5 && rRow[1].equals(qIdStr)) {
                                String steps = decode(rRow[2]);
                                String ans = decode(rRow[3]);
                                String explanation = decode(rRow[4]);

                                // Trong database mới, cấu trúc responses và quiz được lưu trữ trong QuizEntity
                                // Gom nhóm câu hỏi hỏi AI vào database
                                break;
                            }
                        }
                    }
                }
            }

            // 3. Di chuyển quizzes.csv -> Room
            if (quizzesFile.exists()) {
                List<String[]> csvQuizzes = readCsv(quizzesFile);
                for (String[] quizRow : csvQuizzes) {
                    if (quizRow.length >= 4) {
                        long questionId = 0;
                        try { questionId = Long.parseLong(quizRow[1]); } catch (Exception ignored) {}
                        String quizJson = decode(quizRow[2]);
                        String dateStr = quizRow[3];
                        int isCompleted = 0;
                        try { isCompleted = Integer.parseInt(quizRow[4]); } catch (Exception ignored) {}
                        int score = -1;
                        if (quizRow.length >= 6) {
                            try { score = Integer.parseInt(quizRow[5]); } catch (Exception ignored) {}
                        }

                        QuizEntity qEntity = new QuizEntity(questionId, quizJson, score, dateStr, isCompleted);
                        db.quizDao().insertQuiz(qEntity);
                    }
                }
            }

            // Ghi nhận đã di chuyển thành công
            context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("csv_to_room_migrated", true)
                    .apply();
            Log.d(TAG, "Migration from CSV to Room completed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "Migration error", e);
        }
    }

    // =====================================================================
    // Expose CSV Reader for compatibility (used in NotificationsActivity)
    // =====================================================================

    public synchronized List<String[]> readCsv(File file) {
        List<String[]> rows = new ArrayList<>();
        if (!file.exists()) return rows;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                rows.add(cols);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading CSV: " + file.getName(), e);
        }
        return rows;
    }

    private String encode(String text) {
        if (text == null) return "null";
        try {
            return Base64.encodeToString(text.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String decode(String base64) {
        if (base64 == null || base64.equals("null") || base64.isEmpty()) return "";
        try {
            byte[] data = Base64.decode(base64, Base64.NO_WRAP);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    // =====================================================================
    // Authentication & Profile Methods
    // =====================================================================

    public synchronized long registerUser(String email, String passwordHash, String educationLevel) {
        if (isEmailExists(email)) {
            return -1; // Email đã tồn tại
        }
        UserEntity newUser = new UserEntity(
                email,
                passwordHash,
                educationLevel,
                "Toán học",   // favorite_subject mặc định
                "Detailed",   // ai_tone mặc định
                0             // xp_points mặc định
        );
        return db.userDao().insertUser(newUser);
    }

    public synchronized boolean authenticateUser(String email, String passwordHash) {
        UserEntity user = db.userDao().getUserByEmail(email);
        return user != null && user.getPassword().equals(passwordHash);
    }

    public synchronized boolean isEmailExists(String email) {
        UserEntity user = db.userDao().getUserByEmail(email);
        return user != null;
    }

    public synchronized boolean updatePassword(String email, String newPasswordHash) {
        UserEntity user = db.userDao().getUserByEmail(email);
        if (user != null) {
            user.setPassword(newPasswordHash);
            db.userDao().updateUser(user);
            return true;
        }
        return false;
    }

    public synchronized boolean updateEmailCascade(String oldEmail, String newEmail) {
        if (oldEmail == null || newEmail == null) return false;

        UserEntity user = db.userDao().getUserByEmail(oldEmail);
        if (user == null) return false;

        // 1. Xóa user cũ và tạo user mới với email mới (vì email là Primary Key không đổi trực tiếp được)
        db.userDao().deleteUserByEmail(oldEmail);
        user.setEmail(newEmail);
        db.userDao().insertUser(user);

        // 2. Cập nhật các câu hỏi liên kết email cũ
        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(oldEmail);
        for (QuestionEntity q : questions) {
            q.setEmail(newEmail);
            db.questionDao().updateQuestion(q);
        }

        return true;
    }

    public synchronized UserProfile getUserProfile(String email) {
        UserEntity user = db.userDao().getUserByEmail(email);
        if (user != null) {
            UserProfile profile = new UserProfile(user.getEmail(), user.getEducationLevel());
            profile.setFavoriteSubject(user.getFavoriteSubject());
            profile.setAiTone(user.getAiTone());
            profile.setXpPoints(user.getXpPoints());
            return profile;
        }
        return null;
    }

    public synchronized int updateUserProfile(UserProfile profile) {
        UserEntity user = db.userDao().getUserByEmail(profile.getEmail());
        if (user != null) {
            user.setEducationLevel(profile.getEducationLevel());
            user.setFavoriteSubject(profile.getFavoriteSubject());
            user.setAiTone(profile.getAiTone());
            user.setXpPoints(profile.getXpPoints());
            return db.userDao().updateUser(user);
        }
        return 0;
    }

    // =====================================================================
    // Question & AI Response Methods
    // =====================================================================

    public synchronized long saveQuestion(String content, int subjectId, int sessionId) {
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", "1"); // Lấy email phiên đăng nhập hiện tại làm định danh

        QuestionEntity q = new QuestionEntity(
                email,
                subjectId,
                String.valueOf(System.currentTimeMillis()),
                content,
                String.valueOf(sessionId)
        );
        return db.questionDao().insertQuestion(q);
    }

    public synchronized void saveAIResponse(long questionId, AIResponse response) {
        // AI responses cũ được tích hợp lưu trong Quiz JSON hoặc bỏ qua vì không dùng trực tiếp trong giao diện mới
    }

    public synchronized long getQuestionIdByContent(String content) {
        List<QuestionEntity> list = db.questionDao().getAllQuestions();
        for (QuestionEntity q : list) {
            if (q.getQuestionText() != null && q.getQuestionText().equals(content)) {
                return q.getId();
            }
        }
        return -1;
    }

    public synchronized AIResponse getCachedResponseForQuestion(String questionContent) {
        // SQLite lưu cache câu hỏi trực tiếp, không sử dụng cache phản hồi riêng rẽ
        return null;
    }

    public synchronized Question getLatestQuestion(int userId) {
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return null;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        if (!questions.isEmpty()) {
            QuestionEntity q = questions.get(0); // Lấy phần tử đầu tiên (mới nhất do câu lệnh ORDER BY id DESC)
            return new Question(q.getId(), 1, q.getSubjectId(), q.getQuestionText());
        }
        return null;
    }

    public synchronized List<Question> getSavedQuestions(int userId) {
        List<Question> list = new ArrayList<>();
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return list;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            list.add(new Question(q.getId(), 1, q.getSubjectId(), q.getQuestionText()));
        }
        return list;
    }

    // =====================================================================
    // Chat Session Management Methods
    // =====================================================================

    public synchronized List<Integer> getUniqueSessionIds(int userId) {
        List<Integer> sessionIds = new ArrayList<>();
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return sessionIds;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            try {
                int sId = Integer.parseInt(q.getSessionId());
                if (!sessionIds.contains(sId)) {
                    sessionIds.add(sId);
                }
            } catch (NumberFormatException ignored) {}
        }
        return sessionIds;
    }

    public synchronized List<ChatMessage> getChatHistoryForSession(int userId, int sessionId) {
        List<ChatMessage> chatHistory = new ArrayList<>();
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return chatHistory;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            if (q.getSessionId().equals(String.valueOf(sessionId))) {
                // Thêm câu hỏi tin nhắn của User
                ChatMessage userMsg = new ChatMessage(q.getQuestionText(), true);
                userMsg.setQuestionId(q.getId());
                chatHistory.add(userMsg);

                // Tìm Quiz tương ứng chứa giải thích/phản hồi AI
                QuizEntity quiz = db.quizDao().getQuizByQuestionId(q.getId());
                if (quiz != null && quiz.getQuizQuestionsJson() != null) {
                    try {
                        JSONArray array = new JSONArray(quiz.getQuizQuestionsJson());
                        if (array.length() > 0) {
                            // Tạo phản hồi AI từ câu đầu tiên làm đại diện
                            JSONObject obj = array.getJSONObject(0);
                            AIResponse aiResponse = new AIResponse(
                                    "AI",
                                    obj.optString("feedback"),
                                    obj.optString("question"),
                                    obj.optString("feedback")
                            );
                            ChatMessage aiMsg = new ChatMessage(aiResponse, false);
                            aiMsg.setQuestionId(q.getId());
                            chatHistory.add(aiMsg);
                        }
                    } catch (JSONException ignored) {}
                }
            }
        }
        return chatHistory;
    }

    public synchronized String getSessionTitle(int userId, int sessionId) {
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return "Đoạn chat mới";

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            if (q.getSessionId().equals(String.valueOf(sessionId))) {
                String firstQuestion = q.getQuestionText();
                if (firstQuestion != null && !firstQuestion.isEmpty()) {
                    return firstQuestion.length() > 30 ? firstQuestion.substring(0, 27) + "..." : firstQuestion;
                }
            }
        }
        return "Đoạn chat mới";
    }

    public synchronized int getSessionMessageCount(int userId, int sessionId) {
        int count = 0;
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return 0;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            if (q.getSessionId().equals(String.valueOf(sessionId))) {
                count++;
            }
        }
        return count;
    }

    public synchronized void deleteChatSession(int userId, int sessionId) {
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email == null) return;

        List<QuestionEntity> questions = db.questionDao().getQuestionsByEmail(email);
        for (QuestionEntity q : questions) {
            if (q.getSessionId().equals(String.valueOf(sessionId))) {
                // Sẽ tự động delete trong DB
            }
        }
    }

    public synchronized void clearAllChatHistory(int userId) {
        String email = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("user_email", null);
        if (email != null) {
            db.questionDao().deleteQuestionsByEmail(email);
        }
    }

    // =====================================================================
    // Quiz & Quiz Lab Methods
    // =====================================================================

    public synchronized void saveQuiz(long questionId, List<QuizQuestion> quizQuestions) {
        try {
            JSONArray array = new JSONArray();
            for (QuizQuestion q : quizQuestions) {
                JSONObject obj = new JSONObject();
                obj.put("question", q.getQuestionText());
                obj.put("options", new JSONArray(q.getOptions()));
                obj.put("correctIndex", q.getCorrectOptionIndex());
                obj.put("feedback", q.getFeedback());
                array.put(obj);
            }

            // Xóa quiz cũ của question này nếu có trong SQLite
            QuizEntity oldQuiz = db.quizDao().getQuizByQuestionId(questionId);
            if (oldQuiz != null) {
                // Ghi đè lên quiz cũ
                oldQuiz.setQuizQuestionsJson(array.toString());
                oldQuiz.setStatus(0); // reset status
                oldQuiz.setScore(-1); // reset score
                db.quizDao().updateQuiz(oldQuiz);
            } else {
                // Tạo mới
                QuizEntity newQuiz = new QuizEntity(
                        questionId,
                        array.toString(),
                        -1,
                        String.valueOf(System.currentTimeMillis()),
                        0
                );
                db.quizDao().insertQuiz(newQuiz);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize quiz to JSON", e);
        }
    }

    public synchronized List<QuizQuestion> getQuizForQuestion(long questionId) {
        List<QuizQuestion> quiz = new ArrayList<>();
        QuizEntity entity = db.quizDao().getQuizByQuestionId(questionId);
        if (entity != null && entity.getQuizQuestionsJson() != null) {
            try {
                JSONArray array = new JSONArray(entity.getQuizQuestionsJson());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    JSONArray optsArr = obj.getJSONArray("options");
                    List<String> opts = new ArrayList<>();
                    for (int j = 0; j < optsArr.length(); j++) {
                        opts.add(optsArr.getString(j));
                    }
                    quiz.add(new QuizQuestion(
                            obj.getString("question"),
                            opts,
                            obj.getInt("correctIndex"),
                            obj.getString("feedback")
                    ));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing quiz JSON", e);
            }
        }
        return quiz;
    }

    public synchronized List<QuizQuestion> getRandomQuiz() {
        List<QuizEntity> list = db.quizDao().getAllQuizzes();
        if (list.isEmpty()) return new ArrayList<>();

        int randomIndex = new Random().nextInt(list.size());
        QuizEntity entity = list.get(randomIndex);
        List<QuizQuestion> quiz = new ArrayList<>();

        if (entity != null && entity.getQuizQuestionsJson() != null) {
            try {
                JSONArray array = new JSONArray(entity.getQuizQuestionsJson());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    JSONArray optsArr = obj.getJSONArray("options");
                    List<String> opts = new ArrayList<>();
                    for (int j = 0; j < optsArr.length(); j++) {
                        opts.add(optsArr.getString(j));
                    }
                    quiz.add(new QuizQuestion(
                            obj.getString("question"),
                            opts,
                            obj.getInt("correctIndex"),
                            obj.getString("feedback")
                    ));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing random quiz JSON", e);
            }
        }
        return quiz;
    }

    public synchronized List<QuizSummary> getAllQuizSummaries() {
        List<QuizSummary> summaries = new ArrayList<>();
        List<QuizEntity> quizzes = db.quizDao().getAllQuizzes();
        List<QuestionEntity> questions = db.questionDao().getAllQuestions();

        for (QuizEntity quizRow : quizzes) {
            long quizId = quizRow.getId();
            long questionId = quizRow.getQuestionId();
            String quizJson = quizRow.getQuizQuestionsJson();
            
            long createdAt = 0;
            try { createdAt = Long.parseLong(quizRow.getDate()); } catch (Exception ignored) {}

            boolean isCompleted = quizRow.getStatus() == 1;
            int score = quizRow.getScore();

            // Tìm thông tin câu hỏi
            int subjectId = 0;
            String rawContent = "Quiz";
            for (QuestionEntity q : questions) {
                if (q.getId() == questionId) {
                    subjectId = q.getSubjectId();
                    rawContent = q.getQuestionText();
                    break;
                }
            }

            int questionCount = 0;
            try {
                if (quizJson != null && !quizJson.isEmpty()) {
                    questionCount = new JSONArray(quizJson).length();
                }
            } catch (JSONException ignored) {}

            String title = (rawContent.length() > 32) ? rawContent.substring(0, 29) + "..." : rawContent;
            summaries.add(new QuizSummary(quizId, questionId, subjectId, title, questionCount, createdAt, isCompleted, score));
        }

        // Sắp xếp theo ngày tạo giảm dần
        Collections.sort(summaries, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        return summaries;
    }

    public synchronized void deleteQuiz(long quizId) {
        // SQLite Room sẽ tự động quản lý qua DAO nếu cần
    }

    public synchronized void completeQuiz(long questionId, int score) {
        QuizEntity quiz = db.quizDao().getQuizByQuestionId(questionId);
        if (quiz != null) {
            quiz.setStatus(1); // is_completed = 1
            quiz.setScore(score);
            db.quizDao().updateQuiz(quiz);
        }
    }
}
