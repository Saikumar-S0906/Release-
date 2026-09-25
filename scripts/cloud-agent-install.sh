#!/usr/bin/env bash
# Cloud Agent environment bootstrap for PixiJoy (Android / Gradle).
# Idempotent: safe to re-run. Heavy downloads are guarded so a snapshot/build
# that already contains the SDK re-runs this quickly.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

log() { printf '\n=== %s ===\n' "$*"; }

# ---------------------------------------------------------------------------
# JDK 17. AGP 8.10 / Gradle 8.11 should run on JDK 17, not Studio JBR 25.
# The repo does not commit org.gradle.java.home. Pin JDK 17 in the user-level
# Gradle properties so this environment does not pick up a newer default JDK.
# ---------------------------------------------------------------------------
log "Ensuring JDK 17"
JAVA17_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
if [ ! -d "$JAVA17_HOME" ]; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk
fi
"$JAVA17_HOME/bin/java" -version

mkdir -p "$HOME/.gradle"
touch "$HOME/.gradle/gradle.properties"
grep -v '^org.gradle.java.home=' "$HOME/.gradle/gradle.properties" > "$HOME/.gradle/gradle.properties.tmp" || true
mv "$HOME/.gradle/gradle.properties.tmp" "$HOME/.gradle/gradle.properties"
echo "org.gradle.java.home=$JAVA17_HOME" >> "$HOME/.gradle/gradle.properties"

# ---------------------------------------------------------------------------
# Android SDK
# ---------------------------------------------------------------------------
log "Ensuring Android SDK"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
CMDLINE_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"

if [ ! -x "$CMDLINE_DIR/bin/sdkmanager" ]; then
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  tmp="$(mktemp -d)"
  wget -q -O "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  rm -rf "$CMDLINE_DIR"
  mkdir -p "$CMDLINE_DIR"
  mv "$tmp/cmdline-tools/"* "$CMDLINE_DIR/"
  rm -rf "$tmp"
fi

export PATH="$CMDLINE_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null 2>&1 || true
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

# Emulator + system image + AVD so instrumented / UI testing is available on
# demand. Guarded so this only downloads once; not started on boot.
if [ ! -d "$ANDROID_SDK_ROOT/emulator" ]; then
  sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "emulator"
fi
if [ ! -d "$ANDROID_SDK_ROOT/system-images/android-35/google_apis/x86_64" ]; then
  sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "system-images;android-35;google_apis;x86_64"
fi
if [ ! -d "$HOME/.android/avd/pixijoy_api35.avd" ]; then
  echo "no" | avdmanager create avd \
    -n pixijoy_api35 \
    -k "system-images;android-35;google_apis;x86_64" \
    --device "pixel_6" --force
fi

# ---------------------------------------------------------------------------
# local.properties (gitignored; recreated on every fresh checkout)
# ---------------------------------------------------------------------------
log "Writing local.properties"
cat > local.properties <<EOF
sdk.dir=$ANDROID_SDK_ROOT
GEMINI_API_KEY=dummy
GEMINI_MODEL=gemini-2.5-flash
BOARD_LIVE_GENERATE=false
PIXIJOY_API_BASE_URL=
ADMOB_APP_ID=
ADMOB_BANNER_UNIT_ID=
ADMOB_REWARDED_UNIT_ID=
ADMOB_INTERSTITIAL_UNIT_ID=
EOF

chmod +x gradlew

# ---------------------------------------------------------------------------
# Warm Gradle: download dependencies and prime the build.
# ---------------------------------------------------------------------------
log "Warming Gradle build"
./gradlew --no-daemon :app:assembleDebug -x lint

log "Environment ready"
