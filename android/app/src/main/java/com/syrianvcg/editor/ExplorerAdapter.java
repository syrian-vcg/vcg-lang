package com.syrianvcg.editor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * ExplorerAdapter — يعرض المجلدات والملفات معاً في نفس القائمة (المجلدات أولاً)،
 * بنفس روح مستكشف الملفات في المحررات الاحترافية.
 */
public class ExplorerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_FILE   = 1;

    public interface Listener {
        void onFileClick(VcgFile file);
        void onFileDelete(VcgFile file);
        void onFileRename(VcgFile file);
        void onFolderClick(VcgFolder folder);
        void onFolderDelete(VcgFolder folder);
        void onFolderRename(VcgFolder folder);
    }

    private final List<VcgFolder> folders;
    private final List<VcgFile>   files;
    private final Listener        listener;

    public ExplorerAdapter(List<VcgFolder> folders, List<VcgFile> files, Listener listener) {
        this.folders  = folders;
        this.files    = files;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position < folders.size() ? TYPE_FOLDER : TYPE_FILE;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOLDER) {
            return new FolderViewHolder(inflater.inflate(R.layout.item_folder, parent, false));
        }
        return new FileViewHolder(inflater.inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FolderViewHolder) {
            VcgFolder folder = folders.get(position);
            FolderViewHolder h = (FolderViewHolder) holder;
            h.name.setText(folder.getName());
            int count = countItemsIn(folder.getId());
            h.count.setText(count == 0 ? "مجلد فارغ" : count + " عنصر");
            h.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
            h.btnDelete.setOnClickListener(v -> listener.onFolderDelete(folder));
            h.itemView.setOnLongClickListener(v -> { listener.onFolderRename(folder); return true; });
        } else {
            VcgFile f = files.get(position - folders.size());
            FileViewHolder h = (FileViewHolder) holder;
            h.name.setText(f.getName());
            h.preview.setText(f.getPreview());
            h.meta.setText(f.getLineCount() + " سطر");
            h.itemView.setOnClickListener(v -> listener.onFileClick(f));
            h.btnDelete.setOnClickListener(v -> listener.onFileDelete(f));
            h.itemView.setOnLongClickListener(v -> { listener.onFileRename(f); return true; });
        }
    }

    /** Best-effort item count shown on a folder row; files-only since folders are one level deep. */
    private int countItemsIn(String folderId) {
        int c = 0;
        for (VcgFile f : files) if (f.getFolderId().equals(folderId)) c++;
        return c;
    }

    @Override public int getItemCount() { return folders.size() + files.size(); }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView    name, preview, meta;
        ImageButton btnDelete;
        FileViewHolder(View v) {
            super(v);
            name      = v.findViewById(R.id.file_name);
            preview   = v.findViewById(R.id.file_preview);
            meta      = v.findViewById(R.id.file_meta);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView    name, count;
        ImageButton btnDelete;
        FolderViewHolder(View v) {
            super(v);
            name      = v.findViewById(R.id.folder_name);
            count     = v.findViewById(R.id.folder_count);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }

    /** Helper used by the host Activity to keep a flat copy of the currently-shown lists. */
    public static List<Object> combined(List<VcgFolder> folders, List<VcgFile> files) {
        List<Object> all = new ArrayList<>();
        all.addAll(folders);
        all.addAll(files);
        return all;
    }
}
