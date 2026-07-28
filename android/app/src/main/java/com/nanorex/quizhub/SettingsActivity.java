package com.nanorex.quizhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("QuizHub Settings");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 32);

        TextView countLabel = new TextView(this); countLabel.setText("Questions to fetch"); root.addView(countLabel);
        EditText count = new EditText(this); count.setInputType(2); count.setText(String.valueOf(AppPreferences.questionCount(this))); root.addView(count, matchWrap());
        TextView languageLabel = new TextView(this); languageLabel.setText("Display languages (select one or more)"); root.addView(languageLabel, matchWrap());
        String[] codes = {"en", "zh-Hans", "zh-Hant", "ms"};
        String[] names = {"English", "Simplified Chinese", "Traditional Chinese", "Bahasa Melayu"};
        List<String> savedLanguages = AppPreferences.languages(this);
        List<CheckBox> languageControls = new ArrayList<>();
        for (int index = 0; index < codes.length; index++) {
            CheckBox control = new CheckBox(this);
            control.setText(names[index]);
            control.setTag(codes[index]);
            control.setChecked(savedLanguages.contains(codes[index]));
            languageControls.add(control);
            root.addView(control, matchWrap());
        }
        Switch darkMode = new Switch(this); darkMode.setText("Dark mode"); darkMode.setChecked(AppPreferences.darkMode(this)); root.addView(darkMode, matchWrap());
        TextView subjectsLabel = new TextView(this); subjectsLabel.setText("Enabled subjects"); root.addView(subjectsLabel, matchWrap());
        List<CheckBox> subjectControls = new ArrayList<>();
        Set<String> savedSubjects = AppPreferences.enabledSubjects(this);
        for (String subject : loadSubjects()) {
            CheckBox control = new CheckBox(this);
            control.setText(subject);
            control.setChecked(savedSubjects == null || savedSubjects.contains(subject));
            subjectControls.add(control);
            root.addView(control, matchWrap());
        }
        Button save = new Button(this); save.setText("Save settings"); root.addView(save, matchWrap());
        save.setOnClickListener(view -> {
            int number;
            try { number = Math.max(1, Integer.parseInt(count.getText().toString())); } catch (NumberFormatException error) { number = 10; }
            Set<String> enabledSubjects = new HashSet<>();
            for (CheckBox control : subjectControls) if (control.isChecked()) enabledSubjects.add(control.getText().toString());
            List<String> languages = new ArrayList<>();
            for (CheckBox control : languageControls) if (control.isChecked()) languages.add(String.valueOf(control.getTag()));
            if (languages.isEmpty()) languages.add("en");
            AppPreferences.save(this, number, languages, darkMode.isChecked(), enabledSubjects);
            AppCompatDelegate.setDefaultNightMode(darkMode.isChecked() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            finish();
        });
        setContentView(root);
    }

    private List<String> loadSubjects() {
        Set<String> subjects = new HashSet<>();
        try {
            JSONObject manifest = new JSONObject(readAsset("question-banks/index.json"));
            JSONArray files = manifest.optJSONArray("files");
            if (files == null) return new ArrayList<>();
            for (int index = 0; index < files.length(); index++) {
                JSONObject bank = new JSONObject(readAsset("question-banks/" + files.getString(index)));
                JSONArray questions = bank.optJSONArray("questions");
                if (questions == null) continue;
                for (int questionIndex = 0; questionIndex < questions.length(); questionIndex++) {
                    JSONObject question = questions.getJSONObject(questionIndex);
                    if (!"multiple-choice".equals(question.optString("type", "multiple-choice"))) continue;
                    String subject = question.optString("subject", "General");
                    if (!subject.isEmpty()) subjects.add(subject);
                }
            }
        } catch (Exception error) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>(subjects);
        Collections.sort(result);
        return result;
    }

    private String readAsset(String path) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
