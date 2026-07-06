package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.UserProfile;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);

        TextView emailText = findViewById(R.id.profile_email);
        TextView levelText = findViewById(R.id.profile_level);
        TextView toneText = findViewById(R.id.profile_tone);
        Button logoutButton = findViewById(R.id.logout_button);

        String email = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", null);
        if (email != null) {
            UserProfile profile = dbHelper.getUserProfile(email);
            if (profile != null) {
                emailText.setText(profile.getEmail());
                levelText.setText(getString(R.string.profile_level_label, profile.getEducationLevel()));
                toneText.setText(getString(R.string.profile_tone_label, profile.getAiTone()));
            }
        }

        logoutButton.setOnClickListener(v -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        NavigationHelper.setupBottomNavigation(this, R.id.nav_profile);
    }
}
