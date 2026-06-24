package com.syrianvcg.editor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
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
    private VcgCoins coins;
    private VcgAds ads;
    private TextView coinLabel;

    private static final String[] COLOR_TAGS = {
        "#4DC95A", "#6AB0FF", "#F5C842", "#FF7A7A", "#CC99FF", "#00D4FF"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_projects);
        setSupportActionBar(findViewById(R.id.toolbar));
        // إخفاء العنوان الافتراضي — التصميم في RelativeLayout المنفصل
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        // ربط زر ⋮ يدوياً بـ PopupMenu
        findViewById(R.id.btn_more).setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
            popup.show();
        });

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

        coins = new VcgCoins(this);
        ads = new VcgAds();
        VcgAds.init(this);
        ads.preload(this);
        ads.preloadInterstitial(this);
        ads.preloadRewardedInterstitial(this);
        coinLabel = findViewById(R.id.label_coin_balance_home);
        refreshCoinBalance();

        com.google.android.material.button.MaterialButton adBtn =
            findViewById(R.id.btn_watch_ad_home);

        // ══════════════════════════════════════════════════════════════
        // إصلاح: الزر يكون مفعَّلاً دائماً — إذا لم يكن الإعلان جاهزاً
        // يُظهر رسالة ويبدأ التحميل مجدداً بدلاً من تجميد الواجهة.
        // ══════════════════════════════════════════════════════════════
        adBtn.setText("📺 اكسب عملات");
        adBtn.setEnabled(true);
        adBtn.setAlpha(1f);
        adBtn.setOnClickListener(v -> watchAdForCoins());

        // ── بانر AdMob في أسفل الشاشة ──────────────────────────────────────
        FrameLayout bannerContainer = findViewById(R.id.banner_container);
        ads.showBanner(this, bannerContainer);

        FloatingActionButton fab = findViewById(R.id.fab_new_project);
        fab.setOnClickListener(v -> showNewProjectDialog());

        importPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) importProjectFromZip(uri); }
        );

        loadProjects();

        // ── App Shortcuts Intent Handler ────────────────────────────
        handleShortcutIntent(getIntent());
    }

    private void handleShortcutIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case "com.syrianvcg.editor.ACTION_NEW_PROJECT":
                // slight delay so UI is ready
                findViewById(R.id.fab_new_project).postDelayed(
                    this::showNewProjectDialog, 300);
                break;
            case "com.syrianvcg.editor.ACTION_LAST_PROJECT":
                openLastProject();
                break;
        }
    }

    private void openLastProject() {
        if (projects == null || projects.isEmpty()) return;
        VcgProject last = projects.get(0);
        openProject(last);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
        if (coins != null) refreshCoinBalance();
        if (ads != null) ads.resumeBanner();
    }

    @Override
    protected void onPause() {
        if (ads != null) ads.pauseBanner();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ads != null) ads.destroyBanner();
        super.onDestroy();
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
        ads.showInterstitial(this, () -> new Thread(() -> {
            try {
                VcgExport.exportProject(this, storage, project);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "فشل التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start());
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
        if (item.getItemId() == R.id.action_suggestion) {
            showSuggestionDialog();
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

    private void refreshCoinBalance() {
        if (coinLabel != null) coinLabel.setText("🪙 " + coins.getBalance() + " عملة");
    }

    private void watchAdForCoins() {
        if (!ads.isReady()) {
            Toast.makeText(this, "جاري تحميل الإعلان، حاول بعد لحظات...", Toast.LENGTH_SHORT).show();
            ads.preload(this);
            // أعِد المحاولة تلقائياً بعد 3 ثوان إن اكتمل التحميل
            ads.setAdReadyListener(() -> runOnUiThread(() ->
                Toast.makeText(this, "الإعلان جاهز! اضغط مجدداً 📺", Toast.LENGTH_SHORT).show()
            ));
            return;
        }
        ads.show(this, new VcgAds.RewardListener() {
            @Override
            public void onRewardEarned() {
                runOnUiThread(() -> {
                    coins.grantCoinsForAd();
                    refreshCoinBalance();
                    Toast.makeText(ProjectsActivity.this,
                        "🎉 حصلت على " + VcgCoins.COINS_PER_AD + " عملة!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onAdUnavailable(String reason) {
                runOnUiThread(() ->
                    Toast.makeText(ProjectsActivity.this, "تعذّر عرض الإعلان", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showSuggestionDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_suggestion, null);
        com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.input_suggestion);

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("اقتراح جديد 💡")
            .setView(view)
            .setPositiveButton("إرسال", (d, w) -> {
                String text = input.getText() != null ? input.getText().toString() : "";
                VcgFeedback.openSuggestionEmail(this, text);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showAbout() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(8), pad, dp(16));
        scrollView.addView(root);

        // ── Helper lambdas ──────────────────────────────────────────
        java.util.function.Consumer<String> addHeader = title -> {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(title);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            tv.setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary));
            tv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
            tv.setLetterSpacing(0.12f);
            android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(18);
            lp.bottomMargin = dp(6);
            root.addView(tv, lp);
            android.view.View line = new android.view.View(this);
            line.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorPrimary) & 0x33FFFFFF);
            root.addView(line, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        };

        java.util.function.Consumer<String> addBody = text -> {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(text);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tv.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            tv.setLineSpacing(0, 1.55f);
            android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            root.addView(tv, lp);
        };

        java.util.function.BiConsumer<String, String> addStep = (num, desc) -> {
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.TOP);
            android.widget.LinearLayout.LayoutParams rowLp =
                new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(8);
            android.widget.TextView numTv = new android.widget.TextView(this);
            numTv.setText(num);
            numTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f);
            numTv.setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary));
            numTv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
            numTv.setMinWidth(dp(28));
            android.widget.TextView descTv = new android.widget.TextView(this);
            descTv.setText(desc);
            descTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13.5f);
            descTv.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            descTv.setLineSpacing(0, 1.45f);
            android.widget.LinearLayout.LayoutParams descLp =
                new android.widget.LinearLayout.LayoutParams(0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            descLp.setMarginStart(dp(6));
            row.addView(numTv);
            row.addView(descTv, descLp);
            root.addView(row, rowLp);
        };

        java.util.function.Consumer<String> addCode = code -> {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(code);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tv.setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary));
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            tv.setBackground(makeRoundedBg(
                resolveColor(com.google.android.material.R.attr.colorSurfaceVariant), dp(8)));
            int cp = dp(10);
            tv.setPadding(cp, dp(8), cp, dp(8));
            android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            lp.bottomMargin = dp(4);
            root.addView(tv, lp);
        };

        // ── HEADER CARD ─────────────────────────────────────────────
        android.widget.LinearLayout headerCard = new android.widget.LinearLayout(this);
        headerCard.setOrientation(android.widget.LinearLayout.VERTICAL);
        headerCard.setGravity(android.view.Gravity.CENTER);
        headerCard.setBackground(makeRoundedBg(
            resolveColor(com.google.android.material.R.attr.colorSurfaceVariant), dp(14)));
        headerCard.setPadding(dp(16), dp(20), dp(16), dp(16));

        android.widget.TextView logoBadge = new android.widget.TextView(this);
        logoBadge.setText("VCG");
        logoBadge.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f);
        logoBadge.setTextColor(0xFF101010);
        logoBadge.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        logoBadge.setGravity(android.view.Gravity.CENTER);
        logoBadge.setBackground(makeRoundedBg(
            resolveColor(com.google.android.material.R.attr.colorPrimary), dp(10)));
        android.widget.LinearLayout.LayoutParams badgeLp =
            new android.widget.LinearLayout.LayoutParams(dp(72), dp(52));
        badgeLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        badgeLp.bottomMargin = dp(12);
        headerCard.addView(logoBadge, badgeLp);

        android.widget.TextView appName = new android.widget.TextView(this);
        appName.setText("Syrian VCG Editor");
        appName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f);
        appName.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        appName.setTypeface(null, android.graphics.Typeface.BOLD);
        appName.setGravity(android.view.Gravity.CENTER);
        headerCard.addView(appName);

        android.widget.TextView version = new android.widget.TextView(this);
        version.setText("الإصدار  2.1.0  •  لغة VCG البرمجية");
        version.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
        version.setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary));
        version.setGravity(android.view.Gravity.CENTER);
        version.setTypeface(android.graphics.Typeface.MONOSPACE);
        android.widget.LinearLayout.LayoutParams verLp =
            new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        verLp.topMargin = dp(4);
        verLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        headerCard.addView(version, verLp);

        android.widget.LinearLayout.LayoutParams cardLp =
            new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(4);
        root.addView(headerCard, cardLp);

        // ── SECTION 1: عن التطبيق ───────────────────────────────────
        addHeader.accept("◈  عن التطبيق");
        addBody.accept(
            "Syrian VCG Editor هو محرر كود متكامل مصمم لكتابة وتشغيل برامج لغة VCG " +
            "البرمجية السورية مباشرةً على الهاتف.\n\n" +
            "اللغة مبنية على مترجم حقيقي مكتوب بـ C11، وتدعم البرمجة العربية الكاملة — " +
            "يمكنك كتابة أسماء متغيرات ودوال وتعليقات باللغة العربية بشكل طبيعي تماماً."
        );

        // ── SECTION 2: المميزات ─────────────────────────────────────
        addHeader.accept("◈  مميزات المحرر");
        addStep.accept("▸", "محرر كود بتلوين صياغي (Syntax Highlighting) لجميع كلمات لغة VCG");
        addStep.accept("▸", "معاينة مباشرة للمخرجات أثناء الكتابة بدون الحاجة لتشغيل يدوي");
        addStep.accept("▸", "لوحة مفاتيح سريعة بكل كلمات اللغة الأساسية بضغطة واحدة");
        addStep.accept("▸", "ترقيم أسطر تلقائي يتزامن مع التمرير");
        addStep.accept("▸", "Terminal مدمج لتنفيذ الأوامر ومتابعة الأخطاء");
        addStep.accept("▸", "دعم الوسائط: صور وملفات صوتية داخل المشاريع");
        addStep.accept("▸", "استيراد وتصدير المشاريع كـ ZIP");
        addStep.accept("▸", "رفع المشاريع مباشرة إلى GitHub");
        addStep.accept("▸", "دعم الوضع الليلي والفاتح مع ثيمات متعددة");

        // ── SECTION 3: تعلّم اللغة ──────────────────────────────────
        addHeader.accept("◈  تعلّم لغة VCG");
        addBody.accept("لغة VCG بسيطة وسريعة التعلم. إليك أساسيات البرمجة بها:");
        addCode.accept(
            "# هذا تعليق\n" +
            "أظهر(\"مرحباً بالعالم\")\n\n" +
            "# متغيّر\n" +
            "ليكن اسم = \"علي\"\n" +
            "أظهر(\"اسمك: \" + اسم)"
        );
        addStep.accept("①", "الجمل — كل أمر يُكتب في سطر مستقل، ولا حاجة لفاصلة منقوطة.");
        addStep.accept("②", "المتغيرات — تُعرَّف بـ  ليكن  أو  ثابت  (للقيم الثابتة).");
        addStep.accept("③", "الطباعة — استخدم  أظهر()  أو  show()  لعرض النصوص والأرقام.");
        addStep.accept("④", "الشروط — if / إذا  +  else / وإلا  للتحكم في التدفق.");
        addStep.accept("⑤", "الحلقات — while / طالما  لتكرار الكود حتى يتحقق الشرط.");
        addStep.accept("⑥", "الدوال — عرّف بـ  func اسم() { ... }  واستدعها بالاسم.");
        addCode.accept(
            "# مثال: مجموع الأعداد من 1 إلى N\n" +
            "func مجموع(ن) {\n" +
            "    ليكن ناتج = 0\n" +
            "    طالما ن > 0 {\n" +
            "        ناتج = ناتج + ن\n" +
            "        ن = ن - 1\n" +
            "    }\n" +
            "    return ناتج\n" +
            "}\n" +
            "أظهر(مجموع(10))"
        );

        // ── SECTION 4: كيف تبدأ ─────────────────────────────────────
        addHeader.accept("◈  كيف تبدأ؟");
        addStep.accept("①", "اضغط  +  في الصفحة الرئيسية لإنشاء مشروع جديد.");
        addStep.accept("②", "اختر اسم المشروع ولوناً مميزاً له.");
        addStep.accept("③", "افتح المشروع ثم اضغط  +  لإنشاء ملف  .vcg");
        addStep.accept("④", "اكتب كودك واستخدم لوحة المفاتيح السريعة في الأسفل.");
        addStep.accept("⑤", "اضغط زر  ▶  للتشغيل ومشاهدة المخرجات فوراً.");
        addStep.accept("⑥", "فعّل المعاينة المباشرة من زر  👁  ليتحدث التطبيق تلقائياً.");

        // ── SECTION 5: نصائح ────────────────────────────────────────
        addHeader.accept("◈  نصائح وحيل");
        addStep.accept("💡", "اضغط طويلاً على مشروع للوصول السريع لخيارات التعديل والحذف.");
        addStep.accept("💡", "من قائمة  ⋮  يمكنك استيراد مشروع ZIP أو مشاركته.");
        addStep.accept("💡", "Terminal مفيد لمشاهدة أخطاء الترجمة بالتفصيل.");
        addStep.accept("💡", "شاهد إعلاناً للحصول على عملات تُستخدم لفتح مميزات إضافية.");
        addStep.accept("💡", "من الإعدادات يمكن تغيير حجم الخط والثيم وسلوك المحرر.");

        // ── SECTION 6: مفتوح المصدر ─────────────────────────────────
        addHeader.accept("◈  مفتوح المصدر");
        addBody.accept(
            "المشروع مفتوح المصدر بالكامل. يمكنك الاطلاع على الكود، الإبلاغ عن أخطاء، " +
            "أو المساهمة في التطوير عبر GitHub:"
        );

        android.widget.Button ghBtn = new android.widget.Button(this);
        ghBtn.setText("⇢  github.com/syrian-vcg/vcg-lang");
        ghBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.5f);
        ghBtn.setTypeface(android.graphics.Typeface.MONOSPACE);
        ghBtn.setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary));
        ghBtn.setBackground(makeStrokeRoundedBg(
            resolveColor(com.google.android.material.R.attr.colorPrimary), dp(8)));
        ghBtn.setAllCaps(false);
        android.widget.LinearLayout.LayoutParams ghLp =
            new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        ghLp.topMargin = dp(10);
        ghLp.bottomMargin = dp(4);
        root.addView(ghBtn, ghLp);
        ghBtn.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/syrian-vcg/vcg-lang"))));

        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setView(scrollView)
            .setPositiveButton("إغلاق", null)
            .show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int resolveColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private android.graphics.drawable.Drawable makeRoundedBg(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private android.graphics.drawable.Drawable makeStrokeRoundedBg(int strokeColor, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(0x00000000);
        d.setStroke(dp(1), strokeColor);
        d.setCornerRadius(radius);
        return d;
    }
}
