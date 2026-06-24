package com.syrianvcg.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.AssetViewHolder> {

    public interface AssetClickListener {
        void onAssetClick(VcgAsset asset);
        void onAssetDelete(VcgAsset asset);
    }

    private final List<VcgAsset> assets;
    private final AssetClickListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /**
     * ⚠️ كان فك تشفير base64 وفك ضغط الصورة الكاملة (BitmapFactory.decodeByteArray
     * بلا inSampleSize) يحدث بشكل متزامن (synchronous) داخل onBindViewHolder،
     * أي على UI thread مباشرة أثناء التمرير. لصور بحجم عدة ميغابايت هذا يسبب
     * تقطيعاً واضحاً (jank) أو حتى ANR، وقد يستهلك ذاكرة ضخمة لعرض صورة بحجم
     * thumbnail فقط (OutOfMemoryError محتمل مع عدة صور). الحل: فك التشفير على
     * خيط خلفي، تصغير الصورة لحجم الصورة المصغّرة المطلوب فعلياً، وتخزين
     * النتيجة في LruCache بالذاكرة لتجنّب إعادة العمل عند التمرير للخلف.
     */
    private static final ExecutorService decodeExecutor = Executors.newFixedThreadPool(2);
    private static final int THUMB_SIZE_PX = 256;
    private static final LruCache<String, Bitmap> thumbCache =
        new LruCache<String, Bitmap>(20 * 1024 * 1024) { // 20MB من ذاكرة البيتماب
            @Override protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

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
        // نربط الـ ViewHolder بمعرّف الأصل الحالي؛ تُستخدم لاحقاً للتأكد أن
        // النتيجة القادمة من الخيط الخلفي لا تزال تخص العنصر المعروض فعلاً
        // (التمرير السريع قد يُعيد تدوير هذا الـ ViewHolder لعنصر آخر قبل
        // انتهاء فك التشفير).
        h.boundAssetId = a.getId();

        if (a.isImage()) {
            Bitmap cached = thumbCache.get(a.getId());
            if (cached != null) {
                h.thumb.setImageBitmap(cached);
                h.videoIcon.setVisibility(View.GONE);
            } else {
                h.thumb.setImageResource(android.R.drawable.ic_menu_gallery); // placeholder أثناء التحميل
                h.videoIcon.setVisibility(View.GONE);
                decodeExecutor.execute(() -> {
                    Bitmap bmp = decodeThumbnail(a.getBase64Data());
                    if (bmp != null) thumbCache.put(a.getId(), bmp);
                    mainHandler.post(() -> {
                        if (a.getId().equals(h.boundAssetId)) {
                            if (bmp != null) h.thumb.setImageBitmap(bmp);
                            else h.thumb.setImageResource(android.R.drawable.ic_menu_report_image);
                        }
                    });
                });
            }
        } else {
            h.thumb.setImageResource(android.R.drawable.ic_media_play);
            h.videoIcon.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> listener.onAssetClick(a));
        h.btnDelete.setOnClickListener(v -> listener.onAssetDelete(a));
    }

    /** يفكّ base64 ويفك ضغط الصورة بحجم مصغّر فقط (لا الحجم الكامل) لتوفير الذاكرة. */
    private static Bitmap decodeThumbnail(String base64Data) {
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

            // الخطوة 1: قراءة أبعاد الصورة فقط بدون تحميلها كاملة بالذاكرة
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, boundsOptions);

            int sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, THUMB_SIZE_PX);

            // الخطوة 2: فك الضغط الفعلي بحجم مصغّر حسب inSampleSize المحسوبة
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOptions);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private static int calculateInSampleSize(int width, int height, int targetSize) {
        int sampleSize = 1;
        if (width <= 0 || height <= 0) return sampleSize;
        int largerDimension = Math.max(width, height);
        while (largerDimension / (sampleSize * 2) >= targetSize) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    @Override public int getItemCount() { return assets.size(); }

    static class AssetViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb, videoIcon;
        TextView name, size;
        ImageButton btnDelete;
        /** معرّف الأصل المرتبط حالياً بهذا الـ ViewHolder، لحماية من نتائج فك تشفير متأخرة لعنصر تم تدويره. */
        volatile String boundAssetId;
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
