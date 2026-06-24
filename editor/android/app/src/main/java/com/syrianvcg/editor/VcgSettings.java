package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * VcgSettings — مركز إدارة إعدادات المحرر والتطبيق
 * (الخط، ثيم المعاينة، سمة الواجهة، اللغة، الإشعارات، GitHub..)
 */
public class VcgSettings {

    public static final String[] THEMES   = {"olive", "white", "midnight", "amoled", "sand"};
    public static final String[] FONTS    = {"monospace", "sans-serif", "serif"};

    private static final String TAG = "VcgSettings";

    private final SharedPreferences prefs;
    /**
     * تخزين مشفّر مخصّص للبيانات الحساسة فقط (GitHub Personal Access Token).
     * الرمز يعطي صلاحية كاملة على مستودعات GitHub الخاصة بالمستخدم، فتخزينه
     * كنص صريح في SharedPreferences العادية يجعله عُرضة للتسريب (نسخ احتياطي
     * غير مشفّر، أو أي تطبيق آخر بصلاحيات root). نستخدم Jetpack Security
     * (EncryptedSharedPreferences) المعتمد على Android Keystore لحماية الرمز
     * بمفتاح لا يغادر الجهاز أبداً.
     */
    private final SharedPreferences securePrefs;

    public VcgSettings(Context ctx) {
        prefs = ctx.getSharedPreferences("vcg_settings", Context.MODE_PRIVATE);
        securePrefs = createSecurePrefs(ctx);
        migrateLegacyGithubTokenIfNeeded();
    }

    /**
     * إصدارات سابقة من التطبيق خزّنت github_token/github_username كنص صريح
     * داخل vcg_settings العادية. هذا الترحيل لمرة واحدة ينقلها للتخزين
     * المشفّر الجديد ثم يمحوها من المكان القديم، حتى لا يبقى الرمز الحساس
     * مكشوفاً على القرص لمن لديه نسخة محدَّثة من التطبيق.
     */
    private void migrateLegacyGithubTokenIfNeeded() {
        if (!prefs.contains("github_token")) return;
        String legacyToken = prefs.getString("github_token", null);
        String legacyUser  = prefs.getString("github_username", null);
        if (legacyToken != null && !legacyToken.trim().isEmpty()
                && securePrefs.getString("github_token", null) == null) {
            securePrefs.edit()
                .putString("github_token", legacyToken)
                .putString("github_username", legacyUser)
                .apply();
        }
        prefs.edit().remove("github_token").remove("github_username").apply();
    }

    private static SharedPreferences createSecurePrefs(Context ctx) {
        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            return EncryptedSharedPreferences.create(
                ctx,
                "vcg_settings_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // لا يجب أن يحدث هذا عملياً على أجهزة Android سليمة، لكن إن حدث
            // (مثلاً Keystore تالف)، نعود لتخزين عادي بدل تعطيل ربط GitHub
            // كلياً. أفضل من تعطّل التطبيق، ونسجل الخطأ للتشخيص.
            Log.e(TAG, "تعذّر إنشاء تخزين مشفّر، سيتم استخدام تخزين عادي كحل بديل", e);
            return ctx.getSharedPreferences("vcg_settings_secure_fallback", Context.MODE_PRIVATE);
        }
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
    public String getAppTheme() { return prefs.getString("app_theme", VcgThemeHelper.THEME_SYSTEM); }
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

    // ═══════════════════ GitHub (مخزّنة مشفّرة — انظر securePrefs أعلاه) ═══════════════════

    public String getGithubToken() { return securePrefs.getString("github_token", null); }
    public void setGithubToken(String v) { securePrefs.edit().putString("github_token", v).apply(); }

    public String getGithubUsername() { return securePrefs.getString("github_username", null); }
    public void setGithubUsername(String v) { securePrefs.edit().putString("github_username", v).apply(); }

    public boolean isGithubConnected() {
        String t = getGithubToken();
        return t != null && !t.trim().isEmpty();
    }

    public void clearGithub() {
        securePrefs.edit().remove("github_token").remove("github_username").apply();
    }

    // ═══════════════════ إعادة الضبط ═══════════════════

    public void resetToDefaults() {
        // ربط GitHub محفوظ في تخزين منفصل (securePrefs) أصلاً ولا يُمسح بهذا
        // الاستدعاء، فهو ليس "إعداد عرض" بل ربط حساب — لا حاجة لإعادة كتابته
        // يدوياً بعد المسح كما كان سابقاً.
        prefs.edit().clear().apply();
    }
}
