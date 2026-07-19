package com.example.testui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsActivity — Màn hình Cài đặt của EduAI.
 * - Cho phép thay đổi Email (Cascade Update) đảm bảo không mất lịch sử dữ liệu hỏi đáp.
 * - Cho phép đổi Mật khẩu (Validate đúng 8 chữ số 0-9).
 * - Cho phép chỉnh sửa thông tin Hồ sơ học tập: Cấp học, Môn học yêu thích, Phong cách giải thích.
 * - Tự động đồng bộ hóa thiết lập mới với AI Specialist.
 */
public class SettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentEmail;

    private TextView tvCurrentEduLevel;
    private TextView tvCurrentFavSubject;
    private TextView tvCurrentExplanationTone;
    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        // Ánh xạ các TextView hiển thị thông tin học tập
        tvCurrentEduLevel = findViewById(R.id.tv_current_edu_level);
        tvCurrentFavSubject = findViewById(R.id.tv_current_fav_subject);
        tvCurrentExplanationTone = findViewById(R.id.tv_current_explanation_tone);

        // Đọc email của phiên hiện tại
        currentEmail = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);

        // Nút quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Cài đặt nút Thay đổi Email
        findViewById(R.id.layout_change_email).setOnClickListener(v -> showChangeEmailDialog());

        // Cài đặt nút Đổi mật khẩu
        findViewById(R.id.layout_change_password).setOnClickListener(v -> showChangePasswordDialog());

        // Cài đặt các nút Thay đổi Hồ sơ học tập
        findViewById(R.id.layout_change_edu_level).setOnClickListener(v -> showChangeEduLevelDialog());
        findViewById(R.id.layout_change_fav_subject).setOnClickListener(v -> showChangeFavSubjectDialog());
        findViewById(R.id.layout_change_explanation_tone).setOnClickListener(v -> showChangeExplanationToneDialog());

        // Nạp thông tin hồ sơ hiện tại lên giao diện
        loadProfileData();
    }

    private void loadProfileData() {
        if (currentEmail != null) {
            userProfile = dbHelper.getUserProfile(currentEmail);
            if (userProfile != null) {
                if (tvCurrentEduLevel != null) {
                    tvCurrentEduLevel.setText(userProfile.getEducationLevel());
                }
                if (tvCurrentFavSubject != null) {
                    tvCurrentFavSubject.setText(userProfile.getFavoriteSubject());
                }
                if (tvCurrentExplanationTone != null) {
                    tvCurrentExplanationTone.setText(userProfile.getAiTone());
                }
            }
        }
    }

    private void showChangeEduLevelDialog() {
        if (userProfile == null) return;

        final String[] items = {"THCS", "THPT", "Đại học"};
        int checkedItem = 1; // Mặc định THPT
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(userProfile.getEducationLevel())) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn cấp học của bạn");
        
        final int[] selectedIndex = {checkedItem};
        builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> selectedIndex[0] = which);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newLevel = items[selectedIndex[0]];
            userProfile.setEducationLevel(newLevel);
            dbHelper.updateUserProfile(userProfile);
            
            // Đồng bộ sang AI
            syncAiConfig();
            
            Toast.makeText(this, "Đã cập nhật cấp học thành công!", Toast.LENGTH_SHORT).show();
            loadProfileData();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangeFavSubjectDialog() {
        if (userProfile == null) return;

        final String[] items = {"Toán học", "Vật lý", "Hóa học", "Lập trình", "Lịch sử"};
        int checkedItem = 0; // Mặc định Toán
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(userProfile.getFavoriteSubject())) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn môn học yêu thích");

        final int[] selectedIndex = {checkedItem};
        builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> selectedIndex[0] = which);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newSubject = items[selectedIndex[0]];
            userProfile.setFavoriteSubject(newSubject);
            dbHelper.updateUserProfile(userProfile);

            // Đồng bộ sang AI
            syncAiConfig();

            Toast.makeText(this, "Đã cập nhật môn học yêu thích!", Toast.LENGTH_SHORT).show();
            loadProfileData();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangeExplanationToneDialog() {
        if (userProfile == null) return;

        final String[] items = {"Concise", "Detailed"};
        int checkedItem = 1; // Mặc định Detailed
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(userProfile.getAiTone())) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn phong cách giải thích");

        final int[] selectedIndex = {checkedItem};
        builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> selectedIndex[0] = which);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newTone = items[selectedIndex[0]];
            userProfile.setAiTone(newTone);
            dbHelper.updateUserProfile(userProfile);

            // Đồng bộ sang AI
            syncAiConfig();

            Toast.makeText(this, "Đã cập nhật phong cách giải thích!", Toast.LENGTH_SHORT).show();
            loadProfileData();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void syncAiConfig() {
        if (userProfile == null) return;
        Map<String, Object> aiConfig = new HashMap<>();
        aiConfig.put("educationLevel", userProfile.getEducationLevel());
        aiConfig.put("tone", userProfile.getAiTone());
        AIObj.getInstance().initialize(aiConfig);
    }

    private void showChangeEmailDialog() {
        if (currentEmail == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thay đổi Email");
        builder.setMessage("Nhập địa chỉ email mới của bạn:");

        // Tạo EditText nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Email mới");
        input.setPadding(48, 36, 48, 36);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(48, 16, 48, 16);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String newEmail = input.getText().toString().trim();
            if (TextUtils.isEmpty(newEmail)) {
                Toast.makeText(this, "Email không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate email hợp lệ
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(this, "Email không hợp lệ. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newEmail.equalsIgnoreCase(currentEmail)) {
                Toast.makeText(this, "Email mới phải khác Email hiện tại!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra xem email mới đã tồn tại chưa
            if (dbHelper.isEmailExists(newEmail)) {
                Toast.makeText(this, "Email này đã tồn tại trên hệ thống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tiến hành cập nhật đồng bộ (Cascade Update) trên toàn bộ CSV
            boolean success = dbHelper.updateEmailCascade(currentEmail, newEmail);
            if (success) {
                // Cập nhật phiên đăng nhập SharedPreferences
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("user_email", newEmail)
                        .apply();
                currentEmail = newEmail;
                Toast.makeText(this, "Đã thay đổi Email thành công!", Toast.LENGTH_SHORT).show();
                loadProfileData(); // Cập nhật lại thông tin hiển thị email
            } else {
                Toast.makeText(this, "Thay đổi thất bại. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangePasswordDialog() {
        if (currentEmail == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thay đổi Mật khẩu");
        builder.setMessage("Nhập mật khẩu mới (Mật khẩu bắt buộc chứa đúng 8 chữ số):");

        // Tạo EditText nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Mật khẩu mới (8 chữ số)");
        input.setPadding(48, 36, 48, 36);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(48, 16, 48, 16);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String newPassword = input.getText().toString().trim();
            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "Mật khẩu không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate mật khẩu: phải chứa đúng 8 chữ số (chỉ gồm số 0-9)
            if (newPassword.length() != 8 || !newPassword.matches("[0-9]+")) {
                Toast.makeText(this, "Mật khẩu bắt buộc phải chứa đúng 8 chữ số.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tiến hành cập nhật mật khẩu mới vào CSV
            boolean success = dbHelper.updatePassword(currentEmail, newPassword);
            if (success) {
                Toast.makeText(this, "Đã đổi Mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Thay đổi thất bại. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}