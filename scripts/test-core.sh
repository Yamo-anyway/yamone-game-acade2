#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
compiler=(javac)
if ! command -v javac >/dev/null 2>&1; then
  compiler=(java com.sun.tools.javac.Main)
fi
"${compiler[@]}" -encoding UTF-8 -d "$test_dir" \
  app/src/main/java/com/yamone/arcade2/core/*.java \
  app/src/main/java/com/yamone/arcade2/data/RankingGateway.java \
  tests/CoreTests.java
java -cp "$test_dir" CoreTests
