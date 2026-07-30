package com.nanorex.quizhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public final class MainMenuActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); AppCompatDelegate.setDefaultNightMode(AppPreferences.darkMode(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO); setTitle("QuizHub");
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(true); scroll.setScrollbarFadingEnabled(false);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 48, 32, 48); scroll.addView(root);
        TextView title = new TextView(this); title.setText("QuizHub\nMain menu"); title.setTextSize(28); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title, wrap());
        add(root, "Start quiz", MainActivity.class); add(root, "Settings", SettingsActivity.class); add(root, "Statistics", StatisticsActivity.class); add(root, "Manage JSON", ManageActivity.class);
        setContentView(scroll);
    }
    private void add(LinearLayout root, String label, Class<?> target) { Button button = new Button(this); button.setText(label); button.setOnClickListener(view -> startActivity(new Intent(this, target))); root.addView(button, wrap()); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
