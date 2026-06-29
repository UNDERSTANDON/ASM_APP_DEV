package com.example.testui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AnswerHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_history);

        NavigationHelper.setupBottomNavigation(this, R.id.nav_saved);
    }
}