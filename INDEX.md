# محتويات هذه الحزمة / Contents

هذه كل الملفات التي أنشأها Claude أو عدّلها في هذا المحادثة، مرتّبة في مجلدين:

## 1) java-interpreter/
مفسِّر VCG **حقيقي** بلغة Java من الصفر (lexer → parser → AST → tree-walking
interpreter)، بدون أي HTML أو JavaScript في مسار التنفيذ.

```
java-interpreter/
├── README.md                     — شرح كامل للبنية والتشغيل
├── src/com/syrianvcg/vcg/        — كل الكود المصدري (24 ملف .java)
└── examples/                     — أمثلة .vcg جاهزة للتشغيل
    ├── hello.vcg                 — يستخدم show() و print(hi)
    ├── seal_demo.vcg             — يوضّح Seal()
    └── balanced_pattern_demo.vcg — يستخدم pattern Balanced + render
```

التشغيل (يتطلب JDK 17+):
```bash
cd java-interpreter
javac -d out -encoding UTF-8 src/com/syrianvcg/vcg/*.java
java -cp out com.syrianvcg.vcg.Main run examples/hello.vcg
```

## 2) balanced-pattern-html/
أداة **Balanced Pattern** مستقلة بـ HTML/CSS/JS خالصة (لا تحتاج Java أو أي
تثبيت) — افتح `balanced_pattern_tool.html` مباشرة في أي متصفح.

```
balanced-pattern-html/
└── balanced_pattern_tool.html
```

---
ملاحظة: بيئة التنفيذ التي استخدمها Claude لا تحتوي `javac` ولا وصول شبكة
لتثبيته، فلم يُجمَّع الكود فعلياً هنا. رجاءً جمِّعه محلياً للتأكد، وإن ظهر
أي خطأ تجميع أرسل رسالة الخطأ وسأصلحها.
