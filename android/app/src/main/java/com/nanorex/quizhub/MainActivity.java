package com.nanorex.quizhub;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends AppCompatActivity {
    private final List<Question> questions = new ArrayList<>();
    private LinearLayout questionContainer;
    private TextView score;
    private int correct;
    private int answered;
    private boolean attemptRecorded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppPreferences.darkMode(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        setTitle("QuizHub");
        loadQuestionBanks();
        showQuiz();
    }

    private void loadQuestionBanks() {
        try {
            JSONObject manifest = new JSONObject(readAsset("question-banks/index.json"));
            JSONArray files = manifest.optJSONArray("files");
            if (files == null) return;
            for (int index = 0; index < files.length(); index++) {
                JSONObject bank = new JSONObject(readAsset("question-banks/" + files.getString(index)));
                JSONArray bankQuestions = bank.optJSONArray("questions");
                if (bankQuestions == null) continue;
                for (int questionIndex = 0; questionIndex < bankQuestions.length(); questionIndex++) {
                    JSONObject item = bankQuestions.getJSONObject(questionIndex);
                    String type = item.optString("type", "multiple-choice");
                    if ("multiple-choice".equals(type)) questions.add(new Question(item));
                }
            }
        } catch (Exception error) {
            Toast.makeText(this, "Could not load question banks: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showQuiz() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 48);
        scroll.addView(root);

        TextView heading = new TextView(this);
        heading.setText("QuizHub\nRandom practice quiz");
        heading.setTextSize(26);
        heading.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(heading, matchWrap());

        TextView status = new TextView(this);
        status.setText(questions.size() + " question(s) loaded from the shared JSON banks");
        root.addView(status, matchWrap());

        LinearLayout navigation = new LinearLayout(this);
        Button settings = new Button(this); settings.setText("Settings"); settings.setOnClickListener(view -> startActivity(new android.content.Intent(this, SettingsActivity.class)));
        Button statistics = new Button(this); statistics.setText("Statistics"); statistics.setOnClickListener(view -> startActivity(new android.content.Intent(this, StatisticsActivity.class)));
        navigation.addView(settings, new LinearLayout.LayoutParams(0, -2, 1)); navigation.addView(statistics, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(navigation, matchWrap());

        questionContainer = new LinearLayout(this);
        questionContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(questionContainer, matchWrap());

        Button submit = new Button(this);
        submit.setText("Check answers");
        submit.setOnClickListener(view -> checkAnswers());
        root.addView(submit, matchWrap());

        score = new TextView(this);
        score.setTextSize(18);
        root.addView(score, matchWrap());
        setContentView(scroll);
        renderRandomQuestions();
    }

    private void renderRandomQuestions() {
        List<Question> selected = new ArrayList<>(questions);
        Collections.shuffle(selected);
        int count = Math.min(AppPreferences.questionCount(this), selected.size());
        for (int index = 0; index < count; index++) {
            Question question = selected.get(index);
            TextView prompt = new TextView(this);
            prompt.setText((index + 1) + ". " + question.prompt());
            prompt.setTextSize(18);
            prompt.setPadding(0, 28, 0, 8);
            questionContainer.addView(prompt, matchWrap());
            LinearLayout choices = new LinearLayout(this);
            choices.setOrientation(LinearLayout.VERTICAL);
            for (int answerIndex = 0; answerIndex < question.answers.length(); answerIndex++) {
                JSONObject answer = question.answers.optJSONObject(answerIndex);
                if (answer == null) continue;
                CompoundButton button = question.multiple ? new CheckBox(this) : new RadioButton(this);
                button.setText(answer.optString("id") + ". " + localized(answer.opt("text")));
                button.setTag(answer.optString("id"));
                choices.addView(button, matchWrap()); question.controls.add(button);
            }
            questionContainer.addView(choices, matchWrap());
            question.view = choices;
            TextView correctAnswer = new TextView(this);
            correctAnswer.setVisibility(TextView.GONE);
            correctAnswer.setTextSize(16);
            correctAnswer.setPadding(0, 8, 0, 16);
            question.correctAnswerView = correctAnswer;
            questionContainer.addView(correctAnswer, matchWrap());
        }
    }

    private void checkAnswers() {
        correct = 0; answered = 0; int points = 0;
        for (Question question : questions) {
            if (question.view == null) continue;
            points++;
            List<String> selected = new ArrayList<>();
            for (CompoundButton control : question.controls) if (control.isChecked()) selected.add(String.valueOf(control.getTag()));
            if (!selected.isEmpty()) answered++;
            if (selected.size() == question.correctAnswers.size() && selected.containsAll(question.correctAnswers)) { correct++; question.lastScore = 1; } else question.lastScore = 0;
            question.correctAnswerView.setText("Correct answer" + (question.correctAnswers.size() > 1 ? "s" : "") + ": " + question.correctAnswerText());
            question.correctAnswerView.setVisibility(TextView.VISIBLE);
        }
        score.setText("Score: " + correct + " / " + points + " point(s) (" + answered + " answered)");
        if (!attemptRecorded) { StatisticsStore.record(this, correct, points, questions); attemptRecorded = true; }
    }

    private String readAsset(String path) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private static String localized(Object value) {
        if (value instanceof String) return (String) value;
        if (value instanceof JSONObject) {
            JSONObject text = (JSONObject) value;
            return text.optString("en", text.optString("zh-Hans", text.optString("ms", "")));
        }
        return "";
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static final class Question {
        final JSONObject data;
        final JSONArray answers;
        final List<String> correctAnswers = new ArrayList<>();
        final String subject;
        final String topic;
        final boolean multiple;
        final List<CompoundButton> controls = new ArrayList<>();
        LinearLayout view;
        TextView correctAnswerView;
        int lastScore;

        Question(JSONObject data) {
            this.data = data;
            this.answers = data.optJSONArray("answers") == null ? new JSONArray() : data.optJSONArray("answers");
            Object answersValue = data.opt("correctAnswer");
            if (answersValue instanceof JSONArray) for (int index = 0; index < ((JSONArray) answersValue).length(); index++) correctAnswers.add(((JSONArray) answersValue).optString(index));
            else correctAnswers.add(data.optString("correctAnswer"));
            this.multiple = correctAnswers.size() > 1 || data.optInt("selectionCount", 1) > 1;
            this.subject = data.optString("subject", "General");
            this.topic = data.optString("topic", "General");
        }

        String prompt() { return localized(data.opt("question")); }

        String correctAnswerText() {
            List<String> labels = new ArrayList<>();
            for (String id : correctAnswers) {
                for (int index = 0; index < answers.length(); index++) {
                    JSONObject answer = answers.optJSONObject(index);
                    if (answer != null && id.equals(answer.optString("id"))) {
                        labels.add(id + ". " + localized(answer.opt("text")));
                        break;
                    }
                }
            }
            return String.join(" | ", labels);
        }
    }
}
