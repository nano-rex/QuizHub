package com.nanorex.quizhub;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ManageActivity extends AppCompatActivity {
    private static final int CREATE_ARCHIVE = 41;
    private final List<BankFile> files = new ArrayList<>();
    private final List<CheckBox> controls = new ArrayList<>();
    private LinearLayout fileList;
    private LinearLayout root;
    private EditText editor;
    private EditText find;
    private EditText replacement;
    private TextView editorStatus;
    private BankFile editing;
    private int matchIndex;
    private List<int[]> matches = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Manage JSON"); loadFiles();
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(true); scroll.setScrollbarFadingEnabled(false);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 32, 32, 48); scroll.addView(root); setContentView(scroll);
        Button mainMenu = new Button(this); mainMenu.setText("Main menu"); mainMenu.setOnClickListener(view -> startActivity(new Intent(this, MainMenuActivity.class))); root.addView(mainMenu, wrap());
        TextView title = new TextView(this); title.setText("Manage JSON files"); title.setTextSize(26); root.addView(title, wrap());
        fileList = new LinearLayout(this); fileList.setOrientation(LinearLayout.VERTICAL); root.addView(fileList, wrap()); renderFiles();
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.VERTICAL);
        Button export = button("Export selected"); export.setOnClickListener(view -> exportSelected()); actions.addView(export, wrap());
        Button delete = button("Delete selected"); delete.setOnClickListener(view -> deleteSelected()); actions.addView(delete, wrap());
        Button edit = button("Edit selected"); edit.setOnClickListener(view -> editSelected()); actions.addView(edit, wrap()); root.addView(actions, wrap());
        TextView note = new TextView(this); note.setText("Bundled files are protected. Edited copies are stored on this device."); note.setPadding(0, 12, 0, 12); root.addView(note, wrap());
    }

    private void loadFiles() {
        files.clear();
        try {
            JSONObject manifest = new JSONObject(readAsset("question-banks/index.json")); JSONArray names = manifest.optJSONArray("files");
            for (int index = 0; names != null && index < names.length(); index++) files.add(new BankFile(names.getString(index), readAsset("question-banks/" + names.getString(index)), true, null));
        } catch (Exception error) { Toast.makeText(this, "Could not load bundled banks: " + error.getMessage(), Toast.LENGTH_LONG).show(); }
        File directory = new File(getFilesDir(), "managed-banks"); File[] managed = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (managed != null) for (File file : managed) try { files.add(new BankFile(file.getName(), readFile(file), false, file)); } catch (Exception ignored) { }
    }

    private void renderFiles() {
        fileList.removeAllViews(); controls.clear();
        for (int index = 0; index < files.size(); index++) {
            BankFile bank = files.get(index); CheckBox check = new CheckBox(this); check.setText(label(bank)); check.setTag(index); controls.add(check); fileList.addView(check, wrap());
        }
        if (files.isEmpty()) addText(fileList, "No JSON files are available.");
    }

    private String label(BankFile bank) { try { JSONObject data = new JSONObject(bank.json); return data.optString("title", bank.name) + (bank.bundled ? " · Bundled" : " · Local copy"); } catch (Exception error) { return bank.name; } }
    private List<BankFile> selected() { List<BankFile> result = new ArrayList<>(); for (CheckBox check : controls) if (check.isChecked()) result.add(files.get((Integer) check.getTag())); return result; }

    private void exportSelected() {
        List<BankFile> selected = selected(); if (selected.isEmpty()) { Toast.makeText(this, "Select at least one JSON file.", Toast.LENGTH_SHORT).show(); return; }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.setType("application/zip"); intent.putExtra(Intent.EXTRA_TITLE, "quizhub-question-banks.zip"); pendingExport = selected; startActivityForResult(intent, CREATE_ARCHIVE);
    }
    private List<BankFile> pendingExport = new ArrayList<>();

    @Override protected void onActivityResult(int request, int result, Intent data) { super.onActivityResult(request, result, data); if (request == CREATE_ARCHIVE && result == RESULT_OK && data != null) try (OutputStream output = getContentResolver().openOutputStream(data.getData()); ZipOutputStream zip = new ZipOutputStream(output)) { for (BankFile bank : pendingExport) { zip.putNextEntry(new ZipEntry(bank.name)); zip.write(bank.json.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); } Toast.makeText(this, "Export complete.", Toast.LENGTH_SHORT).show(); } catch (Exception error) { Toast.makeText(this, "Could not export: " + error.getMessage(), Toast.LENGTH_LONG).show(); } }

    private void deleteSelected() {
        List<BankFile> selected = selected(); List<BankFile> removable = new ArrayList<>(); for (BankFile bank : selected) if (!bank.bundled) removable.add(bank);
        if (removable.isEmpty()) { Toast.makeText(this, "Bundled JSON files cannot be deleted.", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this).setTitle("Delete selected files?").setMessage("This will delete the selected local copies from this device.").setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { for (BankFile bank : removable) bank.file.delete(); loadFiles(); renderFiles(); }).show();
    }

    private void editSelected() {
        List<BankFile> selected = selected(); if (selected.size() != 1) { Toast.makeText(this, "Select exactly one JSON file to edit.", Toast.LENGTH_SHORT).show(); return; }
        editing = selected.get(0); if (editor != null) root.removeView(editor); if (find != null) root.removeView(find); if (replacement != null) root.removeView(replacement);
        TextView heading = new TextView(this); heading.setText("Editing: " + editing.name); heading.setTextSize(20); root.addView(heading, wrap());
        find = new EditText(this); find.setHint("Find"); find.setSingleLine(true); root.addView(find, wrap()); replacement = new EditText(this); replacement.setHint("Replace with"); replacement.setSingleLine(true); root.addView(replacement, wrap());
        LinearLayout controlsRow = new LinearLayout(this); Button previous = button("Previous"); Button next = button("Next"); Button one = button("Replace current"); Button all = button("Replace all"); Button save = button("Save JSON"); controlsRow.addView(previous, wrap()); controlsRow.addView(next, wrap()); controlsRow.addView(one, wrap()); controlsRow.addView(all, wrap()); controlsRow.addView(save, wrap()); root.addView(controlsRow, wrap());
        editorStatus = new TextView(this); root.addView(editorStatus, wrap()); editor = new EditText(this); editor.setGravity(android.view.Gravity.TOP); editor.setText(editing.json); editor.setMinLines(18); editor.setHorizontallyScrolling(true); editor.setTextSize(12); root.addView(editor, wrap());
        find.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s, int st, int c, int a) { } public void onTextChanged(CharSequence s, int st, int b, int c) { matchIndex = 0; highlight(); } public void afterTextChanged(Editable e) { } });
        previous.setOnClickListener(view -> moveMatch(-1)); next.setOnClickListener(view -> moveMatch(1)); one.setOnClickListener(view -> replaceCurrent()); all.setOnClickListener(view -> replaceAll()); save.setOnClickListener(view -> saveEditor()); highlight();
    }

    private void findMatches(String value) { matches = new ArrayList<>(); String query = find == null ? "" : find.getText().toString(); if (query.isEmpty()) return; Matcher matcher = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(value); while (matcher.find()) matches.add(new int[]{matcher.start(), matcher.end()}); }
    private void highlight() { if (editor == null) return; String value = editor.getText().toString(); findMatches(value); SpannableStringBuilder styled = new SpannableStringBuilder(value); for (int index = 0; index < matches.size(); index++) styled.setSpan(new BackgroundColorSpan(index == matchIndex ? Color.rgb(255, 170, 0) : Color.rgb(245, 217, 10)), matches.get(index)[0], matches.get(index)[1], 0); editor.setText(styled); editor.setSelection(Math.min(value.length(), matches.isEmpty() ? 0 : matches.get(matchIndex)[0])); if (editorStatus != null) editorStatus.setText(find.getText().length() == 0 ? "Ready" : matches.size() + " match(es)"); }
    private void moveMatch(int direction) { if (matches.isEmpty()) return; matchIndex = (matchIndex + direction + matches.size()) % matches.size(); highlight(); }
    private void replaceCurrent() { if (matches.isEmpty()) return; String value = editor.getText().toString(); int[] match = matches.get(matchIndex); editor.setText(value.substring(0, match[0]) + replacement.getText() + value.substring(match[1])); highlight(); }
    private void replaceAll() { String query = find.getText().toString(); if (query.isEmpty()) return; String value = editor.getText().toString(); editor.setText(value.replaceAll("(?i)" + Pattern.quote(query), Matcher.quoteReplacement(replacement.getText().toString()))); highlight(); }
    private void saveEditor() { try { JSONObject parsed = new JSONObject(editor.getText().toString()); File directory = new File(getFilesDir(), "managed-banks"); directory.mkdirs(); if (editing.bundled) { String name = "managed-" + System.currentTimeMillis() + ".json"; editing = new BankFile(name, parsed.toString(2), false, new File(directory, name)); } else editing.json = parsed.toString(2); if (editing.file == null) editing.file = new File(directory, editing.name); java.nio.file.Files.write(editing.file.toPath(), editing.json.getBytes(StandardCharsets.UTF_8)); loadFiles(); renderFiles(); Toast.makeText(this, "JSON saved as a local copy.", Toast.LENGTH_SHORT).show(); } catch (Exception error) { Toast.makeText(this, "Invalid JSON: " + error.getMessage(), Toast.LENGTH_LONG).show(); } }

    private Button button(String text) { Button button = new Button(this); button.setText(text); return button; }
    private static void addText(LinearLayout parent, String value) { TextView text = new TextView(parent.getContext()); text.setText(value); parent.addView(text, wrap()); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private String readAsset(String path) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line); } return result.toString(); }
    private static String readFile(File file) throws Exception { StringBuilder result = new StringBuilder(); try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) result.append(line).append('\n'); } return result.toString(); }
    private static final class BankFile { final String name; String json; final boolean bundled; File file; BankFile(String name, String json, boolean bundled, File file) { this.name = name; this.json = json; this.bundled = bundled; this.file = file; } }
}
