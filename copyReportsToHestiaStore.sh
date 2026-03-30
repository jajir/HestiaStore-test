#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${PROJECT_ROOT}/results"

usage() {
  echo "Usage: ./copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT"
  echo
  echo "Copies generated benchmark Markdown pages into docs/why-hestiastore/"
  echo "and SVG charts into docs/images/ in the target HestiaStore repository."
  echo
  echo "Example:"
  echo "  ./copyReportsToHestiaStore.sh /Users/jan/projects/HestiaStore"
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

INPUT_PATH="${1%/}"
HESTIASTORE_ROOT="${INPUT_PATH}"
DOCS_ROOT="${HESTIASTORE_ROOT}/docs"

if [[ ! -d "${DOCS_ROOT}" ]] && [[ -d "${INPUT_PATH}/why-hestiastore" ]] && [[ -d "${INPUT_PATH}/images" ]]; then
  DOCS_ROOT="${INPUT_PATH}"
  HESTIASTORE_ROOT="$(cd "${INPUT_PATH}/.." && pwd)"
fi

WHY_DIR="${DOCS_ROOT}/why-hestiastore"
IMAGES_DIR="${DOCS_ROOT}/images"

if [[ ! -d "${WHY_DIR}" || ! -d "${IMAGES_DIR}" ]]; then
  echo "Target does not look like a HestiaStore project root or docs directory: ${INPUT_PATH}" >&2
  echo "Expected either <root>/docs/why-hestiastore and <root>/docs/images," >&2
  echo "or a docs directory containing why-hestiastore/ and images/." >&2
  exit 1
fi

mkdir -p "${WHY_DIR}" "${IMAGES_DIR}"

markdown_files=(
  "out-write.md"
  "out-read.md"
  "out-sequential.md"
  "out-multithread-read.md"
  "out-multithread-write.md"
)

copied=0

copy_if_present() {
  local source_file="$1"
  local target_dir="$2"
  local source_path="${RESULTS_DIR}/${source_file}"

  if [[ -f "${source_path}" ]]; then
    install -m 0644 "${source_path}" "${target_dir}/${source_file}"
    echo "Copied ${source_file} -> ${target_dir}/${source_file}"
    copied=$((copied + 1))
  fi
}

for file in "${markdown_files[@]}"; do
  copy_if_present "${file}" "${WHY_DIR}"
done

for source_path in "${RESULTS_DIR}"/out-*.svg; do
  if [[ -f "${source_path}" ]]; then
    file="$(basename "${source_path}")"
    install -m 0644 "${source_path}" "${IMAGES_DIR}/${file}"
    echo "Copied ${file} -> ${IMAGES_DIR}/${file}"
    copied=$((copied + 1))
  fi
done

if [[ "${copied}" -eq 0 ]]; then
  echo "No generated report files were found in ${RESULTS_DIR}." >&2
  echo "Run ./run.sh --reports first." >&2
  exit 1
fi

echo "Copied ${copied} file(s) into ${HESTIASTORE_ROOT}/docs."
