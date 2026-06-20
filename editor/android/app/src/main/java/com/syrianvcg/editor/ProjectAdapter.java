package com.syrianvcg.editor;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface ProjectClickListener {
        void onProjectClick(VcgProject project);
        void onProjectDelete(VcgProject project);
        void onProjectRename(VcgProject project);
    }

    private final List<VcgProject> projects;
    private final VcgStorage storage;
    private final ProjectClickListener listener;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("d MMM, HH:mm", Locale.US);

    public ProjectAdapter(List<VcgProject> projects, VcgStorage storage, ProjectClickListener listener) {
        this.projects = projects;
        this.storage = storage;
        this.listener = listener;
    }

    @NonNull @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder h, int pos) {
        VcgProject p = projects.get(pos);
        h.name.setText(p.getName());
        h.desc.setText(p.getDescription() == null || p.getDescription().isEmpty()
            ? "بدون وصف" : p.getDescription());

        int fileCount = storage.getFilesInProject(p.getId()).size();
        int assetCount = storage.getAssetsInProject(p.getId()).size();
        h.meta.setText(fileCount + " ملف · " + assetCount + " ملف وسائط · " + DATE_FMT.format(p.getLastModified()));

        try {
            int color = Color.parseColor(p.getColorTag());
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(10f * h.itemView.getResources().getDisplayMetrics().density);
            bg.setColor(color);
            h.colorDot.setBackground(bg);
        } catch (Exception ignored) {}

        h.itemView.setOnClickListener(v -> listener.onProjectClick(p));
        h.btnDelete.setOnClickListener(v -> listener.onProjectDelete(p));
        h.btnMore.setOnClickListener(v -> listener.onProjectRename(p));
    }

    @Override public int getItemCount() { return projects.size(); }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView name, desc, meta;
        View colorDot;
        ImageButton btnDelete, btnMore;
        ProjectViewHolder(View v) {
            super(v);
            name      = v.findViewById(R.id.project_name);
            desc      = v.findViewById(R.id.project_desc);
            meta      = v.findViewById(R.id.project_meta);
            colorDot  = v.findViewById(R.id.project_color_dot);
            btnDelete = v.findViewById(R.id.btn_delete_project);
            btnMore   = v.findViewById(R.id.btn_edit_project);
        }
    }
}
