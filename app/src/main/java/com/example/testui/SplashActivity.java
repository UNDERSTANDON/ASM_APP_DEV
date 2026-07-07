package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.splashscreen.SplashScreen;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;
import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cài đặt Splash Screen trước khi gọi super.onCreate
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        
        // Bật chế độ hiển thị toàn màn hình để tránh flicker thanh trạng thái
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_splash);

        // Kiểm tra người dùng login để khởi tạo ai context
        boolean isLoggedIn = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("isLoggedIn", false);
        String userEmail = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);

        if (isLoggedIn && userEmail != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            UserProfile profile = dbHelper.getUserProfile(userEmail);
            if (profile != null) {
                Map<String, Object> aiConfig = new HashMap<>();
                aiConfig.put("educationLevel", profile.getEducationLevel());
                aiConfig.put("tone", profile.getAiTone());
                AIObj.getInstance().initialize(aiConfig);
            }
        }

        // Chuyển sang màn hình tương ứng sau 2 giây (2000ms)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> targetActivity = isLoggedIn ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, targetActivity));
            // Thêm hiệu ứng chuyển cảnh mượt mà
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}
