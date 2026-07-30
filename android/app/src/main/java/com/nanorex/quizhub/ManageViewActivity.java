package com.nanorex.quizhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ManageViewActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("View question bank");
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(true); scroll.setScrollbarFadingEnabled(false);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 48); scroll.addView(root); setContentView(scroll);
        addButton(root, "Main menu", MainMenuActivity.class, null);
        addButton(root, "Manage JSON", ManageActivity.class, null);
        String name = getIntent().getStringExtra("bank-name");
        try {
            JSONObject bank = new JSONObject(load(name, getIntent().getBooleanExtra("bank-bundled", true)));
            TextView heading = new TextView(this); heading.setText(bank.optString("title", name)); heading.setTextSize(24); root.addView(heading, wrap());
            JSONArray questions = bank.optJSONArray("questions");
            for (int i = 0; questions != null && i < questions.length(); i++) renderQuestion(root, i + 1, questions.getJSONObject(i));
        } catch (Exception error) { addText(root, "Could not read JSON: " + error.getMessage()); }
    }

    private void renderQuestion(LinearLayout root, int number, JSONObject question) {
        addText(root, "\n" + number + ". " + question.optString("question", question.optString("prompt", "")));
        JSONArray steps = question.optJSONArray("steps");
        if (steps != null) for (int i = 0; i < steps.length(); i++) try { JSONObject step = steps.getJSONObject(i); addText(root, "  Step " + (i + 1) + ": " + step.optString("prompt", "") + "\n  Answer: " + step.optJSONArray("acceptedAnswers")); } catch (Exception ignored) { }
        JSONArray answers = question.optJSONArray("answers");
        if (answers != null) for (int i = 0; i < answers.length(); i++) try { JSONObject answer = answers.getJSONObject(i); addText(root, "  " + answer.optString("id", "") + ". " + answer.optString("text", "") + (answer.optBoolean("correct", false) ? "  ✓" : "")); } catch (Exception ignored) { }
        if (question.has("answer")) addText(root, "  Answer: " + question.optString("answer"));
        if (question.has("explanation")) addText(root, "  Explanation: " + question.optString("explanation"));
    }

    private void addButton(LinearLayout root, String text, Class<?> target, String unused) { Button button = new Button(this); button.setText(text); button.setOnClickListener(view -> startActivity(new Intent(this, target))); root.addView(button, wrap()); }
    private static void addText(LinearLayout parent, String value) { TextView text = new TextView(parent.getContext()); text.setText(value); text.setTextSize(16); text.setPadding(0, 6, 0, 6); parent.addView(text, wrap()); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private String load(String name, boolean bundled) throws Exception { return bundled ? readAsset("question-banks/" + name) : readFile(new File(getFilesDir(), "managed-banks/" + name)); }
    private String readAsset(String path) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line); } return result.toString(); }
    private static String readFile(File file) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line).append('\n'); } return result.toString(); }
}
