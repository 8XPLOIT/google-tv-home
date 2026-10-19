#!/usr/bin/env bash
# Disable packages for user 0 (reversible, never uninstalls) and log them for restore.sh.
# Usage: tools/disable.sh -f config/debloat.txt
#        tools/disable.sh com.example.one com.example.two
set -euo pipefail
# shellcheck source=common.sh
source "$(dirname "$0")/common.sh"
ensure_connected
require_backup

if [ "${1:-}" = "-f" ]; then pkgs=$(read_list "$2"); else pkgs="$*"; fi
[ -n "$pkgs" ] || { echo "nothing to disable" >&2; exit 1; }
touch "$APPLIED"

for pkg in $pkgs; do
  if ! is_installed "$pkg"; then echo "skip   $pkg (not installed)"; continue; fi
  if is_disabled "$pkg"; then echo "skip   $pkg (already disabled)"; continue; fi
  adb shell pm disable-user --user 0 "$pkg" >/dev/null
  if is_disabled "$pkg"; then
    grep -qx "$pkg" "$APPLIED" || echo "$pkg" >> "$APPLIED"
    echo "off    $pkg"
  else
    echo "FAILED $pkg" >&2
  fi
done
