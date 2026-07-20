package com.example.testui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;

/**
 * ProfileActivity — Màn hình Thông tin cá nhân.
 * - Hiển thị thông tin cấp học, phong cách giải thích và tài khoản của người dùng.
 * - Cho phép người dùng nhập và xóa Gemini API Key cá nhân.
 * - Tự động tải lại thông tin mới cập nhật ở onResume().
 */
public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView emailText;
    private TextView levelText;
    private TextView toneText;
    private TextView apiKeyStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);

        emailText = findViewById(R.id.profile_email);
        levelText = findViewById(R.id.profile_level);
        toneText = findViewById(R.id.profile_tone);
        apiKeyStatusText = findViewById(R.id.profile_api_key_status);
        Button logoutButton = findViewById(R.id.logout_button);

        // Sự kiện click mở màn hình Cài đặt (SettingsActivity)
        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }

        View btnEditProfile = findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }

        // Sự kiện click vào Gemini API Key Config
        View layoutApiKey = findViewById(R.id.layout_profile_api_key);
        if (layoutApiKey != null) {
            layoutApiKey.setOnClickListener(v -> showApiKeyConfigDialog());
        }

        logoutButton.setOnClickListener(v -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        NavigationHelper.setupBottomNavigation(this, R.id.nav_profile);
    }

    private void showApiKeyConfigDialog() {
        String savedKey = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_gemini_api_key", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gemini API Key");
        builder.setMessage("Vui lòng dán Gemini API Key của bạn để kích hoạt cố vấn học tập AI:");

        // Tạo EditText nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setHint("AI Key (AIzaSy...)");
        if (!TextUtils.isEmpty(savedKey)) {
            input.setText(savedKey);
        }
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

        // Nút Lưu Key
        builder.setPositiveButton("Lưu Key", (dialog, which) -> {
            String newKey = input.getText().toString().trim();
            if (TextUtils.isEmpty(newKey)) {
                Toast.makeText(this, "API Key không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("user_gemini_api_key", newKey)
                    .apply();
            Toast.makeText(this, "Đã lưu API Key thành công!", Toast.LENGTH_SHORT).show();
            loadProfileData(); // Cập nhật ngay nhãn hiển thị
        });

        // Nút Xóa Key (màu đỏ/cảnh báo)
        if (!TextUtils.isEmpty(savedKey)) {
            builder.setNeutralButton("Xóa Key", (dialog, which) -> {
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .edit()
                        .remove("user_gemini_api_key")
                        .apply();
                Toast.makeText(this, "Đã xóa API Key khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                loadProfileData(); // Cập nhật ngay nhãn hiển thị
            });
        }

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        if (email != null) {
            UserProfile profile = dbHelper.getUserProfile(email);
            if (profile != null) {
                emailText.setText(profile.getEmail());
                levelText.setText(getString(R.string.profile_level_label, profile.getEducationLevel()));
                toneText.setText(getString(R.string.profile_tone_label, profile.getAiTone()));
            }
        }

        // Đọc trạng thái Gemini API Key
        String savedKey = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_gemini_api_key", "");
        if (apiKeyStatusText != null) {
            if (TextUtils.isEmpty(savedKey)) {
                apiKeyStatusText.setText("Chưa cấu hình (Bấm để nhập)");
                apiKeyStatusText.setTextColor(Color.parseColor("#FF6B6B")); // Màu đỏ
            } else {
                apiKeyStatusText.setText("Đã cấu hình (••••••••)");
                apiKeyStatusText.setTextColor(Color.parseColor("#10B981")); // Màu xanh ngọc
            }
        }
    }
}
