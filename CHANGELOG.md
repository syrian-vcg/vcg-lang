## [2.0.0] - Android Editor Sync — 2026-06-06

### Changed
- VCG Editor APK bumped to v2.0.0 (versionCode 2)
- VcgInterpreter.java (in-app JS runtime) fully rewritten to match
  compiler v2.0: class/extends/new/self, enum, module, async/await,
  safe/guard/test, map/filter/reduce/find, $set/$get/watch, channels,
  all UI/social keywords (h, l, btn, url, key, img, video, youtube,
  facebook, instagram, xsocial)
- Syntax highlighter (VcgCodeEditor.java) updated with all v2.0 keywords
- Quick keyboard (EditorActivity.java) reorganized with v2.0 keyword groups
- 10 sample files on first launch now demonstrate v2.0 features:
  hello, oop, enum, fibonacci, ui_demo, social, reactive, tests, safety
- App launcher icon replaced with official green+star VCG icon (all densities)

# Changelog — Syrian VCG Language

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-06-06 — "Full Edition"

### Added — New Language Concepts (70+ keywords)

#### OOP
- `class Name [extends Base] [implements I] { }` — كلاسات مع وراثة
- `extends`, `implements`, `interface`, `super`, `this`
- `new ClassName(args)` — ينادي `init()` تلقائياً
- `self` binding automatic inside methods

#### Modules
- `module Name { }` — نطاق مستقل
- `export decl` — تصدير للـ `__exports__`
- `from module import name, name2`

#### Async
- `async func name() { }` — دالة متزامنة
- `await expr` — انتظار Promise
- `promise { resolve(val) }` — كتلة Promise
- `defer stmt` — تنفيذ مؤجل

#### Types
- `type Name = expr` — اسم مستعار
- `enum Name { A, B, C }` — تصنيف محكم (frozen)
- `union Name = T1 | T2`
- `generic` (keyword reserved)

#### File I/O
- `file_read(path)`, `file_write(path, data)`, `file_append(path, data)`, `file_exists(path)`

#### Memory
- `ref expr`, `ptr`, `alloc(n)`, `free(ptr)`

#### Safety
- `safe { }` — كتلة آمنة تلتقط الأخطاء
- `unsafe { }` — كتلة غير آمنة
- `guard cond else { }` — حراسة مشروطة

#### Functional (now builtins)
- `map(fn, arr)`, `filter(pred, arr)`, `reduce(fn, init, arr)`, `find(pred, arr)`

#### Testing / Docs
- `test "name" { }` — اختبار مع [PASS]/[FAIL]
- `expect(val)` — كائن assertions
- `mock(target)` — وهمية للاختبار
- `doc "description"` — توثيق

#### Context / Pipeline
- `with expr as name { }` — كتلة سياق مع auto-close
- `|>` — معامل pipeline (أدنى أولوية)
- `\\ x -> expr` — lambda سهم

#### New stdlib functions (30+)
`repeat`, `pad_start`, `pad_end`, `includes`, `indexof`, `count`
`flat`, `unique`, `sum`, `avg`, `first`, `last`, `chunk`, `zip`
`merge`, `has`, `del`, `entries`
`gcd`, `lcm`, `fib`, `factorial`, `is_prime`
`JSON_stringify`, `JSON_parse`
`uuid`, `hash`, `copy`, `type_of`, `sleep`
Constants: `TAU`, `PHI`

#### Infrastructure
- Android APK editor (Java + WebView + VCG interpreter)
- GitHub Actions: CI + Pages + APK build
- `call_func_self()` — proper `self` binding for class methods
- Global interpreter pointer for higher-order builtins

### Changed
- Version bumped: 1.0.0 → 2.0.0
- `ND_NEW` now calls `init()` with proper `self` binding
- Contextual keywords: all v2 keywords usable as variable/param/field names
- All param parsing accepts any non-delimiter token (from, to, via, type…)

---

## [1.0.0] - 2026-06-06

### Added
- Full C11 compiler: lexer → parser → AST → interpreter/codegen
- HTML+JS output with complete runtime
- UI keywords: `h()`, `l()`, `btn()`, `url()`, `key()`, `img()`, `video()`
- Social: `youtube()`, `facebook()`, `instagram()`, `xsocial()`
- Reactive: `$set`, `$get`, `watch()`
- Channels: `c name`, `send()`, `recv()`
- Write-only: `w name = val`
- Execute: `$x expr`
- Public: `public`
- Standard library: 40+ builtins
- `struct`, `try/catch/throw`, `match/when`
- GitHub Pages landing with live JS demo
- Language icon: dark olive + 3 stars + VCG text
- Linguist language detection
- TextMate syntax highlighting grammar
