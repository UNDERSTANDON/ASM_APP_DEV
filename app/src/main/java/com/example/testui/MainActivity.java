package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView askAiBtn = findViewById(R.id.ask_ai_btn);
        askAiBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AskQuestionActivity.class));
        });
    }
}
