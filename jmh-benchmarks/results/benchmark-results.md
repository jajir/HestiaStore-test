# Benchmark Results

| Engine       | Score     | Error     | Confidence Interval        |
|--------------|-----------|-----------|----------------------------|
| H2           |   144 814 |   306 568 | -161 754 .. 451 382 |
| HestiaStore  |   128 180 |   204 514 | -76 334 .. 332 694 |
| HestiaStoreCompress |   196 800 |   141 524 | 55 276 .. 338 324 |
| MapDB        |    30 589 |    11 558 | 19 030 .. 42 147 |

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
        "warmupIterations" : 5,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "10 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 144813.95043471685,
            "scoreError" : 306567.79047940497,
            "scoreConfidence" : [
                -161753.8400446881,
                451381.74091412185
            ],
            "scorePercentiles" : {
                "0.0" : 56025.77535651829,
                "50.0" : 179248.89897164569,
                "90.0" : 217952.95293744546,
                "95.0" : 217952.95293744546,
                "99.0" : 217952.95293744546,
                "99.9" : 217952.95293744546,
                "99.99" : 217952.95293744546,
                "99.999" : 217952.95293744546,
                "99.9999" : 217952.95293744546,
                "100.0" : 217952.95293744546
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    208766.5396138438,
                    217952.95293744546,
                    179248.89897164569,
                    62075.58529413107,
                    56025.77535651829
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```

### results-HestiaStore.json

```json
[
    {
        "jmhVersion": "1.37",
        "benchmark": "org.hestiastore.index.benchmark.plainload.TestHestiaStore.write",
        "mode": "thrpt",
        "threads": 1,
        "forks": 1,
        "jvm": "/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home/bin/java",
        "jvmArgs": [
            "-Ddir=/Volumes/ponrava/test-index",
            "-Dengine=HestiaStore"
        ],
        "jdkVersion": "21.0.7",
        "vmName": "OpenJDK 64-Bit Server VM",
        "vmVersion": "21.0.7",
        "warmupIterations": 5,
        "warmupTime": "20 s",
        "warmupBatchSize": 1,
        "measurementIterations": 5,
        "measurementTime": "10 s",
        "measurementBatchSize": 1,
        "primaryMetric": {
            "score": 128179.98993479079,
            "scoreError": 204514.33711346227,
            "scoreConfidence": [
                -76334.34717867148,
                332694.32704825304
            ],
            "scorePercentiles": {
                "0.0": 66155.4282046176,
                "50.0": 119368.49281223565,
                "90.0": 212849.27879291622,
                "95.0": 212849.27879291622,
                "99.0": 212849.27879291622,
                "99.9": 212849.27879291622,
                "99.99": 212849.27879291622,
                "99.999": 212849.27879291622,
                "99.9999": 212849.27879291622,
                "100.0": 212849.27879291622
            },
            "scoreUnit": "ops/s",
            "rawData": [
                [
                    128125.0261532755,
                    114401.72371090889,
                    66155.4282046176,
                    119368.49281223565,
                    212849.27879291622
                ]
            ]
        },
        "secondaryMetrics": {}
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
        "warmupIterations" : 5,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "10 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 196800.14323363398,
            "scoreError" : 141523.78088347267,
            "scoreConfidence" : [
                55276.362350161304,
                338323.92411710665
            ],
            "scorePercentiles" : {
                "0.0" : 157659.28125959806,
                "50.0" : 200191.72257340542,
                "90.0" : 249485.62156333236,
                "95.0" : 249485.62156333236,
                "99.0" : 249485.62156333236,
                "99.9" : 249485.62156333236,
                "99.99" : 249485.62156333236,
                "99.999" : 249485.62156333236,
                "99.9999" : 249485.62156333236,
                "100.0" : 249485.62156333236
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    249485.62156333236,
                    200191.72257340542,
                    166646.49202976024,
                    157659.28125959806,
                    210017.59874207372
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
        "warmupIterations" : 5,
        "warmupTime" : "20 s",
        "warmupBatchSize" : 1,
        "measurementIterations" : 5,
        "measurementTime" : "10 s",
        "measurementBatchSize" : 1,
        "primaryMetric" : {
            "score" : 30588.733477131093,
            "scoreError" : 11558.357382774302,
            "scoreConfidence" : [
                19030.37609435679,
                42147.090859905395
            ],
            "scorePercentiles" : {
                "0.0" : 25811.634042438855,
                "50.0" : 31994.276084638695,
                "90.0" : 33419.6574651496,
                "95.0" : 33419.6574651496,
                "99.0" : 33419.6574651496,
                "99.9" : 33419.6574651496,
                "99.99" : 33419.6574651496,
                "99.999" : 33419.6574651496,
                "99.9999" : 33419.6574651496,
                "100.0" : 33419.6574651496
            },
            "scoreUnit" : "ops/s",
            "rawData" : [
                [
                    32100.733938775422,
                    33419.6574651496,
                    31994.276084638695,
                    29617.36585465289,
                    25811.634042438855
                ]
            ]
        },
        "secondaryMetrics" : {
        }
    }
]



```
