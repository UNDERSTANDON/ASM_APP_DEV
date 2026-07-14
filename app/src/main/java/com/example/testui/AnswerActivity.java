package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.QuizQuestion;
import com.example.testui.utils.NetworkUtils;
import com.google.android.material.button.MaterialButton;

import io.noties.markwon.Markwon;

import java.io.Serializable;
import java.util.List;

public class AnswerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private MaterialButton btnGenerateQuiz;
    private EditText editQuizInstruction;
    private long currentQuestionId = -1;
    private AIResponse currentAIResponse;
    private String currentQuestionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        dbHelper = new DatabaseHelper(this);

        TextView questionTextView = findViewById(R.id.question_text_view);
        ImageView backButton = findViewById(R.id.back_button);
        TextView moreSolutionsBtn = findViewById(R.id.more_solutions_btn);
        
        btnGenerateQuiz = findViewById(R.id.btn_generate_quiz);
        editQuizInstruction = findViewById(R.id.edit_quiz_instruction);

        currentQuestionText = getIntent().getStringExtra("question");
        if (currentQuestionText != null && !currentQuestionText.isEmpty()) {
            questionTextView.setText(currentQuestionText);
            fetchAIAnswer(currentQuestionText);
        }

        backButton.setOnClickListener(v -> finish());

        moreSolutionsBtn.setOnClickListener(v -> {
            startActivity(new Intent(AnswerActivity.this, ExpandedAnswerActivity.class));
        });

        btnGenerateQuiz.setOnClickListener(v -> {
            generateQuiz();
        });

        NavigationHelper.setupBottomNavigation(this, R.id.nav_home);
    }

    private void generateQuiz() {
        if (currentQuestionId == -1) {
            Toast.makeText(this, "Vui lòng đợi câu trả lời được tải xong.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if quiz already exists in DB
        List<QuizQuestion> existingQuiz = dbHelper.getQuizForQuestion(currentQuestionId);
        if (!existingQuiz.isEmpty()) {
            Intent intent = new Intent(AnswerActivity.this, QuizActivity.class);
            intent.putExtra("quiz_questions", (Serializable) existingQuiz);
            startActivity(intent);
            return;
        }

        String instruction = editQuizInstruction.getText().toString().trim();
        btnGenerateQuiz.setEnabled(false);
        btnGenerateQuiz.setText("Đang tạo bài kiểm tra...");

        String context = "Question: " + currentQuestionText + "\nAnswer: " + 
                (currentAIResponse != null ? currentAIResponse.getLogicalSteps() : "");

        AIObj.getInstance().generateQuiz(context, instruction, new AIObj.AICallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> result) {
                btnGenerateQuiz.setEnabled(true);
                btnGenerateQuiz.setText("Tạo bài kiểm tra");
                
                // Save to DB
                dbHelper.saveQuiz(currentQuestionId, result);
                
                Intent intent = new Intent(AnswerActivity.this, QuizActivity.class);
                intent.putExtra("quiz_questions", (Serializable) result);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                btnGenerateQuiz.setEnabled(true);
                btnGenerateQuiz.setText("Tạo bài kiểm tra");
                Toast.makeText(AnswerActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAIAnswer(String question) {
        // Check local database cache first
        AIResponse cachedResponse = dbHelper.getCachedResponseForQuestion(question);
        if (cachedResponse != null) {
            currentAIResponse = cachedResponse;
            currentQuestionId = dbHelper.getQuestionIdByContent(question);
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
                currentAIResponse = result;
                displayResponse(result);
                
                // Save to DB for offline access
                currentQuestionId = dbHelper.saveQuestion(question, 1, 1); // Mock IDs
                dbHelper.saveAIResponse(currentQuestionId, result);
                
                Toast.makeText(AnswerActivity.this, "Đã lưu câu trả lời ngoại tuyến", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AnswerActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayResponse(AIResponse result) {
        Markwon markwon = Markwon.create(this);

        TextView introText = findViewById(R.id.intro_text);
        if (introText != null) {
            markwon.setMarkdown(introText, result.getSimplifiedExplanation());
        }

        TextView mathBlock1 = findViewById(R.id.math_1_block);
        if (mathBlock1 != null && result.getLogicalSteps() != null) {
            markwon.setMarkdown(mathBlock1, result.getLogicalSteps());
        }
    }
}
