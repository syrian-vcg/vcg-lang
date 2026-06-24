<div align="center">

<img src="assets/icon.svg" width="110" height="110" alt="VCG Logo"/>

# Syrian Private Programming — VCG

**لغة برمجة سورية حقيقية، مفتوحة المصدر، تُترجم إلى HTML + JavaScript**

[![Build](https://github.com/syrian-vcg/vcg-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/ci.yml)
[![Pages](https://github.com/syrian-vcg/vcg-lang/actions/workflows/pages.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/pages.yml)
[![APK](https://github.com/syrian-vcg/vcg-lang/actions/workflows/build-apk.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-0.2.1-brightgreen)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Made in Syria](https://img.shields.io/badge/made%20in-%F0%9F%87%B8%F0%9F%87%BE%20Syria-green)](#)

[🌐 الموقع](https://syrian-vcg.github.io/vcg-lang) ·
[📦 الإصدارات](https://github.com/syrian-vcg/vcg-lang/releases) ·
[📱 تطبيق APK](#-تطبيق-المحرر-apk) ·
[📖 توثيق اللغة](docs/language-spec.md) ·
[🧩 أمثلة](examples/)

</div>

---

## 📋 جدول المحتويات

- [نظرة عامة](#-نظرة-عامة)
- [مثال سريع](#-مثال-سريع)
- [المميزات](#-المميزات)
- [التثبيت](#-التثبيت)
- [واجهة سطر الأوامر](#-واجهة-سطر-الأوامر)
- [الكلمات المحجوزة](#-الكلمات-المحجوزة-70)
- [الدوال المدمجة](#-الدوال-المدمجة-50)
- [مكونات الواجهة والوسائط](#-مكونات-الواجهة-والوسائط)
- [بنية المشروع](#-بنية-المشروع)
- [تطبيق المحرر APK](#-تطبيق-المحرر-apk)
- [GitHub Actions](#-github-actions)
- [خارطة الطريق](#-خارطة-الطريق)
- [المساهمة](#-المساهمة)
- [الرخصة](#-الرخصة)

---

## 🧭 نظرة عامة

**VCG** لغة برمجة مكتوبة بالكامل من الصفر بلغة **C11**، تحتوي على مترجم حقيقي
(lexer → parser → AST → interpreter/codegen) يُخرج صفحات **HTML + JavaScript**
قابلة للتشغيل مباشرة في أي متصفح، أو يمكن تشغيلها مباشرة عبر مفسّر مدمج.

تدعم اللغة البرمجة كائنية التوجه، الوحدات (modules)، الدوال المتزامنة (async)،
أنماط المطابقة (pattern matching)، التعامل الآمن مع الذاكرة (`safe`/`unsafe`)،
وأكثر من **70 كلمة محجوزة** و **50 دالة مدمجة**، إضافة إلى مجموعة عناصر واجهة
وميديا ووسائط اجتماعية جاهزة للاستخدام مباشرة من الكود.

| | |
|---|---|
| 🔧 **المترجم** | C11 كامل — lexer, parser, AST, interpreter, codegen |
| 🌐 **المخرجات** | HTML5 + JavaScript قابل للتشغيل في أي متصفح |
| 📱 **المحرر** | تطبيق Android (APK) بتلوين صياغي وكلمات سريعة |
| 📦 **SDK** | حزمة Java/Android جاهزة للدمج في تطبيقات أخرى |
| 🧪 **الاختبارات** | مجموعة اختبارات آلية + GitHub Actions CI/CD |

---

## ⚡ مثال سريع

```vcg
# مرحبا بالعالم — VCG v2.0
let name = "Syria"
show("مرحباً يا", name)

class Greeter {
    func init(lang) {
        self.lang = lang
    }
    func hello(who) {
        return "Hello " + who + " from " + self.lang
    }
}

let g = new Greeter("VCG")
show(g.hello("World"))

h(1, "لغة VCG البرمجية")
l("مترجم C11 حقيقي", "مخرج HTML+JS", "محرر موبايل APK")
facebook("https://facebook.com/syrianvcg", "تابعنا")
youtube("dQw4w9WgXcQ")
```

> 🔍 أمثلة إضافية أساسية ومتقدمة متوفرة في مجلد [`examples/`](examples/).

---

## ✨ المميزات

| الميزة | التفاصيل |
|--------|---------|
| 🔧 **مترجم C11** | مترجم كامل: lexer → parser → AST → interpreter + codegen |
| 🌐 **مخرج HTML+JS** | يولّد صفحات ويب كاملة تعمل في المتصفح بدون اعتماديات |
| 📱 **محرر موبايل** | تطبيق Android APK مع syntax highlighting وكلمات محجوزة سريعة |
| 🎯 **UI مدمج** | `h()`, `l()`, `btn()`, `img()`, `youtube()`, `facebook()`, `instagram()` |
| ⚡ **Reactive** | `$set()`, `$get()`, `watch()` |
| 🏗️ **OOP** | `class`, `extends`, `implements`, `interface` |
| 🔄 **Async** | `async func`, `await`, `promise{}`, `defer` |
| 🧪 **Testing** | `test{}`, `expect()`, `assert_eq()` |
| 📦 **Modules** | `module`, `export`, `from ... import` |
| 🔒 **Safety** | `safe{}`, `unsafe{}`, `guard...else{}` |

---

## 🚀 التثبيت

### المتطلبات
- مترجم C يدعم C11 (GCC أو Clang)
- `make`
- (اختياري) Android SDK لبناء تطبيق المحرر

### بناء من المصدر

```bash
# استنساخ المستودع
git clone https://github.com/syrian-vcg/vcg-lang.git
cd vcg-lang

# بناء المترجم
make

# التحقق من الإصدار
./vcgc --version    # vcgc 0.2.1

# تشغيل مثال
./vcgc -r examples/basic/hello.vcg

# تحويل إلى HTML
./vcgc examples/advanced/ui_media.vcg -o output.html
```

---

## 💻 واجهة سطر الأوامر

```bash
vcgc file.vcg                   # تحويل إلى HTML (نفس الاسم)
vcgc file.vcg -o output.html    # تحديد اسم الملف
vcgc -r file.vcg                # تشغيل مباشر (interpreter)
vcgc --tokens file.vcg          # عرض الرموز (tokens)
vcgc --ast file.vcg             # عرض شجرة AST
vcgc --version                  # إصدار المترجم
vcgc --help                     # المساعدة
```

---

## 🔑 الكلمات المحجوزة (70+)

<details open>
<summary><b>الأساسية</b></summary>

```
let  const  func  return  if  else  while  for  in
repeat  break  continue  show  input  and  or  not
true  false  nil  match  when  try  catch  throw  assert
```
</details>

<details>
<summary><b>OOP — البرمجة كائنية التوجه</b></summary>

```
class  extends  implements  interface  super  this  new  self  struct
```
</details>

<details>
<summary><b>Modules — الوحدات</b></summary>

```
module  export  from  import  as  public
```
</details>

<details>
<summary><b>Async / Memory — التزامن والذاكرة</b></summary>

```
async  await  promise  defer
ref  ptr  alloc  free
```
</details>

<details>
<summary><b>Types — الأنواع</b></summary>

```
type  enum  union  generic  typeof  sizeof
```
</details>

<details>
<summary><b>Safety / Testing — الأمان والاختبار</b></summary>

```
safe  unsafe  guard
test  expect  mock  doc
```
</details>

<details>
<summary><b>UI / Media / Social — فريدة لـ VCG</b></summary>

```
h  l  btn  url  key  img  video
youtube  facebook  instagram  xsocial
```
</details>

<details>
<summary><b>Reactive / Channels — التفاعلية والقنوات</b></summary>

```
$set  $get  watch  send  recv
w  c  x  pipe
```
</details>

📚 التفاصيل الكاملة والنحو الدقيق لكل كلمة موجودة في [`docs/language-spec.md`](docs/language-spec.md).

---

## 🧰 الدوال المدمجة (50+)

| التصنيف | الدوال |
|---------|--------|
| **رياضيات** | `abs` `floor` `ceil` `round` `sqrt` `sin` `cos` `tan` `log` `pow` `gcd` `lcm` `fib` `factorial` `is_prime` · ثوابت: `PI` `E` `TAU` `PHI` |
| **نصوص** | `len` `str` `int` `float` `upper` `lower` `trim` `split` `join` `includes` `indexof` `count` `repeat` `pad_start` `pad_end` |
| **مصفوفات** | `push` `pop` `sort` `reverse` `slice` `flat` `unique` `sum` `avg` `first` `last` `chunk` `zip` `map` `filter` `reduce` `find` |
| **كائنات** | `keys` `values` `merge` `has` `del` `entries` |
| **أدوات** | `uuid` `hash` `copy` `type_of` `JSON_stringify` `JSON_parse` `file_read` `file_write` `file_exists` |
| **اختبار** | `assert_eq` `assert_ne` `assert_true` `assert_false` |

---

## 🎨 مكونات الواجهة والوسائط

```vcg
# UI / Layout
h(1, "العنوان الرئيسي")
h(2, "عنوان فرعي")
l("بند أول", "بند ثاني", "بند ثالث")
btn("اضغط هنا", "alert('مرحبا!')")
key("vcgc -r file.vcg")
url("https://github.com", "GitHub")

# Media
img("photo.jpg", "وصف الصورة", "300px")
video("clip.mp4", "100%")
youtube("dQw4w9WgXcQ")

# Social Media
facebook("https://fb.com/page", "فيسبوك")
instagram("@account", "انستغرام")
xsocial("@handle", "X")
```

📖 المرجع الكامل لكل المكونات في [`docs/ui-reference.md`](docs/ui-reference.md).

---

## 🗂️ بنية المشروع

```
vcg-lang/
├── compiler/
│   ├── include/vcg.h          ← جميع الأنواع والرموز
│   └── src/
│       ├── lexer.c             ← المحلل اللغوي
│       ├── parser.c            ← المحلل النحوي
│       ├── ast.c                ← شجرة الترميز
│       ├── value.c              ← القيم والبيئات
│       ├── interpreter.c       ← المُفسِّر
│       ├── stdlib.c             ← المكتبة القياسية
│       ├── codegen.c            ← مولّد HTML+JS
│       └── main.c               ← واجهة سطر الأوامر
├── sdk/                        ← حزمة Java/Android SDK
├── editor/android/             ← محرر Android (APK)
├── examples/
│   ├── basic/                   ← hello, variables, loops, functions
│   ├── advanced/                ← fibonacci, OOP, modules, error handling...
│   └── design/                  ← أمثلة واجهات وألوان
├── tests/run_tests.sh          ← اختبارات آلية
├── tools/                       ← Linguist + syntax highlighting (TextMate)
├── docs/                        ← مرجع اللغة والواجهة
├── assets/                      ← الأيقونة والرسومات
├── .github/workflows/           ← CI/CD + APK + Pages
└── Makefile
```

---

## 📱 تطبيق المحرر APK

1. اذهب إلى تبويب **Actions → Build VCG Editor APK**
2. اختر آخر تشغيل (run) ناجح
3. نزّل الملف من **Artifacts**: `vcg-editor-debug-apk`

التطبيق يتضمّن: تلوين صياغي كامل لكلمات VCG، لوحة مفاتيح سريعة للكلمات
المحجوزة، وأمثلة جاهزة للتجربة المباشرة على الهاتف.

---

## ⚙️ GitHub Actions

| Workflow | الوصف | المُشغِّل |
|----------|-------|----------|
| `ci.yml` | بناء المترجم + الاختبارات + توليد HTML | كل push |
| `pages.yml` | نشر الموقع على GitHub Pages | دفعات (push) على main |
| `build-apk.yml` | بناء تطبيق محرر Android (APK) | كل push |
| `link-check.yml` | فحص الروابط في التوثيق | حسب الجدولة |

---

## 🗺️ خارطة الطريق

سجل كامل بكل الإضافات والتغييرات لكل إصدار متوفر في [`CHANGELOG.md`](CHANGELOG.md)،
ويشمل آخر الإضافات مثل `music`, `loading`, `bar`, `edges`, `impact`،
ودعم `firebase`, `admob`, `pdf` وغيرها.

---

## 🤝 المساهمة

المساهمات مرحَّب بها دائماً! راجع [`CONTRIBUTING.md`](CONTRIBUTING.md) للتفاصيل الكاملة.

```bash
git clone https://github.com/syrian-vcg/vcg-lang.git
cd vcg-lang
git checkout -b feature/my-feature
# ... التعديلات ...
git commit -m "feat: add my feature"
git push origin feature/my-feature
# أنشئ Pull Request
```

عند إيجاد مشكلة أو طلب ميزة جديدة، استخدم قوالب **Issues** الجاهزة في
[`.github/ISSUE_TEMPLATE`](.github/ISSUE_TEMPLATE).

---

## 📄 الرخصة

هذا المشروع مرخّص بموجب **رخصة MIT** — حرية الاستخدام والتعديل والتوزيع.
راجع ملف [`LICENSE`](LICENSE) للتفاصيل الكاملة.

---

<div align="center">

**Syrian Private Programming — VCG v0.2.1**
Made with ❤️ in Syria

[⭐ ضع نجمة على GitHub](https://github.com/syrian-vcg/vcg-lang) ·
[🐞 أبلغ عن مشكلة](https://github.com/syrian-vcg/vcg-lang/issues) ·
[💬 ناقش الفكرة](https://github.com/syrian-vcg/vcg-lang/discussions)

</div>
