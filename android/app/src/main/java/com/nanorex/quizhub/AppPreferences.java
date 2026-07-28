package com.nanorex.quizhub;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AppPreferences {
    private static final String NAME = "quizhub-preferences";
    private static final String QUESTION_COUNT = "question-count";
    private static final String LANGUAGE = "language";
    private static final String DARK_MODE = "dark-mode";
    private static final String ENABLED_SUBJECTS = "enabled-subjects";

    private AppPreferences() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int questionCount(Context context) { return preferences(context).getInt(QUESTION_COUNT, 10); }
    public static String language(Context context) { return preferences(context).getString(LANGUAGE, "en"); }
    public static boolean darkMode(Context context) { return preferences(context).getBoolean(DARK_MODE, false); }

    public static Set<String> enabledSubjects(Context context) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.contains(ENABLED_SUBJECTS)) return null;
        String value = preferences.getString(ENABLED_SUBJECTS, "");
        Set<String> subjects = new HashSet<>();
        if (value != null && !value.isEmpty()) subjects.addAll(Arrays.asList(value.split("\\|")));
        return subjects;
    }

    public static boolean isSubjectEnabled(Context context, String subject) {
        Set<String> subjects = enabledSubjects(context);
        return subjects == null || subjects.contains(subject);
    }

    public static void save(Context context, int questionCount, String language, boolean darkMode, Set<String> enabledSubjects) {
        String encodedSubjects = String.join("|", enabledSubjects);
        preferences(context).edit().putInt(QUESTION_COUNT, questionCount).putString(LANGUAGE, language).putBoolean(DARK_MODE, darkMode).putString(ENABLED_SUBJECTS, encodedSubjects).apply();
    }
}
