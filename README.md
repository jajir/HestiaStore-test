# HestiaStore-test

Here are benchmark-oriented tests focused on longer-running scenarios. The project is now a single Maven module in the repository root, so build from the root before running anything:

```bash
mvn clean package
```

## benchmarks

There are comparison of lowlevel functionality like disk IO operatios.

Before each test execution build the jar with executables in the project root:

```bash
mvn clean package -DskipTests=true
```

How to perform test:

```bash

java -Ddir=/Volumes/ponrava/test-index -Dengine=HestiaStore -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

java -Ddir=/Volumes/ponrava/test-index -Dengine=MapDB -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

java -Ddir=/Volumes/ponrava/test-index -Dengine=H2 -cp "target/classes:target/lib/*" org.hestiastore.index.benchmark.plainload.Main

```
