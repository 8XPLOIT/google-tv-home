# shellcheck shell=bash
# Shared settings for the tools/ scripts. Source it, don't run it.
# Point adb at the TV with ANDROID_SERIAL=<tv-ip>:5555, or leave it unset to use the one device
# adb already sees (USB or an earlier `adb connect`).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[ -z "${ANDROID_SERIAL:-}" ] || export ANDROID_SERIAL
BACKUP_DIR=""
[ -f "$ROOT/backups/LATEST" ] && BACKUP_DIR="$ROOT/backups/$(cat "$ROOT/backups/LATEST")"
# shellcheck disable=SC2034  # used by the scripts that source this
APPLIED="$BACKUP_DIR/applied.txt"

ensure_connected() {
  if [ "$(adb get-state 2>/dev/null)" != "device" ]; then
    [ -n "${ANDROID_SERIAL:-}" ] || { echo "no TV connected: set ANDROID_SERIAL=<tv-ip>:5555" >&2; exit 1; }
    adb connect "$ANDROID_SERIAL" >/dev/null
    [ "$(adb get-state 2>/dev/null)" = "device" ] || { echo "TV not reachable at $ANDROID_SERIAL" >&2; exit 1; }
  fi
}

# Everything that changes the TV logs into a backup, so restore.sh can undo it.
require_backup() {
  [ -n "$BACKUP_DIR" ] && [ -d "$BACKUP_DIR" ] || { echo "no backup yet: run tools/backup.sh first" >&2; exit 1; }
}

# Package names from a list file: strip comments and blank lines.
read_list() { sed -e 's/#.*//' -e 's/[[:space:]]//g' "$1" | grep -v '^$'; }

is_disabled() { adb shell pm list packages -d | tr -d '\r' | grep -qx "package:$1"; }
is_installed() { adb shell pm list packages | tr -d '\r' | grep -qx "package:$1"; }
