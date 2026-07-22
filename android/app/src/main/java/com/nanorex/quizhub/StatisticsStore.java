package com.nanorex.quizhub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class StatisticsStore {
    private static final String NAME = "quizhub-statistics";
    private static final String HISTORY = "history";

    private StatisticsStore() { }

    public static void record(Context context, int score, int points, List<MainActivity.Question> questions) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            JSONArray history = new JSONArray(preferences.getString(HISTORY, "[]"));
            JSONArray breakdown = new JSONArray();
            for (MainActivity.Question question : questions) {
                if (question.view == null) continue;
                int selectedId = question.view.getCheckedRadioButtonId();
                if (selectedId == -1) continue;
                JSONObject item = new JSONObject();
                item.put("subject", question.subject);
                item.put("topic", question.topic);
                item.put("questions", 1);
                item.put("points", 1);
                item.put("score", question.correctAnswer.equals(String.valueOf(question.view.findViewById(selectedId).getTag())) ? 1 : 0);
                breakdown.put(item);
            }
            JSONObject attempt = new JSONObject();
            attempt.put("score", score); attempt.put("points", points); attempt.put("breakdown", breakdown);
            history.put(attempt);
            preferences.edit().putString(HISTORY, history.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static JSONArray history(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(HISTORY, "[]"));
        } catch (Exception error) {
            return new JSONArray();
        }
    }

    public static void clear(Context context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
