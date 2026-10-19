#!/usr/bin/env bash
# Android Lint on the launcher, without Gradle. Run build.sh first (lint reads build/classes).
# Fails on any error or warning. Writes build/lint.sarif for GitHub code scanning.
#
# Uses lint 8.2.0 from cmdline-tools 11076708, downloaded once into ~/.cache/tvhome: newer
# standalone releases (9.3.0) crash on {@code} in doc comments.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}"
export ANDROID_HOME="$SDK"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

BUILD=11076708
case "$(uname -s)" in
  Darwin) OS=mac; SHA256=7bc5c72ba0275c80a8f19684fb92793b83a6b5c94d4d179fc5988930282d7e64 ;;
  Linux) OS=linux; SHA256=2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258 ;;
  *) echo "unsupported OS" >&2; exit 1 ;;
esac
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/tvhome"
TOOLS="$CACHE/cmdline-tools-$BUILD"
if [ ! -x "$TOOLS/bin/lint" ]; then
  mkdir -p "$CACHE"
  zip="$CACHE/cmdline-tools-$BUILD.zip"
  curl -sSfL -o "$zip" "https://dl.google.com/android/repository/commandlinetools-$OS-${BUILD}_latest.zip"
  echo "$SHA256  $zip" | shasum -a 256 -c - >/dev/null
  rm -rf "$TOOLS" "$CACHE/cmdline-tools"
  unzip -q "$zip" -d "$CACHE"
  mv "$CACHE/cmdline-tools" "$TOOLS"
  rm "$zip"
fi

[ -d build/classes ] || { echo "run build.sh first" >&2; exit 1; }
# --compile-sdk-version lets lint resolve Android classes; without it NewApi checks see nothing.
"$TOOLS/bin/lint" --config lint.xml --exitcode -Werror --quiet \
  --sdk-home "$SDK" --compile-sdk-version 34 \
  --sources src --resources res --classpath build/classes \
  --sarif build/lint.sarif --text stdout .
