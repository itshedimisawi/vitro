#!/usr/bin/env bash
# Records the showcase flow on a connected phone for the README: the film page scrolled to the
# end and back, a seat booked, then the glass buttons of the ticket pulled to show them stretch.
#
# Usage: scripts/record-demo.sh [output.mp4]
# Install the sample first (./gradlew :sample:installDebug). With ffmpeg on PATH, a GIF is made too.
# Show taps only marks real touches on some phones; the ones injected here may not appear.
set -euo pipefail
export MSYS_NO_PATHCONV=1

OUT="${1:-vitro-demo.mp4}"
PKG=io.github.itshedimisawi.vitro.sample
REMOTE=/sdcard/vitro-demo.mp4
# The SDK's adb, so an older one on PATH does not fight its server. adb reads ANDROID_SERIAL itself.
ADB=(adb)
for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "${LOCALAPPDATA:+$LOCALAPPDATA/Android/Sdk}"; do
    for exe in "$sdk/platform-tools/adb" "$sdk/platform-tools/adb.exe"; do
        [[ -n "$sdk" && -x "$exe" ]] && { ADB=("$exe"); break 2; }
    done
done

# Swipe timings, in milliseconds.
SCROLL_MS=1000    # one steady stroke of the page
PULL_MS=850       # one pull on a glass button
GAP=0.12          # between strokes, in seconds

sh() { "${ADB[@]}" shell "$@"; }
pause() { sleep "$1"; }

# The centre of the element whose text or content description is exactly $1, as "x y".
locate() {
    sh uiautomator dump /sdcard/vitro-ui.xml >/dev/null
    local node
    node=$("${ADB[@]}" exec-out cat /sdcard/vitro-ui.xml | tr '<' '\n' |
        grep -E "(text|content-desc)=\"$1\"" | head -n 1)
    [[ -z "$node" ]] && { echo "Could not find \"$1\" on screen" >&2; exit 1; }
    sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<<"$node" |
        awk '{ printf "%d %d\n", ($1 + $3) / 2, ($2 + $4) / 2 }'
}

launch() {
    sh am force-stop "$PKG"
    sh am start -W -n "$PKG/.MainActivity" >/dev/null
    pause 1.5
}

"${ADB[@]}" get-state >/dev/null
sh pm path "$PKG" >/dev/null || { echo "Install the sample first." >&2; exit 1; }

read -r WIDTH HEIGHT < <(sh wm size | tr -d '\r' | sed -E 's/.*: ([0-9]+)x([0-9]+).*/\1 \2/' | tail -n 1)
X=$((WIDTH / 2))
SCROLL_FROM=$((HEIGHT * 80 / 100))
SCROLL_TO=$((HEIGHT * 33 / 100))

echo "Finding the controls and the length of the page…"
launch
read -r SCHEDULE_X SCHEDULE_Y < <(locate "Schedule and tickets")
# Strokes that reach the end: the screen stops changing once the page is at its end.
STROKES=0
LAST=""
while :; do
    sh input swipe "$X" "$SCROLL_FROM" "$X" "$SCROLL_TO" "$SCROLL_MS"
    pause 1.2
    NOW=$("${ADB[@]}" exec-out screencap | md5sum)
    STROKES=$((STROKES + 1))
    [[ "$NOW" == "$LAST" || $STROKES -ge 12 ]] && break
    LAST="$NOW"
done
STROKES=$((STROKES - 1))
launch
sh input tap "$SCHEDULE_X" "$SCHEDULE_Y"
pause 1.5
read -r SEAT_X SEAT_Y < <(locate "Row 5, seat 5")
read -r BUY_X BUY_Y < <(locate "Buy")
sh input tap "$SEAT_X" "$SEAT_Y"
pause 0.5
sh input tap "$BUY_X" "$BUY_Y"
pause 1.5
read -r WALLET_X WALLET_Y < <(locate "Add to Wallet")
read -r SHARE_X SHARE_Y < <(locate "Share")
PULL=$((WIDTH * 24 / 100))
echo "Page needs $STROKES strokes. Recording…"

launch
sh rm -f "$REMOTE"
"${ADB[@]}" shell screenrecord --bit-rate 16000000 "$REMOTE" &
RECORDER=$!
pause 1.2

# The film: down to the end and back to the top in steady strokes, then book. Each stroke cuts the
# fling of the one before short, so one more than the calibration needed reaches the end.
for ((i = 0; i <= STROKES; i++)); do sh input swipe "$X" "$SCROLL_FROM" "$X" "$SCROLL_TO" "$SCROLL_MS"; pause "$GAP"; done
pause 0.8
for ((i = 0; i <= STROKES; i++)); do sh input swipe "$X" "$SCROLL_TO" "$X" "$SCROLL_FROM" "$SCROLL_MS"; pause "$GAP"; done
pause 1
sh input tap "$SCHEDULE_X" "$SCHEDULE_Y"
pause 1.4

# Seats: one seat, then buy.
sh input tap "$SEAT_X" "$SEAT_Y"
pause 0.9
sh input tap "$BUY_X" "$BUY_Y"
pause 1.4

# The ticket: pull each glass button one way and back, letting it spring home in between.
sh input swipe "$WALLET_X" "$WALLET_Y" $((WALLET_X + PULL)) "$WALLET_Y" "$PULL_MS"; pause 0.7
sh input swipe "$WALLET_X" "$WALLET_Y" $((WALLET_X - PULL)) "$WALLET_Y" "$PULL_MS"; pause 0.9
sh input swipe "$SHARE_X" "$SHARE_Y" "$SHARE_X" $((SHARE_Y + PULL)) "$PULL_MS"; pause 0.7
sh input swipe "$SHARE_X" "$SHARE_Y" $((SHARE_X - PULL)) $((SHARE_Y + PULL / 2)) "$PULL_MS"; pause 1.2

sh pkill -INT screenrecord || true
wait "$RECORDER" 2>/dev/null || true
pause 1
"${ADB[@]}" pull "$REMOTE" "$OUT" >/dev/null
sh rm -f "$REMOTE" /sdcard/vitro-ui.xml
echo "Saved $OUT"

if command -v ffmpeg >/dev/null; then
    GIF="${OUT%.*}.gif"
    ffmpeg -loglevel error -y -i "$OUT" -vf \
        "fps=30,scale=432:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=5" \
        "$GIF"
    echo "Saved $GIF"
fi
