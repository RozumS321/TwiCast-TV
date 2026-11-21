#!/usr/bin/env bash
# Checks only what changed since the base branch (default origin/main), plus uncommitted work.
#   scripts/check.sh            unit tests of the touched areas + debug build
#   scripts/check.sh --release  the same + release build (before merging to main)
#   scripts/check.sh --all      every test, lint and the release build
# All test sources are still compiled, so a change that breaks another area's tests still fails the build;
# only running them is skipped. Lint runs when resources, the manifest or Gradle files changed (or with --all).
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"

base="origin/main"; release=false; all=false
for arg in "$@"; do
  case "$arg" in
    --release) release=true ;;
    --all) all=true; release=true ;;
    *) base="$arg" ;;
  esac
done

changed=$( { git diff --name-only "$base"...HEAD; git diff --name-only HEAD; git ls-files --others --exclude-standard; } | sort -u)
pkg_root="app/src/main/java/app/twicast/tv"
test_root="app/src/test/java/app/twicast/tv"

filters=()
run_all=$all
lint=$all
while IFS= read -r f; do
  [ -z "$f" ] && continue
  case "$f" in
    "$pkg_root"/feature/*|"$pkg_root"/core/*)
      # feature/home/... -> app.twicast.tv.feature.home.*
      area=$(echo "${f#"$pkg_root"/}" | cut -d/ -f1-2 | tr / .)
      filters+=("app.twicast.tv.$area.*") ;;
    "$pkg_root"/data/*) filters+=("app.twicast.tv.data.*") ;;
    "$test_root"/testing/*|"$pkg_root"/*) run_all=true ;;
    "$test_root"/*)
      cls=$(echo "${f#app/src/test/java/}" | sed 's/\.kt$//' | tr / .)
      filters+=("$cls") ;;
    app/src/main/res/*|app/src/main/AndroidManifest.xml) lint=true ;;
    *.gradle.kts|gradle/*|gradle.properties) run_all=true; lint=true ;;
  esac
done <<< "$changed"

tasks=(compileDebugUnitTestKotlin)
# The test task goes last: Gradle takes --tests only right after it.
tests=()
if $run_all; then
  tests=(testDebugUnitTest); echo "Tests: all"
elif [ ${#filters[@]} -gt 0 ]; then
  tests=(testDebugUnitTest)
  for t in $(printf '%s\n' "${filters[@]}" | sort -u); do tests+=(--tests "$t"); done
  echo "Tests: $(printf '%s\n' "${filters[@]}" | sort -u | tr '\n' ' ')"
else
  echo "Tests: none touched"
fi
$lint && tasks+=(lintDebug) && echo "Lint: yes" || echo "Lint: skipped"
$release && tasks+=(assembleRelease) || tasks+=(assembleDebug)

./gradlew -q "${tasks[@]}" ${tests[@]+"${tests[@]}"}
echo "OK"
