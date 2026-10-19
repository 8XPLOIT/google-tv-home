#!/usr/bin/env bash
# Save a screenshot from the TV to shots/NN-<name>.png and print its path.
set -euo pipefail
source "$(dirname "$0")/common.sh"
ensure_connected
mkdir -p "$ROOT/shots"
n=$(ls "$ROOT/shots" 2>/dev/null | grep -c '\.png$' || true)
out="$ROOT/shots/$(printf %02d "$n")-${1:-shot}.png"
adb exec-out screencap -p > "$out"
echo "$out"
