package com.example.smartexpense;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class CategoryManager {

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_CUSTOM_CATEGORIES = "custom_categories";
    private static final String KEY_HISTORY = "history";
    private static final String[] DEFAULT_CATEGORIES = {
            "Food", "Transport", "Shopping", "Bills", "Investments", "Loans", "General"
    };

    private CategoryManager() {
    }

    static ArrayList<String> getCategories(Context context) {
        LinkedHashSet<String> categories = new LinkedHashSet<String>();
        for (String category : DEFAULT_CATEGORIES) {
            categories.add(category);
        }

        SharedPreferences prefs = getPrefs(context);
        addSerializedCategories(categories, prefs.getString(KEY_CUSTOM_CATEGORIES, ""));

        String history = prefs.getString(KEY_HISTORY, "");
        String[] records = history.trim().isEmpty() ? new String[0] : history.split("\\n");
        for (String record : records) {
            String category = getRecordCategory(record);
            if (!category.isEmpty()) {
                categories.add(category);
            }
        }

        return new ArrayList<String>(categories);
    }

    static String sanitizeCategory(String value) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("[^A-Za-z0-9 &/-]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return "General";
        }
        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24).trim();
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String category : DEFAULT_CATEGORIES) {
            if (category.toLowerCase(Locale.ROOT).equals(lower)
                    || singular(category).equals(lower)
                    || singular(lower).equals(category.toLowerCase(Locale.ROOT))) {
                return category;
            }
        }

        String[] words = lower.split(" ");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.length() == 0 ? "General" : title.toString();
    }

    static void saveCategory(Context context, String category) {
        String cleaned = sanitizeCategory(category);
        if (cleaned.equals("General")) {
            return;
        }

        ArrayList<String> categories = getCategories(context);
        for (String existing : categories) {
            if (existing.equalsIgnoreCase(cleaned)) {
                return;
            }
        }

        SharedPreferences prefs = getPrefs(context);
        String current = prefs.getString(KEY_CUSTOM_CATEGORIES, "");
        String updated = current.trim().isEmpty() ? cleaned : current + "|" + cleaned;
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, updated).apply();
    }

    static String getRecordCategory(String record) {
        String[] parts = record.split("\\|");
        if (parts.length >= 3) {
            return sanitizeCategory(parts[2]);
        }
        return "";
    }

    private static void addSerializedCategories(Set<String> categories, String serialized) {
        String[] values = serialized.trim().isEmpty() ? new String[0] : serialized.split("\\|");
        for (String value : values) {
            String category = sanitizeCategory(value);
            if (!category.equals("General")) {
                categories.add(category);
            }
        }
    }

    private static String singular(String value) {
        return value.endsWith("s") && value.length() > 1
                ? value.substring(0, value.length() - 1).toLowerCase(Locale.ROOT)
                : value.toLowerCase(Locale.ROOT);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
