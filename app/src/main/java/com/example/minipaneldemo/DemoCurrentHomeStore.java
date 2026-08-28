package com.example.minipaneldemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;

import java.io.IOException;
import java.security.GeneralSecurityException;

public enum DemoCurrentHomeStore {

    INSTANCE;

    /* ---------- Home ---------- */

    private static final String PREFS_HOME = "DemoCurrentHomeStore";
    private static final String KEY_HOME_ID = "currentHomeId";

    public void setCurrentHomeId(Context context, long homeId) {
        homePrefs(context).edit().putLong(KEY_HOME_ID, homeId).apply();
    }

    public static long getCurrentHomeId(Context context) {
        return homePrefs(context).getLong(KEY_HOME_ID, 0L);
    }

    public void clearCurrentHome(Context context) {
        homePrefs(context).edit().remove(KEY_HOME_ID).apply();
    }

    private static SharedPreferences homePrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_HOME, Context.MODE_PRIVATE);
    }

    /* ---------- Login ---------- */

    private static final String PREFS_LOGIN = "demo_login_prefs";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_PASSWORD = "password";

    private static SharedPreferences loginPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);
    }

    public void saveLoginCredentials(Context context, String country, String account, String password) {
        loginPrefs(context).edit()
                .putString(KEY_COUNTRY, country != null ? country : "")
                .putString(KEY_ACCOUNT, account != null ? account : "")
                .putString(KEY_PASSWORD, password != null ? password : "")
                .apply();
    }

    public void clearLoginCredentials(Context context) {
        loginPrefs(context).edit().clear().apply();
    }

    /**
     * @return true if saved credentials were applied (remember checkbox should stay checked)
     */
    public boolean restoreLoginFields(
            Context context,
            EditText etCountry,
            EditText etAccount,
            EditText etPassword) {
        SharedPreferences sp = loginPrefs(context);
        String account = sp.getString(KEY_ACCOUNT, "");
        if (account == null || account.isEmpty()) {
            return false;
        }
        String country = sp.getString(KEY_COUNTRY, "86");
        String password = sp.getString(KEY_PASSWORD, "");
        if (etCountry != null) {
            etCountry.setText(country != null ? country : "86");
        }
        etAccount.setText(account);
        etPassword.setText(password != null ? password : "");
        return true;
    }

    /** Clears both home selection and saved login fields. */
    public void clearAll(Context context) {
        clearCurrentHome(context);
        clearLoginCredentials(context);
    }
}
