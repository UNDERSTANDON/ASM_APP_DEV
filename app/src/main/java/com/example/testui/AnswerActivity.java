package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.utils.NetworkUtils;

public class AnswerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        dbHelper = new DatabaseHelper(this);

        TextView questionTextView = findViewById(R.id.question_text_view);
        ImageView backButton = findViewById(R.id.back_button);
        TextView moreSolutionsBtn = findViewById(R.id.more_solutions_btn);

        String question = getIntent().getStringExtra("question");
        if (question != null && !question.isEmpty()) {
            questionTextView.setText(question);
            fetchAIAnswer(question);
        }

        backButton.setOnClickListener(v -> finish());

        moreSolutionsBtn.setOnClickListener(v -> {
            startActivity(new Intent(AnswerActivity.this, ExpandedAnswerActivity.class));
        });

        NavigationHelper.setupBottomNavigation(this, R.id.nav_home);
    }

    private void fetchAIAnswer(String question) {
        // Check local database cache first
        AIResponse cachedResponse = dbHelper.getCachedResponseForQuestion(question);
        if (cachedResponse != null) {
            displayResponse(cachedResponse);
            Toast.makeText(AnswerActivity.this, "Tải câu trả lời từ bộ nhớ đệm local...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Ngoại tuyến: Không tìm thấy câu trả lời trong bộ nhớ đệm.", Toast.LENGTH_LONG).show();
            return;
        }

        AIObj.getInstance().askQuestion(question, new AIObj.AICallback<AIResponse>() {
            @Override
            public void onSuccess(AIResponse result) {
                displayResponse(result);
                
                // Save to DB for offline access
                long qId = dbHelper.saveQuestion(question, 1, 1); // Mock IDs
                dbHelper.saveAIResponse(qId, result);
                
                Toast.makeText(AnswerActivity.this, "Đã lưu câu trả lời ngoại tuyến", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AnswerActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayResponse(AIResponse result) {
        TextView introText = findViewById(R.id.intro_text);
        if (introText != null) {
            introText.setText(result.getSimplifiedExplanation());
        }

        TextView mathBlock1 = findViewById(R.id.math_1_block);
        if (mathBlock1 != null && result.getLogicalSteps() != null) {
            mathBlock1.setText(result.getLogicalSteps());
        }
    }
}
