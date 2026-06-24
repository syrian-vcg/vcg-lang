# VCG Language SDK — v2.0.1

حزمة تطوير لغة VCG السورية  
Syrian VCG Language SDK

## الملفات / Files

```
sdk/
├── build/
│   └── vcg-sdk-2.0.1.jar          ← الـ JAR الجاهز للاستخدام
├── src/main/java/com/syrianvcg/vcgsdk/
│   ├── VcgSDK.java                 ← الواجهة الرئيسية
│   ├── VcgCompiler.java            ← مُصرِّف موسّع مع معالجة أخطاء
│   ├── VcgInterpreter.java         ← المُفسِّر الكامل (JS runtime)
│   ├── VcgHighlighter.java         ← تلوين الكود المصدري
│   ├── VcgKeywords.java            ← قائمة الكلمات المحجوزة
│   └── VcgResult.java              ← نتيجة التصريف
└── src/main/resources/META-INF/
    └── MANIFEST.MF
```

## الاستخدام في Android / Usage in Android

### 1. أضف الـ JAR لمشروعك

ضع `vcg-sdk-2.0.1.jar` في مجلد `app/libs/`  
ثم في `build.gradle`:

```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
}
```

### 2. استخدام الـ API

```java
import com.syrianvcg.vcgsdk.VcgSDK;
import com.syrianvcg.vcgsdk.VcgCompiler;
import com.syrianvcg.vcgsdk.VcgHighlighter;

// ── تصريف بسيط
VcgSDK sdk = new VcgSDK();
String html = sdk.compile("show(\"مرحباً بالعالم!\")");

// ── تصريف مع ثيم وعنوان
String html = sdk.compile(code, "تطبيقي", VcgSDK.THEME_MIDNIGHT);

// ── تصريف آمن مع معالجة أخطاء
VcgCompiler compiler = new VcgCompiler()
    .setTheme(VcgSDK.THEME_AMOLED)
    .setTitle("مشروع VCG");

VcgResult result = compiler.compile(vcgCode);
if (result.isSuccess()) {
    webView.loadData(result.getOutput(), "text/html", "UTF-8");
} else {
    Log.e("VCG", result.getError());
}

// ── تحقق من صحة الكود
boolean valid = sdk.validate(code);

// ── تلوين الكود في المحرر
List<VcgHighlighter.Token> tokens = VcgHighlighter.tokenizeLine(line);

// ── الكلمات المحجوزة والدوال المدمجة
String[] keywords = sdk.getKeywords();
String[] builtins = sdk.getBuiltins();
```

## الثيمات المتاحة / Themes

| الثابت               | الاسم      | الوصف        |
|----------------------|------------|--------------|
| `THEME_OLIVE`        | olive      | أخضر زيتوني (افتراضي) |
| `THEME_MIDNIGHT`     | midnight   | أزرق ليلي    |
| `THEME_AMOLED`       | amoled     | أسود AMOLED  |
| `THEME_SAND`         | sand       | رملي ذهبي    |
| `THEME_WHITE`        | white      | فاتح نظيف    |

## المتطلبات / Requirements

- Java 8+ / Android minSdk 24+
- لا توجد تبعيات خارجية (zero dependencies)

## الترخيص / License

MIT — Syrian VCG Project 2026
