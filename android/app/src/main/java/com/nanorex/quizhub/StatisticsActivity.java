package com.nanorex.quizhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class StatisticsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("QuizHub Statistics"); render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 32);
        JSONArray history = StatisticsStore.history(this); int score = 0; int points = 0;
        Map<String, int[]> subjects = new HashMap<>(); Map<String, int[]> topics = new HashMap<>();
        try {
            for (int i = 0; i < history.length(); i++) {
                JSONObject attempt = history.getJSONObject(i); score += attempt.optInt("score"); points += attempt.optInt("points");
                JSONArray breakdown = attempt.optJSONArray("breakdown"); if (breakdown == null) continue;
                for (int j = 0; j < breakdown.length(); j++) {
                    JSONObject item = breakdown.getJSONObject(j);
                    add(subjects, item.optString("subject", "General"), item.optInt("score"), item.optInt("points"));
                    add(topics, item.optString("topic", "General"), item.optInt("score"), item.optInt("points"));
                }
            }
        } catch (Exception ignored) { }
        TextView overall = new TextView(this); overall.setText("Tests taken: " + history.length() + "\nOverall performance: " + percent(score, points) + "%"); overall.setTextSize(20); root.addView(overall, matchWrap());
        addSection(root, "By subject", subjects); addSection(root, "By topic", topics);
        Button clear = new Button(this); clear.setText("Clear statistics"); clear.setOnClickListener(view -> { StatisticsStore.clear(this); render(); }); root.addView(clear, matchWrap());
        setContentView(root);
    }

    private static void addSection(LinearLayout root, String title, Map<String, int[]> values) {
        TextView heading = new TextView(root.getContext()); heading.setText(title); heading.setTextSize(18); root.addView(heading, matchWrap());
        for (Map.Entry<String, int[]> entry : values.entrySet()) root.addView(text(root, entry.getKey() + ": " + percent(entry.getValue()[0], entry.getValue()[1]) + "% (" + entry.getValue()[0] + "/" + entry.getValue()[1] + ")"), matchWrap());
    }

    private static void add(Map<String, int[]> map, String key, int score, int points) { int[] value = map.computeIfAbsent(key, ignored -> new int[2]); value[0] += score; value[1] += points; }
    private static int percent(int score, int points) { return points == 0 ? 0 : Math.round(score * 100f / points); }
    private static TextView text(LinearLayout root, String value) { TextView result = new TextView(root.getContext()); result.setText(value); result.setPadding(0, 8, 0, 8); return result; }
    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
