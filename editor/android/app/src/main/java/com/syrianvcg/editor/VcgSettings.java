package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VcgSettings — مركز إدارة إعدادات المحرر (الخط، الثيم، المسافة البادئة، الترمنال..)
 */
public class VcgSettings {

    public static final String[] THEMES   = {"olive", "midnight", "amoled", "sand"};
    public static final String[] FONTS    = {"monospace", "sans-serif", "JetBrains"};

    private final SharedPreferences prefs;

    public VcgSettings(Context ctx) {
        prefs = ctx.getSharedPreferences("vcg_settings", Context.MODE_PRIVATE);
    }

    public int getFontSize() { return prefs.getInt("font_size", 14); }
    public void setFontSize(int v) { prefs.edit().putInt("font_size", v).apply(); }

    public boolean getWordWrap() { return prefs.getBoolean("word_wrap", false); }
    public void setWordWrap(boolean v) { prefs.edit().putBoolean("word_wrap", v).apply(); }

    public boolean getAutoIndent() { return prefs.getBoolean("auto_indent", true); }
    public void setAutoIndent(boolean v) { prefs.edit().putBoolean("auto_indent", v).apply(); }

    public boolean getSyntaxHighlight() { return prefs.getBoolean("syntax_hl", true); }
    public void setSyntaxHighlight(boolean v) { prefs.edit().putBoolean("syntax_hl", v).apply(); }

    public boolean getLivePreview() { return prefs.getBoolean("live_preview", true); }
    public void setLivePreview(boolean v) { prefs.edit().putBoolean("live_preview", v).apply(); }

    public boolean getShowLineNumbers() { return prefs.getBoolean("show_line_numbers", true); }
    public void setShowLineNumbers(boolean v) { prefs.edit().putBoolean("show_line_numbers", v).apply(); }

    public boolean getAutoSave() { return prefs.getBoolean("auto_save", true); }
    public void setAutoSave(boolean v) { prefs.edit().putBoolean("auto_save", v).apply(); }

    public boolean getVibrateOnRun() { return prefs.getBoolean("vibrate_on_run", true); }
    public void setVibrateOnRun(boolean v) { prefs.edit().putBoolean("vibrate_on_run", v).apply(); }

    public String getTheme() { return prefs.getString("theme", "olive"); }
    public void setTheme(String v) { prefs.edit().putString("theme", v).apply(); }

    public String getFontFamily() { return prefs.getString("font_family", "monospace"); }
    public void setFontFamily(String v) { prefs.edit().putString("font_family", v).apply(); }

    public int getTabSize() { return prefs.getInt("tab_size", 4); }
    public void setTabSize(int v) { prefs.edit().putInt("tab_size", v).apply(); }

    public boolean getKeepTerminalLog() { return prefs.getBoolean("keep_terminal_log", true); }
    public void setKeepTerminalLog(boolean v) { prefs.edit().putBoolean("keep_terminal_log", v).apply(); }

    public void resetToDefaults() {
        prefs.edit().clear().apply();
    }
}
