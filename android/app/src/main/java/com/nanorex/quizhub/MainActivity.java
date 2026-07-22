package com.nanorex.quizhub;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        int count = Math.min(10, selected.size());
        for (int index = 0; index < count; index++) {
            Question question = selected.get(index);
            TextView prompt = new TextView(this);
            prompt.setText((index + 1) + ". " + question.prompt());
            prompt.setTextSize(18);
            prompt.setPadding(0, 28, 0, 8);
            questionContainer.addView(prompt, matchWrap());
            RadioGroup choices = new RadioGroup(this);
            for (int answerIndex = 0; answerIndex < question.answers.length(); answerIndex++) {
                JSONObject answer = question.answers.optJSONObject(answerIndex);
                if (answer == null) continue;
                RadioButton button = new RadioButton(this);
                button.setText(answer.optString("id") + ". " + localized(answer.opt("text")));
                button.setTag(answer.optString("id"));
                choices.addView(button, matchWrap());
            }
            questionContainer.addView(choices, matchWrap());
            question.view = choices;
        }
    }

    private void checkAnswers() {
        correct = 0; answered = 0;
        for (Question question : questions) {
            if (question.view == null) continue;
            int checked = question.view.getCheckedRadioButtonId();
            if (checked == -1) continue;
            answered++;
            RadioButton selected = question.view.findViewById(checked);
            if (question.correctAnswer.equals(String.valueOf(selected.getTag()))) correct++;
        }
        score.setText("Score: " + correct + " / " + answered + " answered");
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

    private static final class Question {
        final JSONObject data;
        final JSONArray answers;
        final String correctAnswer;
        RadioGroup view;

        Question(JSONObject data) {
            this.data = data;
            this.answers = data.optJSONArray("answers") == null ? new JSONArray() : data.optJSONArray("answers");
            this.correctAnswer = data.optString("correctAnswer");
        }

        String prompt() { return localized(data.opt("question")); }
    }
}
