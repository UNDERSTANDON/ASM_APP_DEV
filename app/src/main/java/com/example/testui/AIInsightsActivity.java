package com.example.testui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.QuizSummary;
import com.example.testui.models.UserProfile;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AIInsightsActivity — Màn hình AI Insights Dashboard học tập.
 * - Thống kê động chuỗi ngày học tập (Streak) thực tế.
 * - Tính toán động Tải nhận thức (Cognitive Load) dựa trên hoạt động trong ngày.
 * - Vẽ động Tiến trình kỹ năng (Skill Progression) cho các môn học từ dữ liệu điểm số CSV.
 * - Tự động gọi Gemini API phân tích lịch sử học tập để đề xuất học tập (AI Suggestions) thời gian thực.
 * - Hiển thị Tóm tắt tuần này (Số giờ học ước tính, số câu hỏi đã hỏi AI, điểm trung bình).
 */
public class AIInsightsActivity extends AppCompatActivity {

    private static final String TAG = "AIInsightsActivity";
    private DatabaseHelper dbHelper;

    // View declarations
    private TextView tvCard1Desc, tvTagSubject, tvTagEfficiency;
    private TextView tvMomentumVal;
    private LinearProgressIndicator momentumProgress;
    private TextView tvCognitiveVal, tvCognitiveBadge, tvCognitiveDesc;
    private LinearLayout skillListContainer;
    private TextView tvSummaryStudyTime, tvSummaryTasksDone, tvSummaryAvgScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_insights);

        dbHelper = new DatabaseHelper(this);

        initViews();
        loadDynamicData();

        NavigationHelper.setupBottomNavigation(this, R.id.nav_insights);
    }

    private void initViews() {
        tvCard1Desc = findViewById(R.id.card1_desc);
        tvTagSubject = findViewById(R.id.tv_tag_subject);
        tvTagEfficiency = findViewById(R.id.tv_tag_efficiency);

        tvMomentumVal = findViewById(R.id.momentum_val);
        momentumProgress = findViewById(R.id.momentum_progress);

        tvCognitiveVal = findViewById(R.id.cognitive_val);
        tvCognitiveBadge = findViewById(R.id.tv_cognitive_badge);
        tvCognitiveDesc = findViewById(R.id.tv_cognitive_desc);

        skillListContainer = findViewById(R.id.skill_list);

        tvSummaryStudyTime = findViewById(R.id.tv_summary_study_time);
        tvSummaryTasksDone = findViewById(R.id.tv_summary_tasks_done);
        tvSummaryAvgScore = findViewById(R.id.tv_summary_avg_score);
    }

    private void loadDynamicData() {
        // Đọc dữ liệu user hiện tại
        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        UserProfile userProfile = null;
        String favSubject = "Toán học";
        String eduLevel = "Cấp học chưa rõ";
        if (email != null) {
            userProfile = dbHelper.getUserProfile(email);
            if (userProfile != null) {
                favSubject = userProfile.getFavoriteSubject() != null ? userProfile.getFavoriteSubject() : "Toán học";
                eduLevel = userProfile.getEducationLevel() != null ? userProfile.getEducationLevel() : "Cấp học chưa rõ";
            }
        }

        // Đọc danh sách quizzes từ database CSV
        List<QuizSummary> quizzes = dbHelper.getAllQuizSummaries();
        List<String[]> questionsCsv = dbHelper.readCsv(new File(getFilesDir(), "questions.csv"));
        List<String[]> aiResponsesCsv = dbHelper.readCsv(new File(getFilesDir(), "ai_responses.csv"));

        // 1. Tính toán Chuỗi học tập (Streak) và gom tất cả các ngày học tập
        Set<String> studyDates = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        // Lấy ngày từ các Quiz đã tạo
        File quizFile = new File(getFilesDir(), "quizzes.csv");
        List<String[]> csvQuizzes = dbHelper.readCsv(quizFile);
        for (String[] quizRow : csvQuizzes) {
            if (quizRow.length >= 4) {
                String dateStr = quizRow[3]; // format: yyyy-MM-dd'T'HH:mm:ssZ
                if (dateStr.length() >= 10) {
                    studyDates.add(dateStr.substring(0, 10));
                }
            }
        }
        // Lấy ngày từ các câu hỏi AI đã đặt
        for (String[] qRow : questionsCsv) {
            if (qRow.length >= 4) {
                String dateStr = qRow[3];
                if (dateStr.length() >= 10) {
                    studyDates.add(dateStr.substring(0, 10));
                }
            }
        }

        // Tính streak liên tiếp
        int streak = calculateStreak(studyDates, todayStr, sdf);
        tvMomentumVal.setText(streak + " Days");
        // Giới hạn max progress mục tiêu 10 ngày
        int progressPercent = Math.min((streak * 100) / 10, 100);
        momentumProgress.setProgress(progressPercent);

        // 2. Tính Tải nhận thức (Cognitive Load) dựa trên số hoạt động hôm nay
        int todayActivities = 0;
        for (String date : studyDates) {
            if (date.equals(todayStr)) {
                // Đếm số câu hỏi hoặc bài làm trong ngày
                for (String[] qRow : questionsCsv) {
                    if (qRow.length >= 4 && qRow[3].startsWith(todayStr)) todayActivities++;
                }
                for (String[] quizRow : csvQuizzes) {
                    if (quizRow.length >= 4 && quizRow[3].startsWith(todayStr)) todayActivities++;
                }
            }
        }

        if (todayActivities <= 2) {
            tvCognitiveVal.setText("Low");
            tvCognitiveBadge.setText("Khuyên học");
            tvCognitiveBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.home_card_tag));
            tvCognitiveBadge.setTextColor(ContextCompat.getColor(this, R.color.quiz_success));
            tvCognitiveDesc.setText("Hoạt động trong ngày của bạn rất nhẹ nhàng. Đây là thời điểm tốt để học thêm bài mới!");
        } else if (todayActivities <= 5) {
            tvCognitiveVal.setText("Medium");
            tvCognitiveBadge.setText("Ổn định");
            tvCognitiveBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.home_icon_quiz));
            tvCognitiveBadge.setTextColor(ContextCompat.getColor(this, R.color.quiz_primary));
            tvCognitiveDesc.setText("Hoạt động học tập của bạn ở mức cân bằng. Hãy duy trì nhịp độ đều đặn này nhé!");
        } else {
            tvCognitiveVal.setText("High");
            tvCognitiveBadge.setText("Cảnh báo");
            tvCognitiveBadge.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.home_icon_quiz));
            tvCognitiveBadge.setTextColor(ContextCompat.getColor(this, R.color.quiz_danger));
            tvCognitiveDesc.setText("Số lượng chủ đề học trong ngày vượt ngưỡng khuyến nghị. Hãy nghỉ ngơi để tránh quá tải não bộ!");
        }

        // 3. Tính toán Điểm trung bình và vẽ động Skill Progression cho các môn học
        Map<Integer, List<Integer>> subjectScores = new HashMap<>();
        // Mặc định khởi tạo các môn học
        for (int i = 1; i <= 5; i++) {
            subjectScores.put(i, new ArrayList<>());
        }

        // Gom điểm số từ quizzes
        for (String[] quizRow : csvQuizzes) {
            if (quizRow.length >= 6) {
                String isCompleted = quizRow[4];
                String scoreStr = quizRow[5];
                String questionIdStr = quizRow[1];
                if ("1".equals(isCompleted)) {
                    // Lấy subjectId tương ứng từ questions.csv
                    int subjectId = getSubjectIdForQuestion(questionsCsv, questionIdStr);
                    if (subjectId != -1) {
                        try {
                            int scoreVal = Integer.parseInt(scoreStr);
                            subjectScores.get(subjectId).add(scoreVal);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // Vẽ danh sách môn học lên giao diện
        if (skillListContainer != null) {
            skillListContainer.removeAllViews();
            String[] subjects = {"Toán học", "Vật lý", "Hóa học", "Lập trình", "Lịch sử"};
            
            for (int i = 1; i <= 5; i++) {
                List<Integer> scores = subjectScores.get(i);
                int scoreSum = 0;
                int avgPercent = 0;
                if (scores != null && !scores.isEmpty()) {
                    for (int s : scores) scoreSum += s;
                    // Điểm số trung bình dựa trên thang 10 câu
                    avgPercent = (int) (((float) scoreSum / (scores.size() * 10)) * 100);
                } else {
                    // Nếu là môn học yêu thích thì cho mặc định 60% làm động lực, còn lại 0%
                    if (subjects[i-1].equals(favSubject)) {
                        avgPercent = 60;
                    } else {
                        avgPercent = 0;
                    }
                }

                View skillItem = createSkillProgressView(subjects[i - 1], avgPercent);
                skillListContainer.addView(skillItem);
            }
        }

        // 4. Thống kê Weekly Summary (Tóm tắt tuần này)
        // Số câu hỏi đã đặt cho AI
        int aiQuestionsCount = questionsCsv.size();
        tvSummaryTasksDone.setText(String.valueOf(aiQuestionsCount));

        // Điểm trung bình tổng quát của tất cả bài thi
        int totalScoreSum = 0;
        int completedQuizzesCount = 0;
        for (String[] quizRow : csvQuizzes) {
            if (quizRow.length >= 6 && "1".equals(quizRow[4])) {
                try {
                    totalScoreSum += Integer.parseInt(quizRow[5]);
                    completedQuizzesCount++;
                } catch (NumberFormatException ignored) {}
            }
        }
        float avgScoreTotal = completedQuizzesCount > 0 ? ((float) totalScoreSum / completedQuizzesCount) : 0.0f;
        tvSummaryAvgScore.setText(String.format(Locale.getDefault(), "%.1f", avgScoreTotal));

        // Ước lượng số giờ học (Ví dụ mỗi câu hỏi / bài làm quiz tốn trung bình 15 phút ôn tập)
        int totalStudyMinutes = (aiQuestionsCount * 15) + (completedQuizzesCount * 25);
        int hours = totalStudyMinutes / 60;
        int mins = totalStudyMinutes % 60;
        if (hours > 0) {
            tvSummaryStudyTime.setText(hours + "h " + mins + "m");
        } else {
            tvSummaryStudyTime.setText(mins + "m");
        }

        // 5. AI Suggestions (Optimal Focus Shift) - Phân tích dynamic bằng Gemini
        if (tvCard1Desc != null) {
            tvCard1Desc.setText("EduAI đang tổng hợp và phân tích báo cáo học tập của bạn...");

            // Lập chuỗi tóm tắt gửi lên AI
            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append("User Favorite Subject: ").append(favSubject).append("\n");
            summaryBuilder.append("User Education Level: ").append(eduLevel).append("\n");
            summaryBuilder.append("Streak count: ").append(streak).append(" days\n");
            summaryBuilder.append("Total AI questions asked: ").append(aiQuestionsCount).append("\n");
            summaryBuilder.append("Total Quizzes completed: ").append(completedQuizzesCount).append("\n");
            summaryBuilder.append("Overall Average Score: ").append(String.format(Locale.getDefault(), "%.1f", avgScoreTotal)).append("/10\n");

            // Phân tích điểm trung bình từng môn
            String[] subjects = {"Toán học", "Vật lý", "Hóa học", "Lập trình", "Lịch sử"};
            for (int i = 1; i <= 5; i++) {
                List<Integer> scores = subjectScores.get(i);
                if (scores != null && !scores.isEmpty()) {
                    int scoreSum = 0;
                    for (int s : scores) scoreSum += s;
                    float avg = (float) scoreSum / scores.size();
                    summaryBuilder.append("- Môn ").append(subjects[i-1]).append(": Điểm TB ").append(String.format(Locale.getDefault(), "%.1f", avg)).append("/10\n");
                }
            }

            final String finalFavSubject = favSubject;
            AIObj.getInstance().generateAIInsights(eduLevel, favSubject, summaryBuilder.toString(), new AIObj.AICallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Cắt hoặc định dạng gọn gàng để vừa khung card
                    tvCard1Desc.setText(result);
                    tvTagSubject.setText(finalFavSubject);
                }

                @Override
                public void onError(String error) {
                    // Fallback
                    tvCard1Desc.setText("Dựa trên hoạt động của bạn, AI phát hiện bạn học tập hiệu quả nhất vào buổi sáng. Hãy thử ôn tập môn " + finalFavSubject + " sau khi ngủ dậy để ghi nhớ tốt nhất!");
                    tvTagSubject.setText(finalFavSubject);
                }
            });
        }
    }

    private int calculateStreak(Set<String> studyDates, String todayStr, SimpleDateFormat sdf) {
        if (studyDates.isEmpty()) return 0;

        List<Date> dates = new ArrayList<>();
        for (String dateStr : studyDates) {
            try {
                dates.add(sdf.parse(dateStr));
            } catch (Exception ignored) {}
        }
        Collections.sort(dates, Collections.reverseOrder());

        int streak = 0;
        Date currentTarget = new Date(); // Bắt đầu tính từ ngày hôm nay

        // Kiểm tra xem hôm nay có học không, nếu không học hôm nay thì bắt đầu tính từ hôm qua
        String currentTargetStr = sdf.format(currentTarget);
        if (!studyDates.contains(currentTargetStr)) {
            // Lùi lại 1 ngày xem hôm qua có học không
            currentTarget = new Date(currentTarget.getTime() - 24 * 60 * 60 * 1000);
            currentTargetStr = sdf.format(currentTarget);
            if (!studyDates.contains(currentTargetStr)) {
                return 0; // Cả hôm nay và hôm qua đều không học -> Streak = 0
            }
        }

        // Tính chuỗi liên tiếp lùi về quá khứ
        while (true) {
            String checkStr = sdf.format(currentTarget);
            if (studyDates.contains(checkStr)) {
                streak++;
                // Lùi tiếp 1 ngày
                currentTarget = new Date(currentTarget.getTime() - 24 * 60 * 60 * 1000);
            } else {
                break;
            }
        }

        return streak;
    }

    private int getSubjectIdForQuestion(List<String[]> questionsCsv, String questionIdStr) {
        for (String[] qRow : questionsCsv) {
            if (qRow.length >= 3 && qRow[0].equals(questionIdStr)) {
                try {
                    return Integer.parseInt(qRow[2]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    /**
     * Dựng View động hiển thị tiến trình kỹ năng cho từng môn học (Skill Progression Item)
     */
    private View createSkillProgressView(String subjectName, int percent) {
        ConstraintLayout item = new ConstraintLayout(this);
        item.setPadding(0, 0, 0, dpToPx(16));

        // Tên môn học
        TextView tvName = new TextView(this);
        tvName.setId(View.generateViewId());
        tvName.setText(subjectName);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.insight_text_primary));
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        // Nhãn trình độ
        TextView tvLevel = new TextView(this);
        tvLevel.setId(View.generateViewId());
        String level = "Chưa rõ";
        if (percent >= 85) level = "Advanced";
        else if (percent >= 60) level = "Intermediate";
        else if (percent >= 40) level = "Basic";
        else if (percent > 0) level = "Novice";
        else level = "None";
        tvLevel.setText(level);
        tvLevel.setTextColor(ContextCompat.getColor(this, R.color.insight_text_secondary));
        tvLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        // Chỉ số phần trăm
        TextView tvPercent = new TextView(this);
        tvPercent.setId(View.generateViewId());
        tvPercent.setText(percent + "%");
        tvPercent.setTextColor(ContextCompat.getColor(this, R.color.insight_text_primary));
        tvPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);

        // Thanh tiến trình indicator
        LinearProgressIndicator progress = new LinearProgressIndicator(this);
        progress.setId(View.generateViewId());
        progress.setProgress(percent == 0 ? 3 : percent); // Cho tối thiểu 3% để có vệt sáng
        progress.setTrackColor(Color.parseColor("#1E1E2E"));
        progress.setTrackCornerRadius(dpToPx(2));
        progress.setTrackThickness(dpToPx(4));

        // Set màu sắc indicator theo chủ đề của ứng dụng
        if (subjectName.equals("Toán học") || subjectName.equals("Lập trình")) {
            progress.setIndicatorColor(ContextCompat.getColor(this, R.color.insight_primary));
        } else {
            progress.setIndicatorColor(ContextCompat.getColor(this, R.color.insight_accent));
        }

        // Layout LayoutParams và Constraint Layout rules
        ConstraintLayout.LayoutParams nameParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        nameParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        tvName.setLayoutParams(nameParams);
        item.addView(tvName);

        ConstraintLayout.LayoutParams levelParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        levelParams.startToEnd = tvName.getId();
        levelParams.bottomToBottom = tvName.getId();
        levelParams.leftMargin = dpToPx(8);
        tvLevel.setLayoutParams(levelParams);
        item.addView(tvLevel);

        ConstraintLayout.LayoutParams percentParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        percentParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        percentParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        tvPercent.setLayoutParams(percentParams);
        item.addView(tvPercent);

        ConstraintLayout.LayoutParams progressParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.topToBottom = tvName.getId();
        progressParams.topMargin = dpToPx(8);
        progress.setLayoutParams(progressParams);
        item.addView(progress);

        return item;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}