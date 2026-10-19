#!/usr/bin/env bash
# Build launcher/build/tvhome.apk without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

rm -rf build && mkdir -p build/gen build/classes
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 26 --target-sdk-version 34 -o build/unsigned.apk build/res.zip
javac --release 11 -classpath "$JAR" -d build/classes \
  $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 26 --lib "$JAR" --output build $(find build/classes -name '*.class')
(cd build && zip -qj unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk

KS=keystore/release.jks
if [ ! -f "$KS" ]; then
  mkdir -p keystore
  keytool -genkeypair -keystore "$KS" -storepass tvhome -keypass tvhome -alias tvhome \
    -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=TV Home" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:tvhome --out build/tvhome.apk build/aligned.apk
ls -la build/tvhome.apk
