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

{{TABLE}}
