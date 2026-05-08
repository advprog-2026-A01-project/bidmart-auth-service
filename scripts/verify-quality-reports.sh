#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "build/reports/tests/test/index.html"
  "build/reports/pmd/main.html"
  "build/reports/pmd/test.html"
  "build/reports/jacoco/test/html/index.html"
  "build/reports/jacoco/test/jacocoTestReport.xml"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "ERROR: required quality report is missing: $file"
    exit 1
  fi
done

echo "OK: all required backend quality reports are available."
