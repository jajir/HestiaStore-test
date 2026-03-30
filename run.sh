#!/bin/bash
#
#  mvn -Dmaven.compiler.useIncrementalCompilation=false clean package
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BENCHMARK_DIR="${BENCHMARK_DIR:-/Volumes/ponrava/test-index}"
BENCHMARK_PRELOAD_ENTRY_COUNT="${BENCHMARK_PRELOAD_ENTRY_COUNT:-10000000}"
BENCHMARK_MISS_PROBABILITY="${BENCHMARK_MISS_PROBABILITY:-0.2}"
YOURKIT_AGENT="${YOURKIT_AGENT:-}"
PROFILE="${PROFILE:-}"
GENERATE_REPORTS="${GENERATE_REPORTS:-}"

usage() {
  echo "Usage: ./run.sh [--profile] [--yourkit-agent PATH]"
  echo "       ./run.sh --reports"
  echo
  echo "This script runs the benchmarks enabled near the bottom of run.sh."
  echo "Raw JMH output is written into ./results/results-*.json and ./results/results-*-my.json."
  echo
  echo "Environment:"
  echo "  BENCHMARK_DIR                  Working directory used by the benchmark engines."
  echo "                                 Default: ${BENCHMARK_DIR}"
  echo "  BENCHMARK_PRELOAD_ENTRY_COUNT  Preloaded entry count for multithread read tests."
  echo "                                 Default: ${BENCHMARK_PRELOAD_ENTRY_COUNT}"
  echo "  BENCHMARK_MISS_PROBABILITY     Miss ratio for multithread read tests."
  echo "                                 Default: ${BENCHMARK_MISS_PROBABILITY}"
  echo "  PROFILE                        Set to 1 to enable YourKit profiling."
  echo "  YOURKIT_AGENT                  Explicit path to libyjpagent."
  echo
  echo "Report generation:"
  echo "  ./makeJsonTable.sh"
  echo "  ./makeSumTable.sh"
  echo "  ./makeGraph.sh"
  echo "  ./makeMarkDown.sh"
  echo
  echo "Shortcut:"
  echo "  ./run.sh --reports"
  echo
  echo "Report outputs:"
  echo "  results/out-*-table.json              Normalized summary data"
  echo "  results/out-*.md                      Detailed Markdown reports"
  echo "  results/out-*-table.md                Compact Markdown tables"
  echo "  results/out-*.svg                     SVG charts when throughput tables exist"
  echo "  results/benchmark-results.md          Landing page listing generated reports"
  echo
  echo "Examples:"
  echo "  ./run.sh"
  echo "  ./run.sh --profile"
  echo "  ./run.sh --profile --yourkit-agent /Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib"
  echo "  BENCHMARK_DIR=/tmp/hestia-bench ./run.sh"
  echo "  ./run.sh --reports"
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
  (
    cd "${PROJECT_ROOT}"
    ./makeJsonTable.sh
    ./makeSumTable.sh
    ./makeGraph.sh
    ./makeMarkDown.sh
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
    org.hestiastore.index.benchmark.plainload.Main
}

# Write benchmarks
#run H2
#run MapDB
#run HestiaStoreBasic
#run HestiaStoreCompressWrite
#run ChronicleMap
#run RocksDB
#run LevelDB

# Read benchmarks
#run H2Read
#run MapDBRead
#run HestiaStoreBasicRead
#run HestiaStoreCompressRead
#run ChronicleMapRead
#run RocksDBRead
#run LevelDBRead

# Sequential read benchmarks
#run H2Sequential
#run MapDBSequential
#run HestiaStoreBasicSequential
#run HestiaStoreCompressSequential
#run HestiaStoreCompressSequential2
#run ChronicleMapSequential
#run RocksDBSequential
#run LevelDBSequential

# Multithread read latency benchmarks with percentile output in JMH JSON.
# Default thread count is 4 for this section.
#run HestiaStoreBasicMultithreadRead 4
#run HestiaStoreCompressMultithreadRead 4
#run H2MultithreadRead 4
#run MapDBMultithreadRead 4
#run ChronicleMapMultithreadRead 4
#run RocksDBMultithreadRead 4
#run LevelDBMultithreadRead 4

# Multithread write latency benchmarks with percentile output in JMH JSON.
# Default thread count is 4 for this section.
#run HestiaStoreBasicMultithreadWrite 4
#run HestiaStoreCompressMultithreadWrite 4
run H2MultithreadWrite 4
run MapDBMultithreadWrite 4
run ChronicleMapMultithreadWrite 4
run RocksDBMultithreadWrite 4
run LevelDBMultithreadWrite 4
