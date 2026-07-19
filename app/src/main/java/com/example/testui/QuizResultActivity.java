package com.example.testui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.QuizQuestion;
import com.example.testui.models.UserProfile;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizResultActivity — Màn hình kết quả làm bài trắc nghiệm.
 * - Hiển thị điểm số dạng biểu đồ tròn.
 * - Đồng bộ động các số câu đúng/sai, xếp loại học lực, và các nhãn bộ lọc.
 * - AI Insights động thời gian thực: Gửi kết quả thi chi tiết lên Gemini API để AI phân tích học lực.
 * - Hiển thị chi tiết từng câu hỏi đúng/sai thực tế.
 * - Hỗ trợ nạp dữ liệu fallback từ CSV database để chống lỗi màn hình trống.
 */
public class QuizResultActivity extends AppCompatActivity {

    private List<QuizQuestion> questions = new ArrayList<>();
    private int[] userAnswers;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        dbHelper = new DatabaseHelper(this);

        // 1. Nhận dữ liệu bài làm thực tế từ Intent
        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 10);
        if (total == 0) total = 1;

        long questionId = -1;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("quiz_questions")) {
                questions = (List<QuizQuestion>) intent.getSerializableExtra("quiz_questions");
            }
            userAnswers = intent.getIntArrayExtra("user_answers");
            questionId = intent.getLongExtra("question_id", -1);
        }

        // --- Cơ chế Fallback dự phòng: Nếu dữ liệu trống, tự động nạp từ database ---
        if ((questions == null || questions.isEmpty()) && questionId != -1) {
            questions = dbHelper.getQuizForQuestion(questionId);
            total = questions.size();
        }
        if (userAnswers == null || userAnswers.length < questions.size()) {
            userAnswers = new int[questions.size()];
            // Mock kết quả để tránh màn hình trống (coi như chưa trả lời -1)
            java.util.Arrays.fill(userAnswers, -1);
        }

        // 2. Hiển thị điểm số trên biểu đồ tiến trình tròn
        TextView tvScoreText = findViewById(R.id.tv_score_text);
        TextView tvScorePercent = findViewById(R.id.tv_score_percent);
        ProgressBar scoreCircle = findViewById(R.id.score_circle);
        
        tvScoreText.setText(score + "/" + total);
        int percent = (int) (((float) score / total) * 100);
        tvScorePercent.setText(percent + "%");
        scoreCircle.setProgress(percent);

        // 3. Cập nhật động lời chúc mừng & xếp loại
        TextView tvResultMsg = findViewById(R.id.tv_result_msg);
        if (tvResultMsg != null) {
            if (percent >= 80) {
                tvResultMsg.setText("Xuất sắc! Bạn đã hoàn thành xuất sắc bài quiz!");
            } else if (percent >= 50) {
                tvResultMsg.setText("Chúc mừng! Bạn đã vượt qua bài quiz!");
            } else {
                tvResultMsg.setText("Cố gắng lên! Hãy ôn tập lại để đạt kết quả tốt hơn nhé!");
            }
        }

        TextView tvGradeBadge = findViewById(R.id.tv_grade_badge);
        if (tvGradeBadge != null) {
            String grade;
            if (percent >= 90) grade = "Xếp loại: Xuất sắc";
            else if (percent >= 80) grade = "Xếp loại: Giỏi";
            else if (percent >= 65) grade = "Xếp loại: Khá";
            else if (percent >= 50) grade = "Xếp loại: Trung bình";
            else grade = "Xếp loại: Yếu";
            tvGradeBadge.setText(grade);
        }

        // 4. Cập nhật động số câu Đúng/Sai trên Stats Row
        TextView tvCorrectCount = findViewById(R.id.tv_correct_count);
        TextView tvIncorrectCount = findViewById(R.id.tv_incorrect_count);
        if (tvCorrectCount != null) {
            tvCorrectCount.setText(String.valueOf(score));
        }
        if (tvIncorrectCount != null) {
            tvIncorrectCount.setText(String.valueOf(total - score));
        }

        // 5. Cập nhật động số lượng trên các tab Bộ lọc
        TextView tvFilterAll = findViewById(R.id.tv_filter_all);
        TextView tvFilterCorrect = findViewById(R.id.tv_filter_correct);
        TextView tvFilterIncorrect = findViewById(R.id.tv_filter_incorrect);
        if (tvFilterAll != null) {
            tvFilterAll.setText("Tất cả (" + total + ")");
        }
        if (tvFilterCorrect != null) {
            tvFilterCorrect.setText("Đúng (" + score + ")");
        }
        if (tvFilterIncorrect != null) {
            tvFilterIncorrect.setText("Sai (" + (total - score) + ")");
        }

        // 6. Kích hoạt AI Insights động thời gian thực gửi lên Gemini API
        TextView tvAiRecommendText = findViewById(R.id.ai_recommend_text);
        if (tvAiRecommendText != null && questions != null && !questions.isEmpty()) {
            tvAiRecommendText.setText("EduAI đang phân tích kết quả bài làm của bạn...");
            
            // Tìm môn học của Quiz
            String subjectName = "Toán học";
            if (questionId != -1) {
                File qFile = new File(getFilesDir(), "questions.csv");
                List<String[]> csvQuestions = dbHelper.readCsv(qFile);
                for (String[] qRow : csvQuestions) {
                    if (qRow.length >= 3 && qRow[0].equals(String.valueOf(questionId))) {
                        try {
                            int sId = Integer.parseInt(qRow[2]);
                            switch (sId) {
                                case 1: subjectName = "Toán học"; break;
                                case 2: subjectName = "Vật lý"; break;
                                case 3: subjectName = "Hóa học"; break;
                                case 4: subjectName = "Lập trình"; break;
                                case 5: subjectName = "Lịch sử"; break;
                            }
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                }
            }

            // Đọc thông tin cấp học của user để gửi làm ngữ cảnh
            String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
            String educationLevel = "Not specified";
            if (email != null) {
                UserProfile profile = dbHelper.getUserProfile(email);
                if (profile != null && profile.getEducationLevel() != null) {
                    educationLevel = profile.getEducationLevel();
                }
            }

            // Gom chi tiết bài làm
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                sb.append("Câu ").append(i + 1).append(": ").append(q.getQuestionText()).append("\n");
                sb.append("- Đáp án đúng: ").append(q.getOptions().get(q.getCorrectOptionIndex())).append("\n");
                if (i < userAnswers.length && userAnswers[i] >= 0 && userAnswers[i] < q.getOptions().size()) {
                    sb.append("- Học sinh chọn: ").append(q.getOptions().get(userAnswers[i])).append("\n");
                } else {
                    sb.append("- Học sinh bỏ trống.\n");
                }
                boolean isAnsCorrect = (i < userAnswers.length && userAnswers[i] == q.getCorrectOptionIndex());
                sb.append("- Kết quả: ").append(isAnsCorrect ? "ĐÚNG" : "SAI").append("\n\n");
            }

            // Gọi AI phân tích và đưa ra lời khuyên học tập động
            AIObj.getInstance().generateAIInsights(educationLevel, subjectName, sb.toString(), new AIObj.AICallback<String>() {
                @Override
                public void onSuccess(String result) {
                    tvAiRecommendText.setText(result);
                }

                @Override
                public void onError(String error) {
                    // Fallback nếu lỗi API (hiển thị khuyên dùng ở local)
                    tvAiRecommendText.setText("EduAI đề xuất: Hãy tập trung xem lại các câu trả lời sai ở phần dưới và bấm Làm lại (Retake) để củng cố vững chắc lý thuyết nhé!");
                }
            });
        }

        // 7. Đổ dữ liệu động xem lại chi tiết bài làm
        LinearLayout reviewContainer = findViewById(R.id.ll_review_container);
        if (reviewContainer != null && questions != null && !questions.isEmpty()) {
            reviewContainer.removeAllViews();
            for (int i = 0; i < questions.size(); i++) {
                View card = createQuestionReviewCard(i, questions.get(i), userAnswers[i]);
                reviewContainer.addView(card);
            }
        }

        // 8. Các nút điều hướng
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            Intent mainIntent = new Intent(QuizResultActivity.this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            finish();
        });
        
        final long finalQuestionId = questionId;
        findViewById(R.id.btn_retake).setOnClickListener(v -> {
            // Quay lại màn hình QuizActivity để làm lại
            if (questions != null && !questions.isEmpty()) {
                Intent retakeIntent = new Intent(QuizResultActivity.this, QuizActivity.class);
                retakeIntent.putExtra("quiz_questions", (Serializable) questions);
                retakeIntent.putExtra("question_id", finalQuestionId);
                startActivity(retakeIntent);
            }
            finish();
        });
    }

    // =====================================================================
    // Dynamic View Creation (No Overlapping, Premium Design Layout)
    // =====================================================================

    private View createQuestionReviewCard(int index, QuizQuestion question, int userAnswerIndex) {
        boolean isCorrect = (userAnswerIndex == question.getCorrectOptionIndex());

        // Card container chính
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        card.setBackgroundResource(R.drawable.bg_glass_panel);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        card.setLayoutParams(cardParams);

        // --- Hàng Header (Icon + Tên câu + Tag đúng/sai) ---
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerRow.setLayoutParams(headerParams);

        // Icon trạng thái
        ImageView ivStatus = new ImageView(this);
        ivStatus.setImageResource(isCorrect ? R.drawable.ic_check_circle : R.drawable.ic_close);
        ivStatus.setColorFilter(ContextCompat.getColor(this, isCorrect ? R.color.quiz_success : R.color.quiz_danger));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
        ivStatus.setLayoutParams(iconParams);
        headerRow.addView(ivStatus);

        // Tiêu đề: "Câu số X"
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Câu số: " + (index + 1));
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        titleParams.setMarginStart(dpToPx(12));
        tvTitle.setLayoutParams(titleParams);
        headerRow.addView(tvTitle);

        // Tag trạng thái: "Đúng" / "Sai"
        TextView tvBadge = new TextView(this);
        tvBadge.setText(isCorrect ? "Đúng" : "Sai");
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvBadge.setTextColor(ContextCompat.getColor(this, isCorrect ? R.color.quiz_success : R.color.quiz_danger));
        tvBadge.setBackgroundResource(R.drawable.bg_chip);
        tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, isCorrect ? R.color.home_card_tag : R.color.home_icon_quiz));
        tvBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        headerRow.addView(tvBadge);

        card.addView(headerRow);

        // --- Nội dung câu hỏi ---
        TextView tvQuestionText = new TextView(this);
        tvQuestionText.setText(question.getQuestionText());
        tvQuestionText.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));
        tvQuestionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvQuestionText.setTypeface(null, Typeface.BOLD);
        tvQuestionText.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()), 1.0f);
        LinearLayout.LayoutParams qTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        qTextParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
        tvQuestionText.setLayoutParams(qTextParams);
        card.addView(tvQuestionText);

        // --- Câu trả lời của người dùng ---
        TextView tvUserChoice = new TextView(this);
        String userAnsText = "Bạn chưa chọn đáp án";
        if (userAnswerIndex >= 0 && userAnswerIndex < question.getOptions().size()) {
            userAnsText = "Bạn chọn: " + question.getOptions().get(userAnswerIndex);
        }
        tvUserChoice.setText(userAnsText);
        tvUserChoice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvUserChoice.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        tvUserChoice.setBackgroundResource(isCorrect ? R.drawable.bg_quiz_option_correct : R.drawable.bg_quiz_option_incorrect);
        tvUserChoice.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));
        
        LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        choiceParams.setMargins(0, 0, 0, dpToPx(8));
        tvUserChoice.setLayoutParams(choiceParams);
        card.addView(tvUserChoice);

        // --- Đáp án đúng thực tế (nếu chọn sai) ---
        if (!isCorrect) {
            TextView tvCorrectChoice = new TextView(this);
            String correctAnsText = "Đáp án đúng: " + question.getOptions().get(question.getCorrectOptionIndex());
            tvCorrectChoice.setText(correctAnsText);
            tvCorrectChoice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvCorrectChoice.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
            tvCorrectChoice.setBackgroundResource(R.drawable.bg_quiz_option_correct);
            tvCorrectChoice.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_primary));

            LinearLayout.LayoutParams correctParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            correctParams.setMargins(0, 0, 0, dpToPx(12));
            tvCorrectChoice.setLayoutParams(correctParams);
            card.addView(tvCorrectChoice);
        }

        // --- Giải thích tại sao đúng ---
        LinearLayout expLayout = new LinearLayout(this);
        expLayout.setOrientation(LinearLayout.VERTICAL);
        expLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        expLayout.setBackgroundResource(R.drawable.bg_glass_panel_elevated);
        expLayout.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.quiz_surface));

        LinearLayout.LayoutParams expParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        expLayout.setLayoutParams(expParams);

        TextView tvExpHeader = new TextView(this);
        tvExpHeader.setText("Giải thích tại sao đúng:");
        tvExpHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvExpHeader.setTextColor(ContextCompat.getColor(this, R.color.quiz_primary));
        tvExpHeader.setTypeface(null, Typeface.BOLD);
        tvExpHeader.setPadding(0, 0, 0, dpToPx(6));
        expLayout.addView(tvExpHeader);

        TextView tvExpContent = new TextView(this);
        tvExpContent.setText(question.getFeedback());
        tvExpContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvExpContent.setTextColor(ContextCompat.getColor(this, R.color.quiz_text_secondary));
        tvExpContent.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()), 1.0f);
        expLayout.addView(tvExpContent);

        card.addView(expLayout);

        return card;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}