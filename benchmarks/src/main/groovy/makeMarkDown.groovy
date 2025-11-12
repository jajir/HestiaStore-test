#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.*

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

List<Path> jsonFiles = []
if (Files.exists(resultsDir)) {
    try {
        Files.newDirectoryStream(resultsDir, 'results-*.json').each { jsonFiles << it }
    } catch (ignored) {
        Files.list(resultsDir).filter {
            it.fileName.toString().startsWith('results-') && it.fileName.toString().endsWith('.json')
        }.forEach { jsonFiles << it }
    }
}
jsonFiles.sort { a, b -> a.fileName.toString() <=> b.fileName.toString() }

String buildReport(List<Map<String, Object>> rows, Path conditionsPath, List<Path> rawFiles) {
    StringBuilder out = new StringBuilder()
    out.append('# HestiaStore Benchmark Results\n\n')
    if (Files.exists(conditionsPath)) {
        out.append(new String(Files.readAllBytes(conditionsPath), StandardCharsets.UTF_8)).append('\n\n')
    }
    out.append('## Benchmark Results\n\n')
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n\n')
    } else {
        out.append('| Engine       | Score [ops/s]     | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |\n')
        out.append('|:--------------|-----------------:|-----------:|-----------------------------:|---------------:|---------:|\n')
        rows.each { row ->
            def engine = (row['Engine'] ?: '').toString()
            def score = (row['Score [ops/s]'] ?: '').toString()
            def error = (row['ScoreError'] ?: '').toString()
            def ci = (row['Confidence Interval [ops/s]'] ?: '').toString()
            def occupied = (row['Occupied space'] ?: '').toString()
            def cpuUsage = (row['cpuUsage'] ?: '').toString()
            out.append("| ${engine.padRight(12)} | ${score.padLeft(15)} | ${error.padLeft(9)} | ${ci.padRight(27)} | ${occupied.padRight(14)} | ${cpuUsage.padRight(10)} |\n")
        }
        out.append('\n')
    }
    out.append('meaning of columns:\n\n')
    out.append('- Engine: name of the benchmarked engine (as derived from the JSON filename)\n')
    out.append('- Score [ops/s]: number of operations per second (higher is better)\n')
    out.append('- ScoreError: error margin of the score (lower is better). It\'s computed as `z * (stdev / sqrt(n)) where`\n')
    out.append('  - `z` is the z-score for the desired confidence level (1.96 for 95%)\n')
    out.append('  - `stdev` is the standard deviation of the measurements\n')
    out.append('  - `n` is the number of measurements\n')
    out.append('- Confidence Interval: 95% confidence interval of the score (lower and upper bound). This means that the true mean is likely between this interval of ops/sec. Negative values are possible if the error margin is larger than the score itself.\n')
    out.append('- Occupied space : amount of disk space occupied by the engine\'s data structures (lower is better). It is measured after flushing last data to disk.\n')
    out.append('- CPU Usage: average CPU usage during the benchmark (lower is better). Please note, that it includes all system processes, not only the benchmarked engine.\n\n')

    out.append('## Raw JSON Files\n')
    rawFiles.each { Path file ->
        out.append('\n### ').append(file.fileName.toString()).append('\n\n')
        out.append('```json\n').append(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).append('\n```\n')
    }
    return out.toString()
}

List<Path> writeRaw = jsonFiles.findAll { it.fileName.toString().contains('results-write-') }
List<Path> readRaw = jsonFiles.findAll { it.fileName.toString().contains('results-read-') }
List<Path> sequentialRaw = jsonFiles.findAll { it.fileName.toString().contains('results-sequential-') }

List<Map<String, Object>> writeRows = []
List<Map<String, Object>> readRows = []
List<Map<String, Object>> sequentialRows = []
Path writeTable = resultsDir.resolve('out-write-table.json')
Path readTable = resultsDir.resolve('out-read-table.json')
Path sequentialTable = resultsDir.resolve('out-sequential-table.json')
if (Files.exists(writeTable)) {
    writeRows = mapper.readValue(writeTable.toFile(), List)
}
if (Files.exists(readTable)) {
    readRows = mapper.readValue(readTable.toFile(), List)
}
if (Files.exists(sequentialTable)) {
    sequentialRows = mapper.readValue(sequentialTable.toFile(), List)
}

Path writeOutput = resultsDir.resolve('out-write.md')
Path readOutput = resultsDir.resolve('out-read.md')
Path sequentialOutput = resultsDir.resolve('out-sequential.md')
Files.writeString(writeOutput,
        buildReport(writeRows,
                resultsDir.resolve('out-write-test-conditions.md'), writeRaw),
        StandardCharsets.UTF_8)
Files.writeString(readOutput,
        buildReport(readRows,
                resultsDir.resolve('out-read-test-conditions.md'), readRaw),
        StandardCharsets.UTF_8)
Files.writeString(sequentialOutput,
        buildReport(sequentialRows,
                resultsDir.resolve('out-sequential-test-conditions.md'),
                sequentialRaw),
        StandardCharsets.UTF_8)

println "Wrote ${writeOutput}"
println "Wrote ${readOutput}"
println "Wrote ${sequentialOutput}"
