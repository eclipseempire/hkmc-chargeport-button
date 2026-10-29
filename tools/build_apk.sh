#!/bin/bash
# Build + sign the Charge Port Button APK (com.nemesis.chargeport).
# Requires a JDK and Android build-tools that still ship `dx` (e.g. 30.0.3)
# plus a platform android.jar. See EXTENDING notes in the README.
#   ANDROID_SDK   path to SDK root            (required)
#   BUILD_TOOLS   default 30.0.3              (must have `dx`)
#   PLATFORM      default android-30
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_SDK:?set ANDROID_SDK to your Android SDK root}"
: "${BUILD_TOOLS:=30.0.3}"
: "${PLATFORM:=android-30}"
BT="$ANDROID_SDK/build-tools/$BUILD_TOOLS"
AJ="$ANDROID_SDK/platforms/$PLATFORM/android.jar"
APP="$ROOT/app"; OUT="$APP/build"; KS="$APP/debug.jks"

[ -x "$BT/dx" ] || { echo "no dx in $BT (use build-tools that still ship dx)"; exit 1; }
rm -rf "$OUT"; mkdir -p "$OUT/classes"

echo "== javac =="
javac --release 8 -classpath "$AJ" -d "$OUT/classes" $(find "$APP/src" -name '*.java')
echo "== dex (dx, min-sdk 19 -> DEX 035 for KitKat) =="
"$BT/dx" --dex --min-sdk-version=19 --output="$OUT/classes.dex" "$OUT/classes"
echo "== package manifest =="
"$BT/aapt" package -f -M "$APP/AndroidManifest.xml" -I "$AJ" -F "$OUT/base.apk"
( cd "$OUT" && "$BT/aapt" add base.apk classes.dex >/dev/null )
echo "== align =="
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
echo "== sign (v1 for KitKat) =="
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias cs -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=chargeport" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --v1-signing-enabled true --v2-signing-enabled true --min-sdk-version 19 \
  --out "$APP/chargeport.apk" "$OUT/aligned.apk"
echo "== done: $APP/chargeport.apk =="
echo "Copy app/chargeport.apk -> package/app/chargeport.apk, then run tools/make_package.sh"
