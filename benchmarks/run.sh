#!/bin/bash

set -euo pipefail

BENCHMARK_DIR="${BENCHMARK_DIR:-/Volumes/ponrava/test-index}"
BENCHMARK_PRELOAD_ENTRY_COUNT="${BENCHMARK_PRELOAD_ENTRY_COUNT:-10000000}"
BENCHMARK_MISS_PROBABILITY="${BENCHMARK_MISS_PROBABILITY:-0.2}"
YOURKIT_AGENT="${YOURKIT_AGENT:-}"

java_args=()
if [[ -n "${YOURKIT_AGENT}" ]]; then
  java_args+=("-agentpath:${YOURKIT_AGENT}=exceptions=disable,delay=10000,listen=all")
fi

run() {
  local engine="$1"
  local threads="${2:-1}"

  java \
    -Ddir="${BENCHMARK_DIR}" \
    -Dengine="${engine}" \
    -DbenchmarkThreads="${threads}" \
    -DbenchmarkPreloadEntryCount="${BENCHMARK_PRELOAD_ENTRY_COUNT}" \
    -DbenchmarkMissProbability="${BENCHMARK_MISS_PROBABILITY}" \
    "${java_args[@]}" \
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
run HestiaStoreBasicMultithreadRead 4
#run HestiaStoreCompressMultithreadRead 4
#run H2MultithreadRead 4
#run MapDBMultithreadRead 4
#run ChronicleMapMultithreadRead 4
#run RocksDBMultithreadRead 4
#run LevelDBMultithreadRead 4
