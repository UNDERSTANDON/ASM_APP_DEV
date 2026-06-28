package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AnswerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        TextView questionTextView = findViewById(R.id.question_text_view);
        ImageView backButton = findViewById(R.id.back_button);
        TextView moreSolutionsBtn = findViewById(R.id.more_solutions_btn);

        String question = getIntent().getStringExtra("question");
        if (question != null && !question.isEmpty()) {
            questionTextView.setText(question);
        }

        backButton.setOnClickListener(v -> finish());

        moreSolutionsBtn.setOnClickListener(v -> {
            startActivity(new Intent(AnswerActivity.this, ExpandedAnswerActivity.class));
        });
    }
}
