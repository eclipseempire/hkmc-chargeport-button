#!/bin/bash
# Zip the NEMESIS package into dist/chargeport.button.zip + a USB bundle.
# Run tools/build_apk.sh first and copy the APK to package/app/chargeport.apk.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/package/app/chargeport.apk" ] || {
  echo "missing package/app/chargeport.apk - run tools/build_apk.sh and copy app/chargeport.apk there first"; exit 1; }
mkdir -p "$ROOT/dist"
rm -f "$ROOT/dist/chargeport.button.zip"
( cd "$ROOT/package" && zip -r -X "$ROOT/dist/chargeport.button.zip" . -x '.*' >/dev/null )
echo "wrote dist/chargeport.button.zip"
TMP="$ROOT/dist/usb-bundle"; rm -rf "$TMP"; mkdir -p "$TMP/packages"
cp "$ROOT/dist/chargeport.button.zip" "$TMP/packages/chargeport.button.zip"
cat > "$TMP/NEMESIS.data" <<'EOF'
[nemesis]
version=1.0
type=install
path=packages/chargeport.button.zip
EOF
rm -f "$ROOT/dist/hkmc-chargeport-button-USB.zip"
( cd "$ROOT/dist" && zip -r -X hkmc-chargeport-button-USB.zip usb-bundle >/dev/null && rm -rf usb-bundle )
echo "wrote dist/hkmc-chargeport-button-USB.zip (extract to a FAT32/exFAT USB root)"
