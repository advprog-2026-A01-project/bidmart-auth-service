#!/usr/bin/env bash
set -euo pipefail

REPORT_XML="${REPORT_XML:-build/reports/jacoco/test/jacocoTestReport.xml}"

if [[ ! -f "$REPORT_XML" ]]; then
  echo "ERROR: JaCoCo XML report not found at $REPORT_XML"
  exit 1
fi

python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path

report = Path("build/reports/jacoco/test/jacocoTestReport.xml")
root = ET.parse(report).getroot()

def pct(covered: int, missed: int) -> float:
    total = covered + missed
    if total == 0:
        return 100.0
    return covered * 100.0 / total

print("JaCoCo Coverage Summary")
print("=======================")

for counter in root.findall("counter"):
    counter_type = counter.attrib["type"]
    missed = int(counter.attrib["missed"])
    covered = int(counter.attrib["covered"])
    print(f"{counter_type:12s}: {pct(covered, missed):6.2f}%  covered={covered} missed={missed}")
PY
