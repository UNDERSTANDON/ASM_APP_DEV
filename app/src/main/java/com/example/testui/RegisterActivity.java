package com.example.testui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.testui.database.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private ImageView togglePassword;
    private TextView strengthText;
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private ImageView matchIcon;
    private TextView matchError;
    private TextView loginRedirect;
    private EditText emailEditText;
    private boolean isPasswordVisible = false;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        togglePassword = findViewById(R.id.toggle_password);
        strengthText = findViewById(R.id.strength_text);
        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
        strengthBar4 = findViewById(R.id.strength_bar_4);
        matchIcon = findViewById(R.id.match_icon);
        matchError = findViewById(R.id.match_error);
        loginRedirect = findViewById(R.id.login_redirect);

        togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.register_button).setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Default education level during registration, refined in ProfileSetupActivity
            long result = dbHelper.registerUser(email, password, "Not set");
            if (result != -1) {
                // Store email in session to use in ProfileSetupActivity
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("user_email", email)
                        .apply();

                startActivity(new Intent(RegisterActivity.this, ProfileSetupActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthMeter(s.toString());
                checkMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateStrengthMeter(String password) {
        int strength = 0;
        if (password.length() > 0) strength = 1;
        if (password.length() >= 8) strength = 2;
        if (password.length() >= 8 && password.matches(".*[A-Z].*") && password.matches(".*[0-9].*")) strength = 3;
        if (password.length() >= 8 && password.matches(".*[A-Z].*") && password.matches(".*[0-9].*") && password.matches(".*[^a-zA-Z0-9].*")) strength = 4;

        resetBars();
        switch (strength) {
            case 1:
                strengthText.setText(R.string.strength_weak);
                setBarColor(strengthBar1, R.color.error);
                break;
            case 2:
                strengthText.setText(R.string.strength_medium);
                setBarColor(strengthBar1, R.color.tertiary);
                setBarColor(strengthBar2, R.color.tertiary);
                break;
            case 3:
                strengthText.setText(R.string.strength_strong);
                setBarColor(strengthBar1, R.color.primary);
                setBarColor(strengthBar2, R.color.primary);
                setBarColor(strengthBar3, R.color.primary);
                break;
            case 4:
                strengthText.setText(R.string.strength_very_strong);
                setBarColor(strengthBar1, R.color.primary_fixed);
                setBarColor(strengthBar2, R.color.primary_fixed);
                setBarColor(strengthBar3, R.color.primary_fixed);
                setBarColor(strengthBar4, R.color.primary_fixed);
                break;
            default:
                strengthText.setText(R.string.strength_none);
                break;
        }
    }

    private void resetBars() {
        int defaultColor = ContextCompat.getColor(this, R.color.surface_container_high);
        strengthBar1.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        strengthBar2.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        strengthBar3.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        strengthBar4.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
    }

    private void setBarColor(View bar, int colorRes) {
        bar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    private void checkMatch() {
        String p1 = passwordEditText.getText().toString();
        String p2 = confirmPasswordEditText.getText().toString();

        if (p2.isEmpty()) {
            matchIcon.setVisibility(View.GONE);
            matchError.setVisibility(View.GONE);
            return;
        }

        if (p1.equals(p2)) {
            matchIcon.setVisibility(View.VISIBLE);
            matchError.setVisibility(View.GONE);
        } else {
            matchIcon.setVisibility(View.GONE);
            matchError.setVisibility(View.VISIBLE);
        }
    }
}
