package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.Question;
import com.example.testui.models.QuizQuestion;
import com.example.testui.utils.NetworkUtils;

import java.io.Serializable;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Kiểm tra mạng
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Running in Offline Mode", Toast.LENGTH_LONG).show();
        }

        dbHelper = new DatabaseHelper(this);

        // === Nút Hỏi AI (giữ nguyên chức năng cũ) ===
        TextView askAiBtn = findViewById(R.id.ask_ai_btn);
        askAiBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AskQuestionActivity.class)));

        // === Bottom Navigation (giữ nguyên, chỉ setup 1 lần) ===
        NavigationHelper.setupBottomNavigation(this, R.id.nav_home);

        // Featured cards được load trong onResume() để luôn cập nhật khi quay lại Home
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Làm mới nội dung featured cards mỗi khi quay về Home
        loadContinueLearningCard();
        loadDailyQuizCard();
    }

    /**
     * Tải nội dung Card 1 "Continue Learning" từ câu hỏi gần nhất trong DB.
     * Fallback: hiển thị "Bắt đầu học" nếu chưa có lịch sử.
     */
    private void loadContinueLearningCard() {
        TextView tvContinueTitle    = findViewById(R.id.tv_continue_title);
        TextView tvContinueSubtitle = findViewById(R.id.tv_continue_subtitle);
        View cardContinue           = findViewById(R.id.card_continue);

        Question latestQ = dbHelper.getLatestQuestion(1); // userId=1 (mock)

        if (latestQ != null) {
            // Cắt tên câu hỏi cho vừa card nhỏ
            String rawText = latestQ.getContentText();
            String displayTitle = (rawText != null && rawText.length() > 30)
                    ? rawText.substring(0, 27) + "…"
                    : rawText;

            tvContinueTitle.setText(displayTitle);
            tvContinueSubtitle.setText("Bài học gần đây • " + latestQ.getSubjectName());

            final int sessionId = latestQ.getSubjectId();
            cardContinue.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AskQuestionActivity.class);
                intent.putExtra("session_id", sessionId);
                startActivity(intent);
            });
        } else {
            // Fallback — người dùng mới, chưa có lịch sử
            tvContinueTitle.setText("Bắt đầu học");
            tvContinueSubtitle.setText("Hỏi AI câu hỏi đầu tiên");
            cardContinue.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, AskQuestionActivity.class)));
        }
    }

    /**
     * Tải nội dung Card 2 "Daily Quiz" từ quiz ngẫu nhiên trong DB.
     * Fallback: mở QuizActivity với câu hỏi mẫu mặc định nếu chưa có quiz nào.
     */
    private void loadDailyQuizCard() {
        TextView tvQuizTitle    = findViewById(R.id.tv_quiz_title);
        TextView tvQuizSubtitle = findViewById(R.id.tv_quiz_subtitle);
        View cardQuiz           = findViewById(R.id.card_quiz);

        List<QuizQuestion> randomQuiz = dbHelper.getRandomQuiz();

        if (randomQuiz != null && !randomQuiz.isEmpty()) {
            tvQuizTitle.setText("Daily Quiz Challenge");
            tvQuizSubtitle.setText("New Quiz • " + randomQuiz.size() + " câu");

            final List<QuizQuestion> quizToPass = randomQuiz;
            cardQuiz.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                intent.putExtra("quiz_questions", (Serializable) quizToPass);
                startActivity(intent);
            });
        } else {
            // Fallback — chưa có quiz được lưu
            tvQuizTitle.setText("Daily Vocab Challenge");
            tvQuizSubtitle.setText("Thử làm quiz ngay!");
            cardQuiz.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, QuizActivity.class)));
        }
    }
}
