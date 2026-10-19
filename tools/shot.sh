#!/usr/bin/env bash
# Save a screenshot from the TV to shots/NN-<name>.png and print its path.
set -euo pipefail
# shellcheck source=common.sh
source "$(dirname "$0")/common.sh"
ensure_connected
mkdir -p "$ROOT/shots"
shopt -s nullglob
shots=("$ROOT"/shots/*.png)
n=${#shots[@]}
out="$ROOT/shots/$(printf %02d "$n")-${1:-shot}.png"
adb exec-out screencap -p > "$out"
echo "$out"
