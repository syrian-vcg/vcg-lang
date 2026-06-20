# Syrian VCG Editor — Android APK

محرر أكواد VCG كامل للموبايل، مبني بـ Android Java.

## المميزات

- ✅ **نظام مشاريع** — أنشئ عدة مشاريع، كل مشروع فيه ملفاته وصوره الخاصة
- ✅ **رفع الصور والفيديو** — رفع وسائط من الجهاز، وإدراجها مباشرة بكود VCG عبر `img()` / `video()`
- ✅ **محرر كود** مع تلوين syntax للغة VCG + أرقام أسطر
- ✅ **معاينة مباشرة (Live Preview)** — نتيجة الكود تتحدث أثناء الكتابة بنصف ثانية
- ✅ **Terminal** — سجل تشغيل (logs/errors) + REPL سريع لتجربة تعابير VCG مباشرة
- ✅ **لوحة مفاتيح سريعة** بكل كلمات VCG المحجوزة
- ✅ **تشغيل فوري** — يعرض نتيجة الكود في WebView كامل الشاشة
- ✅ **مدير ملفات** — إنشاء، حفظ، حذف، إعادة تسمية ملفات .vcg لكل مشروع
- ✅ **دعم UI الكامل** — youtube, facebook, instagram, btn, h, l, ...
- ✅ **المتجر التفاعلي** — $set, $get, watch
- ✅ **القنوات** — c, send, recv
- ✅ **إعدادات موسّعة** — 4 ثيمات عرض (زيتوني/ليلي/أسود/رملي)، نوع الخط، حجم الخط، حجم Tab، معاينة مباشرة، حفظ تلقائي، اهتزاز
- ✅ **ثيم زيتوني داكن** مطابق لهوية VCG (قابل للتغيير من الإعدادات)

## بناء الـ APK

### عبر GitHub Actions (تلقائي)
كل push إلى `main` يبني APK تلقائياً.
اذهب إلى: **Actions → Build VCG Editor APK → Artifacts**

### محلياً
```bash
cd editor/android
chmod +x gradlew
./gradlew assembleDebug
# APK في: app/build/outputs/apk/debug/
```

## هيكل المشروع

```
editor/android/
├── app/src/main/
│   ├── java/com/syrianvcg/editor/
│   │   ├── SplashActivity.java      ← شاشة البداية
│   │   ├── ProjectsActivity.java    ← الشاشة الرئيسية: قائمة المشاريع
│   │   ├── MainActivity.java        ← مدير ملفات مشروع واحد
│   │   ├── EditorActivity.java      ← محرر الكود + معاينة مباشرة
│   │   ├── OutputActivity.java      ← عرض النتيجة كاملة الشاشة
│   │   ├── AssetsActivity.java      ← رفع وإدارة الصور/الفيديو
│   │   ├── TerminalActivity.java    ← Terminal: سجل + REPL
│   │   ├── SettingsActivity.java    ← الإعدادات الموسّعة
│   │   ├── VcgCodeEditor.java       ← محرر مخصص + syntax highlighting
│   │   ├── VcgInterpreter.java      ← مُحوِّل VCG → HTML (يدعم الثيمات والأصول)
│   │   ├── VcgHeadlessRunner.java   ← مُنفّذ REPL مصغّر للـ Terminal
│   │   ├── VcgStorage.java          ← حفظ المشاريع/الملفات/الأصول (SharedPreferences)
│   │   ├── VcgSettings.java         ← مركز إدارة إعدادات المحرر
│   │   ├── VcgProject.java          ← نموذج المشروع
│   │   ├── VcgFile.java             ← نموذج الملف (مرتبط بمشروع)
│   │   ├── VcgAsset.java            ← نموذج ملف الوسائط (صورة/فيديو)
│   │   ├── ProjectAdapter.java      ← RecyclerView adapter للمشاريع
│   │   ├── FileAdapter.java         ← RecyclerView adapter للملفات
│   │   ├── AssetAdapter.java        ← RecyclerView adapter للوسائط
│   │   └── VcgJsInterface.java      ← JS Bridge (logs, alerts, نتائج التشغيل)
│   ├── res/
│   │   ├── layout/                  ← تصاميم الشاشات
│   │   ├── drawable/                ← أيقونات ورسومات
│   │   ├── values/                  ← ألوان، نصوص، ستايل
│   │   └── menu/                    ← قوائم
│   └── AndroidManifest.xml
├── build.gradle
└── gradlew
```

## ملاحظات حول التخزين

- المشاريع والملفات والأصول (الصور/الفيديو) تُخزّن محلياً على الجهاز عبر `SharedPreferences` بصيغة JSON.
- الأصول تُخزّن كـ Base64 (حد أقصى 4MB لكل ملف) ويتم إدراجها داخل HTML الناتج كـ `data:` URL مباشرة، فلا حاجة لخادم خارجي.
- ملفات المستخدمين من النسخة القديمة (تخزين مسطّح بدون مشاريع) يتم ترحيلها تلقائياً إلى مشروع جديد اسمه "مشروعي الأول" عند أول فتح بعد التحديث.

## Secrets للتوقيع (اختياري)

لتوقيع الـ APK، أضف في **Settings → Secrets**:
- `KEYSTORE_BASE64` — keystore مشفر بـ base64
- `KEYSTORE_PASS` — كلمة مرور الـ keystore
- `KEY_ALIAS` — اسم المفتاح
- `KEY_PASS` — كلمة مرور المفتاح

```bash
# توليد keystore
keytool -genkey -v -keystore vcg-release.keystore \
  -alias vcg -keyalg RSA -keysize 2048 -validity 10000
# تشفير
base64 vcg-release.keystore | tr -d '\n'
```

