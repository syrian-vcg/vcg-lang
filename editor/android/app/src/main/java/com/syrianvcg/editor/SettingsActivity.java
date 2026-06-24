package com.syrianvcg.editor;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private VcgSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_settings);

        settings = new VcgSettings(this);

        VcgCoins coins = new VcgCoins(this);
        VcgAds ads = new VcgAds();
        VcgAds.init(this);
        ads.preload(this);
        TextView coinLabel = findViewById(R.id.label_coin_balance);
        coinLabel.setText("🪙 " + coins.getBalance());
        findViewById(R.id.btn_watch_ad).setOnClickListener(v -> {
            if (!ads.isReady()) {
                Toast.makeText(this, "الإعلان غير جاهز بعد، حاول بعد لحظات", Toast.LENGTH_SHORT).show();
                ads.preload(this);
                return;
            }
            ads.show(this, new VcgAds.RewardListener() {
                @Override
                public void onRewardEarned() {
                    runOnUiThread(() -> {
                        coins.grantCoinsForAd();
                        coinLabel.setText("🪙 " + coins.getBalance());
                        Toast.makeText(SettingsActivity.this,
                            "🎉 حصلت على " + VcgCoins.COINS_PER_AD + " عملة!", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onAdUnavailable(String reason) {
                    runOnUiThread(() ->
                        Toast.makeText(SettingsActivity.this, "تعذّر عرض الإعلان", Toast.LENGTH_SHORT).show());
                }
            });
        });

        findViewById(R.id.btn_send_suggestion).setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.dialog_suggestion, null);
            com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.input_suggestion);
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle("اقتراح جديد 💡")
                .setView(view)
                .setPositiveButton("إرسال", (d, w) -> {
                    String text = input.getText() != null ? input.getText().toString() : "";
                    VcgFeedback.openSuggestionEmail(this, text);
                })
                .setNegativeButton("إلغاء", null)
                .show();
        });

        setSupportActionBar(findViewById(R.id.settings_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("الإعدادات");
        }

        // Font size
        SeekBar fontSeek    = findViewById(R.id.seek_font_size);
        TextView fontLabel  = findViewById(R.id.label_font_size);
        int fontSize = settings.getFontSize();
        fontSeek.setProgress(fontSize - 10);
        fontLabel.setText(fontSize + "sp");
        fontSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int size = p + 10;
                fontLabel.setText(size + "sp");
                settings.setFontSize(size);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s)  {}
        });

        // Theme selector (ثيم المحرر)
        RadioGroup themeGroup = findViewById(R.id.radio_theme);
        int[] themeIds = {R.id.theme_olive, R.id.theme_white, R.id.theme_midnight, R.id.theme_amoled, R.id.theme_sand};
        String[] themeNames = {"olive", "white", "midnight", "amoled", "sand"};
        String currentTheme = settings.getTheme();
        for (int i = 0; i < themeNames.length; i++) {
            if (themeNames[i].equals(currentTheme)) themeGroup.check(themeIds[i]);
        }
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < themeIds.length; i++) {
                if (themeIds[i] == checkedId) settings.setTheme(themeNames[i]);
            }
        });

        // Font family selector
        RadioGroup fontGroup = findViewById(R.id.radio_font_family);
        int monoId = R.id.font_mono, sansId = R.id.font_sans, serifId = R.id.font_serif;
        String curFont = settings.getFontFamily();
        fontGroup.check("sans-serif".equals(curFont) ? sansId : "serif".equals(curFont) ? serifId : monoId);
        fontGroup.setOnCheckedChangeListener((group, checkedId) ->
            settings.setFontFamily(checkedId == sansId ? "sans-serif" : checkedId == serifId ? "serif" : "monospace"));

        // ══════════════════════════════════════════════════════════════
        // إصلاح: App UI theme selector
        // المشكلة: RadioButton داخل LinearLayout داخل RadioGroup لا يعمل
        // تلقائياً — يجب إدارة الاختيار يدوياً لضمان زر واحد فقط مُحدَّد.
        // ══════════════════════════════════════════════════════════════
        final int[] appThemeIds = {
            R.id.app_theme_system,
            R.id.app_theme_white,
            R.id.app_theme_dark,
            R.id.app_theme_black,
            R.id.app_theme_blue
        };
        final String[] appThemeOrder = {
            VcgThemeHelper.THEME_SYSTEM,
            VcgThemeHelper.THEME_WHITE,
            VcgThemeHelper.THEME_DARK,
            VcgThemeHelper.THEME_BLACK,
            VcgThemeHelper.THEME_BLUE
        };

        // تعيين الزر الصحيح مُحدَّداً عند فتح الشاشة
        String curAppTheme = settings.getAppTheme();
        for (int i = 0; i < appThemeIds.length; i++) {
            RadioButton rb = findViewById(appThemeIds[i]);
            rb.setChecked(appThemeOrder[i].equals(curAppTheme));
        }

        // معالجة الضغط يدوياً: أوقف كل الأزرار ثم فعّل المضغوط فقط
        for (int i = 0; i < appThemeIds.length; i++) {
            final int index = i;
            RadioButton rb = findViewById(appThemeIds[i]);
            rb.setOnClickListener(v -> {
                // أزِل تحديد كل الأزرار أولاً
                for (int id : appThemeIds) {
                    ((RadioButton) findViewById(id)).setChecked(false);
                }
                // حدِّد الزر المضغوط فقط
                rb.setChecked(true);
                // احفظ وأعِد رسم الشاشة إذا تغيّر الاختيار
                if (!appThemeOrder[index].equals(settings.getAppTheme())) {
                    settings.setAppTheme(appThemeOrder[index]);
                    recreate();
                }
            });
        }

        // Word wrap
        Switch wrapSwitch = findViewById(R.id.switch_word_wrap);
        wrapSwitch.setChecked(settings.getWordWrap());
        wrapSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setWordWrap(checked));

        // Auto indent
        Switch indentSwitch = findViewById(R.id.switch_auto_indent);
        indentSwitch.setChecked(settings.getAutoIndent());
        indentSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setAutoIndent(checked));

        // Syntax highlighting
        Switch hlSwitch = findViewById(R.id.switch_syntax_hl);
        hlSwitch.setChecked(settings.getSyntaxHighlight());
        hlSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setSyntaxHighlight(checked));

        // Live preview
        Switch previewSwitch = findViewById(R.id.switch_live_preview);
        previewSwitch.setChecked(settings.getLivePreview());
        previewSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setLivePreview(checked));

        // Line numbers
        Switch lineNumSwitch = findViewById(R.id.switch_line_numbers);
        lineNumSwitch.setChecked(settings.getShowLineNumbers());
        lineNumSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setShowLineNumbers(checked));

        // Auto save
        Switch autoSaveSwitch = findViewById(R.id.switch_auto_save);
        autoSaveSwitch.setChecked(settings.getAutoSave());
        autoSaveSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setAutoSave(checked));

        // Vibrate on run
        Switch vibrateSwitch = findViewById(R.id.switch_vibrate_run);
        vibrateSwitch.setChecked(settings.getVibrateOnRun());
        vibrateSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setVibrateOnRun(checked));

        // Tab size
        SeekBar tabSeek = findViewById(R.id.seek_tab_size);
        TextView tabLabel = findViewById(R.id.label_tab_size);
        int tabSize = settings.getTabSize();
        tabSeek.setProgress(tabSize - 2);
        tabLabel.setText(String.valueOf(tabSize));
        tabSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int size = p + 2;
                tabLabel.setText(String.valueOf(size));
                settings.setTabSize(size);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s)  {}
        });

        // Notifications
        Switch notifSwitch = findViewById(R.id.switch_notifications);
        notifSwitch.setChecked(settings.getNotificationsEnabled());
        notifSwitch.setOnCheckedChangeListener((btn, checked) -> {
            settings.setNotificationsEnabled(checked);
            if (checked) VcgNotifications.requestPermissionIfNeeded(this);
        });

        Switch motivationSwitch = findViewById(R.id.switch_motivation);
        motivationSwitch.setChecked(settings.getMotivationPromptsEnabled());
        motivationSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setMotivationPromptsEnabled(checked));

        setupGithubSection();
        setupIconSection();
        setupLanguageSection();
        setupDataSection();

        // Reset
        findViewById(R.id.btn_reset_settings).setOnClickListener(v ->
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle("استعادة الإعدادات الافتراضية")
                .setMessage("هل تريد إعادة كل الإعدادات لوضعها الافتراضي؟")
                .setPositiveButton("استعادة", (d, w) -> {
                    settings.resetToDefaults();
                    recreate();
                })
                .setNegativeButton("إلغاء", null)
                .show());
    }

    private void setupGithubSection() {
        TextView statusLabel = findViewById(R.id.label_github_status);
        TextInputEditText tokenInput = findViewById(R.id.input_github_token);
        MaterialButton connectBtn = findViewById(R.id.btn_github_connect);
        MaterialButton disconnectBtn = findViewById(R.id.btn_github_disconnect);
        MaterialButton reposBtn = findViewById(R.id.btn_github_repos);

        refreshGithubStatus(statusLabel);

        connectBtn.setOnClickListener(v -> {
            String token = tokenInput.getText() != null ? tokenInput.getText().toString().trim() : "";
            if (token.isEmpty()) {
                Toast.makeText(this, "أدخل الرمز (Token) أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            statusLabel.setText("جاري التحقق...");
            new Thread(() -> {
                try {
                    String username = VcgGitHub.validateTokenAndGetUsername(token);
                    settings.setGithubToken(token);
                    settings.setGithubUsername(username);
                    runOnUiThread(() -> {
                        refreshGithubStatus(statusLabel);
                        Toast.makeText(this, "تم الربط بنجاح: " + username, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    String msg = e.getMessage();
                    runOnUiThread(() -> {
                        statusLabel.setText("فشل الربط: " + msg);
                        Toast.makeText(this, "فشل الربط بـ GitHub: " + msg, Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        disconnectBtn.setOnClickListener(v -> {
            settings.clearGithub();
            tokenInput.setText("");
            refreshGithubStatus(statusLabel);
            Toast.makeText(this, "تم فصل حساب GitHub", Toast.LENGTH_SHORT).show();
        });

        reposBtn.setOnClickListener(v -> {
            if (!settings.isGithubConnected()) {
                Toast.makeText(this, "اربط حسابك أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            String token = settings.getGithubToken();
            new Thread(() -> {
                try {
                    List<VcgGitHub.Repo> repos = VcgGitHub.listRepos(token);
                    String[] names = new String[repos.size()];
                    for (int i = 0; i < repos.size(); i++) names[i] = repos.get(i).toString();
                    runOnUiThread(() -> {
                        if (names.length == 0) {
                            Toast.makeText(this, "لا توجد مستودعات بعد", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AlertDialog.Builder(this, R.style.VCGDialog)
                            .setTitle("مستودعاتك على GitHub")
                            .setItems(names, null)
                            .setPositiveButton("إغلاق", null)
                            .show();
                    });
                } catch (Exception e) {
                    String msg = e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, "فشل جلب المستودعات: " + msg, Toast.LENGTH_LONG).show());
                }
            }).start();
        });
    }

    private void refreshGithubStatus(TextView statusLabel) {
        if (settings.isGithubConnected()) {
            statusLabel.setText("متّصل كـ: " + settings.getGithubUsername() + " ✓");
        } else {
            statusLabel.setText("غير متصل");
        }
    }

    private void setupIconSection() {
        RadioGroup iconGroup = findViewById(R.id.radio_app_icon);
        int[] iconIds = {R.id.icon_olive, R.id.icon_black, R.id.icon_blue, R.id.icon_white};
        String[] iconNames = VcgIconSwitcher.ICONS; // {olive, black, blue, white}
        String currentIcon = settings.getAppIcon();
        for (int i = 0; i < iconNames.length; i++) {
            if (iconNames[i].equals(currentIcon)) iconGroup.check(iconIds[i]);
        }
        iconGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < iconIds.length; i++) {
                if (iconIds[i] == checkedId) {
                    settings.setAppIcon(iconNames[i]);
                    VcgIconSwitcher.applyIcon(this, iconNames[i]);
                    Toast.makeText(this, "تم تغيير الأيقونة — قد تحتاج للعودة للشاشة الرئيسية لرؤيتها", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupLanguageSection() {
        RadioGroup langGroup = findViewById(R.id.radio_app_language);
        String currentLang = settings.getAppLanguage();
        langGroup.check("en".equals(currentLang) ? R.id.lang_en : R.id.lang_ar);
        langGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = checkedId == R.id.lang_en ? "en" : "ar";
            if (lang.equals(settings.getAppLanguage())) return;
            settings.setAppLanguage(lang);
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(lang));
        });
    }

    private void setupDataSection() {
        findViewById(R.id.btn_export_backup).setOnClickListener(v -> {
            try {
                VcgExport.exportFullBackup(this, new VcgStorage(this));
            } catch (Exception e) {
                Toast.makeText(this, "فشل التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
