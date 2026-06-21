package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VcgSettings — مركز إدارة إعدادات المحرر والتطبيق
 * (الخط، ثيم المعاينة، سمة الواجهة، اللغة، الإشعارات، GitHub..)
 */
public class VcgSettings {

    public static final String[] THEMES   = {"olive", "white", "midnight", "amoled", "sand"};
    public static final String[] FONTS    = {"monospace", "sans-serif", "serif"};

    private final SharedPreferences prefs;

    public VcgSettings(Context ctx) {
        prefs = ctx.getSharedPreferences("vcg_settings", Context.MODE_PRIVATE);
    }

    // ═══════════════════ المحرر ═══════════════════

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

    /** ثيم نتيجة المعاينة المعروضة (داخل WebView) */
    public String getTheme() { return prefs.getString("theme", "olive"); }
    public void setTheme(String v) { prefs.edit().putString("theme", v).apply(); }

    public String getFontFamily() { return prefs.getString("font_family", "monospace"); }
    public void setFontFamily(String v) { prefs.edit().putString("font_family", v).apply(); }

    public int getTabSize() { return prefs.getInt("tab_size", 4); }
    public void setTabSize(int v) { prefs.edit().putInt("tab_size", v).apply(); }

    public boolean getKeepTerminalLog() { return prefs.getBoolean("keep_terminal_log", true); }
    public void setKeepTerminalLog(boolean v) { prefs.edit().putBoolean("keep_terminal_log", v).apply(); }

    // ═══════════════════ سمة واجهة التطبيق ═══════════════════

    /** white | dark | black | blue — راجع VcgThemeHelper */
    public String getAppTheme() { return prefs.getString("app_theme", VcgThemeHelper.THEME_WHITE); }
    public void setAppTheme(String v) { prefs.edit().putString("app_theme", v).apply(); }

    // ═══════════════════ أيقونة التطبيق ═══════════════════

    /** olive | black | blue | white — راجع VcgIconSwitcher */
    public String getAppIcon() { return prefs.getString("app_icon", "olive"); }
    public void setAppIcon(String v) { prefs.edit().putString("app_icon", v).apply(); }

    // ═══════════════════ اللغة ═══════════════════

    /** ar | en */
    public String getAppLanguage() { return prefs.getString("app_language", "ar"); }
    public void setAppLanguage(String v) { prefs.edit().putString("app_language", v).apply(); }

    // ═══════════════════ الإشعارات والتذكيرات ═══════════════════

    public boolean getNotificationsEnabled() { return prefs.getBoolean("notifications_enabled", true); }
    public void setNotificationsEnabled(boolean v) { prefs.edit().putBoolean("notifications_enabled", v).apply(); }

    public boolean getMotivationPromptsEnabled() { return prefs.getBoolean("motivation_prompts", true); }
    public void setMotivationPromptsEnabled(boolean v) { prefs.edit().putBoolean("motivation_prompts", v).apply(); }

    public long getLastOpenedAt() { return prefs.getLong("last_opened_at", 0L); }
    public void setLastOpenedAt(long v) { prefs.edit().putLong("last_opened_at", v).apply(); }

    public long getLastPromptShownAt() { return prefs.getLong("last_prompt_shown_at", 0L); }
    public void setLastPromptShownAt(long v) { prefs.edit().putLong("last_prompt_shown_at", v).apply(); }

    // ═══════════════════ GitHub ═══════════════════

    public String getGithubToken() { return prefs.getString("github_token", null); }
    public void setGithubToken(String v) { prefs.edit().putString("github_token", v).apply(); }

    public String getGithubUsername() { return prefs.getString("github_username", null); }
    public void setGithubUsername(String v) { prefs.edit().putString("github_username", v).apply(); }

    public boolean isGithubConnected() {
        String t = getGithubToken();
        return t != null && !t.trim().isEmpty();
    }

    public void clearGithub() {
        prefs.edit().remove("github_token").remove("github_username").apply();
    }

    // ═══════════════════ إعادة الضبط ═══════════════════

    public void resetToDefaults() {
        // نحافظ على ربط GitHub عند إعادة الضبط، فهو ليس "إعداد عرض"
        String token = getGithubToken();
        String user  = getGithubUsername();
        prefs.edit().clear().apply();
        if (token != null) {
            prefs.edit().putString("github_token", token).putString("github_username", user).apply();
        }
    }
}
