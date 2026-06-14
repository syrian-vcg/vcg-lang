<div align="center">

<img src="assets/icon.svg" width="100" height="100" alt="VCG Logo"/>

# Syrian Private Programming VCG

**لغة برمجة سورية  — مفتوحة المصدر**

[![Build](https://github.com/syrian-vcg/vcg-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/ci.yml)
[![Pages](https://github.com/syrian-vcg/vcg-lang/actions/workflows/pages.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/pages.yml)
[![APK](https://github.com/syrian-vcg/vcg-lang/actions/workflows/build-apk.yml/badge.svg)](https://github.com/syrian-vcg/vcg-lang/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-2.0.0-brightgreen)](https://github.com/syrian-vcg/vcg-lang/releases)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[🌐 الموقع](https://syrian-vcg.github.io/vcg-lang) · [📦 تحميل](https://github.com/syrian-vcg/vcg-lang/releases) · [📱 APK](https://github.com/syrian-vcg/vcg-lang/actions/workflows/build-apk.yml) · [📖 التوثيق](#documentation)

</div>

---

## ما هي لغة VCG؟

**VCG** هي لغة برمجة سورية مكتوبة من الصفر بـ C11، تُخرج HTML + JavaScript حقيقي.  
تدعم البرمجة كائنية التوجه، الدوال المتزامنة، الوحدات، التصنيفات، وأكثر من **70 كلمة محجوزة**.

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

---

## المميزات

| الميزة | التفاصيل |
|--------|---------|
| 🔧 **مترجم C11** | مترجم كامل: lexer → parser → AST → interpreter + codegen |
| 🌐 **مخرج HTML+JS** | يولّد صفحات ويب كاملة تعمل في المتصفح |
| 📱 **محرر موبايل** | تطبيق Android APK مع syntax highlighting |
| 🎯 **UI مدمج** | `h()`, `l()`, `btn()`, `img()`, `youtube()`, `facebook()`, `instagram()` |
| ⚡ **Reactive** | `$set()`, `$get()`, `watch()` |
| 🏗️ **OOP** | `class`, `extends`, `implements`, `interface` |
| 🔄 **Async** | `async func`, `await`, `promise{}`, `defer` |
| 🧪 **Testing** | `test{}`, `expect()`, `assert_eq()` |
| 📦 **Modules** | `module`, `export`, `from ... import` |
| 🔒 **Safety** | `safe{}`, `unsafe{}`, `guard...else{}` |

---

## تثبيت سريع

```bash
# استنساخ المستودع
git clone https://github.com/syrian-vcg/vcg-lang.git
cd vcg-lang

# بناء المترجم
make

# التحقق من الإصدار
./vcgc --version    # vcgc 2.0.0

# تشغيل مثال
./vcgc -r examples/basic/hello.vcg

# تحويل إلى HTML
./vcgc examples/advanced/ui_media.vcg -o output.html
```

---

## الكلمات المحجوزة (70+)

### الأساسية
```
let  const  func  return  if  else  while  for  in
repeat  break  continue  show  input  and  or  not
true  false  nil  match  when  try  catch  throw  assert
```

### OOP
```
class  extends  implements  interface  super  this  new  self  struct
```

### Modules
```
module  export  from  import  as  public
```

### Async / Memory
```
async  await  promise  defer
ref  ptr  alloc  free
```

### Types
```
type  enum  union  generic  typeof  sizeof
```

### Safety / Testing
```
safe  unsafe  guard
test  expect  mock  doc
```

### UI / Media / Social (فريدة لـ VCG!)
```
h  l  btn  url  key  img  video
youtube  facebook  instagram  xsocial
```

### Reactive / Channels
```
$set  $get  watch  send  recv
w  c  x  pipe
```

---

## المخرجات المدعومة

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

---

## الدوال المدمجة (50+)

### رياضيات
```
abs  floor  ceil  round  sqrt  sin  cos  tan  log  pow
gcd  lcm  fib  factorial  is_prime
PI  E  TAU  PHI
```

### نصوص
```
len  str  int  float  upper  lower  trim  split  join
includes  indexof  count  repeat  pad_start  pad_end
```

### مصفوفات
```
push  pop  sort  reverse  slice  flat  unique
sum  avg  first  last  chunk  zip  map  filter  reduce  find
```

### كائنات
```
keys  values  merge  has  del  entries
```

### أدوات
```
uuid  hash  copy  type_of
JSON_stringify  JSON_parse
file_read  file_write  file_exists
```

### اختبار
```
assert_eq  assert_ne  assert_true  assert_false
```

---

## البنية الكاملة

```
vcg-lang/
├── compiler/
│   ├── include/vcg.h          ← جميع الأنواع والرموز
│   └── src/
│       ├── lexer.c             ← المحلل اللغوي
│       ├── parser.c            ← المحلل النحوي
│       ├── ast.c               ← شجرة الترميز
│       ├── value.c             ← القيم والبيئات
│       ├── interpreter.c       ← المُفسِّر
│       ├── stdlib.c            ← المكتبة القياسية
│       ├── codegen.c           ← مولّد HTML+JS
│       └── main.c              ← واجهة سطر الأوامر
├── examples/
│   ├── basic/                  ← hello, variables, loops
│   └── advanced/               ← fibonacci, ui_media, v2_concepts...
├── editor/android/             ← محرر Android (APK)
├── tests/run_tests.sh          ← 10 اختبارات آلية
├── tools/                      ← Linguist + syntax highlighting
├── assets/                     ← الأيقونة والرسومات
├── .github/workflows/          ← CI/CD + APK + Pages
└── Makefile
```

---

## واجهة سطر الأوامر

```bash
vcgc file.vcg                   # تحويل إلى HTML (نفس الاسم)
vcgc file.vcg -o output.html    # تحديد اسم الملف
vcgc -r file.vcg                # تشغيل مباشر (interpreter)
vcgc --tokens file.vcg          # عرض الرموز
vcgc --ast file.vcg             # عرض شجرة AST
vcgc --version                  # إصدار المترجم
vcgc --help                     # مساعدة
```

---

## GitHub Actions

| Workflow | الوصف | المشغّل |
|----------|-------|---------|
| `ci.yml` | بناء + اختبارات + HTML | كل push |
| `pages.yml` | نشر GitHub Pages | main branch |
| `build-apk.yml` | بناء Android APK | كل push |

---

## تثبيت APK

1. اذهب إلى **Actions → Build VCG Editor APK**
2. اختر آخر run ناجح
3. حمّل من **Artifacts**: `vcg-editor-debug-apk`

---

## المساهمة

```bash
git fork https://github.com/syrian-vcg/vcg-lang
git checkout -b feature/my-feature
# ... التعديلات ...
git commit -m "feat: add my feature"
git push origin feature/my-feature
# أنشئ Pull Request
```

---

## الرخصة

MIT License — حرية الاستخدام والتعديل والتوزيع.

---

<div align="center">

**Syrian Private Programming VCG v2.0.0**  
Made with ❤️ in Syria · 2026-06-06

[⭐ Star on GitHub](https://github.com/syrian-vcg/vcg-lang)

</div>
