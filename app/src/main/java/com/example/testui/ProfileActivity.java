package com.example.testui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        Spinner spinner = findViewById(R.id.spinner_education);
        String[] levels = {"Trung học phổ thông", "Cao đẳng / Đại học", "Sau đại học", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(1); // Default to College/Uni

        NavigationHelper.setupBottomNavigation(this, R.id.nav_profile);
    }
}