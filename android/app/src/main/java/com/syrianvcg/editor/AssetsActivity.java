package com.syrianvcg.editor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssetsActivity extends AppCompatActivity implements AssetAdapter.AssetClickListener {

    private static final long MAX_ASSET_BYTES = 4L * 1024 * 1024; // 4MB safety cap for SharedPreferences storage

    private VcgStorage storage;
    private String projectId;
    private AssetAdapter adapter;
    private List<VcgAsset> assets = new ArrayList<>();
    private TextView emptyView;

    // PickVisualMedia: منتقي وسائط نظام Android 13+ (Photo Picker API).
    // يعمل تلقائياً على الأجهزة الأقدم (Android 11-12) عبر Google Play Services backport.
    // لا يتطلب أذونات READ_MEDIA_* ويفتح واجهة النظام المدمجة (أكثر خصوصيةً وأماناً).
    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;
    private ActivityResultLauncher<PickVisualMediaRequest> pickVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_assets);

        storage = new VcgStorage(this);
        projectId = getIntent().getStringExtra("projectId");
        String projectName = getIntent().getStringExtra("projectName");

        setSupportActionBar(findViewById(R.id.assets_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("الوسائط — " + (projectName != null ? projectName : ""));
        }

        RecyclerView rv = findViewById(R.id.recycler_assets);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AssetAdapter(assets, this);
        rv.setAdapter(adapter);
        emptyView = findViewById(R.id.empty_assets_view);

        pickImage = registerForActivityResult(new PickVisualMedia(), uri -> {
            if (uri != null) importMedia(uri);
        });
        pickVideo = registerForActivityResult(new PickVisualMedia(), uri -> {
            if (uri != null) importMedia(uri);
        });

        FloatingActionButton fab = findViewById(R.id.fab_upload);
        fab.setOnClickListener(v ->
            pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        findViewById(R.id.btn_pick_video).setOnClickListener(v ->
            pickVideo.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.VideoOnly.INSTANCE)
                .build()));
        findViewById(R.id.btn_pick_image).setOnClickListener(v ->
            pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        loadAssets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssets();
    }

    private void loadAssets() {
        assets.clear();
        assets.addAll(storage.getAssetsInProject(projectId));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(assets.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void importMedia(Uri uri) {
        try {
            String name = queryFileName(uri);
            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";

            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) { toast("تعذر فتح الملف"); return; }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n; long total = 0;
            while ((n = is.read(buf)) != -1) {
                total += n;
                if (total > MAX_ASSET_BYTES) {
                    is.close();
                    toast("الملف كبير جداً (الحد 4MB لكل ملف)");
                    return;
                }
                baos.write(buf, 0, n);
            }
            is.close();

            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            VcgAsset asset = storage.saveAsset(projectId, name, mime, b64, total);
            loadAssets();
            toast("✓ تمت إضافة " + asset.getName());
        } catch (IOException e) {
            toast("خطأ بالاستيراد: " + e.getMessage());
        }
    }

    private String queryFileName(Uri uri) {
        String result = "file_" + System.currentTimeMillis();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    @Override
    public void onAssetClick(VcgAsset asset) {
        // Copy reference snippet to clipboard for quick paste into editor
        String snippet = asset.isVideo()
            ? "video(\"" + asset.getAssetRef() + "\")"
            : "img(\"" + asset.getAssetRef() + "\")";
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("vcg_asset", snippet));
        toast("✓ تم نسخ: " + snippet);
    }

    @Override
    public void onAssetDelete(VcgAsset asset) {
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("حذف الوسائط")
            .setMessage("هل تريد حذف \"" + asset.getName() + "\"؟")
            .setPositiveButton("حذف", (d, w) -> {
                storage.deleteAsset(asset.getId());
                loadAssets();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
