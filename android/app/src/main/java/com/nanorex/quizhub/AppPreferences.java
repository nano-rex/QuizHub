package com.nanorex.quizhub;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPreferences {
    private static final String NAME = "quizhub-preferences";
    private static final String QUESTION_COUNT = "question-count";
    private static final String LANGUAGE = "language";
    private static final String DARK_MODE = "dark-mode";

    private AppPreferences() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int questionCount(Context context) { return preferences(context).getInt(QUESTION_COUNT, 10); }
    public static String language(Context context) { return preferences(context).getString(LANGUAGE, "en"); }
    public static boolean darkMode(Context context) { return preferences(context).getBoolean(DARK_MODE, false); }

    public static void save(Context context, int questionCount, String language, boolean darkMode) {
        preferences(context).edit().putInt(QUESTION_COUNT, questionCount).putString(LANGUAGE, language).putBoolean(DARK_MODE, darkMode).apply();
    }
}
