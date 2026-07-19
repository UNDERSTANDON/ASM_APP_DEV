package com.example.testui;

import android.app.Activity;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    public static void setupBottomNavigation(Activity activity, int selectedItemId) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        // Force clearing the listener before setting the selection to avoid immediate re-trigger
        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;

            Class<?> targetActivity = null;
            if (id == R.id.nav_home) {
                targetActivity = MainActivity.class;
            } else if (id == R.id.nav_insights) {
                targetActivity = AIInsightsActivity.class;
            } else if (id == R.id.nav_quiz) {
                targetActivity = QuizLabActivity.class;
            } else if (id == R.id.nav_saved) {
                targetActivity = AnswerHistoryActivity.class;
            } else if (id == R.id.nav_alerts) {
                targetActivity = NotificationsActivity.class;
            } else if (id == R.id.nav_profile) {
                targetActivity = ProfileActivity.class;
            }

            if (targetActivity != null) {
                Intent intent = new Intent(activity, targetActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                // activity.overridePendingTransition(0, 0); // Disable transition for smoother feel
                return true;
            }
            return false;
        });
    }
}
