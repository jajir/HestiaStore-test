#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${PROJECT_ROOT}/report-env.sh"

usage() {
  echo "Usage: ./copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT"
  echo
  echo "Copies locally generated report assets from ./target/ into"
  echo "docs/why-hestiastore/ and docs/images/ in the target HestiaStore repository."
  echo
  echo "Example:"
  echo "  ./copyReportsToHestiaStore.sh /Users/jan/projects/HestiaStore"
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

resolve_publish_environment "${PROJECT_ROOT}" "${1}"

if [[ -z "${SKIP_REPORT_GENERATION:-}" ]]; then
  (
    cd "${PROJECT_ROOT}"
    ./makeJsonTable.sh
    ./makeSumTable.sh
    ./makeGraph.sh
    ./makeMarkDown.sh
  )
fi

markdown_count=0
svg_count=0
build_count=0
copied_count=0

for path in "${REPORT_DOCS_DIR}"/out-*.md; do
  if [[ -f "${path}" ]]; then
    markdown_count=$((markdown_count + 1))
    install -m 0644 "${path}" "${PUBLISH_DOCS_DIR}/$(basename "${path}")"
    copied_count=$((copied_count + 1))
  fi
done

for path in "${REPORT_IMAGES_DIR}"/out-*.svg; do
  if [[ -f "${path}" ]]; then
    svg_count=$((svg_count + 1))
    install -m 0644 "${path}" "${PUBLISH_IMAGES_DIR}/$(basename "${path}")"
    copied_count=$((copied_count + 1))
  fi
done

for path in "${REPORT_BUILD_DIR}"/out-*; do
  if [[ -f "${path}" ]]; then
    build_count=$((build_count + 1))
  fi
done

if [[ "${markdown_count}" -eq 0 && "${svg_count}" -eq 0 ]]; then
  echo "No generated report assets were found in ${REPORT_DOCS_DIR} or ${REPORT_IMAGES_DIR}." >&2
  exit 1
fi

echo "Copied ${copied_count} file(s) into ${HESTIASTORE_ROOT}/docs."
echo "Local build artifacts: ${REPORT_BUILD_DIR}"
echo "Local Markdown reports: ${REPORT_DOCS_DIR}"
echo "Local charts: ${REPORT_IMAGES_DIR}"
echo "Published Markdown reports: ${PUBLISH_DOCS_DIR}"
echo "Published charts: ${PUBLISH_IMAGES_DIR}"
