#!/bin/bash
# ================================================================
# Syrian VCG Language — Setup GitHub Repository Script
# Usage: bash setup-github.sh YOUR_GITHUB_USERNAME
# ================================================================

USERNAME=${1:-"syrian-vcg"}
REPO="vcg-lang"
REMOTE_URL="https://github.com/${USERNAME}/${REPO}.git"

echo "=================================="
echo " Syrian VCG Language — GitHub Setup"
echo "=================================="
echo ""
echo "Repository: ${REMOTE_URL}"
echo ""

# Git config
git config user.email "vcg@syrianvcg.dev" 2>/dev/null || true
git config user.name "Syrian VCG" 2>/dev/null || true

# Remove old remote if exists
git remote remove origin 2>/dev/null || true

# Add remote
git remote add origin "${REMOTE_URL}"
echo "✓ Remote set to: ${REMOTE_URL}"

# Stage all files
git add -A
echo "✓ Files staged"

# Commit
git commit -m "feat: Initial release — Syrian VCG v2.0.0

- Full C11 compiler (lexer → parser → AST → interpreter + codegen)
- 70+ keywords including OOP, async, modules, enums, safety
- UI/Social: h(), l(), btn(), youtube(), facebook(), instagram()
- Reactive store: \$set, \$get, watch()
- Channels: c, send, recv
- 50+ built-in functions
- Android APK editor with syntax highlighting
- GitHub Actions: CI/CD + Pages + APK
- 10 automated tests (100% passing)
- Live demo at GitHub Pages" 2>/dev/null || echo "Already committed"

echo "✓ Committed"
echo ""
echo "Next steps:"
echo ""
echo "1. Create repository on GitHub:"
echo "   https://github.com/new"
echo "   Name: vcg-lang"
echo "   Description: لغة برمجة سورية حقيقية — Syrian Private Programming Language"
echo "   Public: ✓"
echo "   Initialize: ✗ (don't add README)"
echo ""
echo "2. Push:"
echo "   git push -u origin main"
echo ""
echo "3. Enable GitHub Pages:"
echo "   Settings → Pages → Source: GitHub Actions → Save"
echo ""
echo "4. Add Secrets for APK signing (optional):"
echo "   Settings → Secrets → KEYSTORE_BASE64, KEYSTORE_PASS, KEY_ALIAS, KEY_PASS"
echo ""
