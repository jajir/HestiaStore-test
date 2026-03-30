# Benchmark for 'Sequential read' operation

## Chart

![Sequential read benchmark chart](../images/out-sequential.svg)

## Percentile Chart

This chart shows the latency percentile curve for the benchmarked engines. The X axis runs from p50 to p99.99, and the Y axis uses a logarithmic latency scale so tail-latency differences are easier to compare.

![Sequential read latency percentile chart](../images/out-sequential-percentiles.svg)

## Test Conditions - Sequential Read Benchmarks

- Each sequential scenario uses the same JVM flags, hardware, and scratch directory handling as the write/read suites. The `dir` property is cleaned before every run to guarantee a fresh start.
- Setup writes 10 000 000 deterministic key/value pairs (seed `324432L`) into the engine. Keys are generated via `HashDataProvider` so that the exact ordering is reproducible across runs.
- After preloading, the benchmark resets its sequential cursor. Warm-up iterations walk the keyspace from the first key to the last key so caches and OS I/O buffers reflect streaming access.
- Each run exposes the same single-threaded sequential scan in two JMH modes: `SampleTime` to capture per-operation latency distribution and `Throughput` to capture sustained ordered-read performance.
- The read workload remains single-threaded; each invocation issues exactly one lookup to keep measurements comparable with the other suites.
- Directories remain on disk after the run so disk usage and auxiliary metrics can be collected by reporting scripts.
- Tests for HestiaStoreStream use dedicated stream API. Without using Stream API is performance visible in line HestiaStoreBasic.
- Tests executed on Mac mini 2024, 16 GB RAM, macOS 15.6.1 (24G90).

{{TABLE}}
