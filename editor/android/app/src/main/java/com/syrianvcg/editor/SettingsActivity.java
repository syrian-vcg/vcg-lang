package com.syrianvcg.editor;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private VcgSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new VcgSettings(this);

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

        // Theme selector
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
        int monoId = R.id.font_mono, sansId = R.id.font_sans;
        fontGroup.check("sans-serif".equals(settings.getFontFamily()) ? sansId : monoId);
        fontGroup.setOnCheckedChangeListener((group, checkedId) ->
            settings.setFontFamily(checkedId == sansId ? "sans-serif" : "monospace"));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
