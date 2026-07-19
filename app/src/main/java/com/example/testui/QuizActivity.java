package com.example.testui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testui.models.QuizQuestion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionCount, tvQuestionNumber, tvQuestionText;
    private LinearProgressIndicator overallProgress;
    private LinearLayout mcqOptions;
    private MaterialButton btnCheckAnswer;
    private View btnClose;
    private android.widget.LinearLayout llExplanationBox;
    private TextView tvExplanationContent;
    private androidx.core.widget.NestedScrollView scrollView;

    private List<QuizQuestion> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int selectedOptionIndex = -1;
    private boolean isAnswerChecked = false;
    private long questionId = -1;
    private int[] userAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initViews();
        loadQuestions();
        displayQuestion();
    }

    private void initViews() {
        tvQuestionCount = findViewById(R.id.question_count_label);
        tvQuestionNumber = findViewById(R.id.question_number);
        tvQuestionText = findViewById(R.id.tv_question_text);
        overallProgress = findViewById(R.id.overall_progress);
        mcqOptions = findViewById(R.id.mcq_options);
        btnCheckAnswer = findViewById(R.id.btn_check_answer);
        btnClose = findViewById(R.id.btn_close);
        llExplanationBox = findViewById(R.id.ll_explanation_box);
        tvExplanationContent = findViewById(R.id.tv_explanation_content);
        scrollView = findViewById(R.id.scroll_view);

        btnCheckAnswer.setOnClickListener(v -> onCheckAnswerClicked());
        btnClose.setOnClickListener(v -> finish());
    }

    private void loadQuestions() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("quiz_questions")) {
                questions = (List<QuizQuestion>) intent.getSerializableExtra("quiz_questions");
            }
            questionId = intent.getLongExtra("question_id", -1);
        }
        
        // Fallback for testing if no questions are passed
        if (questions == null || questions.isEmpty()) {
            questions = new ArrayList<>();
            List<String> opts = new ArrayList<>();
            opts.add("Đáp án A"); opts.add("Đáp án B"); opts.add("Đáp án C"); opts.add("Đáp án D");
            questions.add(new QuizQuestion("Đây là câu hỏi kiểm tra giao diện 1?", opts, 0, "Giải thích 1"));
            questions.add(new QuizQuestion("Đây là câu hỏi kiểm tra giao diện 2?", opts, 1, "Giải thích 2"));
        }
        userAnswers = new int[questions.size()];
        java.util.Arrays.fill(userAnswers, -1);
    }

    private void displayQuestion() {
        if (questions == null || questions.isEmpty()) return;
        
        isAnswerChecked = false;
        selectedOptionIndex = -1;
        btnCheckAnswer.setText("Kiểm tra");
        if (llExplanationBox != null) {
            llExplanationBox.setVisibility(View.GONE);
        }

        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);

        tvQuestionCount.setText("Câu " + (currentQuestionIndex + 1) + " / " + questions.size());
        tvQuestionNumber.setText("Câu số: " + (currentQuestionIndex + 1));
        tvQuestionText.setText(currentQuestion.getQuestionText());
        
        int progress = (int) (((currentQuestionIndex) / (float) questions.size()) * 100);
        overallProgress.setProgress(progress);

        mcqOptions.removeAllViews();
        List<String> options = currentQuestion.getOptions();
        for (int i = 0; i < options.size(); i++) {
            TextView optionView = new TextView(this);
            optionView.setText(options.get(i));
            optionView.setTextSize(16);
            optionView.setTextColor(Color.parseColor("#FFFFFF"));
            optionView.setPadding(40, 40, 40, 40);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 24);
            optionView.setLayoutParams(params);
            optionView.setBackgroundResource(R.drawable.bg_quiz_option_default);
            
            final int index = i;
            optionView.setOnClickListener(v -> onOptionSelected(index));
            
            mcqOptions.addView(optionView);
        }
    }

    private void onOptionSelected(int index) {
        if (isAnswerChecked) return;
        
        selectedOptionIndex = index;
        userAnswers[currentQuestionIndex] = index;
        
        for (int i = 0; i < mcqOptions.getChildCount(); i++) {
            View child = mcqOptions.getChildAt(i);
            if (i == index) {
                child.setBackgroundResource(R.drawable.bg_quiz_option_selected);
            } else {
                child.setBackgroundResource(R.drawable.bg_quiz_option_default);
            }
        }
    }

    private void onCheckAnswerClicked() {
        if (!isAnswerChecked) {
            if (selectedOptionIndex == -1) {
                Toast.makeText(this, "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
            boolean isCorrect = (selectedOptionIndex == currentQuestion.getCorrectOptionIndex());
            
            if (isCorrect) {
                score++;
                mcqOptions.getChildAt(selectedOptionIndex).setBackgroundResource(R.drawable.bg_quiz_option_correct);
            } else {
                mcqOptions.getChildAt(selectedOptionIndex).setBackgroundResource(R.drawable.bg_quiz_option_incorrect);
                mcqOptions.getChildAt(currentQuestion.getCorrectOptionIndex()).setBackgroundResource(R.drawable.bg_quiz_option_correct);
            }
            
            isAnswerChecked = true;

            // Hiển thị phần giải thích đáp án
            if (llExplanationBox != null && tvExplanationContent != null) {
                tvExplanationContent.setText(currentQuestion.getFeedback());
                llExplanationBox.setVisibility(View.VISIBLE);

                // Tự động cuộn xuống dưới cùng để dễ xem giải thích
                if (scrollView != null) {
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                }
            }
            
            if (currentQuestionIndex == questions.size() - 1) {
                btnCheckAnswer.setText("Xem kết quả");
            } else {
                btnCheckAnswer.setText("Câu tiếp theo");
            }
            
        } else {
            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
                displayQuestion();
            } else {
                // Lưu kết quả điểm số vào CSV database nếu có liên kết ID
                if (questionId != -1) {
                    com.example.testui.database.DatabaseHelper dbHelper = new com.example.testui.database.DatabaseHelper(this);
                    dbHelper.completeQuiz(questionId, score);
                }

                Intent intent = new Intent(QuizActivity.this, QuizResultActivity.class);
                intent.putExtra("score", score);
                intent.putExtra("total", questions.size());
                intent.putExtra("quiz_questions", (Serializable) questions);
                intent.putExtra("user_answers", userAnswers);
                startActivity(intent);
                finish();
            }
        }
    }
}