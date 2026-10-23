#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/tests/live-wallpaper
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -d out/tests/live-wallpaper app/src/io/freemytv/LiveWallpaper.java $(find tests/live-wallpaper -name '*.java')
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp out/tests/live-wallpaper io.freemytv.LiveWallpaperTest
