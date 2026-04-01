#!/bin/bash

resolve_local_report_environment() {
  local project_root="$1"

  local source_results_dir="${BENCHMARK_RESULTS_DIR:-${project_root}/results}"
  local templates_dir="${BENCHMARK_TEMPLATES_DIR:-${project_root}/results}"
  local target_root="${LOCAL_REPORT_TARGET_DIR:-${project_root}/target}"
  local build_dir="${REPORT_BUILD_DIR:-${target_root}/benchmark-report-build}"
  local docs_dir="${REPORT_DOCS_DIR:-${target_root}/docs/why-hestiastore}"
  local images_dir="${REPORT_IMAGES_DIR:-${target_root}/docs/images}"

  if [[ ! -d "${source_results_dir}" ]]; then
    echo "Results directory not found: ${source_results_dir}" >&2
    return 1
  fi

  if [[ ! -d "${templates_dir}" ]]; then
    echo "Template directory not found: ${templates_dir}" >&2
    return 1
  fi

  mkdir -p "${target_root}" "${build_dir}" "${docs_dir}" "${images_dir}"

  export LOCAL_REPORT_TARGET_DIR="${target_root}"
  export BENCHMARK_RESULTS_DIR="${source_results_dir}"
  export BENCHMARK_TEMPLATES_DIR="${templates_dir}"
  export REPORT_BUILD_DIR="${build_dir}"
  export REPORT_DOCS_DIR="${docs_dir}"
  export REPORT_IMAGES_DIR="${images_dir}"
}

resolve_publish_environment() {
  local project_root="$1"
  local requested_target="${2:-${REPORT_TARGET_DIR:-}}"

  resolve_local_report_environment "${project_root}" || return 1

  if [[ -z "${requested_target}" ]]; then
    echo "Missing report target directory." >&2
    echo "Pass the HestiaStore project root/docs directory as the first argument" >&2
    echo "or set REPORT_TARGET_DIR." >&2
    return 1
  fi

  local input_path="${requested_target%/}"
  local hestia_root=""
  local docs_root=""

  if [[ -d "${input_path}/docs" ]]; then
    hestia_root="${input_path}"
    docs_root="${input_path}/docs"
  elif [[ -d "${input_path}/why-hestiastore" ]] && [[ -d "${input_path}/images" ]]; then
    docs_root="${input_path}"
    hestia_root="$(cd "${input_path}/.." && pwd)"
  else
    echo "Target does not look like a HestiaStore project root or docs directory: ${input_path}" >&2
    echo "Expected either <root>/docs or a docs directory containing why-hestiastore/ and images/." >&2
    return 1
  fi

  local publish_docs_dir="${docs_root}/why-hestiastore"
  local publish_images_dir="${docs_root}/images"
  mkdir -p "${publish_docs_dir}" "${publish_images_dir}"

  export REPORT_TARGET_DIR="${input_path}"
  export REPORT_DOCS_ROOT="${docs_root}"
  export PUBLISH_DOCS_DIR="${publish_docs_dir}"
  export PUBLISH_IMAGES_DIR="${publish_images_dir}"
  export HESTIASTORE_ROOT="${hestia_root}"
}
