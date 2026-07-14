package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.splashscreen.SplashScreen;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;
import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private Handler splashHandler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cài đặt Splash Screen trước khi gọi super.onCreate
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        
        // Bật chế độ hiển thị toàn màn hình để tránh flicker thanh trạng thái
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
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
        splashHandler = new Handler(Looper.getMainLooper());
        splashRunnable = () -> {
            if (!isFinishing() && !isDestroyed()) {
                Class<?> targetActivity = isLoggedIn ? MainActivity.class : LoginActivity.class;
                startActivity(new Intent(SplashActivity.this, targetActivity));
                // Thêm hiệu ứng chuyển cảnh mượt mà
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        };
        splashHandler.postDelayed(splashRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        if (splashHandler != null && splashRunnable != null) {
            splashHandler.removeCallbacks(splashRunnable);
        }
        super.onDestroy();
    }
}
