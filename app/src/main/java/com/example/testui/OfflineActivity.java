package com.example.testui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class OfflineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_retry).setOnClickListener(v -> {
            // Logic to retry connection or refresh state
        });
    }
}