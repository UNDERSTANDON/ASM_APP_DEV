package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Bật chế độ hiển thị toàn màn hình để tránh flicker thanh trạng thái
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_splash);

        // Chuyển sang màn hình đăng nhập sau 2 giây (2000ms)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            // Thêm hiệu ứng chuyển cảnh mượt mà
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}
