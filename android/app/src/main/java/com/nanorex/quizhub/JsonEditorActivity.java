package com.nanorex.quizhub;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.InputType;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonEditorActivity extends AppCompatActivity {
    private EditText editor, find, replacement; private TextView status, lineNumbers; private String name; private boolean bundled; private int current; private List<int[]> matches = new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Edit JSON"); name = getIntent().getStringExtra("bank-name"); bundled = getIntent().getBooleanExtra("bank-bundled", true);
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(true); scroll.setScrollbarFadingEnabled(false); LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 24, 24, 48); scroll.addView(root); setContentView(scroll);
        nav(root, "Main menu", MainMenuActivity.class); nav(root, "Manage JSON", ManageActivity.class); addText(root, "Editing: " + name, 22);
        find = new EditText(this); find.setHint("Find"); find.setSingleLine(true); root.addView(find, wrap()); replacement = new EditText(this); replacement.setHint("Replace with"); replacement.setSingleLine(true); root.addView(replacement, wrap());
        LinearLayout actions = new LinearLayout(this); String[] labels = {"Previous", "Next", "Replace current", "Replace all", "Save JSON"}; for (String label : labels) { Button button = new Button(this); button.setText(label); actions.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); if (label.equals("Previous")) button.setOnClickListener(v -> move(-1)); if (label.equals("Next")) button.setOnClickListener(v -> move(1)); if (label.equals("Replace current")) button.setOnClickListener(v -> replaceCurrent()); if (label.equals("Replace all")) button.setOnClickListener(v -> replaceAll()); if (label.equals("Save JSON")) button.setOnClickListener(v -> save()); } root.addView(actions, wrap());
        status = new TextView(this); root.addView(status, wrap());
        LinearLayout code = new LinearLayout(this); code.setGravity(Gravity.TOP); lineNumbers = new TextView(this); lineNumbers.setGravity(Gravity.TOP | Gravity.RIGHT); lineNumbers.setTextSize(12); code.addView(lineNumbers, new LinearLayout.LayoutParams(58, -2)); editor = new EditText(this); editor.setGravity(Gravity.TOP); editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); editor.setSingleLine(false); editor.setHorizontallyScrolling(false); editor.setTextSize(12); editor.setMinLines(24); editor.setMaxLines(Integer.MAX_VALUE); try { editor.setText(load()); } catch (Exception error) { editor.setText("{}"); status.setText("Could not read JSON: " + error.getMessage()); } code.addView(editor, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(code, wrap());
        find.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int st, int c, int a) {} public void onTextChanged(CharSequence s, int st, int b, int c) { current = 0; highlight(); } public void afterTextChanged(Editable e) {} }); highlight();
    }
    private void nav(LinearLayout root, String label, Class<?> target) { Button button = new Button(this); button.setText(label); button.setOnClickListener(v -> startActivity(new Intent(this, target))); root.addView(button, wrap()); }
    private void addText(LinearLayout root, String value, float size) { TextView text = new TextView(this); text.setText(value); text.setTextSize(size); root.addView(text, wrap()); }
    private void findMatches(String value) { matches = new ArrayList<>(); String query = find.getText().toString(); if (query.isEmpty()) return; Matcher matcher = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(value); while (matcher.find()) matches.add(new int[]{matcher.start(), matcher.end()}); }
    private void highlight() { if (editor == null) return; String value = editor.getText().toString(); findMatches(value); SpannableStringBuilder styled = new SpannableStringBuilder(value); for (int i = 0; i < matches.size(); i++) styled.setSpan(new BackgroundColorSpan(i == current ? Color.rgb(255, 170, 0) : Color.rgb(245, 217, 10)), matches.get(i)[0], matches.get(i)[1], 0); editor.setText(styled); if (!matches.isEmpty()) editor.setSelection(matches.get(Math.min(current, matches.size() - 1))[0]); lineNumbers.setText(lines(value)); status.setText(find.getText().length() == 0 ? "Ready" : matches.size() + " match(es)"); }
    private String lines(String value) { StringBuilder result = new StringBuilder(); int count = value.split("\n", -1).length; for (int i = 1; i <= count; i++) result.append(i).append('\n'); return result.toString(); }
    private void move(int direction) { if (matches.isEmpty()) return; current = (current + direction + matches.size()) % matches.size(); highlight(); }
    private void replaceCurrent() { if (matches.isEmpty()) return; String value = editor.getText().toString(); int[] match = matches.get(current); editor.setText(value.substring(0, match[0]) + replacement.getText() + value.substring(match[1])); highlight(); }
    private void replaceAll() { String query = find.getText().toString(); if (query.isEmpty()) return; editor.setText(editor.getText().toString().replaceAll("(?i)" + Pattern.quote(query), Matcher.quoteReplacement(replacement.getText().toString()))); highlight(); }
    private void save() { try { JSONObject parsed = new JSONObject(editor.getText().toString()); File directory = new File(getFilesDir(), "managed-banks"); directory.mkdirs(); if (bundled) { name = "managed-" + System.currentTimeMillis() + ".json"; bundled = false; } File file = new File(directory, name); Files.write(file.toPath(), parsed.toString(2).getBytes(StandardCharsets.UTF_8)); Toast.makeText(this, "JSON saved as a local copy.", Toast.LENGTH_SHORT).show(); } catch (Exception error) { Toast.makeText(this, "Invalid JSON: " + error.getMessage(), Toast.LENGTH_LONG).show(); } }
    private String load() throws Exception { return bundled ? readAsset("question-banks/" + name) : readFile(new File(getFilesDir(), "managed-banks/" + name)); }
    private String readAsset(String path) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line).append('\n'); } return result.toString(); }
    private static String readFile(File file) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line).append('\n'); } return result.toString(); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
}
