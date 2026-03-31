# Benchmark for 'Multithread Write' operation

## Chart

![Multithread write benchmark chart](../images/out-multithread-write.svg)

## Percentile Chart

This chart shows the latency percentile curve for the benchmarked engines. The X axis runs from p50 to p99.99, and the Y axis uses a logarithmic latency scale so tail-latency differences are easier to compare.

![Multithread write latency percentile chart](../images/out-multithread-write-percentiles.svg)

## Test Conditions - Multithread Write Benchmarks

- Multithread write runs reuse the same controlled JVM flags and hardware as the other benchmark suites. Each trial wipes the working directory supplied through the `dir` system property and creates a fresh storage instance before any benchmark thread starts.
- Each benchmark thread performs the same write operation in two JMH modes during the same run: `SampleTime` to capture latency percentiles and `Throughput` to capture aggregate write throughput.
- The configured thread count for this result set is 4 benchmark threads, matching the `threads4` suffix used by the generated result files.
- Every operation generates a pseudo-random key via `HashDataProvider.makeHash(ThreadLocalRandom.current().nextLong())`, so concurrent writers insert independent keys while using the constant payload `"opice skace po stromech"`.
- Warm-up uses 10 iterations of 20 seconds, followed by 25 measurement iterations of 20 seconds, so the results represent sustained concurrent write pressure rather than startup behavior.
- The benchmark focuses on contention and latency under concurrent insert load. There is no preload phase for this suite; the store starts empty at the beginning of each trial.
- After measurements complete, the storage is closed and the resulting directory remains available so the reporting scripts can capture occupied space and CPU usage.
- Test was performed at Mac mini 2024, 16 GB, macOS 15.6.1 (24G90).

{{TABLE}}
