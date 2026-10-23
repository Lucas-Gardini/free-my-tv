#!/usr/bin/env bash
# Host-side regression tests: real HomeMode, fake Android context and ADB transport.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/tests/classes
if [ ! -f out/tests/json.jar ]; then
  curl -fsSL https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar -o out/tests/json.jar
fi
printf '%s  %s\n' 3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed out/tests/json.jar | sha256sum -c -
CPSEP=:; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) CPSEP=";" ;; esac
FMTV_TEST_BASH="$(command -v bash)"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) FMTV_TEST_BASH="$(cygpath -m "$FMTV_TEST_BASH")" ;; esac
export FMTV_TEST_BASH
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -cp out/tests/json.jar -d out/tests/classes app/src/io/freemytv/HomeMode.java $(find tests/home-mode -name '*.java')
"${JAVA_HOME:+$JAVA_HOME/bin/}java" ${TEST_JAVA_OPTS:-} -cp "out/tests/classes${CPSEP}out/tests/json.jar" io.freemytv.HomeModeTest
