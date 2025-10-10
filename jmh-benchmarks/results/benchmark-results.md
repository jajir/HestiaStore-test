# Benchmark Results

| Engine       | Score [ops/s]     | ScoreError| Confidence Interval [ops/s] |
|:--------------|:-----------------|:-----------|:----------------------------|
| H2           |     4 553 |     5 015 | -462 .. 9 569 |
| HestiaStoreBasic |   178 888 |   117 723 | 61 165 .. 296 611 |
| HestiaStoreCompress |   188 140 |   101 205 | 86 935 .. 289 345 |
| MapDB        |    11 036 |     4 585 | 6 451 .. 15 621 |

meaning of columns:

- Engine: name of the benchmarked engine (as derived from the JSON filename)
- Score [ops/s]: number of operations per second (higher is better)
- ScoreError: error margin of the score (lower is better). It's computed as `z * (stdev / sqrt(n)) where`
  - `z` is the z-score for the desired confidence level (1.96 for 95%)
  - `stdev` is the standard deviation of the measurements
  - `n` is the number of measurements
- Confidence Interval: 95% confidence interval of the score (lower and upper bound). This means that the true mean is likely between this interval of ops/sec. Negative values are possible if the error margin is larger than the score itself.

## Raw JSON Files

### results-H2.json

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "org.hestiastore.index.benchmark.plainload.TestH2.write",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 1,
        "jvm" : "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs" : [
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=H2"
        ],
        "jdkVersion" : "21.0.7",
        "vmName" : "OpenJDK 64-Bit Server VM",
        "vmVersion" : "21.0.7",
        "warmupIterations" : 10,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 25,
        "measurementTime" : "20 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 4553.27604394807,
            "scoreError" : 5015.448548580099,
            "scoreConfidence" : [
                -462.1725046320289,
                9568.724592528168
            ],
            "scorePercentiles" : {
                "0.0" : 443.1719801614942,
                "50.0" : 2050.9548246819836,
                "90.0" : 13550.694776297585,
                "95.0" : 26912.208751227517,
                "99.0" : 31099.611703111263,
                "99.9" : 31099.611703111263,
                "99.99" : 31099.611703111263,
                "99.999" : 31099.611703111263,
                "99.9999" : 31099.611703111263,
                "100.0" : 31099.611703111263
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    31099.611703111263,
                    17141.60186349881,
                    11156.756718163413,
                    5844.7670425337765,
                    3005.9540177318,
                    2040.015291621701,
                    2080.4912123882896,
                    1083.6576905402108,
                    477.9840831695585,
                    8582.533744914519,
                    4923.338732544645,
                    3644.1568664803513,
                    2943.316784670138,
                    1593.2195078599573,
                    443.1719801614942,
                    1505.4228690015284,
                    2106.63470958681,
                    1807.1358933446215,
                    2050.9548246819836,
                    2431.125789723788,
                    1949.6219899699709,
                    1742.6732325355233,
                    1840.9499030633804,
                    1240.882761024833,
                    1095.9218863793842
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```

### results-HestiaStoreBasic.json

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "org.hestiastore.index.benchmark.plainload.TestHestiaStoreBasic.write",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 1,
        "jvm" : "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs" : [
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=HestiaStoreBasic"
        ],
        "jdkVersion" : "21.0.7",
        "vmName" : "OpenJDK 64-Bit Server VM",
        "vmVersion" : "21.0.7",
        "warmupIterations" : 10,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 25,
        "measurementTime" : "20 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 178887.79204286457,
            "scoreError" : 117722.70810578154,
            "scoreConfidence" : [
                61165.083937083036,
                296610.5001486461
            ],
            "scorePercentiles" : {
                "0.0" : 8194.536836690446,
                "50.0" : 112446.34286568612,
                "90.0" : 448312.09938520123,
                "95.0" : 480769.5284606424,
                "99.0" : 484097.3725657068,
                "99.9" : 484097.3725657068,
                "99.99" : 484097.3725657068,
                "99.999" : 484097.3725657068,
                "99.9999" : 484097.3725657068,
                "100.0" : 484097.3725657068
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    112446.34286568612,
                    130055.63629443968,
                    97722.39525856079,
                    134196.56043719364,
                    161737.29759996454,
                    130146.58564382602,
                    62938.589241559785,
                    473004.5588821588,
                    18351.67553959815,
                    484097.3725657068,
                    8194.536836690446,
                    349627.3648069907,
                    49794.72152350557,
                    420263.92808769277,
                    43343.527739870726,
                    378651.26688940625,
                    33846.26104551865,
                    318318.67346914764,
                    83010.19188669018,
                    248400.6340251035,
                    53983.559418234414,
                    111981.0225508783,
                    43382.538512199906,
                    431850.4597205627,
                    92849.10023042752
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```

### results-HestiaStoreCompress.json

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "org.hestiastore.index.benchmark.plainload.TestHestiaStoreCompress.write",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 1,
        "jvm" : "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs" : [
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=HestiaStoreCompress"
        ],
        "jdkVersion" : "21.0.7",
        "vmName" : "OpenJDK 64-Bit Server VM",
        "vmVersion" : "21.0.7",
        "warmupIterations" : 10,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 25,
        "measurementTime" : "20 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 188140.0519346237,
            "scoreError" : 101204.69937956812,
            "scoreConfidence" : [
                86935.3525550556,
                289344.75131419185
            ],
            "scorePercentiles" : {
                "0.0" : 21269.10348910781,
                "50.0" : 158434.78149283008,
                "90.0" : 418188.41092162946,
                "95.0" : 440208.15585060895,
                "99.0" : 443927.32525111595,
                "99.9" : 443927.32525111595,
                "99.99" : 443927.32525111595,
                "99.999" : 443927.32525111595,
                "99.9999" : 443927.32525111595,
                "100.0" : 443927.32525111595
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    194382.80647876597,
                    184020.94160575987,
                    93996.48060699855,
                    165456.51132250277,
                    158434.78149283008,
                    60407.36334622946,
                    73210.37556451834,
                    216597.07458201458,
                    270889.05614325544,
                    135927.6367207479,
                    443927.32525111595,
                    21269.10348910781,
                    431530.0939160927,
                    40736.76974446902,
                    405086.3464998901,
                    44433.82179766358,
                    310961.1589139198,
                    93648.50443826738,
                    198534.16366962015,
                    114289.76895936384,
                    155765.5535271726,
                    39377.609003467514,
                    409293.95559198724,
                    75533.83788974924,
                    365790.25781008316
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```

### results-MapDB.json

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "org.hestiastore.index.benchmark.plainload.TestMapDB.write",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 1,
        "jvm" : "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs" : [
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=MapDB"
        ],
        "jdkVersion" : "21.0.7",
        "vmName" : "OpenJDK 64-Bit Server VM",
        "vmVersion" : "21.0.7",
        "warmupIterations" : 10,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 25,
        "measurementTime" : "20 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 11036.054622770678,
            "scoreError" : 4584.975466188765,
            "scoreConfidence" : [
                6451.079156581914,
                15621.030088959444
            ],
            "scorePercentiles" : {
                "0.0" : 4127.869592911107,
                "50.0" : 9480.258473688486,
                "90.0" : 21865.085334524156,
                "95.0" : 29653.244997156653,
                "99.0" : 32056.20129356556,
                "99.9" : 32056.20129356556,
                "99.99" : 32056.20129356556,
                "99.999" : 32056.20129356556,
                "99.9999" : 32056.20129356556,
                "100.0" : 32056.20129356556
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    32056.20129356556,
                    24046.346972202555,
                    20410.91090940521,
                    12545.193746663872,
                    8760.729712865445,
                    9480.258473688486,
                    8351.582786004437,
                    9086.370563734132,
                    10725.864253423728,
                    12264.658016652342,
                    8629.299107390054,
                    10770.134820908106,
                    12750.096777580342,
                    8102.4216658172445,
                    8345.353869282844,
                    8860.64193414787,
                    9785.604102206158,
                    10814.268691896794,
                    10909.120575644562,
                    10597.728108142523,
                    4127.869592911107,
                    5524.586548624901,
                    5289.70080801502,
                    6882.596913131952,
                    6783.825325361719
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```
