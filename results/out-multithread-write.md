# Benchmark for 'Multithread Write' operation

## Chart

![Multithread write benchmark chart](../images/out-multithread-write.svg)

## Test Conditions - Multithread Write Benchmarks

- Multithread write runs reuse the same controlled JVM flags and hardware as the other benchmark suites. Each trial wipes the working directory supplied through the `dir` system property and creates a fresh storage instance before any benchmark thread starts.
- Each benchmark thread performs the same write operation in two JMH modes during the same run: `SampleTime` to capture latency percentiles and `Throughput` to capture aggregate write throughput.
- The configured thread count for this result set is 4 benchmark threads, matching the `threads4` suffix used by the generated result files.
- Every operation generates a pseudo-random key via `HashDataProvider.makeHash(ThreadLocalRandom.current().nextLong())`, so concurrent writers insert independent keys while using the constant payload `"opice skace po stromech"`.
- Warm-up uses 10 iterations of 20 seconds, followed by 25 measurement iterations of 20 seconds, so the results represent sustained concurrent write pressure rather than startup behavior.
- The benchmark focuses on contention and latency under concurrent insert load. There is no preload phase for this suite; the store starts empty at the beginning of each trial.
- After measurements complete, the storage is closed and the resulting directory remains available so the reporting scripts can capture occupied space and CPU usage.
- Test was performed at Mac mini 2024, 16 GB, macOS 15.6.1 (24G90).

## Benchmark Results

| Engine       | Threads | Throughput [ops/s] | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | CPU Usage |
|:-------------|--------:|-------------------:|-------------:|------------:|------------:|------------:|---------:|
| ChronicleMap |       4 |              2 454 |     1 329.18 |       2.748 |   3 416.064 |   6 283.264 | 10%        |
| H2           |       4 |             39 736 |       115.12 |      49.856 |      302.08 |   1 527.808 | 30%        |
| HestiaStoreBasic |       4 |            423 154 |       45.544 |       2.708 |       6.832 |       10.08 | 21%        |
| HestiaStoreCompress |       4 |            329 076 |      250.942 |       2.456 |       5.912 |        13.2 | 14%        |
| MapDB        |       4 |             15 559 |      218.487 |      90.624 |       409.6 |      752.64 | 19%        |
| RocksDB      |       4 |            108 818 |       42.966 |        8.04 |      14.288 |       30.24 | 15%        |

meaning of columns:

- Engine: name of the benchmarked engine.
- Threads: number of concurrent JMH benchmark threads.
- Throughput [ops/s]: aggregate completed operations per second, higher is better.
- Mean [us/op]: average per-operation latency in microseconds, lower is better.
- p50/p95/p99 [us/op]: latency percentiles from JMH SampleTime results.
- CPU Usage: average CPU usage during the benchmark.
