package com.nanorex.quizhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public final class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("QuizHub Settings");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 32);

        TextView countLabel = new TextView(this); countLabel.setText("Questions to fetch"); root.addView(countLabel);
        EditText count = new EditText(this); count.setInputType(2); count.setText(String.valueOf(AppPreferences.questionCount(this))); root.addView(count, matchWrap());
        TextView languageLabel = new TextView(this); languageLabel.setText("Display language"); root.addView(languageLabel);
        Spinner language = new Spinner(this);
        language.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"English", "Simplified Chinese", "Traditional Chinese", "Bahasa Melayu"}));
        String[] codes = {"en", "zh-Hans", "zh-Hant", "ms"};
        for (int index = 0; index < codes.length; index++) if (codes[index].equals(AppPreferences.language(this))) language.setSelection(index);
        root.addView(language, matchWrap());
        Switch darkMode = new Switch(this); darkMode.setText("Dark mode"); darkMode.setChecked(AppPreferences.darkMode(this)); root.addView(darkMode, matchWrap());
        Button save = new Button(this); save.setText("Save settings"); root.addView(save, matchWrap());
        save.setOnClickListener(view -> {
            int number;
            try { number = Math.max(1, Integer.parseInt(count.getText().toString())); } catch (NumberFormatException error) { number = 10; }
            AppPreferences.save(this, number, codes[language.getSelectedItemPosition()], darkMode.isChecked());
            AppCompatDelegate.setDefaultNightMode(darkMode.isChecked() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            finish();
        });
        setContentView(root);
    }

    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
