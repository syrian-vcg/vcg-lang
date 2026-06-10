package com.syrianvcg.editor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements FileAdapter.FileClickListener {

    private FileAdapter adapter;
    private List<VcgFile> files = new ArrayList<>();
    private VcgStorage storage;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        storage = new VcgStorage(this);

        RecyclerView rv = findViewById(R.id.recycler_files);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(files, this);
        rv.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view);

        FloatingActionButton fab = findViewById(R.id.fab_new);
        fab.setOnClickListener(v -> showNewFileDialog());

        // Load sample if first launch
        SharedPreferences prefs = getSharedPreferences("vcg_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("first_launch", true)) {
            loadSamples();
            prefs.edit().putBoolean("first_launch", false).apply();
        }

        loadFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFiles();
    }

    private void loadFiles() {
        files.clear();
        files.addAll(storage.getAllFiles());
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadSamples() {
        storage.saveFile(new VcgFile("hello.vcg",
            "# Hello World — VCG v1.0\n" +
            "let name = \"Syrian VCG\"\n" +
            "show(\"Hello from\", name)\n" +
            "show(\"Version:\", VCG_VERSION)\n" +
            "show(\"2 ** 10 =\", 2 ** 10)\n" +
            "show(\"sqrt(144) =\", sqrt(144))\n"));

        storage.saveFile(new VcgFile("fibonacci.vcg",
            "# Fibonacci\n" +
            "func fib(n) {\n" +
            "    if n <= 1 { return n }\n" +
            "    return fib(n-1) + fib(n-2)\n" +
            "}\n" +
            "for i in 0..13 {\n" +
            "    show(\"fib(\", i, \") =\", fib(i))\n" +
            "}\n"));

        storage.saveFile(new VcgFile("ui_demo.vcg",
            "# UI Demo\n" +
            "h(1, \"Syrian VCG v1.0\")\n" +
            "h(2, \"UI Elements\")\n" +
            "l(\n" +
            "  \"Full C11 Compiler\",\n" +
            "  \"HTML + JS Output\",\n" +
            "  \"Social Media Built-in\"\n" +
            ")\n" +
            "btn(\"Visit GitHub\", \"window.open('https://github.com/syrian-vcg/vcg-lang')\")\n" +
            "key(\"vcgc file.vcg\")\n" +
            "url(\"https://github.com/syrian-vcg/vcg-lang\", \"VCG on GitHub\")\n"));

        storage.saveFile(new VcgFile("social.vcg",
            "# Social Media\n" +
            "h(1, \"Follow VCG\")\n" +
            "facebook(\"https://facebook.com/syrianvcg\", \"Facebook\")\n" +
            "instagram(\"syrianvcg\", \"Instagram\")\n" +
            "xsocial(\"syrianvcg\", \"X Twitter\")\n" +
            "youtube(\"dQw4w9WgXcQ\")\n"));

        storage.saveFile(new VcgFile("reactive.vcg",
            "# Reactive Store\n" +
            "watch(\"hp\", func(v) {\n" +
            "    show(\"[HP changed]\", v)\n" +
            "})\n" +
            "$set(\"hp\", 100)\n" +
            "$set(\"hp\", 75)\n" +
            "$set(\"hp\", 50)\n" +
            "show(\"Final HP:\", $get(\"hp\"))\n"));
    }

    private void showNewFileDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        TextInputEditText input = view.findViewById(R.id.input_filename);

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("ملف جديد")
            .setView(view)
            .setPositiveButton("إنشاء", (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (name.isEmpty()) name = "untitled";
                if (!name.endsWith(".vcg")) name += ".vcg";
                VcgFile f = new VcgFile(name, "# " + name + "\n\n");
                storage.saveFile(f);
                openEditor(f);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onFileClick(VcgFile file) {
        openEditor(file);
    }

    @Override
    public void onFileDelete(VcgFile file) {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("حذف الملف")
            .setMessage("هل تريد حذف " + file.getName() + "?")
            .setPositiveButton("حذف", (d, w) -> {
                storage.deleteFile(file.getName());
                loadFiles();
                Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void openEditor(VcgFile file) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("filename", file.getName());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("Syrian VCG Editor")
            .setMessage("Version: 1.0.0\nDate: 2026-06-06\n\n" +
                "محرر لغة VCG البرمجية السورية\n" +
                "مترجم حقيقي مكتوب بـ C11\n\n" +
                "github.com/syrian-vcg/vcg-lang")
            .setPositiveButton("إغلاق", null)
            .show();
    }
}
