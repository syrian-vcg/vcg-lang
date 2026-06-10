package com.syrianvcg.editor;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("vcg_settings", MODE_PRIVATE);

        setSupportActionBar(findViewById(R.id.settings_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("الإعدادات");
        }

        // Font size
        SeekBar fontSeek    = findViewById(R.id.seek_font_size);
        TextView fontLabel  = findViewById(R.id.label_font_size);
        int fontSize = prefs.getInt("font_size", 14);
        fontSeek.setProgress(fontSize - 10);
        fontLabel.setText(fontSize + "sp");
        fontSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int size = p + 10;
                fontLabel.setText(size + "sp");
                prefs.edit().putInt("font_size", size).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s)  {}
        });

        // Word wrap
        Switch wrapSwitch = findViewById(R.id.switch_word_wrap);
        wrapSwitch.setChecked(prefs.getBoolean("word_wrap", false));
        wrapSwitch.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean("word_wrap", checked).apply());

        // Auto indent
        Switch indentSwitch = findViewById(R.id.switch_auto_indent);
        indentSwitch.setChecked(prefs.getBoolean("auto_indent", true));
        indentSwitch.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean("auto_indent", checked).apply());

        // Syntax highlighting
        Switch hlSwitch = findViewById(R.id.switch_syntax_hl);
        hlSwitch.setChecked(prefs.getBoolean("syntax_hl", true));
        hlSwitch.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean("syntax_hl", checked).apply());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
