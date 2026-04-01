# Benchmark for 'read' operation

## Chart

![Read benchmark chart](../images/out-read.svg)

## Percentile Chart

This chart shows the latency percentile curve for the benchmarked engines. The X axis runs from p50 to p99.99, and the Y axis uses a logarithmic latency scale so tail-latency differences are easier to compare.

![Read latency percentile chart](../images/out-read-percentiles.svg)

## Test Conditions - Read Benchmarks

- Read-focused runs reuse the same controlled JVM, hardware, and JVM flag configuration as the write suite. Each trial prepares a clean directory pointed to by the `dir` system property before preloading the dataset.
- Setup inserts 10 000 000 deterministic key/value pairs (seed `324432L`) so every engine serves identical data. Keys come from `HashDataProvider`, while values remain the constant string `"opice skace po stromech"`.
- Warm-up iterations issue random lookups (80 % hits, 20 % misses) to trigger JIT compilation, cache population, and to ensure index structures have settled before measurements start.
- Each run exposes the same single-threaded read loop in two JMH modes: `SampleTime` to capture per-operation latency distribution and `Throughput` to capture sustained lookup performance over 20-second windows.
- Each benchmark keeps a consistent random sequence per iteration, ensuring engines experience the same access pattern and allowing apples-to-apples comparisons.
- After measurements finish, readers close their resources but the populated directories remain on disk so sizes can be captured by the reporting scripts.
- Tests executed on Mac mini 2024, 16 GB RAM, macOS 15.6.1 (24G90).

## Data for Throughtput Chart

| Engine | Score [ops/s] | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |
|:-------|--------------:|-----------:|-----------------------------:|---------------:|----------:|
| ChronicleMap |     1 981 013 |    23 056 | 1 957 958 .. 2 004 069 | 2.03 GB | 29% |
| H2 |       506 792 |    14 165 | 492 627 .. 520 957 | 8 KB | 11% |
| HestiaStoreBasic |           818 |         6 | 812 .. 824 | 507.94 MB | 20% |
| HestiaStoreCompress |           749 |        14 | 735 .. 763 | 283.94 MB | 11% |
| LevelDB |       195 086 |     6 474 | 188 612 .. 201 560 | 363.32 MB | 10% |
| MapDB |         1 264 |        70 | 1 194 .. 1 334 | 1.3 GB | 4% |
| RocksDB |       106 795 |     7 214 | 99 580 .. 114 009 | 324.22 MB | 11% |

## Source Data for Percentile Chart

| Engine | p50 [us/op] | p75 [us/op] | p90 [us/op] | p95 [us/op] | p99 [us/op] | p99.5 [us/op] | p99.9 [us/op] | p99.99 [us/op] |
|:-------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| H2 | 1.082 | 1.25 | 1.374 | 1.5 | 1.874 | 2.5 | 3.664 | 11.616 |
| HestiaStoreBasic | 196.864 | 216.576 | 233.216 | 247.808 | 469.504 | 494.592 | 567.296 | 916.398 |
| MapDB | 5.416 | 5.952 | 6.496 | 6.744 | 8.288 | 10 | 351.744 | 643.072 |
