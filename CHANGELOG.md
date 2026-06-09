# Changelog — Syrian VCG Language

## [1.0.0] - 2026-06-06

### New UI/Media Keywords
- **`youtube(url)`** — Embed responsive YouTube iframe
- **`facebook(url, text)`** — Styled Facebook link/button
- **`instagram(handle, text)`** — Instagram gradient link
- **`xsocial(handle, text)`** — X/Twitter black button
- **`url(href, text)`** — Styled hyperlink
- **`btn(label, action)`** — Interactive button with onclick
- **`key(combo)`** — Keyboard shortcut badge
- **`video(src, w, h)`** — HTML5 video player
- **`img(src, alt, width)`** — Lazy-loading image
- **`h(level, text)`** — Heading h1-h6 with VCG style
- **`l(item1, item2, ...)`** — Styled bullet list

### New v1.0 Concepts
- **`$set(key, val)`** — Reactive store write + watcher trigger
- **`$get(key)`** — Reactive store read
- **`public`** — Export symbols to __exports__
- **`w name = val`** — Write-only binding with audit log
- **`c name`** — Channel variable (async queue)
- **`watch(key, fn)`** — Register reactive watcher
- **`send(ch, val)`** / **`recv(ch)`** — Channel I/O
- **`pipe(val, f1, f2...)`** — Functional pipeline
- **`$x expr`** — Execute/IIFE operator

### Compiler
- Version: 1.0.0
- Release date: 2026-06-06
- Full C11 compiler: lexer → parser → AST → interpreter/codegen
- Contextual keywords: x, c, w usable as identifiers
- Keywords allowed as struct field names and param names
- GitHub Pages landing with live demo
- GitHub Actions CI/CD with multi-platform build
- GitHub Linguist language detection
- TextMate grammar for syntax highlighting
- Language icon: dark olive green + 3 white stars
