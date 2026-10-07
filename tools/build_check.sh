#!/bin/bash
# Build Check for PixiJoy on this arm64 Linux workspace (no Android Studio needed).
# Installs JDK 17 + Android cmdline tools, runs AGP's x86-64 aapt2 through qemu, then unit tests.
# Usage: bash tools/build_check.sh [gradle tasks...]  (default: all JVM unit tests)
set -e
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk} GRADLE_USER_HOME=${GRADLE_USER_HOME:-/opt/gradle-home}
if ! command -v java >/dev/null; then
  apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk-headless unzip wget >/dev/null
fi
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(command -v java))))
if [ ! -x $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager ]; then
  mkdir -p $ANDROID_HOME/cmdline-tools && cd /tmp
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O clt.zip
  unzip -q -o clt.zip -d $ANDROID_HOME/cmdline-tools && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
  yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
fi
cd "$(dirname "$0")/.."
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties
EXTRA=""
if [ "$(uname -m)" = "aarch64" ]; then
  # AGP only ships x86-64 aapt2 for Linux: run it under qemu-user with amd64 glibc.
  if [ ! -x /opt/aapt2 ]; then
    dpkg --add-architecture amd64 && apt-get update -qq >/dev/null
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq qemu-user-static libc6:amd64 libstdc++6:amd64 zlib1g:amd64 >/dev/null
    sh ./gradlew --no-daemon -q :ml:processDebugResources >/dev/null 2>&1 || true
    cp "$(ls -d $GRADLE_USER_HOME/caches/*/transforms/*/transformed/aapt2-*-linux | head -1)/aapt2" /opt/aapt2-x86
    printf '#!/bin/sh\nexec /usr/bin/qemu-x86_64-static /opt/aapt2-x86 "$@"\n' > /opt/aapt2 && chmod +x /opt/aapt2
  fi
  EXTRA="-Pandroid.aapt2FromMavenOverride=/opt/aapt2"
  echo "NOTE: Robolectric tests that load Conscrypt fail on aarch64 hosts (no conscrypt aarch_64 JNI) — environment, not code."
fi
TASKS=${@:-":ml:testDebugUnitTest :movie:testDebugUnitTest :photo:testDebugUnitTest :app:testDebugUnitTest"}
sh ./gradlew --no-daemon --continue $EXTRA $TASKS
