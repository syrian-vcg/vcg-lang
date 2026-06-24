# ميزات بناء التطبيقات في VCG — توثيق شامل

## نظرة عامة

أُضيفت إلى **VCG v2.0** حزمة كاملة لبناء تطبيقات Android وتصديرها بثلاثة أشكال:
**APK** + **ZIP** (للمشروع كاملاً) + **PDF** (وثائق التطبيق).

---

## 1. إعدادات التطبيق

توضع هذه الأوامر في **أول ملف `.vcg`** في المشروع.

```vcg
$set.name_app("اسم التطبيق")
$set.app.package("com.example.myapp")
$set.app.version("1.0.0")
$set.get.icon("assets/icon.png")
```

| الأمر | الوصف | مثال |
|-------|-------|------|
| `$set.name_app(n)` | اسم التطبيق الظاهر للمستخدم | `$set.name_app("متجري")` |
| `$set.app.package(p)` | معرّف الحزمة (Package ID) | `$set.app.package("com.store.app")` |
| `$set.app.version(v)` | رقم الإصدار | `$set.app.version("2.1.0")` |
| `$set.get.icon(path)` | مسار أيقونة التطبيق | `$set.get.icon("assets/icon.png")` |

**قراءة القيم المُخزَّنة:**
```vcg
show($set.name_app())     # → "متجري"
show($set.app.version())  # → "2.1.0"
```

---

## 2. تخزين البيانات — `data_vcg` و `get.data`

### `data_vcg name = value`

يُعرِّف بيانات مُخزَّنة في ذاكرة التطبيق الدائمة (قابلة للقراءة من أي ملف).

```vcg
data_vcg products = [
    { id: 1, name: "قلم", price: 5.0 },
    { id: 2, name: "كتاب", price: 25.0 }
]

data_vcg app_settings = {
    theme: "dark",
    lang: "ar"
}
```

### `get.data(ref)`

يقرأ البيانات المُخزَّنة مسبقاً.

```vcg
let all_products = get.data(products)
let settings     = get.data(app_settings)

show(len(all_products))  # → 2
show(settings.theme)     # → "dark"
```

**ملاحظة:** يمكن تمرير المرجع مباشرةً أو باسمه كنص:
```vcg
let p1 = get.data(products)       # مرجع مباشر
let p2 = get.data("products")     # بالاسم
```

---

## 3. إنشاء ملفات مضغوطة — `make_zip`

```vcg
make_zip("اسم_الملف.zip", [
    "data/users.json",
    "assets/logo.png",
    "main.vcg"
])

# أو عبر كائن (تُؤخَذ المفاتيح كأسماء ملفات)
make_zip("export.zip", {
    "users.json": users_data,
    "config.json": config
})
```

يُعرض في المحرر معاينة بصرية للملفات المضمَّنة.

---

## 4. إنشاء ملفات PDF — `make_pdf`

```vcg
make_pdf("اسم_التقرير", "محتوى التقرير هنا...")

# مثال عملي
let report = "تقرير المبيعات\n" + "إجمالي: " + total
make_pdf("تقرير_2026", report)
```

---

## 5. تصدير التطبيق — `export(to_set)`

الأمر الرئيسي لبدء عملية البناء. يُوضع عادةً في **آخر الملف**.

```vcg
export(to_set)
```

**ما يحدث عند التنفيذ:**
1. يعرض المحرر بطاقة مرئية بمعلومات التطبيق
2. يُرسل إشعاراً لـ Android Bridge لبدء البناء الحقيقي
3. تُنشأ ثلاثة ملفات:
   - `AppName-1.0.0.apk`
   - `AppName-1.0.0-project.zip`
   - `AppName-1.0.0-docs.pdf`

---

## 6. ملف Generate_Stack.apk.yml

هذا الملف هو **نقطة التحكم المركزية** في عملية البناء.

```yaml
app:
  name:    "اسم تطبيقي"
  package: "com.example.myapp"
  version: "1.0.0"
  icon:    "assets/icon.png"

sources:
  entry: "main.vcg"
  files:
    - "main.vcg"
    - "data/store.vcg"

outputs:
  apk: true
  zip: true
  pdf: true

android:
  min_sdk:    21
  target_sdk: 34

build:
  type:    "debug"   # debug أو release
  verbose: true
```

### آلية الاكتشاف التلقائي

عند **فتح وتشغيل** ملف `Generate_Stack.apk.yml` في VCG Editor:
- يكتشف المحرر الملف تلقائياً (عبر `VcgAppBuilder.isStackFile()`)
- **يُطلق شاشة البناء `VcgBuildActivity`** بدلاً من شاشة الإخراج العادية
- تظهر شريط تقدم حي + سجل البناء + أزرار المشاركة

---

## 7. مثال مشروع متكامل

هيكل المجلد:
```
my_app/
├── Generate_Stack.apk.yml   ← نقطة التحكم
├── main.vcg                 ← الملف الرئيسي
├── data/
│   └── data_store.vcg       ← البيانات
└── assets/
    └── icon.png
```

**main.vcg:**
```vcg
# إعدادات التطبيق
$set.name_app("تطبيقي")
$set.app.package("com.me.myapp")
$set.app.version("1.0.0")
$set.get.icon("assets/icon.png")

# البيانات
data_vcg users = [
    { name: "أحمد", score: 100 },
    { name: "سامر", score: 85 }
]

# الواجهة
h(1, $set.name_app())
let all = get.data(users)
for u in all {
    l(u.name + " — " + u.score + " نقطة")
}

# تصدير
export(to_set)
```

---

## 8. الملفات المُضافة

| الملف | الوصف |
|-------|-------|
| `VcgAppBuilder.java` | محرك البناء الرئيسي (APK+ZIP+PDF) |
| `VcgBuildActivity.java` | شاشة التقدم التفاعلية |
| `VcgInterpreterPatch.java` | مرجع للتوثيق والتكامل |
| `Generate_Stack.apk.yml` | قالب ملف البناء |
| `examples/app_builder/` | أمثلة عملية |

---

## ملاحظات تقنية

- **APK المُنشأ** هو قالب وهمي لأغراض التطوير. للبناء الحقيقي يتطلب بيئة Gradle.
- **ZIP** يحتوي على كامل ملفات المشروع + manifest + `Generate_Stack.apk.yml` مُعدَّل.
- **PDF** يُنشأ باستخدام كاتب PDF خفيف مدمج (بدون مكتبات خارجية).
- `data_vcg` يُخزِّن البيانات في `_datastore` و `_store['data:name']` للوصول من `$get`.
