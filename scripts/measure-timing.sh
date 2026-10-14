#!/usr/bin/env bash
# M8 timing check: cold start and library scroll, measured with adb on a running emulator or device.
#
# Usage: scripts/measure-timing.sh [serial]
#   serial     adb serial (default: the only attached device)
#   ADB        adb binary (default: adb)
#   COLD_RUNS  cold-start repetitions (default: 5)
#   SWIPES     library swipes for the scroll check (default: 30)
#
# Cold start: force-stop, then `am start -W`; reports the median TotalTime.
#   Budget: under 800 ms (LEAF-BUILD-CHECKLIST, M8).
# Scroll: resets gfxinfo, performs SWIPES downward swipes on the library, then reads the
#   frame-time percentiles. Pass criterion used here (an assumption, not in the checklist text):
#   the 90th-percentile frame time is at most 16 ms, which is 60 fps.
#
# The script exits non-zero when either budget is missed. Emulator numbers are not device
# numbers; the report says so.

set -euo pipefail

ADB="${ADB:-adb}"
PACKAGE="app.leaf.reader"
ACTIVITY="$PACKAGE/.MainActivity"
COLD_RUNS="${COLD_RUNS:-5}"
SWIPES="${SWIPES:-30}"
COLD_BUDGET_MS=800
FRAME_BUDGET_MS=16
SERIAL="${1:-}"

adb_cmd() {
  if [[ -n "$SERIAL" ]]; then
    "$ADB" -s "$SERIAL" "$@"
  else
    "$ADB" "$@"
  fi
}

# Prints the value of a field from `am start -W` output, e.g. "TotalTime: 412" -> 412.
am_field() {
  tr -d '\r' | sed -n "s/^$1: *\([0-9][0-9]*\).*/\1/p" | head -n 1
}

median() {
  sort -n | awk '{ values[NR] = $1 } END {
    if (NR == 0) { print "none"; exit }
    if (NR % 2) print values[(NR + 1) / 2]
    else print int((values[NR / 2] + values[NR / 2 + 1]) / 2)
  }'
}

cold_start_ms() {
  adb_cmd shell am force-stop "$PACKAGE" >/dev/null
  sleep 2
  adb_cmd shell am start -W -n "$ACTIVITY" | am_field TotalTime
}

measure_cold_starts() {
  local i total
  local -a totals=()
  for ((i = 1; i <= COLD_RUNS; i++)); do
    total="$(cold_start_ms)"
    if [[ -z "$total" ]]; then
      echo "error: no TotalTime from am start on run $i" >&2
      exit 2
    fi
    echo "  cold start $i: ${total} ms" >&2
    totals+=("$total")
  done
  printf '%s\n' "${totals[@]}" | median
}

# Reads a percentile line such as "90th percentile: 14ms" from gfxinfo output.
gfx_percentile_ms() {
  tr -d '\r' | sed -n "s/^ *$1 percentile: *\([0-9][0-9]*\)ms.*/\1/p" | head -n 1
}

measure_scroll() {
  local size height y_start y_end i
  size="$(adb_cmd shell wm size | tr -d '\r' | sed -n 's/^Physical size: *//p' | head -n 1)"
  height="${size#*x}"
  if [[ -z "$height" || ! "$height" =~ ^[0-9]+$ ]]; then
    echo "error: could not read the screen size from wm size" >&2
    exit 2
  fi
  y_start=$((height * 4 / 5))
  y_end=$((height * 3 / 10))
  adb_cmd shell am force-stop "$PACKAGE" >/dev/null
  adb_cmd shell am start -n "$ACTIVITY" >/dev/null
  sleep 5
  adb_cmd shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null
  for ((i = 1; i <= SWIPES; i++)); do
    adb_cmd shell input swipe 540 "$y_start" 540 "$y_end" 300
  done
  sleep 1
  local report
  report="$(adb_cmd shell dumpsys gfxinfo "$PACKAGE")"
  SCROLL_P90="$(printf '%s\n' "$report" | gfx_percentile_ms 90th)"
  SCROLL_JANKY="$(printf '%s\n' "$report" | tr -d '\r' | sed -n 's/^Janky frames: *\([0-9][0-9]*\).*/\1/p' | head -n 1)"
  SCROLL_FRAMES="$(printf '%s\n' "$report" | tr -d '\r' | sed -n 's/^Total frames rendered: *\([0-9][0-9]*\).*/\1/p' | head -n 1)"
  if [[ -z "$SCROLL_P90" ]]; then
    echo "error: no 90th percentile in gfxinfo output (is the app drawing frames?)" >&2
    exit 2
  fi
}

api_level="$(adb_cmd shell getprop ro.build.version.sdk | tr -d '\r')"
model="$(adb_cmd shell getprop ro.product.model | tr -d '\r')"
echo "Leaf timing check on API $api_level ($model). Emulator numbers are not device numbers."

echo "Cold start ($COLD_RUNS runs, budget ${COLD_BUDGET_MS} ms):"
COLD_MEDIAN="$(measure_cold_starts)"
echo "  median: ${COLD_MEDIAN} ms"

echo "Library scroll ($SWIPES swipes, 90th-percentile frame budget ${FRAME_BUDGET_MS} ms):"
measure_scroll
echo "  frames rendered: ${SCROLL_FRAMES:-unknown}, janky: ${SCROLL_JANKY:-unknown}, 90th percentile: ${SCROLL_P90} ms"

status=0
if (( COLD_MEDIAN > COLD_BUDGET_MS )); then
  echo "FAIL: median cold start ${COLD_MEDIAN} ms is over ${COLD_BUDGET_MS} ms"
  status=1
fi
if (( SCROLL_P90 > FRAME_BUDGET_MS )); then
  echo "FAIL: scroll 90th-percentile frame ${SCROLL_P90} ms is over ${FRAME_BUDGET_MS} ms"
  status=1
fi
if (( status == 0 )); then
  echo "PASS: both budgets met on this emulator"
fi
exit "$status"
