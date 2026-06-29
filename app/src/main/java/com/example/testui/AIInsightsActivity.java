package com.example.testui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AIInsightsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_insights);

        NavigationHelper.setupBottomNavigation(this, R.id.nav_insights);
    }
}