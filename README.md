# HestiaStore-test

here are some test focusing on more complicated long running tasks. There are maven modules focused on specifics tasks. Some pf test run in separate thread. Because of that builded jar have to created. So before any testing run:

```bash
mvn clean package
```

## integration-test

There are some basic test stiing to forcibly interrupt process of creeating index to verify that index stays consistent.

To run tests, run `GracefulDegradationIT` from IDE.

## jmh-benchmarks

There are comparison of lowlevel functionality like disk IO operatios.

before each test execution build jar with executables in correct project directory:

```bash
cd jmh-benchmarks
mvn clean package -DskipTests=true
```

How to perform test:

```bash

java -Ddir=/Volumes/ponrava/test-index -Dengine=HestiaStore -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

java -Ddir=/Volumes/ponrava/test-index -Dengine=MapDB -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

java -Ddir=/Volumes/ponrava/test-index -Dengine=H2 -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

```
