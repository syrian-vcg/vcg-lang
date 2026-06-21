package com.syrianvcg.editor;

import android.content.Intent;
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
    private String projectId;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        storage = new VcgStorage(this);
        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");

        if (projectId == null) {
            // Safety net: shouldn't happen, but avoid crashing if launched directly.
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(projectName != null ? projectName : "المشروع");
        }

        RecyclerView rv = findViewById(R.id.recycler_files);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(files, this);
        rv.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view);

        FloatingActionButton fab = findViewById(R.id.fab_new);
        fab.setOnClickListener(v -> showNewFileDialog());

        findViewById(R.id.btn_assets).setOnClickListener(v -> openAssets());
        findViewById(R.id.btn_terminal).setOnClickListener(v -> openTerminal());

        loadFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFiles();
    }

    private void loadFiles() {
        files.clear();
        files.addAll(storage.getFilesInProject(projectId));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
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
                VcgFile f = new VcgFile(projectId, name, "# " + name + "\n\n");
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
                storage.deleteFile(projectId, file.getName());
                loadFiles();
                Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onFileRename(VcgFile file) {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        TextInputEditText input = view.findViewById(R.id.input_filename);
        input.setText(file.getName());

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("إعادة تسمية")
            .setView(view)
            .setPositiveButton("حفظ", (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (name.isEmpty()) return;
                if (!name.endsWith(".vcg")) name += ".vcg";
                storage.renameFile(projectId, file.getName(), name);
                loadFiles();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void openEditor(VcgFile file) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("projectName", projectName);
        intent.putExtra("filename", file.getName());
        startActivity(intent);
    }

    private void openAssets() {
        Intent intent = new Intent(this, AssetsActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }

    private void openTerminal() {
        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_files, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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
                .setMessage("اربط حسابك في GitHub أولاً من الإعدادات (يحتاج Personal Access Token).")
                .setPositiveButton("فتح الإعدادات", (d, w) -> startActivity(new Intent(this, SettingsActivity.class)))
                .setNegativeButton("إلغاء", null)
                .show();
            return;
        }

        VcgProject project = storage.getProject(projectId);
        if (project == null) return;
        String suggested = project.getName().toLowerCase().replaceAll("[^a-z0-9_\\-]+", "-");
        if (suggested.isEmpty()) suggested = "vcg-project";

        View view = getLayoutInflater().inflate(R.layout.dialog_github_upload, null);
        TextInputEditText repoInput = view.findViewById(R.id.input_repo_name);
        TextView userLabel = view.findViewById(R.id.label_github_user);
        repoInput.setText(suggested);
        userLabel.setText("متّصل كـ: " + settings.getGithubUsername());

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("رفع \"" + project.getName() + "\" إلى GitHub")
            .setView(view)
            .setPositiveButton("إنشاء ورفع", (d, w) -> {
    String typedRepoName = repoInput.getText() != null ? repoInput.getText().toString().trim() : "";
    String finalRepoName = typedRepoName.isEmpty() ? suggested : typedRepoName;
    uploadProjectToGithub(project, settings, finalRepoName);
}) 
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void uploadProjectToGithub(VcgProject project, VcgSettings settings, String repoName) {
        Toast.makeText(this, "جاري الرفع إلى GitHub...", Toast.LENGTH_SHORT).show();
        String token = settings.getGithubToken();
        new Thread(() -> {
            try {
                String fullRepo = VcgGitHub.createRepo(token, repoName, true);
                List<VcgFile> filesToUpload = storage.getFilesInProject(projectId);
                for (VcgFile f : filesToUpload) {
                    byte[] content = (f.getContent() != null ? f.getContent() : "").getBytes("UTF-8");
                    VcgGitHub.putFile(token, fullRepo, f.getName(), content, "VCG Editor: " + f.getName());
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "تم الرفع بنجاح إلى " + fullRepo + " ✓", Toast.LENGTH_LONG).show();
                    VcgNotifications.notify(this, 4, "تم الرفع إلى GitHub ✓",
                        project.getName() + " → " + fullRepo);
                });
            } catch (Exception e) {
                String msg = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "فشل الرفع: " + msg, Toast.LENGTH_LONG).show());
            }
        }).start();
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
