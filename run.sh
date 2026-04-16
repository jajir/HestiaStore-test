#!/bin/bash
#
#  mvn clean package
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BENCHMARK_DIR="${BENCHMARK_DIR:-/Volumes/ponrava/test-index}"
BENCHMARK_PRELOAD_ENTRY_COUNT="${BENCHMARK_PRELOAD_ENTRY_COUNT:-10000000}"
BENCHMARK_MISS_PROBABILITY="${BENCHMARK_MISS_PROBABILITY:-0.2}"
YOURKIT_AGENT="${YOURKIT_AGENT:-}"
PROFILE="${PROFILE:-}"
GENERATE_REPORTS="${GENERATE_REPORTS:-}"
REPORT_TARGET_DIR="${REPORT_TARGET_DIR:-}"

usage() {
  echo "Usage: ./run.sh [--profile] [--yourkit-agent PATH]"
  echo "       ./run.sh --reports --target HESTIASTORE_PROJECT_ROOT"
  echo
  echo "This script runs the benchmarks enabled near the bottom of run.sh."
  echo "Raw JMH output is written into ./results/results-*-latency.json, ./results/results-*-throughput.json,"
  echo "and matching ./results/results-*-latency-my.json / ./results/results-*-throughput-my.json files."
  echo
  echo "Environment:"
  echo "  BENCHMARK_DIR                  Working directory used by the benchmark engines."
  echo "                                 Default: ${BENCHMARK_DIR}"
  echo "  BENCHMARK_PRELOAD_ENTRY_COUNT  Preloaded entry count for multi-thread read tests."
  echo "                                 Default: ${BENCHMARK_PRELOAD_ENTRY_COUNT}"
  echo "  BENCHMARK_MISS_PROBABILITY     Miss ratio for multi-thread read tests."
  echo "                                 Default: ${BENCHMARK_MISS_PROBABILITY}"
  echo "  PROFILE                        Set to 1 to enable YourKit profiling."
  echo "  YOURKIT_AGENT                  Explicit path to libyjpagent."
  echo
  echo "Report generation:"
  echo "  ./makeJsonTable.sh"
  echo "  ./makeSumTable.sh"
  echo "  ./makeGraph.sh"
  echo "  ./makeMarkDown.sh"
  echo "  ./copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT"
  echo "  ./makeAll.sh HESTIASTORE_PROJECT_ROOT"
  echo
  echo "Shortcut:"
  echo "  ./run.sh --reports --target /Users/jan/projects/HestiaStore"
  echo
  echo "Report outputs:"
  echo "  ./target/benchmark-report-build/out-*-table.json"
  echo "                                       Normalized summary data"
  echo "  ./target/benchmark-report-build/out-*-table.md"
  echo "                                       Primary Markdown tables for {{TABLE}}"
  echo "  ./target/benchmark-report-build/out-*-table2.md"
  echo "                                       Secondary Markdown tables for {{TABLE1}}"
  echo "  ./target/docs/why-hestiastore/out-*.md"
  echo "                                       Detailed Markdown reports"
  echo "  ./target/docs/images/out-*.svg"
  echo "                                       SVG charts"
  echo "  <target>/docs/why-hestiastore/out-*.md"
  echo "                                       Published Markdown reports after copy"
  echo "  <target>/docs/images/out-*.svg"
  echo "                                       Published SVG charts after copy"
  echo
  echo "Examples:"
  echo "  ./run.sh"
  echo "  ./run.sh --profile"
  echo "  ./run.sh --profile --yourkit-agent /Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib"
  echo "  BENCHMARK_DIR=/tmp/hestia-bench ./run.sh"
  echo "  ./run.sh --reports --target /Users/jan/projects/HestiaStore"
  echo "  PROFILE=1 ./run.sh"
}

require_groovy() {
  if ! command -v groovy >/dev/null 2>&1; then
    echo "Groovy is required for report generation." >&2
    exit 1
  fi
}

generate_reports() {
  require_groovy
  if [[ -z "${REPORT_TARGET_DIR}" ]]; then
    echo "Missing report target. Use --target HESTIASTORE_PROJECT_ROOT or set REPORT_TARGET_DIR." >&2
    exit 1
  fi
  (
    cd "${PROJECT_ROOT}"
    ./makeAll.sh "${REPORT_TARGET_DIR}"
  )
}

resolve_default_yourkit_agent() {
  local candidates=(
    "/Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib"
    "/Applications/YourKit-Java-Profiler.app/Contents/Resources/bin/mac/libyjpagent.dylib"
  )
  local candidate

  for candidate in "${candidates[@]}"; do
    if [[ -f "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done

  return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      PROFILE=1
      shift
      ;;
    --yourkit-agent)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --yourkit-agent" >&2
        usage >&2
        exit 1
      fi
      YOURKIT_AGENT="$2"
      shift 2
      ;;
    --reports)
      GENERATE_REPORTS=1
      shift
      ;;
    --target)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for --target" >&2
        usage >&2
        exit 1
      fi
      REPORT_TARGET_DIR="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -n "${GENERATE_REPORTS}" ]]; then
  generate_reports
  exit 0
fi

if [[ -z "${YOURKIT_AGENT}" ]]; then
  YOURKIT_AGENT="$(resolve_default_yourkit_agent || true)"
fi

java_args=()
if [[ -n "${PROFILE}" ]]; then
  if [[ -z "${YOURKIT_AGENT}" ]]; then
    echo "Profiling requested, but no YourKit agent library was found." >&2
    echo "Set YOURKIT_AGENT or pass --yourkit-agent PATH." >&2
    exit 1
  fi
  if [[ ! -f "${YOURKIT_AGENT}" ]]; then
    echo "YourKit agent library not found: ${YOURKIT_AGENT}" >&2
    exit 1
  fi
  echo "profiling enabled"
  echo "using YourKit agent: ${YOURKIT_AGENT}"
  java_args+=("-agentpath:${YOURKIT_AGENT}=exceptions=disable,delay=10000,listen=all")
fi

#	-agentpath:/Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=exceptions=disable,delay=10000,listen=all \


run() {
  local engine="$1"
  local threads="${2:-1}"

  java \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
    -Xmx10000m \
    -Ddir="${BENCHMARK_DIR}" \
    -Dengine="${engine}" \
    -DbenchmarkThreads="${threads}" \
    -DbenchmarkPreloadEntryCount="${BENCHMARK_PRELOAD_ENTRY_COUNT}" \
    -DbenchmarkMissProbability="${BENCHMARK_MISS_PROBABILITY}" \
    "${java_args[@]+"${java_args[@]}"}" \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.index.benchmark.runner.BenchmarkMain
}

# Single-thread write benchmarks
write_single_thread_benchmarks(){
    run writeSingleThreadH2
    run writeSingleThreadMapDB
    run writeSingleThreadHestiaStoreBasic
    run writeSingleThreadHestiaStoreCompress
    run writeSingleThreadChronicleMap
    run writeSingleThreadRocksDB
    run writeSingleThreadLevelDB
}

# Single-thread read benchmarks
read_single_thread_benchmarks(){
    run readSingleThreadH2
    run readSingleThreadMapDB
    run readSingleThreadHestiaStoreBasic
    run readSingleThreadHestiaStoreCompress
    run readSingleThreadChronicleMap
    run readSingleThreadRocksDB
    run readSingleThreadLevelDB
}

# Sequential read benchmarks
sequential_read_benchmarks(){
    run sequentialReadH2
    run sequentialReadMapDB
    run sequentialReadHestiaStoreBasic
    run sequentialReadHestiaStoreCompress
    run sequentialReadHestiaStoreStream
    run sequentialReadChronicleMap
    run sequentialReadRocksDB
    run sequentialReadLevelDB
}

# Multi-thread read latency benchmarks with percentile output in JMH JSON.
# Default thread count is 4 for this section.
read_multi_thread_benchmarks(){
    run readMultiThreadHestiaStoreBasic 4
    run readMultiThreadHestiaStoreCompress 4
    run readMultiThreadH2 4
    run readMultiThreadMapDB 4
    run readMultiThreadChronicleMap 4
    run readMultiThreadRocksDB 4
    run readMultiThreadLevelDB 4
}

# Multi-thread write latency benchmarks with percentile output in JMH JSON.
# Default thread count is 4 for this section.
write_multi_thread_benchmarks(){
    run writeMultiThreadHestiaStoreBasic 4
    run writeMultiThreadHestiaStoreCompress 4
    run writeMultiThreadH2 4
    run writeMultiThreadMapDB 4
    run writeMultiThreadChronicleMap 4
    run writeMultiThreadRocksDB 4
    run writeMultiThreadLevelDB 4
}

HestiaStore_benchmarks(){
    #run readMultiThreadHestiaStoreBasic 4
    #run readMultiThreadHestiaStoreCompress 4

    run writeMultiThreadHestiaStoreBasic 4
    run writeMultiThreadHestiaStoreCompress 4

    #run sequentialReadHestiaStoreBasic
    #run sequentialReadHestiaStoreCompress
    #run sequentialReadHestiaStoreStream
    #run readSingleThreadHestiaStoreBasic
    #run readSingleThreadHestiaStoreCompress

    #run writeSingleThreadHestiaStoreBasic
    #run writeSingleThreadHestiaStoreCompress
}

#write_single_thread_benchmarks
#ead_single_thread_benchmarks
#sequential_read_benchmarks
#read_multi_thread_benchmarks
#write_multi_thread_benchmarks

HestiaStore_benchmarks