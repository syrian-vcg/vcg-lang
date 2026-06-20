package com.syrianvcg.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.AssetViewHolder> {

    public interface AssetClickListener {
        void onAssetClick(VcgAsset asset);
        void onAssetDelete(VcgAsset asset);
    }

    private final List<VcgAsset> assets;
    private final AssetClickListener listener;

    public AssetAdapter(List<VcgAsset> assets, AssetClickListener listener) {
        this.assets = assets;
        this.listener = listener;
    }

    @NonNull @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_asset, parent, false);
        return new AssetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder h, int pos) {
        VcgAsset a = assets.get(pos);
        h.name.setText(a.getName());
        h.size.setText(a.getSizeLabel());

        if (a.isImage()) {
            try {
                byte[] bytes = Base64.decode(a.getBase64Data(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                h.thumb.setImageBitmap(bmp);
                h.videoIcon.setVisibility(View.GONE);
            } catch (Exception e) {
                h.thumb.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            h.thumb.setImageResource(android.R.drawable.ic_media_play);
            h.videoIcon.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> listener.onAssetClick(a));
        h.btnDelete.setOnClickListener(v -> listener.onAssetDelete(a));
    }

    @Override public int getItemCount() { return assets.size(); }

    static class AssetViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb, videoIcon;
        TextView name, size;
        ImageButton btnDelete;
        AssetViewHolder(View v) {
            super(v);
            thumb     = v.findViewById(R.id.asset_thumb);
            videoIcon = v.findViewById(R.id.asset_video_icon);
            name      = v.findViewById(R.id.asset_name);
            size      = v.findViewById(R.id.asset_size);
            btnDelete = v.findViewById(R.id.btn_delete_asset);
        }
    }
}
