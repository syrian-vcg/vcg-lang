package com.syrianvcg.editor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class ProjectsActivity extends AppCompatActivity
        implements ProjectAdapter.ProjectClickListener {

    private ProjectAdapter adapter;
    private List<VcgProject> projects = new ArrayList<>();
    private VcgStorage storage;
    private TextView emptyView;
    private ActivityResultLauncher<String[]> importPicker;

    private static final String[] COLOR_TAGS = {
        "#4DC95A", "#6AB0FF", "#F5C842", "#FF7A7A", "#CC99FF", "#00D4FF"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_projects);
        setSupportActionBar(findViewById(R.id.toolbar));

        storage = new VcgStorage(this);
        storage.migrateLegacyIfNeeded();

        VcgNotifications.createChannel(this);
        VcgNotifications.requestPermissionIfNeeded(this);
        maybeShowReadyPrompt();

        RecyclerView rv = findViewById(R.id.recycler_projects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectAdapter(projects, storage, this);
        rv.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view);

        FloatingActionButton fab = findViewById(R.id.fab_new_project);
        fab.setOnClickListener(v -> showNewProjectDialog());

        importPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) importProjectFromZip(uri); }
        );

        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void maybeShowReadyPrompt() {
        VcgSettings settings = new VcgSettings(this);
        if (!settings.getMotivationPromptsEnabled()) return;

        long now = System.currentTimeMillis();
        long last = settings.getLastPromptShownAt();
        boolean firstEver = settings.getLastOpenedAt() == 0L;
        boolean longAway = (now - last) > (6L * 60 * 60 * 1000); // 6 ساعات

        if (firstEver || longAway) {
            settings.setLastPromptShownAt(now);
            String msg = VcgNotifications.randomReadyPrompt();
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle(firstEver ? "أهلاً بك في VCG! 👋" : "مرحباً بعودتك")
                .setMessage(msg)
                .setPositiveButton("هيا بنا 🚀", null)
                .setNegativeButton("ليس الآن", null)
                .show();
            VcgNotifications.notifyReadyToCode(this);
        }
        settings.setLastOpenedAt(now);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // لا حاجة لأي إجراء إضافي — إن رُفضت الصلاحية ستبقى التذكيرات داخل التطبيق فقط (الحوارات) تعمل بشكل طبيعي.
    }

    private void loadProjects() {
        projects.clear();
        projects.addAll(storage.getAllProjects());
        // نصفّر تخزين عدد الملفات/الأصول المؤقت لأن هذا هو اللحظة الوحيدة
        // التي قد تكون فيها البيانات تغيّرت فعلياً (مثلاً المستخدم رجع من
        // مشروع أضاف فيه ملفاً جديداً). راجع التعليق في ProjectAdapter.
        adapter.invalidateCounts();
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showNewProjectDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_project, null);
        TextInputEditText input = view.findViewById(R.id.input_project_name);
        TextInputEditText inputDesc = view.findViewById(R.id.input_project_desc);

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("مشروع جديد")
            .setView(view)
            .setPositiveButton("إنشاء", (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (name.isEmpty()) name = "مشروع بلا اسم";
                String desc = inputDesc.getText() != null ? inputDesc.getText().toString().trim() : "";
                String color = COLOR_TAGS[projects.size() % COLOR_TAGS.length];

                VcgProject p = new VcgProject(VcgStorage.newId(), name, desc, color);
                storage.saveProject(p);

                // Seed with a starter file
                storage.saveFile(new VcgFile(p.getId(), "main.vcg",
                    "# " + name + "\nshow(\"مرحباً من " + name + "\")\n"));

                openProject(p);
                VcgNotifications.notifyProjectCreated(this, p.getName());
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onProjectClick(VcgProject project) {
        openProject(project);
    }

    @Override
    public void onProjectDelete(VcgProject project) {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("حذف المشروع")
            .setMessage("هل تريد حذف \"" + project.getName() + "\" وكل ملفاته وصوره؟ لا يمكن التراجع.")
            .setPositiveButton("حذف", (d, w) -> {
                storage.deleteProject(project.getId());
                loadProjects();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onProjectRename(VcgProject project) {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_project, null);
        TextInputEditText input = view.findViewById(R.id.input_project_name);
        TextInputEditText inputDesc = view.findViewById(R.id.input_project_desc);
        input.setText(project.getName());
        inputDesc.setText(project.getDescription());

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("تعديل المشروع")
            .setView(view)
            .setPositiveButton("حفظ", (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (!name.isEmpty()) project.setName(name);
                project.setDescription(inputDesc.getText() != null ? inputDesc.getText().toString().trim() : "");
                storage.saveProject(project);
                loadProjects();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onProjectExport(VcgProject project) {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("تصدير \"" + project.getName() + "\"")
            .setMessage("سيتم إنشاء ملف مضغوط (.vcgzip) يحتوي كل ملفات وصور هذا المشروع، يمكنك مشاركته أو حفظه واستيراده لاحقاً.")
            .setPositiveButton("تصدير", (d, w) -> exportProjectAsZip(project))
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void exportProjectAsZip(VcgProject project) {
        Toast.makeText(this, "جاري تجهيز الملف المضغوط...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                VcgExport.exportProject(this, storage, project);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "فشل التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void importProjectFromZip(Uri uri) {
        Toast.makeText(this, "جاري استرداد المشروع وفك ضغطه...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                VcgProject imported = VcgExport.importProject(this, storage, uri);
                runOnUiThread(() -> {
                    loadProjects();
                    Toast.makeText(this, "تم استرداد \"" + imported.getName() + "\" بنجاح ✓", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                    "فشل الاستيراد: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openProject(VcgProject project) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("projectId", project.getId());
        intent.putExtra("projectName", project.getName());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_import_project) {
            importPicker.launch(new String[]{"application/zip", "application/octet-stream", "*/*"});
            return true;
        }
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
            .setMessage("Version: 2.1.0\n\n" +
                "محرر لغة VCG البرمجية السورية\n" +
                "مترجم حقيقي مكتوب بـ C11\n\n" +
                "github.com/syrian-vcg/vcg-lang")
            .setPositiveButton("إغلاق", null)
            .show();
    }
}
