# VCG Examples

Start here: **`index.vcg`** — a single file that tours *every* concept in
the language, section by section (comments, types, operators, control
flow, pattern matching, functions, structs, enums, classes, error
handling, the reactive store, channels, modules, and all UI/media
output elements).

Run any file with:

```
vcgc -r examples/<path>/<file>.vcg
```

or open it in the VCG Editor app and press ▶ تشغيل.

## index.vcg
A complete language tour in one file — 20 numbered sections covering
every keyword and built-in described below. Best place to start.

## basic/
| File | Covers |
|---|---|
| `hello.vcg` | First program, `show`, arithmetic |
| `variables.vcg` | Types, strings, arrays, `const`, math functions |
| `loops.vcg` | `while`, `repeat`, `for ... in`, `range`, `break`/`continue` |
| `functions.vcg` | `func`, recursion, functions as values, `map`/`filter`/`reduce` |

## advanced/
| File | Covers |
|---|---|
| `structs.vcg` | Structs, maps/dictionaries, OOP-style helper functions |
| `classes_oop.vcg` | `class`, `extends`, `super`, `self`, `new` |
| `error_handling.vcg` | `try`/`catch`, `throw`, `assert`, `safe`, `assert_eq` family |
| `pattern_matching.vcg` | `match`/`case`, `when`/guards |
| `modules.vcg` | `module`, `export`, `import`, `from`, `as` |
| `new_concepts.vcg` | `$set`/`$get` reactive store, `public`, `w`, `c` channels, `watch` |
| `ui_media.vcg` | UI/media output: `h`, `l`, `url`, `btn`, `key`, `img`, `youtube`, social embeds |
| `fibonacci.vcg`, `sorting.vcg`, `calculator.vcg` | Classic algorithm demos |
| `v2_concepts.vcg` | Misc v2.0 language feature tour |

## domains/ — real-world use-case templates
See [`docs/USE_CASES.md`](../docs/USE_CASES.md) for the full breakdown of VCG's
five main use domains. Each file below is a ready-to-run starting point:

| File | Domain |
|---|---|
| `education.vcg` | Interactive teaching/learning content |
| `business_app.vcg` | Small-business apps (inventory, POS) with `data_vcg` + APK export |
| `social_landing.vcg` | Link-in-bio / social landing pages |
| `automation_data.vcg` | Data cleaning, transforms, and validated reports |
| `interactive_docs.vcg` | Living documentation with `test` blocks as runnable examples |
