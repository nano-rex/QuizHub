package com.nanorex.quizhub;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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
    private Button newQuiz;
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
                    if ("multiple-choice".equals(type) || "multi-step".equals(type)) questions.add(new Question(item));
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
        newQuiz = new Button(this);
        newQuiz.setText("Start a new quiz");
        newQuiz.setVisibility(Button.GONE);
        newQuiz.setOnClickListener(view -> {
            attemptRecorded = false;
            score.setText("");
            newQuiz.setVisibility(Button.GONE);
            renderRandomQuestions();
        });
        root.addView(newQuiz, matchWrap());
        setContentView(scroll);
        renderRandomQuestions();
    }

    private void renderRandomQuestions() {
        questionContainer.removeAllViews();
        for (Question question : questions) {
            question.view = null;
            question.controls.clear();
            question.stepInputs.clear();
            question.correctAnswerView = null;
            question.lastScore = 0;
            question.lastPoints = 0;
        }
        List<Question> selected = new ArrayList<>();
        for (Question question : questions) if (AppPreferences.isSubjectEnabled(this, question.subject)) selected.add(question);
        List<String> languages = AppPreferences.languages(this);
        Collections.shuffle(selected);
        int count = Math.min(AppPreferences.questionCount(this), selected.size());
        for (int index = 0; index < count; index++) {
            Question question = selected.get(index);
            TextView prompt = new TextView(this);
            prompt.setText((index + 1) + ". " + question.prompt(languages));
            prompt.setTextSize(18);
            prompt.setPadding(0, 28, 0, 8);
            questionContainer.addView(prompt, matchWrap());
            LinearLayout choices = new LinearLayout(this);
            choices.setOrientation(LinearLayout.VERTICAL);
            if (question.multiStep) {
                JSONArray steps = question.data.optJSONArray("steps");
                for (int stepIndex = 0; steps != null && stepIndex < steps.length(); stepIndex++) {
                    JSONObject step = steps.optJSONObject(stepIndex);
                    if (step == null) continue;
                    TextView stepPrompt = new TextView(this);
                    stepPrompt.setText((stepIndex + 1) + ". " + localized(step.opt("prompt"), languages));
                    choices.addView(stepPrompt, matchWrap());
                    EditText input = new EditText(this);
                    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                    input.setSingleLine(true);
                    choices.addView(input, matchWrap());
                    question.stepInputs.add(input);
                }
            } else {
                if (question.multiple) {
                    TextView instruction = new TextView(this);
                    instruction.setText("Select all that apply");
                    choices.addView(instruction, matchWrap());
                }
                for (int answerIndex = 0; answerIndex < question.answers.length(); answerIndex++) {
                    JSONObject answer = question.answers.optJSONObject(answerIndex);
                    if (answer == null) continue;
                    CompoundButton button = question.multiple ? new CheckBox(this) : new RadioButton(this);
                    button.setText(answer.optString("id") + ". " + localized(answer.opt("text"), languages));
                    button.setTag(answer.optString("id"));
                    choices.addView(button, matchWrap()); question.controls.add(button);
                }
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
            if (question.multiStep) {
                int questionCorrect = 0;
                for (int stepIndex = 0; stepIndex < question.stepInputs.size(); stepIndex++) {
                    EditText input = question.stepInputs.get(stepIndex);
                    JSONObject step = question.data.optJSONArray("steps").optJSONObject(stepIndex);
                    boolean stepCorrect = isStepCorrect(input.getText().toString(), step);
                    if (stepCorrect) questionCorrect++;
                    if (!input.getText().toString().trim().isEmpty()) answered++;
                }
                points += question.stepInputs.size();
                correct += questionCorrect;
                question.lastScore = questionCorrect;
                question.lastPoints = question.stepInputs.size();
                question.correctAnswerView.setText("Correct answers:\n" + question.correctAnswerText(AppPreferences.languages(this)));
                question.correctAnswerView.setVisibility(TextView.VISIBLE);
                continue;
            }
            points++;
            List<String> selected = new ArrayList<>();
            for (CompoundButton control : question.controls) if (control.isChecked()) selected.add(String.valueOf(control.getTag()));
            if (!selected.isEmpty()) answered++;
            if (selected.size() == question.correctAnswers.size() && selected.containsAll(question.correctAnswers)) { correct++; question.lastScore = 1; } else question.lastScore = 0;
            question.correctAnswerView.setText("Correct answer" + (question.correctAnswers.size() > 1 ? "s" : "") + ": " + question.correctAnswerText(AppPreferences.languages(this)));
            question.correctAnswerView.setVisibility(TextView.VISIBLE);
            question.lastPoints = 1;
        }
        score.setText("Score: " + correct + " / " + points + " point(s) (" + answered + " answered)");
        newQuiz.setVisibility(Button.VISIBLE);
        if (!attemptRecorded) { StatisticsStore.record(this, correct, points, questions); attemptRecorded = true; }
    }

    private static boolean isStepCorrect(String value, JSONObject step) {
        String normalized = value.trim().replace(",", "").toLowerCase();
        JSONArray accepted = step.optJSONArray("acceptedAnswers");
        if (accepted == null || normalized.isEmpty()) return false;
        for (int index = 0; index < accepted.length(); index++) {
            Object answer = accepted.opt(index);
            if (String.valueOf(answer).trim().replace(",", "").toLowerCase().equals(normalized)) return true;
        }
        if (step.has("tolerance")) {
            try {
                double actual = Double.parseDouble(normalized);
                double tolerance = step.optDouble("tolerance");
                for (int index = 0; index < accepted.length(); index++) {
                    if (Math.abs(actual - accepted.optDouble(index)) <= tolerance) return true;
                }
            } catch (NumberFormatException ignored) { }
        }
        return false;
    }

    private String readAsset(String path) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private static String localized(Object value, List<String> languages) {
        if (value instanceof String) return (String) value;
        if (value instanceof JSONObject) {
            JSONObject text = (JSONObject) value;
            List<String> lines = new ArrayList<>();
            for (String language : languages) {
                String translation = text.optString(language, "");
                if (!translation.isEmpty()) lines.add(languageName(language) + ": " + translation);
            }
            if (!lines.isEmpty()) return String.join("\n", lines);
            return text.optString("en", text.optString("zh-Hans", text.optString("ms", "")));
        }
        return "";
    }

    private static String languageName(String code) {
        if ("zh-Hans".equals(code)) return "简体中文";
        if ("zh-Hant".equals(code)) return "繁體中文";
        if ("ms".equals(code)) return "Bahasa Melayu";
        return "English";
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
        final boolean multiStep;
        final List<CompoundButton> controls = new ArrayList<>();
        final List<EditText> stepInputs = new ArrayList<>();
        LinearLayout view;
        TextView correctAnswerView;
        int lastScore;
        int lastPoints;

        Question(JSONObject data) {
            this.data = data;
            this.answers = data.optJSONArray("answers") == null ? new JSONArray() : data.optJSONArray("answers");
            Object answersValue = data.opt("correctAnswer");
            if (answersValue instanceof JSONArray) for (int index = 0; index < ((JSONArray) answersValue).length(); index++) correctAnswers.add(((JSONArray) answersValue).optString(index));
            else correctAnswers.add(data.optString("correctAnswer"));
            this.multiple = correctAnswers.size() > 1 || data.optInt("selectionCount", 1) > 1;
            this.multiStep = "multi-step".equals(data.optString("type"));
            this.subject = data.optString("subject", "General");
            this.topic = data.optString("topic", "General");
        }

        String prompt(List<String> languages) { return localized(data.opt("question"), languages); }

        String correctAnswerText(List<String> languages) {
            List<String> labels = new ArrayList<>();
            if (multiStep) {
                JSONArray steps = data.optJSONArray("steps");
                for (int index = 0; steps != null && index < steps.length(); index++) {
                    JSONArray accepted = steps.optJSONObject(index).optJSONArray("acceptedAnswers");
                    if (accepted != null && accepted.length() > 0) labels.add((index + 1) + ". " + accepted.join(" / "));
                }
                return String.join("\n", labels);
            }
            for (String id : correctAnswers) {
                for (int index = 0; index < answers.length(); index++) {
                    JSONObject answer = answers.optJSONObject(index);
                    if (answer != null && id.equals(answer.optString("id"))) {
                        labels.add(id + ". " + localized(answer.opt("text"), languages));
                        break;
                    }
                }
            }
            return String.join(" | ", labels);
        }
    }
}
