#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
CPSEP=:; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) CPSEP=";" ;; esac
mkdir -p out/tests/local-ip
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -cp "$AJ" -d out/tests/local-ip app/src/io/freemytv/LocalIp.java tests/local-ip/LocalIpTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "out/tests/local-ip${CPSEP}$AJ" io.freemytv.LocalIpTest
