package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class AskQuestionActivity extends AppCompatActivity {

    private EditText questionInput;
    private View sendButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_question);

        questionInput = findViewById(R.id.question_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString();
            // If it looks like the placeholder or is empty, use a default for testing
            if (question.isEmpty() || question.startsWith("Type your question")) {
                question = "2x² - 5x + 3 = 0";
            }
            Intent intent = new Intent(AskQuestionActivity.this, AnswerActivity.class);
            intent.putExtra("question", question);
            startActivity(intent);
        });
    }
}
