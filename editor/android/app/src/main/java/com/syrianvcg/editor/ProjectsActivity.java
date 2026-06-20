package com.syrianvcg.editor;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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

    private static final String[] COLOR_TAGS = {
        "#4DC95A", "#6AB0FF", "#F5C842", "#FF7A7A", "#CC99FF", "#00D4FF"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);
        setSupportActionBar(findViewById(R.id.toolbar));

        storage = new VcgStorage(this);
        storage.migrateLegacyIfNeeded();

        RecyclerView rv = findViewById(R.id.recycler_projects);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectAdapter(projects, storage, this);
        rv.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view);

        FloatingActionButton fab = findViewById(R.id.fab_new_project);
        fab.setOnClickListener(v -> showNewProjectDialog());

        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void loadProjects() {
        projects.clear();
        projects.addAll(storage.getAllProjects());
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
