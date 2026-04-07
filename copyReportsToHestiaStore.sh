#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${PROJECT_ROOT}/report-env.sh"

usage() {
  echo "Usage: ./copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT"
  echo
  echo "Synchronizes locally generated report assets from ./target/ into"
  echo "the canonical benchmark docs and images in the target HestiaStore repository."
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

sync_script="${HESTIASTORE_ROOT}/benchmarks/scripts/sync_benchmark_docs.py"

if [[ ! -f "${sync_script}" ]]; then
  echo "Benchmark doc sync script not found: ${sync_script}" >&2
  exit 1
fi

if ! compgen -G "${REPORT_DOCS_DIR}/out-*.md" > /dev/null \
    && ! compgen -G "${REPORT_IMAGES_DIR}/out-*.svg" > /dev/null; then
  echo "No generated report assets were found in ${REPORT_DOCS_DIR} or ${REPORT_IMAGES_DIR}." >&2
  exit 1
fi

python3 "${sync_script}" \
  --source-root "${LOCAL_REPORT_TARGET_DIR}" \
  --target-root "${HESTIASTORE_ROOT}"

echo "Synchronized benchmark docs and charts into ${HESTIASTORE_ROOT}/docs."
echo "Local build artifacts: ${REPORT_BUILD_DIR}"
echo "Local Markdown reports: ${REPORT_DOCS_DIR}"
echo "Local charts: ${REPORT_IMAGES_DIR}"
echo "Published Markdown reports: ${PUBLISH_DOCS_DIR}"
echo "Published charts: ${PUBLISH_IMAGES_DIR}"
