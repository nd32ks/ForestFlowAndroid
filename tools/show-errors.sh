#!/bin/bash
# Prints every compile error on the current branch in one go, grouped by file with counts.
# Usage: tools/show-errors.sh   (run from the project root)
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
LOG=$(mktemp)
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --continue > "$LOG" 2>&1
echo "Branch: $(git branch --show-current)"
if grep -q "Minimum supported Gradle\|Gradle requires JVM" "$LOG"; then grep -E "Minimum supported|Gradle requires" "$LOG"; fi
if grep -q "error: .* not found\|failed linking" "$LOG"; then echo "--- resource (AAPT) errors ---"; grep -E "error:" "$LOG"; fi
echo "--- Kotlin compile errors, by file ---"
grep -E "^e: " "$LOG" | sed -E 's#^e: file://.*/forestflow/##' | awk -F: '{print $1}' | sort | uniq -c
echo "--- distinct messages ---"
grep -E "^e: " "$LOG" | sed -E 's#^e: file://.*/forestflow/##; s#^[^ ]+ ##' | sed -E "s/ on receiver of type 'Modifier'//" | sort | uniq -c | sort -rn
echo "--- first occurrence of each ---"
grep -E "^e: " "$LOG" | sed -E 's#^e: file://.*/forestflow/##' | awk '{k=$0; sub(/^[^ ]+ /,"",k); if(!(k in s)){s[k]=1; print}}'
rm -f "$LOG"
