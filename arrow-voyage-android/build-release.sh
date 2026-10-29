#!/usr/bin/env bash
# Builds the Play Store bundle. Needs: JDK 17+, Android SDK (ANDROID_HOME or local.properties), keystore.properties next to this script.
set -e
cd "$(dirname "$0")"
[ -f keystore.properties ] || { echo "keystore.properties missing: copy keystore.properties.example and fill it in (see README.md)"; exit 1; }
./gradlew --no-daemon :app:bundleRelease
echo
echo "AAB ready: $(pwd)/app/build/outputs/bundle/release/app-release.aab"
