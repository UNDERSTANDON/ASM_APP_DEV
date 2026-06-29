package com.example.testui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ProfileSetupActivity extends AppCompatActivity {

    private int currentStep = 1;

    private LinearLayout step1Content, step2Content, step3Content;
    private FrameLayout step1Circle, step2Circle, step3Circle;
    private TextView step1Label, step2Label, step3Label;
    private Button continueButton, finishButton;
    private TextView skipButton;

    private View selectedLevel = null;
    private View selectedStyle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        step1Content = findViewById(R.id.step1_content);
        step2Content = findViewById(R.id.step2_content);
        step3Content = findViewById(R.id.step3_content);

        step1Circle = findViewById(R.id.step1_circle);
        step2Circle = findViewById(R.id.step2_circle);
        step3Circle = findViewById(R.id.step3_circle);

        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);

        continueButton = findViewById(R.id.continue_button);
        finishButton = findViewById(R.id.finish_button);
        skipButton = findViewById(R.id.skip_button);

        setupStep1();
        setupStep3();

        continueButton.setOnClickListener(v -> {
            if (currentStep < 3) {
                currentStep++;
                updateUI();
            }
        });

        skipButton.setOnClickListener(v -> {
            // Handle skip - for now just go to next step or finish
            if (currentStep < 3) {
                currentStep++;
                updateUI();
            }
        });

        finishButton.setOnClickListener(v -> {
            // Finalize and go to main app
            startActivity(new Intent(ProfileSetupActivity.this, MainActivity.class));
            finish();
        });
    }

    private void setupStep1() {
        View.OnClickListener listener = v -> {
            if (selectedLevel != null) selectedLevel.setSelected(false);
            selectedLevel = v;
            selectedLevel.setSelected(true);
        };
        findViewById(R.id.level_thcs).setOnClickListener(listener);
        findViewById(R.id.level_thpt).setOnClickListener(listener);
        findViewById(R.id.level_uni).setOnClickListener(listener);
        
        // Default selection
        findViewById(R.id.level_thpt).performClick();
    }

    private void setupStep3() {
        View.OnClickListener listener = v -> {
            if (selectedStyle != null) selectedStyle.setSelected(false);
            selectedStyle = v;
            selectedStyle.setSelected(true);
        };
        findViewById(R.id.style_concise_btn).setOnClickListener(listener);
        findViewById(R.id.style_detailed_btn).setOnClickListener(listener);
        findViewById(R.id.style_step_btn).setOnClickListener(listener);

        // Default selection
        findViewById(R.id.style_detailed_btn).performClick();
    }

    private void updateUI() {
        step1Content.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        step2Content.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        step3Content.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);

        updateStepper();

        if (currentStep == 3) {
            continueButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.VISIBLE);
            finishButton.setVisibility(View.GONE);
        }
    }

    private void updateStepper() {
        // Reset colors
        int inactiveColor = ContextCompat.getColor(this, R.color.on_surface_variant);
        int activeColor = ContextCompat.getColor(this, R.color.primary);

        step1Circle.setBackgroundResource(currentStep >= 1 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step1Label.setTextColor(currentStep >= 1 ? activeColor : inactiveColor);

        step2Circle.setBackgroundResource(currentStep >= 2 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step2Label.setTextColor(currentStep >= 2 ? activeColor : inactiveColor);

        step3Circle.setBackgroundResource(currentStep >= 3 ? R.drawable.bg_step_circle_active : R.drawable.bg_step_circle);
        step3Label.setTextColor(currentStep >= 3 ? activeColor : inactiveColor);
        
        // Update text colors for numbers inside circles if needed
        ((TextView)step1Circle.getChildAt(0)).setTextColor(currentStep >= 1 ? activeColor : inactiveColor);
        ((TextView)step2Circle.getChildAt(0)).setTextColor(currentStep >= 2 ? activeColor : inactiveColor);
        ((TextView)step3Circle.getChildAt(0)).setTextColor(currentStep >= 3 ? activeColor : inactiveColor);
    }
}
