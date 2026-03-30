#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

Path findProjectRoot(Path start) {
    Path cur = start
    Path lastPom = null
    while (cur != null) {
        if (Files.exists(cur.resolve('pom.xml'))) {
            lastPom = cur
        }
        cur = cur.getParent()
    }
    return lastPom ?: start
}

Path cwd = Paths.get('.')
Path rootDir = findProjectRoot(cwd)
Path resultsDir = rootDir.resolve('results')
ObjectMapper mapper = new ObjectMapper()

String readConditions(Path path) {
    return Files.exists(path)
            ? new String(Files.readAllBytes(path), StandardCharsets.UTF_8) + '\n\n'
            : ''
}

String buildThroughputReport(List<Map<String, Object>> rows, Path conditionsPath,
        String titleSuffix) {
    StringBuilder out = new StringBuilder()
    out.append("# HestiaStore Benchmark for '${titleSuffix}' operations\n\n")    
    out.append(readConditions(conditionsPath))
    out.append('## Benchmark Results\n\n')
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n\n')
    } else {
        out.append('| Engine       | Score [ops/s]     | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |\n')
        out.append('|:-------------|------------------:|-----------:|-----------------------------:|---------------:|---------:|\n')
        rows.each { row ->
            def engine = (row['Engine'] ?: '').toString()
            def score = (row['Score [ops/s]'] ?: '').toString()
            def error = (row['ScoreError'] ?: '').toString()
            def ci = (row['Confidence Interval [ops/s]'] ?: '').toString()
            def occupied = (row['Occupied space'] ?: '').toString()
            def cpuUsage = (row['cpuUsage'] ?: '').toString()
            out.append("| ${engine.padRight(12)} | ${score.padLeft(16)} | ${error.padLeft(9)} | ${ci.padRight(27)} | ${occupied.padRight(14)} | ${cpuUsage.padRight(10)} |\n")
        }
        out.append('\n')
    }
    out.append('meaning of columns:\n\n')
    out.append('- Engine: name of the benchmarked engine.\n')
    out.append('- Score [ops/s]: number of operations per second, higher is better.\n')
    out.append('- ScoreError: error margin of the mean score.\n')
    out.append('- Confidence Interval [ops/s]: 95% confidence interval of the mean throughput.\n')
    out.append('- Occupied space: amount of disk space occupied by the engine data.\n')
    out.append('- CPU Usage: average CPU usage during the benchmark.\n\n')
    return out.toString()
}

String buildMultithreadLatencyReport(List<Map<String, Object>> rows,
        Path conditionsPath, String title) {
    StringBuilder out = new StringBuilder()
    out.append('# HestiaStore Benchmark Results\n\n')
    out.append("## ${title}\n\n")
    out.append(readConditions(conditionsPath))
    out.append('## Benchmark Results\n\n')
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n\n')
    } else {
        out.append('| Engine       | Threads | Throughput [ops/s] | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | CPU Usage |\n')
        out.append('|:-------------|--------:|-------------------:|-------------:|------------:|------------:|------------:|---------:|\n')
        rows.each { row ->
            def engine = (row['Engine'] ?: '').toString()
            def threads = (row['Threads'] ?: '').toString()
            def throughput = (row['Throughput [ops/s]'] ?: '').toString()
            def mean = (row['Mean [us/op]'] ?: '').toString()
            def p50 = (row['p50 [us/op]'] ?: '').toString()
            def p95 = (row['p95 [us/op]'] ?: '').toString()
            def p99 = (row['p99 [us/op]'] ?: '').toString()
            def cpuUsage = (row['cpuUsage'] ?: '').toString()
            out.append("| ${engine.padRight(12)} | ${threads.padLeft(7)} | ${throughput.padLeft(18)} | ${mean.padLeft(12)} | ${p50.padLeft(11)} | ${p95.padLeft(11)} | ${p99.padLeft(11)} | ${cpuUsage.padRight(10)} |\n")
        }
        out.append('\n')
    }
    out.append('meaning of columns:\n\n')
    out.append('- Engine: name of the benchmarked engine.\n')
    out.append('- Threads: number of concurrent JMH benchmark threads.\n')
    out.append('- Throughput [ops/s]: aggregate completed operations per second, higher is better.\n')
    out.append('- Mean [us/op]: average per-operation latency in microseconds, lower is better.\n')
    out.append('- p50/p95/p99 [us/op]: latency percentiles from JMH SampleTime results.\n')
    out.append('- CPU Usage: average CPU usage during the benchmark.\n\n')
    return out.toString()
}

List<Map<String, Object>> writeRows = []
List<Map<String, Object>> readRows = []
List<Map<String, Object>> sequentialRows = []
List<Map<String, Object>> multithreadReadRows = []
List<Map<String, Object>> multithreadWriteRows = []
Path writeTable = resultsDir.resolve('out-write-table.json')
Path readTable = resultsDir.resolve('out-read-table.json')
Path sequentialTable = resultsDir.resolve('out-sequential-table.json')
Path multithreadReadTable = resultsDir.resolve('out-multithread-read-table.json')
Path multithreadWriteTable = resultsDir.resolve('out-multithread-write-table.json')
if (Files.exists(writeTable)) {
    writeRows = mapper.readValue(writeTable.toFile(), List)
}
if (Files.exists(readTable)) {
    readRows = mapper.readValue(readTable.toFile(), List)
}
if (Files.exists(sequentialTable)) {
    sequentialRows = mapper.readValue(sequentialTable.toFile(), List)
}
if (Files.exists(multithreadReadTable)) {
    multithreadReadRows = mapper.readValue(multithreadReadTable.toFile(), List)
}
if (Files.exists(multithreadWriteTable)) {
    multithreadWriteRows = mapper.readValue(multithreadWriteTable.toFile(), List)
}

Path writeOutput = resultsDir.resolve('out-write.md')
Path readOutput = resultsDir.resolve('out-read.md')
Path sequentialOutput = resultsDir.resolve('out-sequential.md')
Path multithreadReadOutput = resultsDir.resolve('out-multithread-read.md')
Path multithreadWriteOutput = resultsDir.resolve('out-multithread-write.md')
Files.writeString(writeOutput,
        buildThroughputReport(writeRows,
                resultsDir.resolve('out-write-test-conditions.md'),
                'Write'),
        StandardCharsets.UTF_8)
Files.writeString(readOutput,
        buildThroughputReport(readRows,
                resultsDir.resolve('out-read-test-conditions.md'),
                'Read'),
        StandardCharsets.UTF_8)
Files.writeString(sequentialOutput,
        buildThroughputReport(sequentialRows,
                resultsDir.resolve('out-sequential-test-conditions.md'),
                'Sequential Read'),
        StandardCharsets.UTF_8)
Files.writeString(multithreadReadOutput,
        buildMultithreadLatencyReport(multithreadReadRows,
                resultsDir.resolve('out-multithread-read-test-conditions.md'),
                'Multithread Read Latency'),
        StandardCharsets.UTF_8)
Files.writeString(multithreadWriteOutput,
        buildMultithreadLatencyReport(multithreadWriteRows,
                resultsDir.resolve('out-multithread-write-test-conditions.md'),
                'Multithread Write Latency'),
        StandardCharsets.UTF_8)

println "Wrote ${writeOutput}"
println "Wrote ${readOutput}"
println "Wrote ${sequentialOutput}"
println "Wrote ${multithreadReadOutput}"
println "Wrote ${multithreadWriteOutput}"
