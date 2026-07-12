package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 10);
        
        if (total == 0) total = 1;

        TextView tvScoreText = findViewById(R.id.tv_score_text);
        TextView tvScorePercent = findViewById(R.id.tv_score_percent);
        ProgressBar scoreCircle = findViewById(R.id.score_circle);
        
        tvScoreText.setText(score + "/" + total);
        
        int percent = (int) (((float) score / total) * 100);
        tvScorePercent.setText(percent + "%");
        scoreCircle.setProgress(percent);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            Intent intent = new Intent(QuizResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        findViewById(R.id.btn_retake).setOnClickListener(v -> {
            // Can go back to QuizActivity, but since we didn't pass questions, we just finish to let user trigger it again.
            finish();
        });
    }
}