package com.nanorex.quizhub;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.text.Editable;
import android.text.TextWatcher;

public final class QuestionBankActivity extends AppCompatActivity {
    private final List<JSONObject> banks = new ArrayList<>();
    private LinearLayout questions;
    private Spinner selector;
    private EditText search;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Question Banks");
        questions = new LinearLayout(this); questions.setOrientation(LinearLayout.VERTICAL);
        loadBanks();
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(true); scroll.setScrollbarFadingEnabled(false);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 48);
        Button mainMenu = new Button(this); mainMenu.setText("Main menu"); mainMenu.setOnClickListener(view -> startActivity(new android.content.Intent(this, MainMenuActivity.class))); root.addView(mainMenu, matchWrap());
        TextView heading = new TextView(this); heading.setText("Question banks"); heading.setTextSize(26); heading.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(heading, matchWrap());
        selector = new Spinner(this);
        List<String> labels = new ArrayList<>();
        labels.add("All question banks (" + totalQuestions() + ")");
        for (JSONObject bank : banks) labels.add(bank.optString("title", "Question bank") + " (" + bank.optJSONArray("questions").length() + ")");
        selector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        root.addView(selector, matchWrap());
        search = new EditText(this); search.setHint("Search questions in selected scope"); search.setSingleLine(true); root.addView(search, matchWrap());
        root.addView(questions, matchWrap());
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) { render(position); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        search.addTextChangedListener(new TextWatcher() { @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { } @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(selector.getSelectedItemPosition()); } @Override public void afterTextChanged(Editable s) { } });
        scroll.addView(root); setContentView(scroll);
        if (banks.isEmpty()) addText("No JSON question banks are available.", 18);
    }

    private void loadBanks() {
        try {
            JSONObject manifest = new JSONObject(readAsset("question-banks/index.json")); JSONArray files = manifest.optJSONArray("files");
            for (int index = 0; files != null && index < files.length(); index++) banks.add(new JSONObject(readAsset("question-banks/" + files.getString(index))));
        } catch (Exception error) { addText("Could not load question banks: " + error.getMessage(), 16); }
    }

    private void render(int selection) {
        questions.removeAllViews(); List<String> languages = AppPreferences.languages(this); String query = search == null ? "" : search.getText().toString().trim().toLowerCase(); int number = 0;
        int start = selection == 0 ? 0 : selection - 1; int end = selection == 0 ? banks.size() : selection;
        for (int bankIndex = start; bankIndex < end; bankIndex++) {
            JSONObject bank = banks.get(bankIndex); JSONArray items = bank.optJSONArray("questions");
            for (int index = 0; items != null && index < items.length(); index++) {
                JSONObject question = items.optJSONObject(index); if (question == null || (!query.isEmpty() && !question.toString().toLowerCase().contains(query))) continue;
                number++;
                addText(number + ". " + (selection == 0 ? bank.optString("title", "Question bank") + " · " : "") + question.optString("subject", "General") + " · " + question.optString("topic", "General"), 18);
            addText(localized(question.opt("question"), languages), 16);
            if ("multi-step".equals(question.optString("type"))) {
                JSONArray steps = question.optJSONArray("steps");
                for (int stepIndex = 0; steps != null && stepIndex < steps.length(); stepIndex++) {
                    JSONObject step = steps.optJSONObject(stepIndex); JSONArray accepted = step.optJSONArray("acceptedAnswers");
                    addText((stepIndex + 1) + ". " + localized(step.opt("prompt"), languages) + "\nAnswer: " + values(accepted), 15);
                }
            } else if ("source-reference".equals(question.optString("type"))) {
                addText(question.optString("answerStatus", "Reference entry"), 15);
            } else {
                JSONArray answers = question.optJSONArray("answers"); Object correctValue = question.opt("correctAnswer"); List<String> correct = new ArrayList<>();
                if (correctValue instanceof JSONArray) for (int answerIndex = 0; answerIndex < ((JSONArray) correctValue).length(); answerIndex++) correct.add(((JSONArray) correctValue).optString(answerIndex)); else correct.add(question.optString("correctAnswer"));
                for (int answerIndex = 0; answers != null && answerIndex < answers.length(); answerIndex++) {
                    JSONObject answer = answers.optJSONObject(answerIndex); String id = answer.optString("id");
                    addText(id + ". " + localized(answer.opt("text"), languages) + (correct.contains(id) ? " ✓" : ""), 15);
                }
            }
                addText("", 8);
            }
        }
    }

    private int totalQuestions() { int total = 0; for (JSONObject bank : banks) total += bank.optJSONArray("questions").length(); return total; }

    private void addText(String value, float size) { TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setPadding(0, 8, 0, 8); questions.addView(text, matchWrap()); }
    private static String values(JSONArray values) { List<String> result = new ArrayList<>(); for (int index = 0; values != null && index < values.length(); index++) result.add(values.optString(index)); return String.join(" / ", result); }
    private static String localized(Object value, List<String> languages) { if (value instanceof String) return (String) value; if (!(value instanceof JSONObject)) return ""; JSONObject text = (JSONObject) value; List<String> result = new ArrayList<>(); for (String language : languages) if (!text.optString(language, "").isEmpty()) result.add(languageName(language) + ": " + text.optString(language)); return String.join("\n", result); }
    private static String languageName(String code) { if ("zh-Hans".equals(code)) return "简体中文"; if ("zh-Hant".equals(code)) return "繁體中文"; if ("ms".equals(code)) return "Bahasa Melayu"; return "English"; }
    private String readAsset(String path) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line); } return result.toString(); }
    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
