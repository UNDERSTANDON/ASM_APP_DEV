package com.example.testui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.QuizQuestion;
import com.example.testui.models.QuizSummary;
import com.example.testui.models.UserProfile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * QuizLabActivity — Màn hình quản lý và thực hành trắc nghiệm (Quiz Lab).
 * - Sử dụng AI để sinh trực tiếp 10 câu hỏi ngẫu nhiên theo môn học.
 * - Hiển thị danh sách trắc nghiệm phân loại theo ngày (Hôm nay, Hôm qua, Trước đây).
 * - Hiển thị trạng thái bài làm (Đã làm kèm điểm số hoặc Chưa làm).
 */
public class QuizLabActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout quizListContainer;
    private TextView tvSelectedSubject;

    // Lọc theo subjectId: null = Tất cả, 1-5 = môn cụ thể
    private Integer selectedSubjectId = null;

    private static final String[] SUBJECT_NAMES = {
            "Tất cả", "Toán học", "Vật lý", "Hóa học", "Lập trình", "Lịch sử"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_lab);

        dbHelper = new DatabaseHelper(this);
        quizListContainer = findViewById(R.id.quiz_list_container);
        tvSelectedSubject = findViewById(R.id.tv_selected_subject);

        // Nút "Tạo Câu Hỏi" → Sử dụng AI sinh câu hỏi ngẫu nhiên trực tiếp
        TextView btnCreateQuiz = findViewById(R.id.btn_create_quiz);
        btnCreateQuiz.setOnClickListener(v -> createQuizByAi());

        // Lọc môn học
        LinearLayout subjectSelectorRow = findViewById(R.id.subject_selector_row);
        subjectSelectorRow.setOnClickListener(v -> showSubjectDialog());

        // Bottom Navigation
        NavigationHelper.setupBottomNavigation(this, R.id.nav_quiz);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQuizList();
    }

    // =====================================================================
    // AI Quiz Generation Flow
    // =====================================================================

    private void createQuizByAi() {
        // 1. Xác định môn học để tạo trắc nghiệm
        String subjectToGenerate;
        int targetSubjectId;

        if (selectedSubjectId == null) {
            // Chưa chọn môn cụ thể -> Lấy môn học yêu thích trong profile làm mặc định
            String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
            UserProfile profile = email != null ? dbHelper.getUserProfile(email) : null;
            subjectToGenerate = profile != null ? profile.getFavoriteSubject() : "Toán học";
            targetSubjectId = mapSubjectNameToId(subjectToGenerate);
        } else {
            subjectToGenerate = SUBJECT_NAMES[selectedSubjectId];
            targetSubjectId = selectedSubjectId;
        }

        // 2. Xác định danh sách lớp học dựa trên cấp học của người dùng
        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        UserProfile profile = email != null ? dbHelper.getUserProfile(email) : null;
        String educationLevel = profile != null ? profile.getEducationLevel() : "THPT";

        final String[] grades;
        if ("THCS".equalsIgnoreCase(educationLevel)) {
            grades = new String[]{"Lớp 6", "Lớp 7", "Lớp 8", "Lớp 9"};
        } else if ("THPT".equalsIgnoreCase(educationLevel)) {
            grades = new String[]{"Lớp 10", "Lớp 11", "Lớp 12"};
        } else if ("Đại học".equalsIgnoreCase(educationLevel)) {
            grades = new String[]{"Năm thứ 1", "Năm thứ 2", "Năm thứ 3", "Năm thứ 4"};
        } else {
            grades = new String[]{"Lớp 6", "Lớp 7", "Lớp 8", "Lớp 9", "Lớp 10", "Lớp 11", "Lớp 12"};
        }

        // 3. Hiển thị Dialog chọn Lớp học
        AlertDialog.Builder gradeBuilder = new AlertDialog.Builder(this);
        gradeBuilder.setTitle("Thiết lập Quiz: Chọn Lớp học");
        final int[] selectedGradeIdx = {0};
        gradeBuilder.setSingleChoiceItems(grades, 0, (dialog, which) -> selectedGradeIdx[0] = which);

        final String finalSubjectToGenerate = subjectToGenerate;
        final int finalTargetSubjectId = targetSubjectId;

        gradeBuilder.setPositiveButton("Tiếp tục", (dialog, which) -> {
            String selectedGrade = grades[selectedGradeIdx[0]];
            showSemesterDialog(finalSubjectToGenerate, finalTargetSubjectId, selectedGrade);
        });
        gradeBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        gradeBuilder.show();
    }

    private void showSemesterDialog(String subject, int subjectId, String grade) {
        final String[] semesters = {"Kì 1", "Kì 2"};
        AlertDialog.Builder semBuilder = new AlertDialog.Builder(this);
        semBuilder.setTitle("Thiết lập Quiz: Chọn Kì học");
        final int[] selectedSemIdx = {0};
        semBuilder.setSingleChoiceItems(semesters, 0, (dialog, which) -> selectedSemIdx[0] = which);

        semBuilder.setPositiveButton("Bắt đầu tạo bài", (dialog, which) -> {
            String selectedSem = semesters[selectedSemIdx[0]];
            startTargetedQuizGeneration(subject, subjectId, grade, selectedSem);
        });
        semBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        semBuilder.show();
    }

    private void startTargetedQuizGeneration(String subject, int subjectId, String grade, String semester) {
        // Hiển thị Dialog Loading mượt mà
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("EduAI Quiz Lab");
        progressDialog.setMessage("Đang sử dụng AI để tạo 10 câu hỏi trắc nghiệm môn " + subject + " - " + grade + " - " + semester + " bám sát chương trình học...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Gọi AI Specialist tạo trắc nghiệm đúng mục tiêu
        AIObj.getInstance().generateTargetedQuiz(subject, grade, semester, new AIObj.AICallback<List<QuizQuestion>>() {
            @Override
            public void onSuccess(List<QuizQuestion> result) {
                progressDialog.dismiss();
                if (result == null || result.isEmpty()) {
                    Toast.makeText(QuizLabActivity.this, "AI không trả về câu hỏi nào. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Lưu tiêu đề câu hỏi chi tiết: Luyện tập [Môn] - [Lớp] - [Kì]
                int sessionId = (int) (System.currentTimeMillis() / 1000);
                String quizTitle = "Luyện tập " + subject + " - " + grade + " - " + semester;
                long questionId = dbHelper.saveQuestion(quizTitle, subjectId, sessionId);

                // Lưu 10 câu trắc nghiệm vào quizzes.csv
                dbHelper.saveQuiz(questionId, result);

                // Chuyển thẳng sang làm bài
                Intent intent = new Intent(QuizLabActivity.this, QuizActivity.class);
                intent.putExtra("quiz_questions", (Serializable) result);
                intent.putExtra("question_id", questionId);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(QuizLabActivity.this, "Tạo trắc nghiệm thất bại: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int mapSubjectNameToId(String subjectName) {
        if (subjectName == null) return 1;
        switch (subjectName) {
            case "Toán học": return 1;
            case "Vật lý":   return 2;
            case "Hóa học":  return 3;
            case "Lập trình": return 4;
            case "Lịch sử":  return 5;
            default:         return 1;
        }
    }

    private void showSubjectDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chọn môn học")
                .setItems(SUBJECT_NAMES, (dialog, which) -> {
                    if (which == 0) {
                        selectedSubjectId = null;
                        tvSelectedSubject.setText("Chọn Môn Học");
                    } else {
                        selectedSubjectId = which;
                        tvSelectedSubject.setText(SUBJECT_NAMES[which]);
                    }
                    loadQuizList();
                })
                .show();
    }

    // =====================================================================
    // Load and Categorize Quiz List by Date (No Overlapping UI)
    // =====================================================================

    private void loadQuizList() {
        quizListContainer.removeAllViews();

        List<QuizSummary> allSummaries = dbHelper.getAllQuizSummaries();

        // Lọc theo bộ lọc môn học
        List<QuizSummary> filtered = new ArrayList<>();
        for (QuizSummary s : allSummaries) {
            if (selectedSubjectId == null || s.getSubjectId() == selectedSubjectId) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState();
            return;
        }

        // Gom nhóm theo ngày
        List<QuizSummary> todayList = new ArrayList<>();
        List<QuizSummary> yesterdayList = new ArrayList<>();
        List<QuizSummary> olderList = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (QuizSummary s : filtered) {
            long diffMs = now - s.getCreatedAt();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (s.getCreatedAt() == 0) {
                olderList.add(s);
            } else if (diffDays == 0) {
                todayList.add(s);
            } else if (diffDays == 1) {
                yesterdayList.add(s);
            } else {
                olderList.add(s);
            }
        }

        // Inflate các nhóm lên giao diện
        if (!todayList.isEmpty()) {
            addDateHeader("Hôm nay");
            for (QuizSummary s : todayList) inflateQuizCard(s);
        }

        if (!yesterdayList.isEmpty()) {
            addDateHeader("Hôm qua");
            for (QuizSummary s : yesterdayList) inflateQuizCard(s);
        }

        if (!olderList.isEmpty()) {
            addDateHeader("Trước đây");
            for (QuizSummary s : olderList) inflateQuizCard(s);
        }
    }

    private void addDateHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(ContextCompat.getColor(this, R.color.home_subtitle));
        header.setTextSize(13);
        header.setLetterSpacing(0.05f);
        header.setAllCaps(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // Thiết lập khoảng cách thoáng đãng, tránh đè nén giao diện
        params.setMargins(4, 24, 0, 12);
        header.setLayoutParams(params);

        quizListContainer.addView(header);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private void inflateQuizCard(QuizSummary summary) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_quiz_lab_card, quizListContainer, false);

        ImageView ivIcon = cardView.findViewById(R.id.iv_subject_icon);
        TextView tvTitle = cardView.findViewById(R.id.tv_quiz_item_title);
        TextView tvCount = cardView.findViewById(R.id.tv_quiz_item_count);
        TextView tvDate = cardView.findViewById(R.id.tv_quiz_item_date);

        // Gán nội dung
        tvTitle.setText(summary.getTitle());
        tvCount.setText(summary.getQuestionCount() + " Câu Hỏi");

        // Thiết lập tag điểm số trạng thái làm bài (Đã làm hoặc Chưa làm)
        if (summary.isCompleted()) {
            tvDate.setText("Đã làm: " + summary.getScore() + "/" + summary.getQuestionCount());
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.quiz_success)); // Màu xanh lá sáng
        } else {
            tvDate.setText("Chưa làm");
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.ql_date_text)); // Màu xanh da trời sáng
        }

        // Set icon môn học
        setSubjectIcon(ivIcon, summary.getSubjectId());

        // Click làm bài
        cardView.setOnClickListener(v -> openQuiz(summary));

        // Long press để xóa
        cardView.setOnLongClickListener(v -> {
            confirmDeleteQuiz(summary);
            return true;
        });

        quizListContainer.addView(cardView);
    }

    private void setSubjectIcon(ImageView ivIcon, int subjectId) {
        int iconRes;
        int colorRes;
        switch (subjectId) {
            case 1:
                iconRes = R.drawable.ic_calculate;
                colorRes = R.color.home_card_tag;
                break;
            case 2:
                iconRes = R.drawable.ic_atom;
                colorRes = R.color.home_icon_quiz;
                break;
            case 3:
                iconRes = R.drawable.ic_science;
                colorRes = R.color.quiz_success;
                break;
            case 4:
                iconRes = R.drawable.ic_database;
                colorRes = R.color.quiz_warning;
                break;
            case 5:
                iconRes = R.drawable.ic_history_edu;
                colorRes = R.color.streak_active;
                break;
            default:
                iconRes = R.drawable.ic_school;
                colorRes = R.color.ql_count_text;
                break;
        }
        ivIcon.setImageResource(iconRes);
        ivIcon.setColorFilter(ContextCompat.getColor(this, colorRes));
    }

    private void openQuiz(QuizSummary summary) {
        List<QuizQuestion> questions = dbHelper.getQuizForQuestion(summary.getQuestionId());
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "Quiz không có câu hỏi nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (summary.isCompleted()) {
            // Đã làm -> Mở thẳng xem lại kết quả
            Intent intent = new Intent(QuizLabActivity.this, QuizResultActivity.class);
            intent.putExtra("score", summary.getScore());
            intent.putExtra("total", summary.getQuestionCount());
            intent.putExtra("quiz_questions", (Serializable) questions);
            intent.putExtra("question_id", summary.getQuestionId());
            // Tạo mảng userAnswers giả lập để nạp review khi xem lại lịch sử
            int[] answers = new int[questions.size()];
            for (int i = 0; i < questions.size(); i++) {
                answers[i] = questions.get(i).getCorrectOptionIndex(); // Mặc định hiển thị đúng
            }
            intent.putExtra("user_answers", answers);
            startActivity(intent);
        } else {
            // Chưa làm -> Mở QuizActivity để làm bài
            Intent intent = new Intent(QuizLabActivity.this, QuizActivity.class);
            intent.putExtra("quiz_questions", (Serializable) questions);
            intent.putExtra("question_id", summary.getQuestionId());
            startActivity(intent);
        }
    }

    private void confirmDeleteQuiz(QuizSummary summary) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Quiz")
                .setMessage("Bạn có chắc muốn xóa bài trắc nghiệm này không?\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteQuiz(summary.getQuizId());
                    Toast.makeText(this, "Đã xóa bài trắc nghiệm", Toast.LENGTH_SHORT).show();
                    loadQuizList();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEmptyState() {
        TextView emptyText = new TextView(this);
        emptyText.setText("Chưa có bài trắc nghiệm nào.\nNhấn \"Tạo Câu Hỏi\" để bắt đầu sinh bằng AI!");
        emptyText.setTextColor(ContextCompat.getColor(this, R.color.home_subtitle));
        emptyText.setTextSize(14f);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyText.setPadding(0, 48, 0, 0);
        quizListContainer.addView(emptyText);
    }
}
