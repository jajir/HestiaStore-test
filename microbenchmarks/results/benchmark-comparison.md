# Microbenchmark Comparison

| File | Method | Score | Unit | Error |
|------|--------|-------|------|-------|
| DiffKeyReaderBenchmark | readKey | 19038.66 | ops/ms | 956.95 |
| DiffKeyReaderBenchmark-arrays | readKey | 15566.18 | ops/ms | 3389.01 |
| DiffKeyReaderBenchmark-original | readKey | 16414.82 | ops/ms | 1607.64 |
| SingleChunkEntryWriterBenchmark-current | put | 7040.91 | ops/ms | 821.9 |
| UniqueCacheBenchmark | get | 929.33 | ops/ms | 20.44 |
| UniqueCacheBenchmark | put | 237.91 | ops/ms | 305.33 |
| UniqueCacheBenchmark-HashMap | get | 13.89 | ops/ms | 20.89 |
| UniqueCacheBenchmark-HashMap | put | 11.31 | ops/ms | 2.88 |
| UniqueCacheBenchmark-TreeMap | get | 408.06 | ops/ms | 38.54 |
| UniqueCacheBenchmark-TreeMap | put | 99.73 | ops/ms | 121.19 |
| UniqueCacheBenchmark-fastUtil | get | 603.19 | ops/ms | 26.48 |
| UniqueCacheBenchmark-fastUtil | put | 168.94 | ops/ms | 192.21 |
