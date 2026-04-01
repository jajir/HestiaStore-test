#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${PROJECT_ROOT}/report-env.sh"

usage() {
  echo "Usage: ./makeAll.sh HESTIASTORE_PROJECT_ROOT"
  echo
  echo "Runs the full report-generation pipeline."
  echo "Generated assets are stored in ./target/ and then copied into"
  echo "the target HestiaStore project in the final step."
  echo
  echo "Example:"
  echo "  ./makeAll.sh /Users/jan/projects/HestiaStore"
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

resolve_local_report_environment "${PROJECT_ROOT}"
resolve_publish_environment "${PROJECT_ROOT}" "${1}"

run_step() {
  local step_number="$1"
  local script_name="$2"
  local description="$3"
  local mode="${4:-normal}"

  echo
  echo "[${step_number}/5] ${script_name}"
  echo "Generates ${description}."
  if [[ "${mode}" == "publish-only" ]]; then
    SKIP_REPORT_GENERATION=1 "${PROJECT_ROOT}/${script_name}" "${REPORT_TARGET_DIR}"
  else
    "${PROJECT_ROOT}/${script_name}"
  fi
}

run_step 1 "makeJsonTable.sh" \
  "normalized summary JSON tables in ${REPORT_BUILD_DIR}"
run_step 2 "makeSumTable.sh" \
  "compact Markdown summary tables in ${REPORT_BUILD_DIR}"
run_step 3 "makeGraph.sh" \
  "SVG charts in ${REPORT_IMAGES_DIR}"
run_step 4 "makeMarkDown.sh" \
  "final Markdown benchmark reports in ${REPORT_DOCS_DIR}"
run_step 5 "copyReportsToHestiaStore.sh" \
  "copied Markdown reports into ${PUBLISH_DOCS_DIR} and charts into ${PUBLISH_IMAGES_DIR}" \
  "publish-only"

echo
echo "Report generation finished."
echo "Build artifacts: ${REPORT_BUILD_DIR}"
echo "Markdown reports: ${REPORT_DOCS_DIR}"
echo "Charts: ${REPORT_IMAGES_DIR}"
