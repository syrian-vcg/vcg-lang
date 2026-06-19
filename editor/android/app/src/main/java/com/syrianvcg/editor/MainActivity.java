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
            "# Hello World — VCG v2.0\n" +
            "let name = \"Syrian VCG\"\n" +
            "show(\"Hello from\", name)\n" +
            "show(\"Version:\", VCG_VERSION)\n" +
            "show(\"Edition:\", VCG_EDITION)\n" +
            "show(\"2**10 =\", 2**10)\n" +
            "show(\"sqrt(144) =\", sqrt(144))\n" +
            "show(\"uuid:\", uuid())\n" +
            "show(\"PHI =\", PHI)\n"));

        storage.saveFile(new VcgFile("oop.vcg",
            "# OOP — VCG v2.0\n" +
            "class Animal {\n" +
            "    func init(name, sound) {\n" +
            "        self.name = name\n" +
            "        self.sound = sound\n" +
            "    }\n" +
            "    func speak() {\n" +
            "        return self.name + \": \" + self.sound\n" +
            "    }\n" +
            "}\n" +
            "class Dog extends Animal {\n" +
            "    func init(name) {\n" +
            "        self.name = name\n" +
            "        self.sound = \"Woof!\"\n" +
            "    }\n" +
            "}\n" +
            "let dog = new Dog(\"Rex\")\n" +
            "show(dog.speak())\n"));

        storage.saveFile(new VcgFile("enum.vcg",
            "# Enum — VCG v2.0\n" +
            "enum Color { Red, Green, Blue }\n" +
            "enum Status { Pending, Active, Done }\n" +
            "show(\"Red =\", Color.Red)\n" +
            "show(\"Done =\", Status.Done)\n" +
            "let s = Status.Active\n" +
            "if s == Status.Active { show(\"Active!\") }\n"));

        storage.saveFile(new VcgFile("fibonacci.vcg",
            "# Fibonacci + Functional\n" +
            "func fib(n) {\n" +
            "    if n <= 1 { return n }\n" +
            "    return fib(n-1) + fib(n-2)\n" +
            "}\n" +
            "for i in 0..13 {\n" +
            "    show(\"fib(\", i, \") =\", fib(i))\n" +
            "}\n" +
            "let nums = [1,2,3,4,5,6,7,8,9,10]\n" +
            "func double(x) { return x * 2 }\n" +
            "func is_even(x) { return x % 2 == 0 }\n" +
            "show(\"doubled:\", map(double, nums))\n" +
            "show(\"evens:\", filter(is_even, nums))\n" +
            "show(\"sum:\", sum(nums), \"avg:\", avg(nums))\n"));

        storage.saveFile(new VcgFile("ui_demo.vcg",
            "# UI Demo — VCG v2.0\n" +
            "h(1, \"Syrian VCG v2.0\")\n" +
            "h(2, \"مميزات اللغة\")\n" +
            "l(\"OOP كامل\", \"Enums\", \"Modules\", \"Async\", \"Tests\")\n" +
            "h(3, \"أوامر\")\n" +
            "key(\"vcgc file.vcg\")\n" +
            "key(\"vcgc -r file.vcg\")\n" +
            "btn(\"GitHub\", \"window.open('https://github.com/syrian-vcg/vcg-lang')\")\"\n" +
            "url(\"https://github.com/syrian-vcg/vcg-lang\", \"VCG on GitHub\")\n"));

        storage.saveFile(new VcgFile("social.vcg",
            "# Social Media — VCG v2.0\n" +
            "h(1, \"تابعنا\")\n" +
            "facebook(\"https://facebook.com/syrianvcg\", \"Facebook\")\n" +
            "instagram(\"syrianvcg\", \"Instagram\")\n" +
            "xsocial(\"syrianvcg\", \"X Twitter\")\n" +
            "youtube(\"dQw4w9WgXcQ\")\n" +
            "url(\"https://github.com/syrian-vcg/vcg-lang\", \"GitHub\")\n"));

        storage.saveFile(new VcgFile("reactive.vcg",
            "# Reactive + Channels — VCG v2.0\n" +
            "watch(\"hp\", func(v) { show(\"[HP]\", v) })\n" +
            "$set(\"hp\", 100)\n" +
            "$set(\"hp\", 75)\n" +
            "$set(\"hp\", 50)\n" +
            "show(\"Final HP:\", $get(\"hp\"))\n" +
            "c inbox\n" +
            "send(inbox, \"msg1\") send(inbox, \"msg2\")\n" +
            "let m = recv(inbox)\n" +
            "while m != nil { show(m) m = recv(inbox) }\n"));

        storage.saveFile(new VcgFile("tests.vcg",
            "# Testing — VCG v2.0\n" +
            "func add(a,b) { return a+b }\n" +
            "test \"addition\" {\n" +
            "    assert_eq(add(2,3), 5)\n" +
            "    assert_true(add(1,1)==2)\n" +
            "}\n" +
            "test \"math\" {\n" +
            "    assert_eq(fib(10), 55)\n" +
            "    assert_true(is_prime(17))\n" +
            "    assert_eq(gcd(48,18), 6)\n" +
            "}\n" +
            "show(\"Tests done!\")\n"));

        storage.saveFile(new VcgFile("safety.vcg",
            "# Safety — VCG v2.0\n" +
            "safe {\n" +
            "    show(\"inside safe block\")\n" +
            "    throw \"test error\"\n" +
            "}\n" +
            "show(\"continued after safe\")\n" +
            "let v = 42\n" +
            "guard v > 0 else { show(\"guard failed\") }\n" +
            "show(\"guard passed\", v)\n" +
            "try { throw \"oops\" } catch e { show(\"caught:\", e) }\n"));
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
