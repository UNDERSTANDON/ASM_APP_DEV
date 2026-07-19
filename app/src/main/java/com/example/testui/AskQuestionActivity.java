package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class AskQuestionActivity extends AppCompatActivity {

    private EditText questionInput;
    private View sendButton;
    private ImageView backButton;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private DatabaseHelper dbHelper;
    private int currentSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_question);

        dbHelper = new DatabaseHelper(this);
        messageList = new ArrayList<>();

        currentSessionId = getIntent().getIntExtra("session_id", -1);
        if (currentSessionId == -1) {
            currentSessionId = (int) (System.currentTimeMillis() / 1000);
        }

        questionInput = findViewById(R.id.question_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);

        // Setup RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setAdapter(chatAdapter);

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        // Setup history button
        ImageView historyButton = findViewById(R.id.history_button);
        if (historyButton != null) {
            historyButton.setOnClickListener(v -> {
                Intent intent = new Intent(AskQuestionActivity.this, AnswerHistoryActivity.class);
                startActivity(intent);
            });
        }

        // Load chat history from SQLite
        loadChatHistory();

        // Setup send button
        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString().trim();
            if (question.isEmpty()) {
                // If empty, ask a general learning tip prompt
                question = "Hãy giới thiệu bản thân và gợi ý một chủ đề học tập thú vị hoặc lời khuyên học tập phù hợp với cấp học của tôi.";
            }

            sendMessage(question);
        });
    }

    private void sendMessage(String question) {
        // 1. Add user message
        ChatMessage userMessage = new ChatMessage(question, true);
        messageList.add(userMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        // Clear input box
        questionInput.setText("");

        // Check if we have a cached response for this question (exact match)
        AIResponse cachedResponse = dbHelper.getCachedResponseForQuestion(question);
        if (cachedResponse != null) {
            long qId = dbHelper.getQuestionIdByContent(question);
            userMessage.setQuestionId(qId);
            
            ChatMessage aiMessage = new ChatMessage(cachedResponse, false);
            aiMessage.setQuestionId(qId);
            
            messageList.add(aiMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.scrollToPosition(messageList.size() - 1);
            Toast.makeText(AskQuestionActivity.this, "Tải từ bộ nhớ đệm...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Add thinking message
        ChatMessage thinkingMessage = new ChatMessage(true);
        messageList.add(thinkingMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        // 3. Call AI
        AIObj.getInstance().askQuestion(question, new AIObj.AICallback<AIResponse>() {
            @Override
            public void onSuccess(AIResponse result) {
                // Remove thinking indicator
                removeThinkingMessage();

                // Lấy môn học yêu thích của người dùng để làm mặc định nếu AI không nhận diện được môn học cụ thể
                String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
                String favSubj = "Toán học";
                if (email != null) {
                    com.example.testui.models.UserProfile profile = dbHelper.getUserProfile(email);
                    if (profile != null && profile.getFavoriteSubject() != null) {
                        favSubj = profile.getFavoriteSubject();
                    }
                }
                int subjectId = getSubjectIdFromAIResponse(result, favSubj);

                // Save to local database
                long qId = dbHelper.saveQuestion(question, subjectId, currentSessionId);
                dbHelper.saveAIResponse(qId, result);
                
                userMessage.setQuestionId(qId);

                // Add AI response
                ChatMessage aiMessage = new ChatMessage(result, false);
                aiMessage.setQuestionId(qId);
                
                messageList.add(aiMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String error) {
                // Remove thinking indicator
                removeThinkingMessage();

                // Add error message
                ChatMessage errorMessage = new ChatMessage("Rất tiếc, đã xảy ra lỗi: " + error + "\nBạn vui lòng kiểm tra lại kết nối mạng và thử lại.", false);
                messageList.add(errorMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }
        });
    }

    private int getSubjectIdFromAIResponse(AIResponse response, String favoriteSubject) {
        String aiSubject = response.getSubject();
        if (aiSubject == null || aiSubject.trim().isEmpty() || aiSubject.equalsIgnoreCase("General") || aiSubject.equalsIgnoreCase("Out of Scope")) {
            // Không nhận diện được môn cụ thể -> Dùng môn học yêu thích
            return mapSubjectNameToId(favoriteSubject);
        }
        
        int mappedId = mapSubjectNameToId(aiSubject);
        if (mappedId == 0) {
            // Môn lạ -> Dùng môn học yêu thích
            return mapSubjectNameToId(favoriteSubject);
        }
        return mappedId;
    }

    private int mapSubjectNameToId(String subjectName) {
        if (subjectName == null) return 1; // Default to Math if null
        String nameLower = subjectName.toLowerCase();
        if (nameLower.contains("math") || nameLower.contains("toán")) {
            return 1;
        } else if (nameLower.contains("phys") || nameLower.contains("lý") || nameLower.contains("science") || nameLower.contains("khoa học")) {
            return 2;
        } else if (nameLower.contains("chem") || nameLower.contains("hóa")) {
            return 3;
        } else if (nameLower.contains("program") || nameLower.contains("code") || nameLower.contains("it") || nameLower.contains("tin học") || nameLower.contains("lập trình")) {
            return 4;
        } else if (nameLower.contains("hist") || nameLower.contains("sử")) {
            return 5;
        }
        return 1; // Default fallback to Math
    }

    private void addAIMessage(ChatMessage message) {
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void removeThinkingMessage() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i).isThinking()) {
                messageList.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void loadChatHistory() {
        messageList.clear();
        
        // Add welcome message from AI
        messageList.add(new ChatMessage("Chào bạn! Tôi là Mentor học tập AI của bạn. Hãy nhập bất kỳ câu hỏi nào về Toán, Khoa học, Lịch sử hoặc Lập trình để tôi trợ giúp nhé!", false));
        
        // Retrieve saved chat history for mock user ID 1 and currentSessionId
        List<ChatMessage> savedHistory = dbHelper.getChatHistoryForSession(1, currentSessionId);
        if (savedHistory != null && !savedHistory.isEmpty()) {
            messageList.addAll(savedHistory);
        }
        
        chatAdapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            chatRecyclerView.scrollToPosition(messageList.size() - 1);
        }
    }
}
