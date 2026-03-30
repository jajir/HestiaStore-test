# HestiaStore-test

This repository contains JMH-based storage benchmarks plus helper scripts that turn raw benchmark JSON files into final Markdown, JSON, and SVG reports.

Raw benchmark files are written into `./results/` with names like:

- `results-*.json` for the JMH output
- `results-*-my.json` for extra local metadata such as disk usage and CPU usage

## Prerequisites

- Java and Maven for building and running the benchmark suite
- Groovy for the report-generation scripts

## Build

Build the project from the repository root before running benchmarks:

```bash
mvn clean package -Dmaven.compiler.useIncrementalCompilation=false -DskipTests=true
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

If you already have raw benchmark results in `./results/`, you do not need to rerun the benchmarks. Generate the final reports directly with:

```bash
./run.sh --reports
```

That shortcut runs the full reporting pipeline:

```bash
./makeJsonTable.sh
./makeSumTable.sh
./makeGraph.sh
./makeMarkDown.sh
./copyReportsToHestiaStore.sh ../HestiaStore
```

What each step does:

- `makeJsonTable.sh` normalizes `results-*.json` files into summary JSON tables.
- `makeSumTable.sh` creates compact Markdown summary tables.
- `makeGraph.sh` creates SVG bar charts for write, read, sequential, multithread read, and multithread write reports when those summary tables exist.
- `makeMarkDown.sh` creates the detailed Markdown reports from the matching templates in `results/`.
- `copyReportsToHestiaStore.sh HESTIASTORE_PROJECT_ROOT` copies the generated Markdown benchmark pages into `docs/why-hestiastore/` and the SVG charts into `docs/images/` inside the target HestiaStore repository.

## Generated Files

After report generation, look in `./results/` for:

- `out-*-table.json` for machine-readable summary tables
- `out-*.md` for detailed Markdown reports
- `out-*-table.md` for compact Markdown tables
- `out-*.svg` for charts when the corresponding summary data is available
- `out-*-test-template.md` in `results/` for detailed report templates

Typical detailed report files are:

- `out-write.md`
- `out-read.md`
- `out-sequential.md`
- `out-multithread-read.md`
- `out-multithread-write.md`

Detailed Markdown reports are rendered from matching templates such as `results/out-write-test-template.md`. The template must contain `{{TABLE}}`.

## Typical Workflow

Run benchmarks and then generate reports:

```bash
mvn clean package -DskipTests=true
BENCHMARK_DIR=/tmp/hestia-bench ./run.sh
./run.sh --reports
./copyReportsToHestiaStore.sh /Users/jan/projects/HestiaStore
```

If you already finished the benchmark run and only want the final reports:

```bash
./run.sh --reports
./copyReportsToHestiaStore.sh /Users/jan/projects/HestiaStore
```
