#!/usr/bin/env bash
# fix-adaptive-icons.sh
# يُصلح خطأ: <adaptive-icon> elements require a sdk version of at least 26
#
# الاستخدام (من جذر المستودع vcg-lang):
#   bash fix-adaptive-icons.sh
#
# ما يفعله:
# 1. ينشئ مجلد mipmap-anydpi-v26
# 2. ينقل ملفات ic_launcher*.xml إليه (تُستخدم فقط على API 26+)
# 3. يحذف أي ic_launcher*.xml من مجلدات mipmap-*dpi الأخرى
# 4. يولّد ic_launcher.png و ic_launcher_round.png لكل كثافة إن لم تكن موجودة
#    (يستخدم ImageMagick إن وُجد، وإلا ينسخ من أي PNG متاح)

set -euo pipefail

RES_DIR="editor/android/app/src/main/res"

if [ ! -d "$RES_DIR" ]; then
  echo "✗ المجلد $RES_DIR غير موجود. شغّل السكربت من جذر المستودع."
  exit 1
fi

echo "→ استخدام: $RES_DIR"

# 1) أنشئ mipmap-anydpi-v26
mkdir -p "$RES_DIR/mipmap-anydpi-v26"

# 2) ابحث عن أول نسخة xml متاحة لنقلها
SRC_XML=""
SRC_XML_ROUND=""
for d in mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi mipmap-mdpi; do
  [ -z "$SRC_XML" ]       && [ -f "$RES_DIR/$d/ic_launcher.xml" ]       && SRC_XML="$RES_DIR/$d/ic_launcher.xml"
  [ -z "$SRC_XML_ROUND" ] && [ -f "$RES_DIR/$d/ic_launcher_round.xml" ] && SRC_XML_ROUND="$RES_DIR/$d/ic_launcher_round.xml"
done

if [ -n "$SRC_XML" ]; then
  cp "$SRC_XML" "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml"
  echo "✓ نُسخ ic_launcher.xml إلى mipmap-anydpi-v26/"
fi
if [ -n "$SRC_XML_ROUND" ]; then
  cp "$SRC_XML_ROUND" "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml"
  echo "✓ نُسخ ic_launcher_round.xml إلى mipmap-anydpi-v26/"
fi

# 3) احذف ملفات xml من كل مجلدات mipmap-*dpi
for d in "$RES_DIR"/mipmap-*dpi; do
  [ -d "$d" ] || continue
  rm -f "$d/ic_launcher.xml" "$d/ic_launcher_round.xml"
done
echo "✓ حُذفت ملفات ic_launcher*.xml من مجلدات الكثافة"

# 4) تأكد من وجود PNG لكل كثافة
declare -A SIZES=( [mipmap-mdpi]=48 [mipmap-hdpi]=72 [mipmap-xhdpi]=96 [mipmap-xxhdpi]=144 [mipmap-xxxhdpi]=192 )

# اعثر على أي PNG ic_launcher متاح كاحتياط
FALLBACK_PNG=""
for d in mipmap-xxxhdpi mipmap-xxhdpi mipmap-xhdpi mipmap-hdpi mipmap-mdpi; do
  [ -f "$RES_DIR/$d/ic_launcher.png" ] && FALLBACK_PNG="$RES_DIR/$d/ic_launcher.png" && break
done

HAS_MAGICK=0
if command -v magick >/dev/null 2>&1; then HAS_MAGICK=1; fi
if command -v convert >/dev/null 2>&1 && [ $HAS_MAGICK -eq 0 ]; then HAS_MAGICK=2; fi

resize_png() {
  local src="$1" dst="$2" size="$3"
  if [ $HAS_MAGICK -eq 1 ]; then
    magick "$src" -resize "${size}x${size}" "$dst"
  elif [ $HAS_MAGICK -eq 2 ]; then
    convert "$src" -resize "${size}x${size}" "$dst"
  else
    cp "$src" "$dst"
  fi
}

for d in "${!SIZES[@]}"; do
  mkdir -p "$RES_DIR/$d"
  size="${SIZES[$d]}"
  for name in ic_launcher.png ic_launcher_round.png; do
    target="$RES_DIR/$d/$name"
    if [ ! -f "$target" ]; then
      if [ -n "$FALLBACK_PNG" ]; then
        resize_png "$FALLBACK_PNG" "$target" "$size"
        echo "✓ أُنشئ $target (${size}px)"
      else
        echo "⚠ تحذير: لا يوجد PNG مرجعي. أنشئ $target يدوياً (${size}x${size})"
      fi
    fi
  done
done

echo ""
echo "✅ تم. الآن نفّذ:"
echo "   git add $RES_DIR"
echo "   git commit -m \"fix: move adaptive-icon to mipmap-anydpi-v26\""
echo "   git push"
