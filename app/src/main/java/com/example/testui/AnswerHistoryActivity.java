package com.example.testui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testui.database.DatabaseHelper;
import java.util.List;

public class AnswerHistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout savedQuestionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_history);

        dbHelper = new DatabaseHelper(this);
        savedQuestionsContainer = findViewById(R.id.saved_questions_container);

        // Set up "+ new chat" button
        View newChatBtn = findViewById(R.id.new_chat_btn);
        if (newChatBtn != null) {
            newChatBtn.setOnClickListener(v -> {
                Intent intent = new Intent(AnswerHistoryActivity.this, AskQuestionActivity.class);
                startActivity(intent);
            });
        }

        // Set up "delete all" button
        View clearAllBtn = findViewById(R.id.clear_all_btn);
        if (clearAllBtn != null) {
            clearAllBtn.setOnClickListener(v -> confirmClearAllHistory());
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_saved);

        loadChatSessions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatSessions();
    }

    private void loadChatSessions() {
        savedQuestionsContainer.removeAllViews();
        List<Integer> sessionIds = dbHelper.getUniqueSessionIds(1); // Mock user ID 1

        if (sessionIds != null && !sessionIds.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < sessionIds.size(); i++) {
                final int sessionId = sessionIds.get(i);
                View cardView = inflater.inflate(R.layout.item_chat_session, savedQuestionsContainer, false);

                TextView sessionTitle = cardView.findViewById(R.id.session_title);
                TextView sessionSubtitle = cardView.findViewById(R.id.session_subtitle);
                View deleteBtn = cardView.findViewById(R.id.delete_session_btn);

                String title = dbHelper.getSessionTitle(1, sessionId);
                int messageCount = dbHelper.getSessionMessageCount(1, sessionId);

                sessionTitle.setText(title);
                sessionSubtitle.setText(messageCount + " tin nhắn");

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(AnswerHistoryActivity.this, AskQuestionActivity.class);
                    intent.putExtra("session_id", sessionId);
                    startActivity(intent);
                });

                deleteBtn.setOnClickListener(v -> confirmDeleteSession(sessionId));

                savedQuestionsContainer.addView(cardView);
            }
        } else {
            // Show empty state placeholder
            TextView emptyTextView = new TextView(this);
            emptyTextView.setText("Chưa có đoạn chat nào. Hãy bấm '+ Chat mới' để bắt đầu trò chuyện!");
            emptyTextView.setTextColor(getResources().getColor(R.color.on_surface_variant));
            emptyTextView.setTextSize(14);
            emptyTextView.setGravity(android.view.Gravity.CENTER);
            emptyTextView.setPadding(0, 50, 0, 0);
            savedQuestionsContainer.addView(emptyTextView);
        }
    }

    private void confirmDeleteSession(final int sessionId) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa đoạn chat")
                .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện này không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteChatSession(1, sessionId);
                    Toast.makeText(AnswerHistoryActivity.this, "Đã xóa đoạn chat", Toast.LENGTH_SHORT).show();
                    loadChatSessions();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmClearAllHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tất cả lịch sử")
                .setMessage(
                        "Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    dbHelper.clearAllChatHistory(1);
                    Toast.makeText(AnswerHistoryActivity.this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                    loadChatSessions();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}