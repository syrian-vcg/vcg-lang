# VCG — Native Java Interpreter (مفسِّر VCG بلغة Java من الصفر)

مفسِّر **حقيقي** ومُجمَّع (تجميع وتنفيذ) للغة **VCG** مكتوب بالكامل بلغة Java —
لا يعتمد على HTML أو JavaScript في أي مرحلة من التنفيذ. الكود VCG يُحلَّل إلى
رموز (Lexer) ثم إلى شجرة AST (Parser) ثم يُنفَّذ مباشرة على JVM (Interpreter).

## البنية / Architecture

```
src/com/syrianvcg/vcg/
├── Token.java / TokenType.java     — تمثيل الرموز
├── Lexer.java                      — المحلّل اللفظي
├── Node.java                       — تعريفات AST (تعبيرات وعبارات)
├── Parser.java                     — المحلّل النحوي (recursive descent)
├── Environment.java                — جدول المتغيّرات (نطاقات متداخلة)
├── Interpreter.java                — المُنفِّذ الأساسي (tree-walking)
├── Ops.java                        — العمليات الحسابية/المقارنة
├── VcgStruct / VcgFunction /
│   VcgClass / VcgInstance /
│   VcgCallable / VcgRange /
│   VcgChannel                      — أنواع القيم في وقت التنفيذ
├── Builtins.java                   — الدوال المدمجة (typeof, len, split...)
├── ColorLib.java                   — color()/tone()/mix_color()
├── UiLib.java                      — text()/btn()/ui()/style()/design()
├── SettingsLib.java                — settings_new()
├── BalancedPattern.java            — pattern Balanced { ... } / render
├── OutputSink.java                 — تجميع المخرجات (show/print/html)
├── HtmlReport.java                 — تصدير تقرير HTML ثابت (بدون أي مفسِّر JS)
├── Main.java                       — نقطة الدخول CLI
└── Repl.java                       — واجهة تفاعلية (REPL)
```

## البناء والتشغيل / Build & Run

يحتاج **JDK 17 أو أحدث** (تم تطويره وفحصه يدوياً على JDK 21).

```bash
# تجميع
javac -d out -encoding UTF-8 src/com/syrianvcg/vcg/*.java

# تشغيل ملف .vcg
java -cp out com.syrianvcg.vcg.Main run examples/hello.vcg

# تشغيل + تصدير تقرير HTML ثابت (بدون أي JS مفسِّر بداخله)
java -cp out com.syrianvcg.vcg.Main run examples/hello.vcg --html out.html

# واجهة تفاعلية REPL
java -cp out com.syrianvcg.vcg.Main repl
```

اختياري: تحزيم كـ jar قابل للتشغيل:
```bash
jar --create --file vcgc.jar --main-class com.syrianvcg.vcg.Main -C out .
java -jar vcgc.jar run examples/hello.vcg
```

## ما الجديد بالمقارنة مع نسخة SDK القديمة؟

النسخة القديمة (`sdk/.../VcgInterpreter.java`) كانت في الواقع تُولِّد ملف HTML
يحتوي مفسِّراً مكتوباً بـ **JavaScript** كنص داخل سلسلة Java، ثم يُشغَّل ذلك
الكود JS داخل متصفح/WebView. هذا الإصدار مختلف جذرياً:

- **لا HTML ولا JavaScript** في مسار التنفيذ — تحليل وتنفيذ Java بالكامل.
- محلل لفظي ونحوي ومُنفِّذ AST حقيقي (lexer → parser → tree-walking interpreter).
- يدعم تقريباً كل ما ورد في `docs/language-spec.md`: المتغيرات، الدوال،
  الأصناف (OOP)، الوحدات (modules)، التعدادات (enums)، try/catch، match،
  guard، safe، المصفوفات والكائنات، lambda، pipeline `|>`، المدى `..`،
  القنوات (channels)، الألوان/الستايل/عناصر الواجهة، `settings_new()`،
  والمتجر التفاعلي `$set`/`$get`/`watch`.
- ميزتان جديدتان بالطلب:
  - **`print(...)`** — مرادف لـ `show(...)`، يطبع كل المعطيات.
  - **`Seal(...)` / `Seal()`** — يطبع إشعار حقوق النشر (الافتراضي
    `© All rights reserved.` أو نصاً مخصصاً)، ثم **يختم** البرنامج: أي
    عبارة بعده تُسبِّب خطأ تنفيذ صريحاً يوضح أن البرنامج مختوم.

## أمثلة / Examples

- `examples/hello.vcg` — يستخدم `show`, `print(hi)`.
- `examples/seal_demo.vcg` — يوضّح `Seal()` ووقف التنفيذ بعدها.
- `examples/balanced_pattern_demo.vcg` — يستخدم `pattern Balanced { ... }`
  و`render` (تُصدِّر `<style>` عبر `out.html(...)`؛ تُجمَّع هذه المخرجات
  في تقرير HTML ثابت عند استخدام `--html`).

## أداة Balanced Pattern المستقلة (HTML فقط)

ملف منفصل تماماً: `balanced_pattern_tool.html` — أداة تفاعلية بـ HTML/CSS/JS
خالصة (لا تحتاج Java أو أي تثبيت) لتجربة Balanced Pattern: غيّر اللون
الأساسي ووحدة القياس ونصف القطر، وشاهد مقياس التباعد والدرجات اللونية
ومعاينة `bp-section`/`bp-card`/`bp-btn` تتحدّث فوراً، مع كود VCG/CSS جاهز
للنسخ.

## ملاحظة عن بيئة الفحص

تم تطوير هذا الكود ومراجعته يدوياً بعناية، لكن بيئة التنفيذ الحالية لا تحوي
`javac` (فقط JRE) ولا يوجد وصول للشبكة لتثبيت JDK، فلم يتمكن Claude من
تجميعه وتشغيله هنا للتأكد النهائي. يُرجى تجميعه محلياً للتحقق، وإن وُجد أي
خطأ تجميع، أرسل رسالة الخطأ وسأصلحها فوراً.
