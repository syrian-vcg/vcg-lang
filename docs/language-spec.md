# VCG Language Specification v2.0

## 1. Comments
```vcg
# Single line comment
// Also single line
/* Multi-line
   comment */
```

## 2. Variables
```vcg
let x = 10           # mutable
const PI = 3.14159   # immutable
w log = "event"      # write-only (audit log)
```

## 3. Data Types
```vcg
let i = 42           # int
let f = 3.14         # float
let s = "hello"      # string
let b = true         # bool (true/false)
let n = nil          # nil
let a = [1, 2, 3]   # array
let o = { x: 1 }    # struct/object
```

## 4. Operators
```vcg
# Arithmetic
+  -  *  /  %  **     # power

# Comparison
==  !=  <  >  <=  >=

# Logical
and  or  not

# Assignment
=  +=  -=  *=  /=

# Range
1..10                  # [1,2,...,9]

# Pipeline
val |> func1 |> func2  # func2(func1(val))

# Bitwise
|  &  <<  >>
```

## 5. Control Flow
```vcg
# if/else
if x > 0 {
    show("positive")
} else if x < 0 {
    show("negative")
} else {
    show("zero")
}

# Ternary
let label = x > 0 ? "pos" : "non-pos"

# while
while x > 0 { x -= 1 }

# for-in
for item in [1, 2, 3] { show(item) }
for i in 0..10 { show(i) }

# repeat
repeat 5 { show("hello") }

# match/when
match status {
    when 0 -> show("off")
    when 1 -> show("on")
}
```

## 6. Functions
```vcg
func add(a, b) {
    return a + b
}

# Lambda
let double = \x -> x * 2

# Async
async func load(url) {
    let data = await fetch(url)
    return data
}

# Variadic
func sum_all(..nums) {
    return reduce(add, 0, nums)
}
```

## 7. Classes (OOP)
```vcg
class Animal {
    func init(name, sound) {
        self.name = name
        self.sound = sound
    }
    func speak() {
        return self.name + ": " + self.sound
    }
}

class Dog extends Animal {
    func init(name) {
        self.name  = name
        self.sound = "Woof"
    }
    func fetch(item) {
        return self.name + " fetches " + item
    }
}

let dog = new Dog("Rex")
show(dog.speak())      # Rex: Woof
show(dog.fetch("ball"))
```

## 8. Modules
```vcg
module Math2 {
    func square(x) { return x * x }
    let PI2 = 3.14159
}

show(Math2.PI2)
show(Math2.square(5))

from Math2 import square
show(square(9))
```

## 9. Enums
```vcg
enum Color { Red, Green, Blue }
enum Status { Pending, Active, Done }

show(Color.Red)    # 0
show(Status.Done)  # 2
```

## 10. Error Handling
```vcg
try {
    throw "something went wrong"
} catch err {
    show("caught:", err)
}

# Safe block
safe {
    # If error occurs, continues silently
    risky_operation()
}

# Guard
guard x > 0 else {
    show("x must be positive")
    return
}
```

## 11. Reactive Store
```vcg
watch("score", func(v) {
    show("score changed to", v)
})

$set("score", 100)
$set("score", 200)    # triggers watcher
show($get("score"))   # 200
```

## 13. مكتبة v2.1 الجديدة (إضافات يونيو 2026)

### معالجة النصوص
```vcg
split("a,b,c", ",")              # ["a","b","c"]
replace("hi world", "world", "VCG")   # "hi VCG"
trim("   spaced   ")              # "spaced"
upper("hello")                    # "HELLO"
lower("WORLD")                    # "world"
starts_with("syrian-vcg", "syrian")   # true
ends_with("file.vcg", ".vcg")     # true
```

### مصفوفات: تعديل وتقطيع
```vcg
let arr = [3, 1, 4, 1, 5]
sort(arr)                 # [1,1,3,4,5] — ترتيب تصاعدي افتراضي
sort(arr, \a,b -> b - a)  # ترتيب مخصّص عبر دالة مقارنة
reverse(arr)              # عكس الترتيب (يدعم النصوص أيضاً)
push(arr, 100)             # إضافة عنصر للنهاية (يعدّل arr مباشرة)
pop(arr)                   # حذف وإرجاع آخر عنصر
shift(arr)                 # حذف وإرجاع أول عنصر
unshift(arr, 0)            # إضافة عنصر للبداية
slice(arr, 1, 3)           # تقطيع جزء من المصفوفة أو النص (مثل Python)
```

### Base64
```vcg
let enc = base64_encode("نص عربي UTF-8")
show(enc)
show(base64_decode(enc))   # يرجع النص الأصلي بالكامل
```

### الألوان والستايل والديزاين (Color / Style / Design)
```vcg
# color() — يقبل hex، اسم لون جاهز، أو r,g,b[,a]
let c1 = color("#FF6B6B")        # → {r,g,b,a,hex,rgb,rgba}
let c2 = color("vcg_olive")       # ألوان اسمية جاهزة (vcg_olive, vcg_dark, vcg_accent, red, blue, ...)
let c3 = color(20, 200, 100, 0.8) # rgba مباشرة

show(c1.hex)    # "#FF6B6B"
show(c1.rgba)   # "rgba(255,107,107,1.00)"

# style()/design() — يلصقان "وسماً" على struct، ليُستخدما مع المتجر التفاعلي $set/$get
$set("style", style({
    bg:     color("vcg_dark").hex,
    accent: color("vcg_accent").hex,
    radius: 12
}))

$set("design", design({
    font: "Cairo",
    spacing: 8
}))

show($get("style"))
show($get("design"))

# store_zip() — يرجع كل محتوى المتجر التفاعلي كمصفوفة [مفتاح, قيمة]
# (فكرة "$get .zip()" — تجميع كل عناصر $set في قائمة واحدة دفعة واحدة)
show(store_zip())   # [["style", {...}], ["design", {...}]]
```

📄 ملف جاهز للاستخدام: `examples/design/colors.vcg` — يحتوي لوحة ألوان VCG كاملة
(زيتوني/داكن/تمييز + ألوان عامة) ودوال `lighten()`/`darken()`/`mix()` و `default_style()`.

### مكوّنات واجهة (UI): text / text_s / btn / ui
```vcg
text("نص عادي")                              # struct {content, style}
text_s("نص بستايل", style({ color: "#fff", size: 14 }))  # نص مع ستايل مخصّص (s = styled)
btn("اضغط هنا", "on_click_name", style({ bg: "#4DA65A" }))  # زر: label, onclick, style

# ui(...) يجمع أي عدد من العناصر في شجرة واحدة، تُسجَّل عبر $set:
$set("ui", ui(
    text_s("العنوان", style({ size: 22 })),
    btn("اضغط هنا", "on_start")
))

let page = $get("ui")
for el in page.children {
    if kind(el) == "Button" { show(el.label) }   # kind() يرجع الاسم الموسوم: Text/Button/UI/Style/...
}
```
📄 مثال متكامل: `examples/design/ui_demo.vcg` — صفحة كاملة (عنوان + وصف + زرّين) مبنية
بألوان VCG عبر `color()`، ومسجَّلة كاملة بالمتجر التفاعلي عبر `$set("ui", ...)` و `$set("style", ...)`.

### kind(x)
يشبه `type_of(x)` لكنه يرجع الاسم الموسوم للـ struct بدل "struct" العامة، مفيد للتمييز
بين عناصر الواجهة المختلفة (`Text`, `Button`, `UI`, `Style`, `Design`, `Color`).

### استدعاء متسلسل: .color() على عناصر الواجهة
عناصر `text()` / `text_s()` / `btn()` تحمل method موثوق `.color(...)` يُحدّث الستايل
مباشرة ويُرجع العنصر نفسه (chaining حقيقي، مُختبَر، يحفظ التغييرات فعلاً):
```vcg
let b1 = btn("اضغط هنا").color("#FF6B6B")        # {r,g,b,a,hex,rgb,rgba} → بيُحدّث style.bg
let t1 = text("نص").color("vcg_olive")           # ألوان VCG الاسمية الجاهزة → style.color
let b2 = btn("ثاني").color(20, 200, 100, 0.8)     # rgba مباشرة

show(b1.style.bg)   # "#FF6B6B"
show(t1.style.color)# "#3D4A2F"
```

### نظام إعدادات التطبيق/الصفحة الكامل: settings_new()
```vcg
let settings = settings_new()

settings.name("محرر VCG").package("com.syrianvcg.editor").version("2.1.0").icon("icons/icon.png")
settings.background.color("#1A1D14")      # أو "vcg_olive" أو (r,g,b[,a])

show(settings.name())                      # قراءة بدون معاملات
show(settings.background.value.hex)

$set("settings", settings.snapshot())      # تسجيل لقطة كاملة بالمتجر التفاعلي
let saved = $get("settings")               # { name, package, version, icon, background }
```
📄 مثال كامل وجاهز: `examples/design/setting_app/setting.app.vcg` (+ مجلد `icons/` لوضع
أيقونة التطبيق بصيغة png/jpg/svg) — يغطي: الاسم، اسم الحزمة، رقم الإصدار، الأيقونة،
ولون الخلفية، مع تسجيل كل شيء عبر `$set`/`$get`.




## 14. Channels
```vcg
c tasks

send(tasks, "task 1")
send(tasks, "task 2")

let t = recv(tasks)
while t != nil {
    show("processing:", t)
    t = recv(tasks)
}
```

## 15. Testing
```vcg
test "math works" {
    assert_eq(2 + 2, 4)
    assert_true(10 > 5)
    assert_false(1 == 2)
}

test "strings" {
    assert_eq(len("hello"), 5)
    assert_ne("a", "b")
}
```
