package com.example.smartexpense;

import android.content.Context;
import android.content.SharedPreferences;

final class AuthManager {

    private static final String PREFS_NAME = "auth_data";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_SIGNED_IN = "signed_in";

    private AuthManager() {
    }

    static boolean isSignedIn(Context context) {
        return getPrefs(context).getBoolean(KEY_SIGNED_IN, false);
    }

    static boolean hasAccount(Context context) {
        return !getPrefs(context).getString(KEY_EMAIL, "").isEmpty();
    }

    static void createAccount(Context context, String name, String email, String password) {
        getPrefs(context).edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_SIGNED_IN, true)
                .apply();
    }

    static boolean login(Context context, String email, String password) {
        SharedPreferences prefs = getPrefs(context);
        String savedEmail = prefs.getString(KEY_EMAIL, "");
        String savedPassword = prefs.getString(KEY_PASSWORD, "");

        if (savedEmail.equalsIgnoreCase(email) && savedPassword.equals(password)) {
            prefs.edit().putBoolean(KEY_SIGNED_IN, true).apply();
            return true;
        }

        return false;
    }

    static void logout(Context context) {
        getPrefs(context).edit().putBoolean(KEY_SIGNED_IN, false).apply();
    }

    static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_NAME, "");
    }

    static String getUserEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
