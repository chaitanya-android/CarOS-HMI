#!/usr/bin/env bash
# Install all CarOSMini APKs to the correct emulators.
# Run from the project root after a successful build:
#   ./scripts/install-all.sh

set -euo pipefail

AAOS_AVD="${AAOS_AVD:-CarOSMini_AAOS}"
PHONE_AVD="${PHONE_AVD:-CarOSMini_Phone}"

# Resolve emulator serial by AVD name
find_serial() {
    local avd_name="$1"
    adb devices -l | grep "emulator" | while read -r serial rest; do
        local name
        name=$(adb -s "$serial" emu avd name 2>/dev/null | head -1 | tr -d '\r')
        if [[ "$name" == "$avd_name" ]]; then
            echo "$serial"
            return
        fi
    done
}

AAOS_SERIAL=$(find_serial "$AAOS_AVD")
PHONE_SERIAL=$(find_serial "$PHONE_AVD")

if [[ -z "$AAOS_SERIAL" ]]; then
    echo "ERROR: AAOS emulator '$AAOS_AVD' not found. Start it first."
    exit 1
fi

echo "AAOS emulator : $AAOS_SERIAL"
echo "Phone emulator: ${PHONE_SERIAL:-not running}"

BUILD_DIR="."

install_apk() {
    local serial="$1"
    local apk="$2"
    echo "  -> adb -s $serial install -r $apk"
    adb -s "$serial" install -r "$apk"
}

echo ""
echo "=== Installing AAOS apps ==="
# core-service MUST be installed first — it declares the BIND_CORE_SERVICE
# signature permission. Apps installed before it will not receive the grant.
install_apk "$AAOS_SERIAL" "$BUILD_DIR/core-service/build/outputs/apk/debug/core-service-debug.apk"
install_apk "$AAOS_SERIAL" "$BUILD_DIR/app-launcher/build/outputs/apk/debug/app-launcher-debug.apk"
install_apk "$AAOS_SERIAL" "$BUILD_DIR/app-media/build/outputs/apk/debug/app-media-debug.apk"
install_apk "$AAOS_SERIAL" "$BUILD_DIR/app-nav/build/outputs/apk/debug/app-nav-debug.apk"
install_apk "$AAOS_SERIAL" "$BUILD_DIR/app-dashboard/build/outputs/apk/debug/app-dashboard-debug.apk"

if [[ -n "$PHONE_SERIAL" ]]; then
    echo ""
    echo "=== Installing Phone companion app ==="
    COMPANION_APK="$BUILD_DIR/companion-phone/build/outputs/apk/debug/companion-phone-debug.apk"
    if [[ -f "$COMPANION_APK" ]]; then
        install_apk "$PHONE_SERIAL" "$COMPANION_APK"
    else
        echo "  (companion-phone module not built yet — skipping)"
    fi
fi

echo ""
echo "Done. All APKs installed."
