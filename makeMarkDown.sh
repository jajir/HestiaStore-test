#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${PROJECT_ROOT}/results"
INDEX_FILE="${RESULTS_DIR}/benchmark-results.md"

mkdir -p "${RESULTS_DIR}"

groovy "${PROJECT_ROOT}/src/main/groovy/makeMarkDown.groovy"

{
  echo "# Benchmark Reports"
  echo
  echo "Generated from raw benchmark files in \`results/\`."
  echo
  echo "## Detailed Markdown Reports"
  for file in \
    out-write.md \
    out-read.md \
    out-sequential.md \
    out-multithread-read.md \
    out-multithread-write.md; do
    if [[ -f "${RESULTS_DIR}/${file}" ]]; then
      echo "- \`${file}\`"
    fi
  done
  echo
  echo "## Compact Summary Tables"
  for file in \
    out-write-table.md \
    out-read-table.md \
    out-sequential-table.md \
    out-multithread-read-table.md \
    out-multithread-write-table.md; do
    if [[ -f "${RESULTS_DIR}/${file}" ]]; then
      echo "- \`${file}\`"
    fi
  done
  echo
  echo "## Machine-Readable Summaries"
  for file in \
    out-write-table.json \
    out-read-table.json \
    out-sequential-table.json \
    out-multithread-read-table.json \
    out-multithread-write-table.json; do
    if [[ -f "${RESULTS_DIR}/${file}" ]]; then
      echo "- \`${file}\`"
    fi
  done
  echo
  echo "## SVG Charts"
  echo
  for file in \
    out-write.svg \
    out-read.svg \
    out-sequential.svg \
    out-multithread-read.svg \
    out-multithread-write.svg; do
    if [[ -f "${RESULTS_DIR}/${file}" ]]; then
      echo "- \`${file}\`"
    fi
  done
} > "${INDEX_FILE}"

echo "Wrote ${INDEX_FILE}"
