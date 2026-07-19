package com.example.testui.ai;

import android.os.Handler;
import android.os.Looper;

import com.example.testui.models.AIResponse;
import com.example.testui.models.QuizQuestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIObj {

    private Map<String, Object> config;
    private static AIObj instance;
    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface AICallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private AIObj() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized AIObj getInstance() {
        if (instance == null) {
            instance = new AIObj();
        }
        return instance;
    }

    /**
     * Flexible initialization for the AI Specialist to use.
     * @param config Map containing education level, subjects, API keys, system prompts, etc.
     */
    public void initialize(Map<String, Object> config) {
        this.config = config;
    }

    private String getApiKeyOrError(AICallback<?> callback) {
        android.content.Context context = com.example.testui.database.AppDatabase.appContext;
        String apiKey = "";
        if (context != null) {
            apiKey = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                    .getString("user_gemini_api_key", "");
        }
        if (android.text.TextUtils.isEmpty(apiKey)) {
            callback.onError("Vui lòng cấu hình Gemini API Key trong trang Cá nhân của bạn để sử dụng tính năng này!");
            return null;
        }
        return apiKey;
    }

    /**
     * Main method to ask a question using Gemini API.
     */
    public void askQuestion(String question, AICallback<AIResponse> callback) {
        String educationLevel = "Not specified";
        String tone = "Detailed";
        if (config != null) {
            if (config.containsKey("educationLevel") && config.get("educationLevel") != null) {
                educationLevel = config.get("educationLevel").toString();
            }
            if (config.containsKey("tone") && config.get("tone") != null) {
                tone = Objects.requireNonNull(config.get("tone")).toString();
            }
        }

        // System instructions directing Gemini's behavior and response format
        String systemInstruction = "You are a professional AI Study Mentor for students. " +
                "You must solve the student's question accurately (aim for 100% accuracy) and return the response in a structured JSON format. " +
                "You must response the student's question with Vietnamese, regardless of which language the student is using. " +
                "Adjust your explanations, mathematical rigor, and vocabulary so it is appropriate for a student at the '" + educationLevel + "' level. " +
                "Follow the formatting and detail guidelines of '" + tone + "' style. " +
                "Use Markdown formatting (bold, italics, lists, etc.) within your text values to make the information clear and readable.\n\n" +
                "### IMPORTANT FORMATTING RULES:\n" +
                "- DO NOT use LaTeX math mode (e.g., do not use $...$ or $$...$$). The app only supports standard Markdown.\n" +
                "- Use bold for key terms or formulas if needed, but keep them as plain text (e.g., use **H2SO4** instead of $H_2SO_4$).\n" +
                "- For subscripts and superscripts in chemical formulas or math, use standard characters or clear notation (e.g., CO2, x^2).\n" +
                "- Always use proper Markdown lists (using * or 1., 2.) for multiple items to ensure they render on separate lines.\n\n" +
                "### SAFETY AND SCOPE GUIDELINES:\n" +
                "- ONLY answer questions related to academic subjects (Mathematics, Science, History, etc.).\n" +
                "- If a question is harmful, dangerous, illegal, or promotes substance abuse (e.g., manufacturing drugs, explosives), you MUST refuse to answer.\n" +
                "- If the question is entirely outside the scope of education or violates safety policies, return a JSON with \"subject\": \"Out of Scope\" and \"finalAnswer\": \"I am sorry, but I can only assist with academic and educational questions. I cannot provide information on dangerous, illegal, or non-educational topics.\".\n\n" +
                "You MUST return the output strictly as a JSON object, using markdown formatting. " +
                "The JSON must have the following keys:\n" +
                "- \"subject\": The academic subject of the question (e.g., Mathematics, Science, Programming, History, Languages).\n" +
                "- \"logicalSteps\": A step-by-step logical derivation or walkthrough of the solution.\n" +
                "- \"finalAnswer\": The final concise answer or value.\n" +
                "- \"simplifiedExplanation\": A simplified summary or TL;DR for quick reading.\n" +
                "- \"formulas\": An array of strings representing formulas used (or empty list if none).\n" +
                "- \"alternatives\": An array of strings representing alternative approaches or ways to solve it (or empty list).\n" +
                "- \"commonMistakes\": An array of strings representing common pitfalls or mistakes to avoid (or empty list).";

        JSONObject jsonPayload = new JSONObject();
        try {
            // Set System Instruction
            JSONObject systemInstructionObj = new JSONObject();
            JSONArray siParts = new JSONArray();
            siParts.put(new JSONObject().put("text", systemInstruction));
            systemInstructionObj.put("parts", siParts);
            jsonPayload.put("system_instruction", systemInstructionObj);

            // Set User Content
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", question));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonPayload.put("contents", contents);
            
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            jsonPayload.put("generationConfig", generationConfig);
        } catch (JSONException e) {
            callback.onError("Failed to create request payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String apiKey = getApiKeyOrError(callback);
        if (apiKey == null) return;

        String url = GeminiConfig.BASE_URL + GeminiConfig.MODEL_NAME + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseBody = response) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "Empty response body";
                        android.util.Log.e("AIObj", "API Error " + response.code() + " for URL: " + url.replace(apiKey, "REDACTED"));
                        android.util.Log.e("AIObj", "Error Body: " + errBody);
                        mainHandler.post(() -> callback.onError("API error (code " + response.code() + "): " + errBody));
                        return;
                    }

                    if (response.body() == null) {
                        mainHandler.post(() -> callback.onError("Received empty response from Gemini API"));
                        return;
                    }

                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        mainHandler.post(() -> callback.onError("No candidates found in AI response"));
                        return;
                    }
                    
                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject content = candidate.optJSONObject("content");
                    if (content == null) {
                        mainHandler.post(() -> callback.onError("No content found in the AI candidate response"));
                        return;
                    }
                    
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts == null || parts.length() == 0) {
                        mainHandler.post(() -> callback.onError("No parts found in the AI content response"));
                        return;
                    }
                    
                    String responseText = parts.getJSONObject(0).optString("text");
                    if (responseText == null || responseText.trim().isEmpty()) {
                        mainHandler.post(() -> callback.onError("Empty text generated by AI"));
                        return;
                    }

                    JSONObject structuredJson = new JSONObject(responseText);
                    
                    String subject = structuredJson.optString("subject", "General");
                    String finalAnswer = structuredJson.optString("finalAnswer", "");
                    String simplifiedExplanation = structuredJson.optString("simplifiedExplanation", "");

                    // Robust parsing for logicalSteps (handle both String and JSONArray)
                    String logicalSteps = "";
                    Object stepsObj = structuredJson.opt("logicalSteps");
                    if (stepsObj instanceof JSONArray) {
                        JSONArray stepsArray = (JSONArray) stepsObj;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < stepsArray.length(); i++) {
                            sb.append(i + 1).append(". ").append(stepsArray.optString(i)).append("\n");
                        }
                        logicalSteps = sb.toString().trim();
                    } else if (stepsObj != null) {
                        logicalSteps = stepsObj.toString();
                    }
                    
                    AIResponse aiResponse = new AIResponse(subject, logicalSteps, finalAnswer, simplifiedExplanation);
                    
                    aiResponse.setFormulas(jsonArrayToList(structuredJson.optJSONArray("formulas")));
                    aiResponse.setAlternatives(jsonArrayToList(structuredJson.optJSONArray("alternatives")));
                    aiResponse.setCommonMistakes(jsonArrayToList(structuredJson.optJSONArray("commonMistakes")));
                    
                    mainHandler.post(() -> callback.onSuccess(aiResponse));
                    
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse response: " + e.getMessage()));
                }
            }
        });
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.optString(i, ""));
            }
        }
        return list;
    }

    /**
     * Method to generate a quiz based on a specific question/answer.
     */
    public void generateQuiz(String context, String instruction, AICallback<List<QuizQuestion>> callback) {
        String educationLevel = "Not specified";
        String tone = "Detailed";
        if (config != null) {
            if (config.containsKey("educationLevel") && config.get("educationLevel") != null) {
                educationLevel = config.get("educationLevel").toString();
            }
            if (config.containsKey("tone") && config.get("tone") != null) {
                tone = java.util.Objects.requireNonNull(config.get("tone")).toString();
            }
        }

        String systemInstruction = "You are a professional quiz creator for students. " +
                "Generate a high-quality multiple-choice quiz based on the provided context. " +
                "The context is a student's question and the AI's detailed answer.\n\n" +
                "Adjust the complexity, mathematical rigor, and difficulty of the questions to match a student at the '" + educationLevel + "' level.\n" +
                "Write the 'feedback' (explanation for the correct answer) following the formatting and detail guidelines of '" + tone + "' style.\n\n" +
                "Follow these rules:\n" +
                "1. Create exactly 10 multiple-choice questions.\n" +
                "2. Each question must have exactly 4 options.\n" +
                "3. Provide the index of the correct option (0-indexed).\n" +
                "4. Provide a helpful explanation/feedback for the correct answer.\n" +
                "5. Ensure the language of the entire quiz is Vietnamese.\n" +
                "6. If the user provided specific instructions like 'focus on math', follow them.\n\n" +
                "You MUST return the output strictly as a JSON array of objects. " +
                "Each object must have the following keys:\n" +
                "- \"questionText\": The text of the question.\n" +
                "- \"options\": An array of 4 strings representing the choices.\n" +
                "- \"correctOptionIndex\": The 0-based index of the correct choice.\n" +
                "- \"feedback\": A brief explanation of why the answer is correct.\n";

        String userPrompt = "Context: " + context + "\n\nAdditional Instructions: " + instruction;

        JSONObject jsonPayload = new JSONObject();
        try {
            JSONObject systemInstructionObj = new JSONObject();
            JSONArray siParts = new JSONArray();
            siParts.put(new JSONObject().put("text", systemInstruction));
            systemInstructionObj.put("parts", siParts);
            jsonPayload.put("system_instruction", systemInstructionObj);

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userPrompt));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonPayload.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            jsonPayload.put("generationConfig", generationConfig);
        } catch (JSONException e) {
            callback.onError("Failed to create request payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String apiKey = getApiKeyOrError(callback);
        if (apiKey == null) return;
        String url = GeminiConfig.BASE_URL + GeminiConfig.MODEL_NAME + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseBody = response) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "Empty response body";
                        mainHandler.post(() -> callback.onError("API error (code " + response.code() + "): " + errBody));
                        return;
                    }

                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        mainHandler.post(() -> callback.onError("No candidates found"));
                        return;
                    }

                    String responseText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    JSONArray quizArray = new JSONArray(responseText);
                    List<QuizQuestion> quizQuestions = new ArrayList<>();
                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject qObj = quizArray.getJSONObject(i);
                        String qText = qObj.optString("questionText");
                        JSONArray optsArr = qObj.optJSONArray("options");
                        List<String> options = new ArrayList<>();
                        if (optsArr != null) {
                            for (int j = 0; j < optsArr.length(); j++) {
                                options.add(optsArr.getString(j));
                            }
                        }
                        int correctIdx = qObj.optInt("correctOptionIndex");
                        String feedback = qObj.optString("feedback");
                        quizQuestions.add(new QuizQuestion(qText, options, correctIdx, feedback));
                    }

                    mainHandler.post(() -> callback.onSuccess(quizQuestions));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse quiz: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Tự động sinh bài trắc nghiệm có bộ lọc Lớp học và Kì học chuẩn theo chương trình học.
     */
    public void generateTargetedQuiz(String subject, String grade, String semester, AICallback<List<QuizQuestion>> callback) {
        String educationLevel = "Not specified";
        String tone = "Detailed";
        if (config != null) {
            if (config.containsKey("educationLevel") && config.get("educationLevel") != null) {
                educationLevel = config.get("educationLevel").toString();
            }
            if (config.containsKey("tone") && config.get("tone") != null) {
                tone = java.util.Objects.requireNonNull(config.get("tone")).toString();
            }
        }

        String systemInstruction = "You are an expert exam creator. " +
                "Generate a high-quality multiple-choice practice quiz for the academic subject: '" + subject + "'.\n\n" +
                "Requirements:\n" +
                "- The difficulty, syllabus topics, and conceptual depth MUST strictly align with the Vietnamese national curriculum for '" + grade + "' during '" + semester + "'.\n" +
                "- Set the conceptual depth appropriate for a student at the '" + educationLevel + "' education level (specifically targeting '" + grade + "', '" + semester + "').\n" +
                "- Structure the 'feedback' explanation for each question following the '" + tone + "' style.\n" +
                "- Ensure all questions are different and random (do not repeat common questions).\n" +
                "- The entire output must be written in Vietnamese.\n\n" +
                "Follow these rules:\n" +
                "1. Create exactly 10 multiple-choice questions.\n" +
                "2. Each question must have exactly 4 options.\n" +
                "3. Provide the index of the correct option (0-indexed).\n" +
                "4. Provide a helpful explanation/feedback (giải thích tại sao đúng) for the correct answer.\n" +
                "You MUST return the output strictly as a JSON array of objects. Each object must have the keys: \"questionText\", \"options\" (array of 4 strings), \"correctOptionIndex\" (int), and \"feedback\" (string).";

        String userPrompt = "Generate 10 questions for the subject: " + subject + ", Grade: " + grade + ", Semester: " + semester;

        JSONObject jsonPayload = new JSONObject();
        try {
            JSONObject systemInstructionObj = new JSONObject();
            JSONArray siParts = new JSONArray();
            siParts.put(new JSONObject().put("text", systemInstruction));
            systemInstructionObj.put("parts", siParts);
            jsonPayload.put("system_instruction", systemInstructionObj);

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userPrompt));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonPayload.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            jsonPayload.put("generationConfig", generationConfig);
        } catch (JSONException e) {
            callback.onError("Failed to create request payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String apiKey = getApiKeyOrError(callback);
        if (apiKey == null) return;
        String url = GeminiConfig.BASE_URL + GeminiConfig.MODEL_NAME + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseBody = response) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "Empty response body";
                        mainHandler.post(() -> callback.onError("API error (code " + response.code() + "): " + errBody));
                        return;
                    }

                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        mainHandler.post(() -> callback.onError("No candidates found"));
                        return;
                    }

                    String responseText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    JSONArray quizArray = new JSONArray(responseText);
                    List<QuizQuestion> quizQuestions = new ArrayList<>();
                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject qObj = quizArray.getJSONObject(i);
                        String qText = qObj.optString("questionText");
                        JSONArray optsArr = qObj.optJSONArray("options");
                        List<String> options = new ArrayList<>();
                        if (optsArr != null) {
                            for (int j = 0; j < optsArr.length(); j++) {
                                options.add(optsArr.getString(j));
                            }
                        }
                        int correctIdx = qObj.optInt("correctOptionIndex");
                        String feedback = qObj.optString("feedback");
                        quizQuestions.add(new QuizQuestion(qText, options, correctIdx, feedback));
                    }

                    mainHandler.post(() -> callback.onSuccess(quizQuestions));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse quiz: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Tự động sinh bài trắc nghiệm ngẫu nhiên gồm 10 câu hỏi dựa trên môn học.
     * Thích hợp khi tạo quiz trực tiếp từ Quiz Lab screen.
     */
    public void generateRandomSubjectQuiz(String subject, AICallback<List<QuizQuestion>> callback) {
        String educationLevel = "Not specified";
        String tone = "Detailed";
        if (config != null) {
            if (config.containsKey("educationLevel") && config.get("educationLevel") != null) {
                educationLevel = config.get("educationLevel").toString();
            }
            if (config.containsKey("tone") && config.get("tone") != null) {
                tone = java.util.Objects.requireNonNull(config.get("tone")).toString();
            }
        }

        String systemInstruction = "You are an expert exam creator. " +
                "Generate a high-quality, completely random multiple-choice practice quiz for the academic subject: '" + subject + "'.\n\n" +
                "Requirements:\n" +
                "- Set the difficulty, mathematical complexity, and conceptual depth appropriate for a student at the '" + educationLevel + "' education level.\n" +
                "- Structure the 'feedback' explanation for each question following the '" + tone + "' style.\n" +
                "- Ensure all questions are different and random (do not repeat common questions).\n" +
                "- The entire output must be written in Vietnamese.\n\n" +
                "Follow these rules:\n" +
                "1. Create exactly 10 multiple-choice questions.\n" +
                "2. Each question must have exactly 4 options.\n" +
                "3. Provide the index of the correct option (0-indexed).\n" +
                "4. Provide a helpful explanation/feedback for the correct answer.\n" +
                "You MUST return the output strictly as a JSON array of objects. Each object must have the keys: \"questionText\", \"options\" (array of 4 strings), \"correctOptionIndex\" (int), and \"feedback\" (string).";

        String userPrompt = "Generate 10 random questions for the subject: " + subject;

        JSONObject jsonPayload = new JSONObject();
        try {
            JSONObject systemInstructionObj = new JSONObject();
            JSONArray siParts = new JSONArray();
            siParts.put(new JSONObject().put("text", systemInstruction));
            systemInstructionObj.put("parts", siParts);
            jsonPayload.put("system_instruction", systemInstructionObj);

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userPrompt));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonPayload.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            jsonPayload.put("generationConfig", generationConfig);
        } catch (JSONException e) {
            callback.onError("Failed to create request payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String apiKey = getApiKeyOrError(callback);
        if (apiKey == null) return;
        String url = GeminiConfig.BASE_URL + GeminiConfig.MODEL_NAME + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseBody = response) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "Empty response body";
                        mainHandler.post(() -> callback.onError("API error (code " + response.code() + "): " + errBody));
                        return;
                    }

                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        mainHandler.post(() -> callback.onError("No candidates found"));
                        return;
                    }

                    String responseText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    JSONArray quizArray = new JSONArray(responseText);
                    List<QuizQuestion> quizQuestions = new ArrayList<>();
                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject qObj = quizArray.getJSONObject(i);
                        String qText = qObj.optString("questionText");
                        JSONArray optsArr = qObj.optJSONArray("options");
                        List<String> options = new ArrayList<>();
                        if (optsArr != null) {
                            for (int j = 0; j < optsArr.length(); j++) {
                                options.add(optsArr.getString(j));
                            }
                        }
                        int correctIdx = qObj.optInt("correctOptionIndex");
                        String feedback = qObj.optString("feedback");
                        quizQuestions.add(new QuizQuestion(qText, options, correctIdx, feedback));
                    }

                    mainHandler.post(() -> callback.onSuccess(quizQuestions));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse quiz: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Gửi chi tiết kết quả làm bài trắc nghiệm lên Gemini để phân tích học lực động (AI Insights).
     */
    public void generateAIInsights(String educationLevel, String subjectName, String quizDetails, AICallback<String> callback) {
        String systemInstruction = "You are an expert academic advisor. " +
                "Analyze the student's quiz results and education level to generate a helpful, encouraging learning insight in Vietnamese.\n\n" +
                "Requirements:\n" +
                "- Write the response in Vietnamese.\n" +
                "- Structure the response clearly with bullet points:\n" +
                "  1. Nhận xét chung: A motivating comment based on the quiz score.\n" +
                "  2. Điểm mạnh: Highlight the topics or questions they got correct.\n" +
                "  3. Điểm cần cải thiện: Point out the specific sub-topics or concepts they got wrong, explaining briefly what went wrong.\n" +
                "  4. Khuyên dùng: Give a concrete study plan or recommendation.\n" +
                "- Keep the tone professional, supportive, and friendly.";

        String userPrompt = "Subject: " + subjectName + "\n" +
                "Education Level: " + educationLevel + "\n" +
                "Quiz Performance:\n" + quizDetails;

        JSONObject jsonPayload = new JSONObject();
        try {
            JSONObject systemInstructionObj = new JSONObject();
            JSONArray siParts = new JSONArray();
            siParts.put(new JSONObject().put("text", systemInstruction));
            systemInstructionObj.put("parts", siParts);
            jsonPayload.put("system_instruction", systemInstructionObj);

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userPrompt));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonPayload.put("contents", contents);
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonPayload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String apiKey = getApiKeyOrError(callback);
        if (apiKey == null) return;
        String url = GeminiConfig.BASE_URL + GeminiConfig.MODEL_NAME + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response responseBody = response) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "Empty response body";
                        mainHandler.post(() -> callback.onError("API error (code " + response.code() + "): " + errBody));
                        return;
                    }

                    String responseString = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    JSONArray candidates = jsonResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        mainHandler.post(() -> callback.onError("No candidates found"));
                        return;
                    }

                    String responseText = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    mainHandler.post(() -> callback.onSuccess(responseText));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse insights: " + e.getMessage()));
                }
            }
        });
    }
}
