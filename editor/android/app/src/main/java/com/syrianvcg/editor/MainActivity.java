package com.syrianvcg.editor;

import android.content.Intent;
import android.os.Bundle;
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
    private List<VcgFile> items = new ArrayList<>();
    private VcgStorage storage;
    private TextView emptyView;
    private TextView breadcrumbView;
    private String projectId;
    private String projectName;

    /** المسار الحالي المعروض — "" يعني الجذر، "lib/" يعني مجلد lib */
    private String currentPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        storage     = new VcgStorage(this);
        projectId   = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        if (projectId == null) { finish(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(projectName != null ? projectName : "المشروع");
        }

        RecyclerView rv = findViewById(R.id.recycler_files);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(items, this);
        rv.setAdapter(adapter);

        emptyView      = findViewById(R.id.empty_view);
        breadcrumbView = findViewById(R.id.breadcrumb_path);

        FloatingActionButton fab = findViewById(R.id.fab_new);
        fab.setOnClickListener(v -> showNewItemDialog());

        findViewById(R.id.btn_assets).setOnClickListener(v -> openAssets());
        findViewById(R.id.btn_terminal).setOnClickListener(v -> openTerminal());

        loadCurrentFolder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentFolder();
    }

    // ── تحميل محتويات المجلد الحالي ─────────────────────────

    private void loadCurrentFolder() {
        items.clear();
        items.addAll(storage.getFilesInFolder(projectId, currentPath));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        updateBreadcrumb();
    }

    private void updateBreadcrumb() {
        if (currentPath.isEmpty()) {
            breadcrumbView.setText("/ " + projectName);
        } else {
            breadcrumbView.setText("/ " + projectName + " / " +
                currentPath.replace("/", " / ").trim());
        }
    }

    // ── حوار إنشاء ملف أو مجلد جديد ────────────────────────

    private void showNewItemDialog() {
        // خيار: ملف أو مجلد
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("إنشاء جديد")
            .setItems(new String[]{"📄  ملف جديد", "📁  مجلد جديد"}, (d, which) -> {
                if (which == 0) showNewFileDialog();
                else            showNewFolderDialog();
            })
            .setNegativeButton("إلغاء", null)
            .show();
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
                // المسار الكامل = المجلد الحالي + اسم الملف
                String fullPath = currentPath + name;
                if (storage.fileExists(projectId, fullPath)) {
                    Toast.makeText(this, "الملف موجود بالفعل", Toast.LENGTH_SHORT).show();
                    return;
                }
                VcgFile f = new VcgFile(projectId, fullPath, "# " + name + "\n\n");
                storage.saveFile(f);
                openEditor(f);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showNewFolderDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        TextInputEditText input = view.findViewById(R.id.input_filename);

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("مجلد جديد")
            .setView(view)
            .setPositiveButton("إنشاء", (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (name.isEmpty()) return;
                // أزل أي امتداد إذا أضافه المستخدم
                name = name.replace("/", "");
                String fullPath = currentPath + name + "/";
                if (storage.fileExists(projectId, fullPath)) {
                    Toast.makeText(this, "المجلد موجود بالفعل", Toast.LENGTH_SHORT).show();
                    return;
                }
                storage.createFolder(projectId, fullPath);
                loadCurrentFolder();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // ── FileClickListener ────────────────────────────────────

    @Override
    public void onFileClick(VcgFile file) {
        openEditor(file);
    }

    @Override
    public void onFolderClick(VcgFile folder) {
        // ادخل المجلد
        currentPath = folder.getPath();
        loadCurrentFolder();
    }

    @Override
    public void onFileDelete(VcgFile file) {
        showOptionsDialog(file);
    }

    @Override
    public void onFileRename(VcgFile file) {
        showRenameDialog(file);
    }

    // ── زر الرجوع ────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        if (!currentPath.isEmpty()) {
            // ارجع للمجلد الأب
            String parent = getParentPath(currentPath);
            currentPath = parent;
            loadCurrentFolder();
        } else {
            super.onBackPressed();
        }
    }

    // ── حوارات الخيارات / إعادة التسمية / الحذف ─────────────

    private void showOptionsDialog(VcgFile item) {
        String title = item.isFolder() ? "مجلد: " + item.getName() : item.getName();
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle(title)
            .setItems(new String[]{"✏️  إعادة تسمية", "🗑️  حذف"}, (d, which) -> {
                if (which == 0) showRenameDialog(item);
                else            confirmDelete(item);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showRenameDialog(VcgFile item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        TextInputEditText input = view.findViewById(R.id.input_filename);
        input.setText(item.getName());

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle(item.isFolder() ? "إعادة تسمية المجلد" : "إعادة تسمية الملف")
            .setView(view)
            .setPositiveButton("حفظ", (d, w) -> {
                String newName = input.getText() != null ? input.getText().toString().trim() : "";
                if (newName.isEmpty()) return;
                if (item.isFolder()) {
                    storage.renameFolder(projectId, item.getPath(), newName);
                } else {
                    if (!newName.endsWith(".vcg")) newName += ".vcg";
                    String newPath = currentPath + newName;
                    storage.renameFile(projectId, item.getPath(), newPath);
                }
                loadCurrentFolder();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void confirmDelete(VcgFile item) {
        String msg = item.isFolder()
            ? "سيتم حذف المجلد \"" + item.getName() + "\" وكل محتوياته. لا يمكن التراجع."
            : "هل تريد حذف " + item.getName() + "?";

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle(item.isFolder() ? "حذف المجلد" : "حذف الملف")
            .setMessage(msg)
            .setPositiveButton("حذف", (d, w) -> {
                if (item.isFolder()) {
                    storage.deleteFolder(projectId, item.getPath());
                } else {
                    storage.deleteFile(projectId, item.getPath());
                }
                loadCurrentFolder();
                Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // ── Navigation ───────────────────────────────────────────

    private void openEditor(VcgFile file) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("projectId",   projectId);
        intent.putExtra("projectName", projectName);
        intent.putExtra("filename",    file.getPath()); // مسار كامل
        startActivity(intent);
    }

    private void openAssets() {
        Intent intent = new Intent(this, AssetsActivity.class);
        intent.putExtra("projectId",   projectId);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }

    private void openTerminal() {
        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_files, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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
        if (item.getItemId() == R.id.action_github_upload) {
            showGithubUploadDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_export_project) {
            exportProject();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Helpers ──────────────────────────────────────────────

    private String getParentPath(String path) {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(0, slash + 1) : "";
    }

    private void exportProject() {
        VcgProject project = storage.getProject(projectId);
        if (project == null) return;
        try {
            VcgExport.exportProject(this, storage, project);
        } catch (Exception e) {
            Toast.makeText(this, "فشل التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showGithubUploadDialog() {
        VcgSettings settings = new VcgSettings(this);
        if (!settings.isGithubConnected()) {
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle("GitHub غير مرتبط")
                .setMessage("اربط حسابك في GitHub أولاً من الإعدادات.")
                .setPositiveButton("فتح الإعدادات", (d, w) ->
                    startActivity(new Intent(this, SettingsActivity.class)))
                .setNegativeButton("إلغاء", null)
                .show();
            return;
        }
        VcgProject project = storage.getProject(projectId);
        if (project == null) return;
        String suggested = project.getName().toLowerCase().replaceAll("[^a-z0-9_\\-]+", "-");
        if (suggested.isEmpty()) suggested = "vcg-project";
        final String suggestedFinal = suggested;

        View view = getLayoutInflater().inflate(R.layout.dialog_github_upload, null);
        TextInputEditText repoInput = view.findViewById(R.id.input_repo_name);
        TextView userLabel          = view.findViewById(R.id.label_github_user);
        repoInput.setText(suggestedFinal);
        userLabel.setText("متّصل كـ: " + settings.getGithubUsername());

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("رفع \"" + project.getName() + "\" إلى GitHub")
            .setView(view)
            .setPositiveButton("إنشاء ورفع", (d, w) -> {
                String repoName = repoInput.getText() != null
                    ? repoInput.getText().toString().trim() : suggestedFinal;
                if (repoName.isEmpty()) repoName = suggestedFinal;
                uploadToGithub(project, settings, repoName);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void uploadToGithub(VcgProject project, VcgSettings settings, String repoName) {
        Toast.makeText(this, "جاري الرفع إلى GitHub...", Toast.LENGTH_SHORT).show();
        String token = settings.getGithubToken();
        new Thread(() -> {
            try {
                String fullRepo = VcgGitHub.createRepo(token, repoName, true);
                // رفع كل الملفات مع مساراتها الكاملة (بما فيها المجلدات)
                List<VcgFile> allFiles = storage.getAllInProject(projectId);
                for (VcgFile f : allFiles) {
                    if (f.isFolder()) continue; // المجلدات تُنشأ ضمنياً على GitHub
                    byte[] content = (f.getContent() != null ? f.getContent() : "").getBytes("UTF-8");
                    VcgGitHub.putFile(token, fullRepo, f.getPath(), content,
                        "VCG Editor: " + f.getPath());
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "تم الرفع بنجاح إلى " + fullRepo + " ✓",
                        Toast.LENGTH_LONG).show();
                    VcgNotifications.notifyGitHubPushed(this, project.getName(), fullRepo);
                });
            } catch (Exception e) {
                String msg = e.getMessage();
                runOnUiThread(() ->
                    Toast.makeText(this, "فشل الرفع: " + msg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showAbout() {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("Syrian VCG Editor")
            .setMessage("Version: 2.1.0\n\nمحرر لغة VCG البرمجية السورية\n\ngithub.com/syrian-vcg/vcg-lang")
            .setPositiveButton("إغلاق", null)
            .show();
    }
}
