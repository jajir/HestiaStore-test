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

## Benchmark Results

| Engine       | Score [ops/s]     | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |
|:-------------|------------------:|-----------:|-----------------------------:|---------------:|---------:|
| ChronicleMap |        1 707 702 |    82 988 | 1 624 713 .. 1 790 690      | 2.03 GB        | 13%        |
| H2           |          364 687 |    43 577 | 321 110 .. 408 264          | 8 KB           | 14%        |
| HestiaStoreBasic |              592 |        68 | 524 .. 660                  | 507.94 MB      | 15%        |
| HestiaStoreStream |        4 792 777 |   144 132 | 4 648 646 .. 4 936 909      | 283.94 MB      | 12%        |
| LevelDB      |          190 698 |     6 694 | 184 004 .. 197 391          | 363.32 MB      | 10%        |
| MapDB        |            1 528 |       228 | 1 300 .. 1 756              | 1.3 GB         | 5%         |
| RocksDB      |          109 551 |    10 513 | 99 038 .. 120 064           | 324.23 MB      | 10%        |

meaning of columns:

- Engine: name of the benchmarked engine.
- Score [ops/s]: number of operations per second, higher is better.
- ScoreError: error margin of the mean score.
- Confidence Interval [ops/s]: 95% confidence interval of the mean throughput.
- Occupied space: amount of disk space occupied by the engine data.
- CPU Usage: average CPU usage during the benchmark.
