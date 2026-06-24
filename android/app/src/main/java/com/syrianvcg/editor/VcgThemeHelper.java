package com.syrianvcg.editor;

import android.app.Activity;

/**
 * VcgThemeHelper — يطبّق سمة واجهة التطبيق (أسود / داكن / أبيض / أزرق)
 * المُختارة في الإعدادات على كل شاشة، قبل عرضها.
 *
 * يجب استدعاء apply(this) في كل Activity مباشرة بعد super.onCreate()
 * وقبل setContentView(...).
 */
public final class VcgThemeHelper {

    private VcgThemeHelper() {}

    public static final String THEME_WHITE = "white";
    public static final String THEME_DARK  = "dark";
    public static final String THEME_BLACK = "black";
    public static final String THEME_BLUE  = "blue";

    public static final String[] APP_THEMES = {THEME_WHITE, THEME_DARK, THEME_BLACK, THEME_BLUE};

    public static void apply(Activity activity) {
        VcgSettings settings = new VcgSettings(activity);
        activity.setTheme(styleFor(settings.getAppTheme()));
    }

    public static int styleFor(String name) {
        if (name == null) return R.style.Theme_VCGEditor_White;
        switch (name) {
            case THEME_DARK:  return R.style.Theme_VCGEditor_Dark;
            case THEME_BLACK: return R.style.Theme_VCGEditor_Black;
            case THEME_BLUE:  return R.style.Theme_VCGEditor_Blue;
            default:          return R.style.Theme_VCGEditor_White;
        }
    }

    public static boolean isDark(String name) {
        return THEME_DARK.equals(name) || THEME_BLACK.equals(name) || THEME_BLUE.equals(name);
    }

    public static String displayName(String name) {
        if (name == null) return "أبيض";
        switch (name) {
            case THEME_DARK:  return "داكن";
            case THEME_BLACK: return "أسود";
            case THEME_BLUE:  return "أزرق";
            default:          return "أبيض";
        }
    }
}
