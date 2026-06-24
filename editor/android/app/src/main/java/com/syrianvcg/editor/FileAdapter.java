package com.syrianvcg.editor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface FileClickListener {
        void onFileClick(VcgFile file);
        void onFileDelete(VcgFile file);
        void onFileRename(VcgFile file);
        /** يُستدعى عند الضغط على مجلد للدخول إليه */
        void onFolderClick(VcgFile folder);
    }

    private final List<VcgFile>      files;
    private final FileClickListener  listener;

    public FileAdapter(List<VcgFile> files, FileClickListener listener) {
        this.files    = files;
        this.listener = listener;
    }

    @NonNull @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder h, int pos) {
        VcgFile f = files.get(pos);

        h.name.setText(f.getName());
        h.preview.setText(f.getPreview());

        if (f.isFolder()) {
            // أيقونة ونص مجلد
            h.icon.setText("📁");
            h.meta.setText("مجلد");
            h.itemView.setOnClickListener(v -> listener.onFolderClick(f));
        } else {
            h.icon.setText("📄");
            h.meta.setText(f.getLineCount() + " سطر");
            h.itemView.setOnClickListener(v -> listener.onFileClick(f));
        }

        h.btnMore.setOnClickListener(v -> listener.onFileDelete(f));
        h.itemView.setOnLongClickListener(v -> { listener.onFileRename(f); return true; });
    }

    @Override public int getItemCount() { return files.size(); }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView    name, preview, meta, icon;
        ImageButton btnMore;

        FileViewHolder(View v) {
            super(v);
            name    = v.findViewById(R.id.file_name);
            preview = v.findViewById(R.id.file_preview);
            meta    = v.findViewById(R.id.file_meta);
            icon    = v.findViewById(R.id.file_icon_text);
            btnMore = v.findViewById(R.id.btn_delete);
        }
    }
}
