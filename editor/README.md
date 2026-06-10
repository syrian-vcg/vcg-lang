# Syrian VCG Editor — Android APK

محرر أكواد VCG كامل للموبايل، مبني بـ Android Java.

## المميزات

- ✅ **محرر كود** مع تلوين syntax للغة VCG
- ✅ **لوحة مفاتيح سريعة** بكل كلمات VCG المحجوزة
- ✅ **تشغيل فوري** — يعرض نتيجة الكود في WebView
- ✅ **مدير ملفات** — إنشاء، حفظ، حذف ملفات .vcg
- ✅ **5 أمثلة جاهزة** عند أول تشغيل
- ✅ **دعم UI الكامل** — youtube, facebook, instagram, btn, h, l, ...
- ✅ **المتجر التفاعلي** — $set, $get, watch
- ✅ **القنوات** — c, send, recv
- ✅ **إعدادات** — حجم الخط، تلوين، مسافة بادئة
- ✅ **ثيم زيتوني داكن** مطابق لهوية VCG

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
│   │   ├── MainActivity.java        ← مدير الملفات
│   │   ├── EditorActivity.java      ← محرر الكود
│   │   ├── OutputActivity.java      ← عرض النتيجة
│   │   ├── SettingsActivity.java    ← الإعدادات
│   │   ├── VcgCodeEditor.java       ← محرر مخصص + syntax highlighting
│   │   ├── VcgInterpreter.java      ← مُحوِّل VCG → HTML
│   │   ├── VcgStorage.java          ← حفظ الملفات (SharedPreferences)
│   │   ├── VcgFile.java             ← نموذج الملف
│   │   ├── FileAdapter.java         ← RecyclerView adapter
│   │   └── VcgJsInterface.java      ← JS Bridge
│   ├── res/
│   │   ├── layout/                  ← تصاميم الشاشات
│   │   ├── drawable/                ← أيقونات ورسومات
│   │   ├── values/                  ← ألوان، نصوص، ستايل
│   │   └── menu/                    ← قوائم
│   └── AndroidManifest.xml
├── build.gradle
└── gradlew
```

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
