package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.testui.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Running in Offline Mode", Toast.LENGTH_LONG).show();
        }

        TextView askAiBtn = findViewById(R.id.ask_ai_btn);
        askAiBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AskQuestionActivity.class)));

        NavigationHelper.setupBottomNavigation(this, R.id.nav_home);
    }
}
