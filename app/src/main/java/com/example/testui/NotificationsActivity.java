package com.example.testui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.QuizSummary;
import com.example.testui.models.UserProfile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * NotificationsActivity — Màn hình thông báo thông minh học tập.
 * - Quét database CSV sinh thông báo động:
 *   + Thông báo nhắc nhở làm bài Quiz hằng ngày (nếu hôm nay chưa làm).
 *   + Thông báo chúc mừng thành tích khi đạt điểm tốt >= 8/10.
 *   + Thông báo gợi ý ôn tập cho các bài quiz điểm yếu < 5/10.
 *   + Thông báo chào mừng cá nhân hóa hệ thống.
 * - Nút "Đánh dấu tất cả đã đọc" lưu trạng thái thực tế vào SharedPreferences.
 */
public class NotificationsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout notificationsContainer;
    private View btnMarkAllRead;
    private boolean isMarkedAllRead = false;

    // Lớp nội bộ để lưu cấu trúc dữ liệu thông báo tự sinh
    private static class NotificationModel implements Comparable<NotificationModel> {
        String id;
        String type; // ACHIEVEMENT, REMINDER, REVIEW, SYSTEM
        String title;
        String message;
        String timeStr;
        Date rawDate;
        int iconRes;
        int colorRes;
        int bgTintRes;

        @Override
        public int compareTo(NotificationModel o) {
            // Sắp xếp từ mới nhất đến cũ nhất
            return o.rawDate.compareTo(this.rawDate);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        dbHelper = new DatabaseHelper(this);
        notificationsContainer = findViewById(R.id.ll_notifications_container);
        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);

        // Đọc trạng thái đã đọc của tất cả thông báo
        isMarkedAllRead = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("notif_marked_all_read", false);

        loadNotifications();

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean("notif_marked_all_read", true).apply();
                isMarkedAllRead = true;
                Toast.makeText(this, "Đã đánh dấu đọc tất cả thông báo", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Load lại để ẩn chấm đỏ unread
            });
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_alerts);
    }

    private void loadNotifications() {
        if (notificationsContainer == null) return;
        notificationsContainer.removeAllViews();

        List<NotificationModel> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        // Đọc dữ liệu CSV
        List<QuizSummary> quizzes = dbHelper.getAllQuizSummaries();
        List<String[]> questionsCsv = dbHelper.readCsv(new File(getFilesDir(), "questions.csv"));

        // Lấy thông tin môn học yêu thích & cấp học của user
        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        String favSubject = "Toán học";
        String eduLevel = "Học sinh";
        String userCreatedAt = todayStr;

        if (email != null) {
            UserProfile profile = dbHelper.getUserProfile(email);
            if (profile != null) {
                if (profile.getFavoriteSubject() != null) favSubject = profile.getFavoriteSubject();
                if (profile.getEducationLevel() != null) eduLevel = profile.getEducationLevel();
            }
            // Lấy ngày tạo user từ database CSV
            List<String[]> csvUsers = dbHelper.readCsv(new File(getFilesDir(), "users.csv"));
            for (String[] userRow : csvUsers) {
                if (userRow.length >= 4 && userRow[1].equals(email)) {
                    if (userRow[3].length() >= 10) userCreatedAt = userRow[3].substring(0, 10);
                    break;
                }
            }
        }

        // --- 1. Tạo thông báo nhắc nhở làm bài hàng ngày (REMINDER) ---
        boolean isQuizDoneToday = false;
        File quizFile = new File(getFilesDir(), "quizzes.csv");
        List<String[]> csvQuizzes = dbHelper.readCsv(quizFile);
        for (String[] qRow : csvQuizzes) {
            if (qRow.length >= 4) {
                String dateStr = qRow[3];
                if (dateStr.startsWith(todayStr) && "1".equals(qRow[4])) {
                    isQuizDoneToday = true;
                    break;
                }
            }
        }

        if (!isQuizDoneToday) {
            NotificationModel m = new NotificationModel();
            m.id = "rem_today";
            m.type = "REMINDER";
            m.title = "Thử thách trắc nghiệm hôm nay!";
            m.message = "Hãy dành 5 phút làm bài trắc nghiệm ngẫu nhiên môn " + favSubject + " để kích hoạt não bộ và duy trì chuỗi học tập nhé.";
            m.timeStr = "Vừa xong";
            m.rawDate = new Date(); // Mới nhất
            m.iconRes = R.drawable.ic_lightbulb;
            m.colorRes = R.color.primary;
            m.bgTintRes = Color.parseColor("#1A7DD3FC");
            list.add(m);
        }

        // --- 2. Tạo thông báo chúc mừng thành tích (ACHIEVEMENT) & Đề xuất ôn tập (REVIEW) ---
        for (int i = 0; i < csvQuizzes.size(); i++) {
            String[] row = csvQuizzes.get(i);
            if (row.length >= 6 && "1".equals(row[4])) {
                String questionIdStr = row[1];
                String scoreStr = row[5];
                String dateStr = row[3];

                int score = 0;
                try {
                    score = Integer.parseInt(scoreStr);
                } catch (NumberFormatException ignored) {}

                Date quizDate = new Date();
                try {
                    // format: 2026-07-19T14:02:57Z
                    SimpleDateFormat quizSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    if (dateStr.length() >= 19) {
                        quizDate = quizSdf.parse(dateStr.substring(0, 19));
                    }
                } catch (Exception ignored) {}

                // Nhận diện tên môn học
                String subjectName = "Toán học";
                int subjectId = getSubjectIdForQuestion(questionsCsv, questionIdStr);
                switch (subjectId) {
                    case 1: subjectName = "Toán học"; break;
                    case 2: subjectName = "Vật lý"; break;
                    case 3: subjectName = "Hóa học"; break;
                    case 4: subjectName = "Lập trình"; break;
                    case 5: subjectName = "Lịch sử"; break;
                }

                // Điểm >= 8 -> ACHIEVEMENT (Màu xanh lá)
                if (score >= 8) {
                    NotificationModel m = new NotificationModel();
                    m.id = "ach_" + row[0];
                    m.type = "ACHIEVEMENT";
                    m.title = "Thành tích xuất sắc! 🎉";
                    m.message = "Chúc mừng bạn đã hoàn thành bài quiz môn " + subjectName + " với điểm số cao " + score + "/10. Hãy tiếp tục phát huy!";
                    m.timeStr = getRelativeTime(quizDate, displaySdf);
                    m.rawDate = quizDate;
                    m.iconRes = R.drawable.ic_check_circle;
                    m.colorRes = R.color.quiz_success;
                    m.bgTintRes = Color.parseColor("#1A10B981");
                    list.add(m);
                }
                // Điểm < 5 -> REVIEW (Màu đỏ/cam)
                else if (score < 5) {
                    NotificationModel m = new NotificationModel();
                    m.id = "rev_" + row[0];
                    m.type = "REVIEW";
                    m.title = "Đề xuất ôn tập chủ đề";
                    m.message = "Bạn có bài làm môn " + subjectName + " chưa đạt kết quả tốt (" + score + "/10). Hãy xem lại hộp giải thích và bấm làm lại.";
                    m.timeStr = getRelativeTime(quizDate, displaySdf);
                    m.rawDate = quizDate;
                    m.iconRes = R.drawable.ic_error;
                    m.colorRes = R.color.quiz_danger;
                    m.bgTintRes = Color.parseColor("#1AFF6B6B");
                    list.add(m);
                }
            }
        }

        // --- 3. Tạo thông báo hệ thống chào mừng (SYSTEM) ---
        Date welcomeDate = new Date();
        try {
            welcomeDate = sdf.parse(userCreatedAt);
        } catch (Exception ignored) {}
        
        NotificationModel mWelcome = new NotificationModel();
        mWelcome.id = "sys_welcome";
        mWelcome.type = "SYSTEM";
        mWelcome.title = "Chào mừng bạn đến với EduAI! 🧠";
        mWelcome.message = "Cố vấn học thuật AI đã được thiết lập thành công theo cấp học " + eduLevel + " của bạn và sẵn sàng đồng hành!";
        mWelcome.timeStr = getRelativeTime(welcomeDate, displaySdf);
        mWelcome.rawDate = welcomeDate;
        mWelcome.iconRes = R.drawable.ic_notifications;
        mWelcome.colorRes = R.color.home_icon_quiz;
        mWelcome.bgTintRes = Color.parseColor("#1AC084FC");
        list.add(mWelcome);

        // Sắp xếp các thông báo theo thời gian từ mới đến cũ
        Collections.sort(list);

        // Thêm nhãn danh mục
        TextView tvCategoryNew = new TextView(this);
        tvCategoryNew.setText("CẬP NHẬT GẦN ĐÂY");
        tvCategoryNew.setTextColor(ContextCompat.getColor(this, R.color.tertiary));
        tvCategoryNew.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvCategoryNew.setTypeface(null, Typeface.BOLD);
        tvCategoryNew.setPadding(0, dpToPx(8), 0, dpToPx(12));
        notificationsContainer.addView(tvCategoryNew);

        // Dựng View động cho từng thông báo
        for (int i = 0; i < list.size(); i++) {
            // Sau 2 thông báo đầu tiên (mới nhất), ta phân tách nhóm thông báo cũ hơn
            if (i == 2) {
                TextView tvCategoryEarlier = new TextView(this);
                tvCategoryEarlier.setText("CŨ HƠN");
                tvCategoryEarlier.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvCategoryEarlier.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tvCategoryEarlier.setTypeface(null, Typeface.BOLD);
                tvCategoryEarlier.setPadding(0, dpToPx(24), 0, dpToPx(12));
                notificationsContainer.addView(tvCategoryEarlier);
            }

            View item = createNotificationItemView(list.get(i), i < 2 && !isMarkedAllRead);
            notificationsContainer.addView(item);
        }
    }

    private View createNotificationItemView(NotificationModel model, boolean isUnread) {
        ConstraintLayout layout = new ConstraintLayout(this);
        layout.setBackgroundResource(R.drawable.bg_glass_panel_elevated);
        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(16));
        layout.setLayoutParams(layoutParams);

        if (!isUnread) {
            layout.setAlpha(0.7f); // Đã đọc thì mờ đi một chút
        }

        // Chấm tròn đỏ Unread Dot
        View unreadDot = new View(this);
        unreadDot.setId(View.generateViewId());
        unreadDot.setBackgroundResource(R.drawable.bg_unread_indicator);
        ConstraintLayout.LayoutParams dotParams = new ConstraintLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dotParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        dotParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        dotParams.topMargin = dpToPx(6);
        unreadDot.setLayoutParams(dotParams);
        
        if (!isUnread) {
            unreadDot.setVisibility(View.GONE);
        }
        layout.addView(unreadDot);

        // Khung Icon Container tròn
        FrameLayout iconFrame = new FrameLayout(this);
        iconFrame.setId(View.generateViewId());
        iconFrame.setBackgroundResource(R.drawable.bg_circle);
        iconFrame.setBackgroundColor(model.bgTintRes); // Đặt màu background mờ
        ConstraintLayout.LayoutParams frameParams = new ConstraintLayout.LayoutParams(dpToPx(40), dpToPx(40));
        frameParams.startToEnd = unreadDot.getId();
        frameParams.leftMargin = isUnread ? dpToPx(12) : dpToPx(0);
        frameParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        iconFrame.setLayoutParams(frameParams);

        ImageView ivIcon = new ImageView(this);
        ivIcon.setImageResource(model.iconRes);
        ivIcon.setColorFilter(ContextCompat.getColor(this, model.colorRes));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
        iconParams.gravity = Gravity.CENTER;
        ivIcon.setLayoutParams(iconParams);
        iconFrame.addView(ivIcon);
        
        layout.addView(iconFrame);

        // Mốc thời gian (Time)
        TextView tvTime = new TextView(this);
        tvTime.setId(View.generateViewId());
        tvTime.setText(model.timeStr);
        tvTime.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        ConstraintLayout.LayoutParams timeParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        timeParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        tvTime.setLayoutParams(timeParams);
        layout.addView(tvTime);

        // Tiêu đề thông báo
        TextView tvTitle = new TextView(this);
        tvTitle.setId(View.generateViewId());
        tvTitle.setText(model.title);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(null, Typeface.BOLD);
        ConstraintLayout.LayoutParams titleParams = new ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.startToEnd = iconFrame.getId();
        titleParams.leftMargin = dpToPx(12);
        titleParams.endToStart = tvTime.getId();
        titleParams.rightMargin = dpToPx(12);
        titleParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        tvTitle.setLayoutParams(titleParams);
        layout.addView(tvTitle);

        // Nội dung chi tiết thông báo
        TextView tvMsg = new TextView(this);
        tvMsg.setText(model.message);
        tvMsg.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvMsg.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), 1.0f);
        ConstraintLayout.LayoutParams msgParams = new ConstraintLayout.LayoutParams(
                0,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.startToStart = tvTitle.getId();
        msgParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        msgParams.topToBottom = tvTitle.getId();
        msgParams.topMargin = dpToPx(4);
        tvMsg.setLayoutParams(msgParams);
        layout.addView(tvMsg);

        // Sự kiện Click: Nếu là thông báo cần ôn tập -> Bấm vào mở thẳng QuizLabActivity để xem lại ôn thi!
        if (model.type.equals("REVIEW") || model.type.equals("ACHIEVEMENT")) {
            layout.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationsActivity.this, QuizLabActivity.class);
                startActivity(intent);
            });
        }

        return layout;
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

    private String getRelativeTime(Date date, SimpleDateFormat displaySdf) {
        long diff = new Date().getTime() - date.getTime();
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffDays > 2) {
            return displaySdf.format(date);
        } else if (diffDays == 2) {
            return "2 ngày trước";
        } else if (diffDays == 1) {
            return "Hôm qua";
        } else if (diffHours > 0) {
            return diffHours + " giờ trước";
        } else if (diffMinutes > 0) {
            return diffMinutes + " phút trước";
        } else {
            return "Vừa xong";
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}