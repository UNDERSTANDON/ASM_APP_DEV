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
            ChatMessage aiMessage = new ChatMessage(cachedResponse, false);
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

                // Add AI response
                ChatMessage aiMessage = new ChatMessage(result, false);
                messageList.add(aiMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);

                // Save to local database
                long qId = dbHelper.saveQuestion(question, 1, currentSessionId);
                dbHelper.saveAIResponse(qId, result);
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
