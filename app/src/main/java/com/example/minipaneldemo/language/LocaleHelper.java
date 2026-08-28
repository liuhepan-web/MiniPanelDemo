package com.example.minipaneldemo.language;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * In-app language switching helper.
 *
 * Uses {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)} so the
 * locale applies to all Activities, including SDK BizBundle pages.
 *
 * @see <a href="https://github.com/tuya/tuya-ui-bizbundle-android-demo/blob/feature/setLanguage/docs/%E5%BA%94%E7%94%A8%E5%86%85%E5%A4%9A%E8%AF%AD%E8%A8%80%E5%88%87%E6%8D%A2.md">In-App Language Switch</a>
 */
public final class LocaleHelper {

    /** Empty string means follow the system. */
    public static final String LANG_SYSTEM = "";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_CHINESE = "zh";

    private LocaleHelper() {
    }

    /**
     * Apply app-wide language. Persisted by AppCompat / system across cold starts.
     *
     * @param language one of {@link #LANG_ENGLISH}, {@link #LANG_CHINESE}, {@link #LANG_SYSTEM}
     */
    public static void switchLanguage(Context context, String language) {
        LocaleListCompat locales = LANG_SYSTEM.equals(language)
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(language);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    /**
     * Switch language then cold-restart the app so all pages (including BizBundle) reload.
     *
     * @param activity current activity
     * @param language language tag or {@link #LANG_SYSTEM}
     */
    public static void switchLanguageAndRestart(Activity activity, String language) {
        switchLanguage(activity, language);
        Intent intent = activity.getPackageManager()
                .getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    /**
     * Currently applied language ("" means follow the system).
     */
    public static String getLanguage(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return LANG_SYSTEM;
        }
        Locale locale = locales.get(0);
        if (locale == null) {
            return LANG_SYSTEM;
        }
        String lang = locale.getLanguage();
        if (LANG_CHINESE.equals(lang)) {
            return LANG_CHINESE;
        }
        if (LANG_ENGLISH.equals(lang)) {
            return LANG_ENGLISH;
        }
        return lang;
    }
}
