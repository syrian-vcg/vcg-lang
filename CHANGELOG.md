# Homy v1.0.0 - Official Release\n\nWelcome to the inaugural release of **Homy**! This version represents the baseline release of our platform, establishing a solid, production-ready foundation compiled from our core development repositories.\n\n## 🚀 Key Features & Highlights\n- **Project Bootstrap**: Successfully migrated the complete codebase to public source control from our core archive.\n- **Architecture Foundation**: Established the primary application structures, directories, and configuration baselines.\n- **Initial Integration**: Fully consolidated all core modules and utilities into a unified release candidate.\n\n## 🛠️ Installation & Setup\nTo get started with Homy v1.0.0, follow these steps:\n\n1. **Clone the repository:**\n   ```bash\n   git clone https://github.com/example/homy.git\n   cd homy\n   ```\n2. **Install dependencies:**\n   ```bash\n   npm install\n   ```\n3. **Start the application:**\n   ```bash\n   npm start\n   ```\n\n## 👥 Contributors\nWe would like to express our gratitude to the core development team who made this bootstrap release possible:\n- **homy** (Lead Architect & Core Contributor)

---

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
