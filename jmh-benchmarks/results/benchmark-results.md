# Benchmark Results

| Engine       | Score [ops/s]     | ScoreError| Confidence Interval [ops/s] |
|:--------------|:-----------------|:-----------|:----------------------------|
| ChronicleMap |     5 457 |       491 | 4 966 .. 5 949 |
| H2           |     4 553 |     5 015 | -462 .. 9 569 |
| HestiaStoreBasic |   177 683 |    92 859 | 84 824 .. 270 542 |
| HestiaStoreCompress |   175 266 |    91 667 | 83 599 .. 266 934 |
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

### results-ChronicleMap.json

```json
[
    {
        "jmhVersion" : "1.37",
        "benchmark" : "org.hestiastore.index.benchmark.plainload.TestChronicleMap.write",
        "mode" : "thrpt",
        "threads" : 1,
        "forks" : 1,
        "jvm" : "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs" : [
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=ChronicleMap"
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
            "score" : 5457.14741479028,
            "scoreError" : 491.49764074324713,
            "scoreConfidence" : [
                4965.649774047033,
                5948.645055533527
            ],
            "scorePercentiles" : {
                "0.0" : 3116.665979119926,
                "50.0" : 5561.639627896674,
                "90.0" : 6139.242475149538,
                "95.0" : 6237.191004658345,
                "99.0" : 6240.687649932694,
                "99.9" : 6240.687649932694,
                "99.99" : 6240.687649932694,
                "99.999" : 6240.687649932694,
                "99.9999" : 6240.687649932694,
                "100.0" : 6240.687649932694
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    6240.687649932694,
                    6229.032165684865,
                    6052.200649040857,
                    6079.382681459319,
                    5996.037869642332,
                    5886.7676987980885,
                    5923.10121928639,
                    5873.664122843153,
                    5723.319834586519,
                    5720.50797642651,
                    5660.306337519573,
                    5626.574717192369,
                    5561.639627896674,
                    5503.58552430377,
                    5479.00238111464,
                    5369.990146009594,
                    5373.825706367314,
                    3116.665979119926,
                    5210.57210215023,
                    5233.8084144428285,
                    5191.641101010117,
                    4965.587898654336,
                    4741.647284078965,
                    5046.295827033852,
                    4622.840455162099
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```

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
            "score" : 177682.87216009997,
            "scoreError" : 92858.92516866824,
            "scoreConfidence" : [
                84823.94699143173,
                270541.7973287682
            ],
            "scorePercentiles" : {
                "0.0" : 30773.970833180825,
                "50.0" : 132752.22092879278,
                "90.0" : 385720.9308353102,
                "95.0" : 431201.5284740148,
                "99.0" : 448504.5597346563,
                "99.9" : 448504.5597346563,
                "99.99" : 448504.5597346563,
                "99.999" : 448504.5597346563,
                "99.9999" : 448504.5597346563,
                "100.0" : 448504.5597346563
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    188491.07290435527,
                    105673.12681055554,
                    99547.35065579806,
                    157258.3768768587,
                    130181.28220733274,
                    132752.22092879278,
                    88122.61725715928,
                    165239.15931413657,
                    279094.63500968576,
                    114604.19953804015,
                    448504.5597346563,
                    30773.970833180825,
                    381670.21096058364,
                    64401.30525237135,
                    304794.5390343551,
                    92168.66499604877,
                    318899.45932907786,
                    78866.47866670632,
                    149268.43331412165,
                    69400.84407941077,
                    144962.7360133756,
                    54755.842703173956,
                    382316.35881494946,
                    69496.56990192072,
                    390827.7888658513
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
            "score" : 175266.23613937016,
            "scoreError" : 91667.29643939182,
            "scoreConfidence" : [
                83598.93969997833,
                266933.532578762
            ],
            "scorePercentiles" : {
                "0.0" : 16635.501455111847,
                "50.0" : 151782.71572091928,
                "90.0" : 415774.9767932164,
                "95.0" : 452469.96806805057,
                "99.0" : 466102.31406427606,
                "99.9" : 466102.31406427606,
                "99.99" : 466102.31406427606,
                "99.999" : 466102.31406427606,
                "99.9999" : 466102.31406427606,
                "100.0" : 466102.31406427606
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    192429.3022958132,
                    181114.28540496845,
                    177451.96000586808,
                    132830.78704138054,
                    159434.92701823832,
                    130127.06765728783,
                    102099.14819613729,
                    310279.5960569462,
                    97704.0309198598,
                    178456.47670261923,
                    151782.71572091928,
                    466102.31406427606,
                    16635.501455111847,
                    420661.1607435246,
                    33532.05366511412,
                    213035.08597689585,
                    79100.11185219321,
                    333436.3725766594,
                    84360.41431639387,
                    135253.32924904427,
                    78937.73978443726,
                    158078.0413982833,
                    54046.06867596903,
                    412517.52082634426,
                    82249.89187996843
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
