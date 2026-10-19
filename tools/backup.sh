#!/usr/bin/env bash
# Save the TV's current state to backups/<timestamp>/ before changing anything: package lists,
# all settings, and the current Home. restore.sh reads it. Run once, on the untouched TV.
set -euo pipefail
# shellcheck source=common.sh
source "$(dirname "$0")/common.sh"
ensure_connected

ts=$(date +%Y%m%d-%H%M%S)
dir="$ROOT/backups/$ts"
mkdir -p "$dir"
tv() { adb shell "$@" | tr -d '\r'; }

{
  for p in ro.product.manufacturer ro.product.brand ro.product.model ro.product.device \
           ro.build.version.release ro.build.version.sdk ro.build.fingerprint ro.build.characteristics; do
    echo "$p=$(tv getprop "$p")"
  done
  echo "HOME=$(tv cmd package resolve-activity --brief -a android.intent.action.MAIN \
    -c android.intent.category.HOME | tail -1)"
  echo "DREAM=$(tv settings get secure screensaver_components)"
  echo "IME=$(tv settings get secure default_input_method)"
  echo "WM_SIZE=$(tv wm size)"
} > "$dir/device.txt"

tv pm list packages -f -U > "$dir/packages-all.txt"
for f in 3 d e s; do tv pm list packages -$f | sed 's/^package://' | sort > "$dir/packages-$f.txt"; done
# Already off before we touched anything; restore.sh leaves these off.
cp "$dir/packages-d.txt" "$dir/disabled-baseline.txt"
for ns in global secure system; do tv settings list $ns > "$dir/settings-$ns.txt"; done
tv dumpsys package preferred > "$dir/preferred-activities.txt"
: > "$dir/applied.txt"

echo "$ts" > "$ROOT/backups/LATEST"
echo "$dir"
grep '^HOME=' "$dir/device.txt"
