package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private int currentStep = 1;

    private LinearLayout step1Content, step2Content, step3Content;
    private FrameLayout step1Circle, step2Circle, step3Circle;
    private TextView step1Label, step2Label, step3Label;
    private Button continueButton, finishButton;
    private TextView skipButton;

    private View selectedLevel = null;   // null = chưa chọn cấp học
    private String selectedSubject = null; // null = chưa chọn môn học
    private View selectedStyle = null;   // null = chưa chọn phong cách giải thích
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        dbHelper = new DatabaseHelper(this);
        step1Content = findViewById(R.id.step1_content);
        step2Content = findViewById(R.id.step2_content);
        step3Content = findViewById(R.id.step3_content);

        step1Circle = findViewById(R.id.step1_circle);
        step2Circle = findViewById(R.id.step2_circle);
        step3Circle = findViewById(R.id.step3_circle);

        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);

        continueButton = findViewById(R.id.continue_button);
        finishButton = findViewById(R.id.finish_button);
        skipButton = findViewById(R.id.skip_button);

        // Khởi tạo các Step
        setupStep1();
        setupStep2();
        setupStep3();

        continueButton.setOnClickListener(v -> {
            if (currentStep == 1) {
                // Ràng buộc Step 1: Bắt buộc chọn cấp học
                if (selectedLevel == null) {
                    Toast.makeText(this, "Vui lòng chọn cấp học của bạn để tiếp tục.", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStep = 2;
                updateUI();
            } else if (currentStep == 2) {
                // Ràng buộc Step 2: Bắt buộc chọn môn học yêu thích
                if (selectedSubject == null) {
                    Toast.makeText(this, "Vui lòng chọn 1 môn học yêu thích để tiếp tục.", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStep = 3;
                updateUI();
            }
        });

        // Bỏ nút bỏ qua để bắt buộc người dùng thiết lập đầy đủ thông tin tiền đề
        skipButton.setVisibility(View.GONE);

        finishButton.setOnClickListener(v -> {
            // Ràng buộc Step 3: Bắt buộc chọn phong cách giải thích
            if (selectedStyle == null) {
                Toast.makeText(this, "Vui lòng chọn phong cách giải thích để hoàn tất.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveProfileAndFinish();
        });
    }

    private void saveProfileAndFinish() {
        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        if (email == null) {
            Toast.makeText(this, "Session error: Không tìm thấy email đăng ký.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Xác định cấp học
        String educationLevel = "THPT"; // Fallback
        if (selectedLevel != null) {
            int id = selectedLevel.getId();
            if (id == R.id.level_thcs) educationLevel = "THCS";
            else if (id == R.id.level_thpt) educationLevel = "THPT";
            else if (id == R.id.level_uni) educationLevel = "Đại học";
        }

        // 2. Xác định phong cách giải thích
        String tone = "Detailed"; // Fallback
        if (selectedStyle != null) {
            int id = selectedStyle.getId();
            if (id == R.id.style_concise_btn) tone = "Concise";
            else if (id == R.id.style_detailed_btn) tone = "Detailed";
            else if (id == R.id.style_step_btn) tone = "Step-by-Step";
        }

        // 3. Cập nhật User Profile vào CSV Database
        UserProfile profile = new UserProfile(email, educationLevel);
        profile.setFavoriteSubject(selectedSubject);
        profile.setAiTone(tone);
        dbHelper.updateUserProfile(profile);

        // Lưu trạng thái đăng nhập hoàn tất
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .apply();

        // Khởi tạo cấu hình cho AI Specialist
        Map<String, Object> aiConfig = new HashMap<>();
        aiConfig.put("educationLevel", educationLevel);
        aiConfig.put("tone", tone);
        AIObj.getInstance().initialize(aiConfig);

        // Chuyển hướng đến Trang chủ
        Toast.makeText(this, "Thiết lập tài khoản thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupStep1() {
        View.OnClickListener listener = v -> {
            // Tắt sáng các ô khác
            if (selectedLevel != null) {
                selectedLevel.setSelected(false);
            }
            // Sáng ô được chọn
            selectedLevel = v;
            selectedLevel.setSelected(true);
        };

        findViewById(R.id.level_thcs).setOnClickListener(listener);
        findViewById(R.id.level_thpt).setOnClickListener(listener);
        findViewById(R.id.level_uni).setOnClickListener(listener);

        // Mặc định không chọn ô nào (selectedLevel = null) để bắt buộc người dùng bấm chọn
    }

    private void setupStep2() {
        ChipGroup chipGroup = findViewById(R.id.subject_chip_group);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedSubject = null;
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    String chipText = chip.getText().toString();
                    // Chuẩn hóa tên môn học tương ứng với 5 môn chính
                    if (chipText.contains("Toán")) {
                        selectedSubject = "Toán học";
                    } else if (chipText.contains("Lý") || chipText.contains("Vật lý")) {
                        selectedSubject = "Vật lý";
                    } else if (chipText.contains("Hóa") || chipText.contains("Hóa học")) {
                        selectedSubject = "Hóa học";
                    } else if (chipText.contains("Tin") || chipText.contains("Lập trình")) {
                        selectedSubject = "Lập trình";
                    } else {
                        selectedSubject = chipText; // Ngoại ngữ hoặc môn khác
                    }
                }
            }
        });
    }

    private void setupStep3() {
        View.OnClickListener listener = v -> {
            // Tắt sáng các ô khác
            if (selectedStyle != null) {
                selectedStyle.setSelected(false);
            }
            // Sáng ô được chọn
            selectedStyle = v;
            selectedStyle.setSelected(true);
        };

        findViewById(R.id.style_concise_btn).setOnClickListener(listener);
        findViewById(R.id.style_detailed_btn).setOnClickListener(listener);
        findViewById(R.id.style_step_btn).setOnClickListener(listener);

        // Mặc định không chọn ô nào để người dùng tự tay chọn
    }

    private void updateUI() {
        step1Content.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        step2Content.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        step3Content.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);

        updateStepper();

        if (currentStep == 3) {
            continueButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.VISIBLE);
            finishButton.setVisibility(View.GONE);
        }
    }

    private void updateStepper() {
        int inactiveColor = ContextCompat.getColor(this, R.color.on_surface_variant);
        int activeColor = ContextCompat.getColor(this, R.color.primary);

        step1Circle.setBackgroundResource(currentStep >= 1 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step1Label.setTextColor(currentStep >= 1 ? activeColor : inactiveColor);

        step2Circle.setBackgroundResource(currentStep >= 2 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step2Label.setTextColor(currentStep >= 2 ? activeColor : inactiveColor);

        step3Circle.setBackgroundResource(currentStep >= 3 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step3Label.setTextColor(currentStep >= 3 ? activeColor : inactiveColor);

        ((TextView) step1Circle.getChildAt(0)).setTextColor(currentStep >= 1 ? activeColor : inactiveColor);
        ((TextView) step2Circle.getChildAt(0)).setTextColor(currentStep >= 2 ? activeColor : inactiveColor);
        ((TextView) step3Circle.getChildAt(0)).setTextColor(currentStep >= 3 ? activeColor : inactiveColor);
    }

    @Override
    public void onBackPressed() {
        if (currentStep > 1) {
            currentStep--;
            updateUI();
        } else {
            super.onBackPressed();
        }
    }
}
