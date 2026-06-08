# المساهمة في لغة VCG (Contributing to VCG)

يسعدنا جداً رغبتك في تطوير **لغة VCG البرمجية**! مساهمتك تساعدنا في بناء لغة برمجة عربية قوية ومتاحة للجميع.

## 🛠️ إعداد بيئة التطوير (Development Setup)

### المتطلبات الأساسية:
- مترجم لغة C يدعم معيار C11 (مثل `gcc` أو `clang`).
- أداة `make` لبناء الكومبيلر.

### خطوات البناء والتشغيل:
1. قم بعمل Fork للمستودع ثم Clone محلياً:
   ```bash
   git clone https://github.com/your-username/vcg-lang.git
   cd vcg-lang
   ```
2. قم ببناء المترجم باستخدام ملف الـ Makefile:
   ```bash
   make
   ```
3. لتشغيل الاختبارات والتأكد من سلامة الكود:
   ```bash
   chmod +x tests/run_tests.sh
   ./tests/run_tests.sh
   ```

## 📝 إرشادات كتابة الكود (Code Style Guidelines)
- يرجى كتابة كود C نظيف ومرتب، مع الالتزام بالهيكلية الحالية في مجلد `compiler/src/` و `compiler/include/`.
- يرجى التعليق على الأكواد المعقدة باللغة العربية أو الإنجليزية لتسهيل المراجعة.
- تأكد من إضافة أمثلة تطبيقية للميزات الجديدة في مجلد `examples/`.

## 🚀 إرسال طلبات السحب (Pull Requests)
1. قم بإنشاء فرع جديد للميزة أو الإصلاح: `git checkout -b feature/amazing-feature`.
2. قم بعمل Commit لتغييراتك بوصف واضح.
3. قم بالدفع للفرع: `git push origin feature/amazing-feature`.
4. افتح Pull Request جديد واشرح التغييرات التي قمت بها بالتفصيل.