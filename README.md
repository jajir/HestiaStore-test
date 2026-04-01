# HestiaStore-test

This repository contains JMH-based storage benchmarks plus helper scripts that turn raw benchmark JSON files into final Markdown, JSON, and SVG reports published into a target HestiaStore project.

The plain-load write, read, and sequential suites emit both `SampleTime`
latency and `Throughput` results in the same raw JMH JSON files so reports can
combine sustained ops/s with percentile latency data.

Raw benchmark files are written into `./results/` with names like:

- `results-*.json` for the JMH output
- `results-*-my.json` for extra local metadata such as disk usage and CPU usage

## Prerequisites

- Java and Maven for building and running the benchmark suite
- Groovy for the report-generation scripts

## Build

Build the project from the repository root before running benchmarks:

```bash
mvn clean package -DskipTests=true
```

## Run Benchmarks

The main benchmark runner is `./run.sh`.

```bash
./run.sh
```

`run.sh` executes the benchmark calls that are currently enabled near the bottom of the script. Comment or uncomment the `run ...` lines there to choose which benchmark families should run.

Useful environment variables:

```bash
BENCHMARK_DIR=/tmp/hestia-bench ./run.sh
BENCHMARK_PRELOAD_ENTRY_COUNT=10000000 ./run.sh
BENCHMARK_MISS_PROBABILITY=0.2 ./run.sh
PROFILE=1 ./run.sh
```

Profiling examples:

```bash
./run.sh --profile
./run.sh --profile --yourkit-agent /Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib
```

To see the full runner help:

```bash
./run.sh --help
```

## Generate Final Reports

If you already have raw benchmark results in `./results/`, you do not need to rerun the benchmarks. Generate the final reports with:

```bash
./makeAll.sh /Users/jan/projects/HestiaStore
```

Or, if you prefer the existing runner entrypoint:

```bash
./run.sh --reports --target /Users/jan/projects/HestiaStore
```

That shortcut runs the full reporting pipeline:

```bash
./makeAll.sh /Users/jan/projects/HestiaStore

# equivalent manual steps:
./makeJsonTable.sh
./makeSumTable.sh
./makeGraph.sh
./makeMarkDown.sh
./copyReportsToHestiaStore.sh /Users/jan/projects/HestiaStore
```

What each step does:

- `makeJsonTable.sh` normalizes `results-*.json` files into summary JSON tables in `./target/benchmark-report-build/`.
- `makeSumTable.sh` creates compact Markdown summary tables in `./target/benchmark-report-build/`.
- `makeGraph.sh` creates SVG charts in `./target/docs/images/`.
- `makeMarkDown.sh` creates the detailed Markdown reports in `./target/docs/why-hestiastore/` from the matching templates in `results/`.
- `copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT` copies the generated Markdown pages and SVG charts from local `./target/` into the target HestiaStore repository; when run standalone, it can still execute the local generation steps first.
- `makeAll.sh HESTIASTORE_PROJECT_ROOT` runs all five report steps in order and prints what each step produces.

## Generated Files

After local generation, look in `./target/` in this repository for:

- `target/benchmark-report-build/out-*-table.json` for machine-readable summary tables
- `target/benchmark-report-build/out-*-table.md` for primary Markdown tables used by `{{TABLE}}`
- `target/benchmark-report-build/out-*-table2.md` for secondary Markdown tables used by `{{TABLE1}}`
- `target/docs/why-hestiastore/out-*.md` for detailed Markdown reports
- `target/docs/images/out-*.svg` for charts when the corresponding summary data is available
- `results/out-*-test-template.md` in this repository for detailed report templates

After the copy step, the published files are also available in the target HestiaStore project under:

- `docs/why-hestiastore/out-*.md`
- `docs/images/out-*.svg`

Typical detailed report files are:

- `out-write.md`
- `out-read.md`
- `out-sequential.md`
- `out-multithread-read.md`
- `out-multithread-write.md`

Detailed Markdown reports are rendered from matching templates such as `results/out-write-test-template.md`. The template must contain `{{TABLE}}`, which is filled from `target/benchmark-report-build/out-*-table.md`, and may also contain `{{TABLE1}}`, which is filled from `target/benchmark-report-build/out-*-table2.md`.

## Typical Workflow

Run benchmarks and then generate reports:

```bash
mvn clean package -DskipTests=true
BENCHMARK_DIR=/tmp/hestia-bench ./run.sh
./makeAll.sh /Users/jan/projects/HestiaStore
./run.sh --reports --target /Users/jan/projects/HestiaStore
```

If you already finished the benchmark run and only want the final reports:

```bash
./makeAll.sh /Users/jan/projects/HestiaStore
./run.sh --reports --target /Users/jan/projects/HestiaStore
```
