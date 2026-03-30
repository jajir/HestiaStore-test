#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.Field
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

@Field final String TABLE_PLACEHOLDER = '{{TABLE}}'

Path cwd = Paths.get('.')
Path rootDir = findProjectRoot(cwd)
Path resultsDir = rootDir.resolve('results')
ObjectMapper mapper = new ObjectMapper()

Path resolveSummaryJson(Path resultsDir, String reportName) {
    List<Path> candidates = [
            resultsDir.resolve("${reportName}.json"),
            resultsDir.resolve("${reportName}-table.json")
    ]
    Path resolved = candidates.find { Files.exists(it) }
    if (resolved == null) {
        throw new FileNotFoundException(
                "No summary JSON found for ${reportName}. Tried: " +
                        candidates.collect { it.fileName.toString() }.join(', '))
    }
    return resolved
}

Path resolveTemplate(Path resultsDir, String reportName) {
    List<Path> candidates = [
            resultsDir.resolve("${reportName}-test-template.md"),
            resultsDir.resolve("${reportName}-template.md"),
            resultsDir.resolve("${reportName}-templete.md")
    ]
    Path resolved = candidates.find { Files.exists(it) }
    if (resolved == null) {
        throw new FileNotFoundException(
                "No template found for ${reportName}. Tried: " +
                        candidates.collect { it.fileName.toString() }.join(', '))
    }
    return resolved
}

String buildThroughputTableSection(List<Map<String, Object>> rows) {
    StringBuilder out = new StringBuilder()
    out.append('## Benchmark Results\n\n')
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n\n')
    } else {
        boolean includeLatency = rows[0].containsKey('Mean [us/op]')
        if (includeLatency) {
            out.append('| Engine       | Score [ops/s]     | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | Occupied space | CPU Usage |\n')
            out.append('|:-------------|------------------:|-------------:|------------:|------------:|------------:|---------------:|---------:|\n')
            rows.each { row ->
                def engine = (row['Engine'] ?: '').toString()
                def score = (row['Score [ops/s]'] ?: '').toString()
                def mean = (row['Mean [us/op]'] ?: '').toString()
                def p50 = (row['p50 [us/op]'] ?: '').toString()
                def p95 = (row['p95 [us/op]'] ?: '').toString()
                def p99 = (row['p99 [us/op]'] ?: '').toString()
                def occupied = (row['Occupied space'] ?: '').toString()
                def cpuUsage = (row['cpuUsage'] ?: '').toString()
                out.append("| ${engine.padRight(12)} | ${score.padLeft(16)} | ${mean.padLeft(12)} | ${p50.padLeft(11)} | ${p95.padLeft(11)} | ${p99.padLeft(11)} | ${occupied.padRight(14)} | ${cpuUsage.padRight(10)} |\n")
            }
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
        }
        out.append('\n')
    }
    out.append('meaning of columns:\n\n')
    out.append('- Engine: name of the benchmarked engine.\n')
    out.append('- Score [ops/s]: number of operations per second, higher is better.\n')
    if (!rows.isEmpty() && rows[0].containsKey('Mean [us/op]')) {
        out.append('- Mean [us/op]: average per-operation latency in microseconds, lower is better.\n')
        out.append('- p50/p95/p99 [us/op]: latency percentiles from JMH SampleTime results.\n')
    } else {
        out.append('- ScoreError: error margin of the mean score.\n')
        out.append('- Confidence Interval [ops/s]: 95% confidence interval of the mean throughput.\n')
    }
    out.append('- Occupied space: amount of disk space occupied by the engine data.\n')
    out.append('- CPU Usage: average CPU usage during the benchmark.\n')
    return out.toString()
}

String buildMultithreadTableSection(List<Map<String, Object>> rows) {
    StringBuilder out = new StringBuilder()
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
    out.append('- CPU Usage: average CPU usage during the benchmark.\n')
    return out.toString()
}

boolean isMultithreadReport(List<Map<String, Object>> rows, String reportName) {
    return reportName.startsWith('out-multithread-') ||
            (!rows.isEmpty() &&
                    rows[0].containsKey('Threads') &&
                    rows[0].containsKey('Throughput [ops/s]'))
}

String renderTemplate(String template, String tableSection) {
    if (!template.contains(TABLE_PLACEHOLDER)) {
        throw new IllegalArgumentException(
                "Template must contain ${TABLE_PLACEHOLDER}")
    }
    String rendered = template.replace(TABLE_PLACEHOLDER, tableSection.trim())
    return rendered.endsWith('\n') ? rendered : rendered + '\n'
}

if (args.length == 0) {
    System.err.println('Usage: makeMarkDown.groovy REPORT_NAME [REPORT_NAME ...]')
    System.exit(1)
}

args.each { String reportName ->
    Path summaryJson = resolveSummaryJson(resultsDir, reportName)
    Path templatePath = resolveTemplate(resultsDir, reportName)
    Path outputPath = resultsDir.resolve("${reportName}.md")

    List<Map<String, Object>> rows = mapper.readValue(summaryJson.toFile(), List)
    String tableSection = isMultithreadReport(rows, reportName)
            ? buildMultithreadTableSection(rows)
            : buildThroughputTableSection(rows)
    String template = Files.readString(templatePath, StandardCharsets.UTF_8)
    String rendered = renderTemplate(template, tableSection)

    Files.writeString(outputPath, rendered, StandardCharsets.UTF_8)
    println "Wrote ${outputPath}"
}
