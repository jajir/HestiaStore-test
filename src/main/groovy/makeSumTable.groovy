#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

Path findProjectRoot(Path start) {
    Path cur = start
    Path lastPom = null
    while (cur != null) {
        if (Files.exists(cur.resolve("pom.xml"))) {
            lastPom = cur
        }
        cur = cur.parent
    }
    return lastPom ?: start
}

Path resolveDirectoryFromEnv(String envName, Path fallback) {
    String raw = System.getenv(envName)
    if (raw == null || raw.trim().isEmpty()) {
        return fallback
    }
    return Paths.get(raw).toAbsolutePath().normalize()
}

Path cwd = Paths.get(".").toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)
Path rawResultsDir = resolveDirectoryFromEnv("BENCHMARK_RESULTS_DIR",
        rootDir.resolve("results"))
Path outputDir = resolveDirectoryFromEnv("REPORT_BUILD_DIR", rawResultsDir)

if (!Files.isDirectory(rawResultsDir)) {
    System.err.println("Results directory not found: ${rawResultsDir}")
    System.exit(1)
}
Files.createDirectories(outputDir)

Path writeTable = outputDir.resolve("out-write-single-thread-table.json")
Path readTable = outputDir.resolve("out-read-single-thread-table.json")
Path sequentialTable = outputDir.resolve("out-sequential-read-table.json")
Path readMultiThreadTable = outputDir.resolve("out-read-multi-thread-table.json")
Path writeMultiThreadTable = outputDir.resolve("out-write-multi-thread-table.json")

ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

def dfs = new DecimalFormatSymbols(Locale.US)
dfs.groupingSeparator = ' ' as char
def latencyFormat = new DecimalFormat('#,##0.###', dfs)

def percentileSpecs = [
        [label: 'p50 [us/op]', percentile: 50d, keys: ['50.0', '50']],
        [label: 'p75 [us/op]', percentile: 75d, keys: ['75.0', '75']],
        [label: 'p90 [us/op]', percentile: 90d, keys: ['90.0', '90']],
        [label: 'p95 [us/op]', percentile: 95d, keys: ['95.0', '95']],
        [label: 'p99 [us/op]', percentile: 99d, keys: ['99.0', '99']],
        [label: 'p99.5 [us/op]', percentile: 99.5d, keys: ['99.5']],
        [label: 'p99.9 [us/op]', percentile: 99.9d, keys: ['99.9']],
        [label: 'p99.99 [us/op]', percentile: 99.99d, keys: ['99.99']]
]

def normalizeNumber = { Object value ->
    if (value == null) {
        return null
    }
    if (value instanceof Number) {
        double number = value.doubleValue()
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return null
        }
        return number
    }
    if (value instanceof CharSequence) {
        try {
            double number = Double.parseDouble(value.toString())
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                return null
            }
            return number
        } catch (NumberFormatException ignored) {
            return null
        }
    }
    return null
}

def stringifyCellValue = { Object value ->
    if (value == null) {
        return ""
    }
    if (value instanceof Map) {
        List values = value["values"] instanceof List ? value["values"] as List : null
        List strings = value["strings"] instanceof List ? value["strings"] as List : null
        if (values != null && strings != null && strings.size() == values.size() + 1) {
            StringBuilder rebuilt = new StringBuilder()
            for (int i = 0; i < values.size(); i++) {
                rebuilt.append(strings[i] == null ? "" : strings[i].toString())
                rebuilt.append(values[i] == null ? "" : values[i].toString())
            }
            rebuilt.append(strings[values.size()] == null ? "" : strings[values.size()].toString())
            return rebuilt.toString()
        }
    }
    return value.toString()
}

def reportNameForScenario = { String scenario ->
    switch (scenario) {
        case 'WriteSingleThread':
            return 'out-write-single-thread'
        case 'ReadSingleThread':
            return 'out-read-single-thread'
        case 'SequentialRead':
            return 'out-sequential-read'
        case 'ReadMultiThread':
            return 'out-read-multi-thread'
        case 'WriteMultiThread':
            return 'out-write-multi-thread'
        default:
            return null
    }
}

def describeResultFile = { Path file ->
    String rawEngine = file.fileName.toString()
            .replace('results-', '')
            .replace('.json', '')
    if (rawEngine.endsWith('-my')) {
        return null
    }

    def matcher = rawEngine =~ /^(write-single-thread|read-single-thread|sequential-read|write-multi-thread|read-multi-thread)-(.+?)(?:-threads(\d+))?-(latency|throughput)$/
    if (!matcher.matches()) {
        return null
    }

    Map<String, String> scenarioNames = [
            'write-single-thread': 'WriteSingleThread',
            'read-single-thread' : 'ReadSingleThread',
            'sequential-read'    : 'SequentialRead',
            'write-multi-thread' : 'WriteMultiThread',
            'read-multi-thread'  : 'ReadMultiThread'
    ]
    String scenarioToken = matcher.group(1)
    String threads = matcher.group(3) ?: ''
    String metric = matcher.group(4)
    [
            scenario: scenarioNames[scenarioToken],
            engine  : matcher.group(2),
            threads : threads,
            metric  : metric,
            key     : "${scenarioToken}|${matcher.group(2)}|${threads}".toString()
    ]
}

def selectLatencyResultFiles = { Path targetResultsDir ->
    Map<String, Map<String, Object>> grouped = [:]

    Files.newDirectoryStream(targetResultsDir, 'results-*.json').each { Path file ->
        if (file.fileName.toString().endsWith('-my.json')) {
            return
        }

        Map description = describeResultFile(file) as Map
        if (description == null) {
            return
        }

        Map<String, Object> group = grouped[description.key as String]
                ?: [latencyFile: null]
        if ((description.metric ?: '').toString() == 'latency') {
            group.latencyFile = file
        }
        grouped[description.key as String] = group
    }

    grouped.values()
            .collect { it.latencyFile as Path }
            .findAll { it != null }
            .sort { a, b -> a.fileName.toString() <=> b.fileName.toString() }
}

def collectHistogramBins
collectHistogramBins = { Object node, Map<Double, Long> binsByValue ->
    if (!(node instanceof List)) {
        return
    }
    List values = node as List
    if (values.size() >= 2 &&
            !(values[0] instanceof List) &&
            !(values[1] instanceof List)) {
        Double latency = normalizeNumber(values[0])
        Double countValue = normalizeNumber(values[1])
        if (latency != null && countValue != null && latency > 0d && countValue > 0d) {
            binsByValue[latency] = binsByValue[latency] + countValue.longValue()
        }
        return
    }
    values.each { nested ->
        collectHistogramBins(nested, binsByValue)
    }
}

def flattenHistogramBins = { Object rawDataHistogram ->
    Map<Double, Long> binsByValue = [:].withDefault { 0L }
    collectHistogramBins(rawDataHistogram, binsByValue)
    binsByValue.entrySet()
            .sort { a, b -> a.key <=> b.key }
            .collect { [latency: it.key, count: it.value] }
}

def percentileFromHistogram = { List<Map<String, Object>> bins, double percentile ->
    if (bins.isEmpty()) {
        return null
    }
    long totalCount = bins.collect { (it.count as Number).longValue() }.sum() as long
    if (totalCount <= 0L) {
        return null
    }
    long threshold = Math.max(1L, (long) Math.ceil((percentile / 100d) * totalCount))
    long cumulative = 0L
    for (Map<String, Object> bin : bins) {
        cumulative += (bin.count as Number).longValue()
        if (cumulative >= threshold) {
            return bin.latency as Double
        }
    }
    return bins[-1].latency as Double
}

def percentileMetricValue = { Map scorePercentiles, List<Map<String, Object>> bins, Map spec ->
    for (String key : spec.keys as List<String>) {
        if (scorePercentiles?.containsKey(key)) {
            Double value = normalizeNumber(scorePercentiles[key])
            if (value != null && value > 0d) {
                return value
            }
        }
    }
    percentileFromHistogram(bins, spec.percentile as double)
}

List<Map<String, Object>> writeRows = Files.exists(writeTable)
        ? mapper.readValue(writeTable.toFile(), List)
        : []
List<Map<String, Object>> readRows = Files.exists(readTable)
        ? mapper.readValue(readTable.toFile(), List)
        : []
List<Map<String, Object>> sequentialRows = Files.exists(sequentialTable)
        ? mapper.readValue(sequentialTable.toFile(), List)
        : []
List<Map<String, Object>> readMultiThreadRows = Files.exists(readMultiThreadTable)
        ? mapper.readValue(readMultiThreadTable.toFile(), List)
        : []
List<Map<String, Object>> writeMultiThreadRows = Files.exists(writeMultiThreadTable)
        ? mapper.readValue(writeMultiThreadTable.toFile(), List)
        : []

if (writeRows.isEmpty() && readRows.isEmpty() && sequentialRows.isEmpty()
        && readMultiThreadRows.isEmpty() && writeMultiThreadRows.isEmpty()) {
    System.err.println("No summary JSON files found in ${outputDir}")
    System.exit(1)
}

def collectPercentileRowsByReport = {
    Map<String, List<Map<String, Object>>> rowsByReport = [:].withDefault { [] }

    selectLatencyResultFiles(rawResultsDir).each { Path file ->
        Map description = describeResultFile(file) as Map
        String reportName = reportNameForScenario(description?.scenario as String)
        if (description == null || reportName == null) {
            return
        }

        byte[] bytes = Files.readAllBytes(file)
        if (bytes.length == 0) {
            return
        }

        List entries
        try {
            entries = mapper.readValue(bytes, List)
        } catch (Exception ignored) {
            return
        }
        if (!entries) {
            return
        }

        Map sampleEntry = entries.find {
            (it['mode'] ?: '').toString() == 'sample' &&
                    ((it['primaryMetric'] ?: [:])['scoreUnit'] ?: '').toString() == 'us/op'
        } as Map
        if (sampleEntry == null) {
            return
        }

        Map primaryMetric = sampleEntry['primaryMetric'] as Map ?: [:]
        Map scorePercentiles = primaryMetric['scorePercentiles'] as Map ?: [:]
        List<Map<String, Object>> bins = flattenHistogramBins(primaryMetric['rawDataHistogram'])

        Map<String, Object> row = ['Engine': description.engine]
        boolean complete = true
        percentileSpecs.each { spec ->
            Double value = percentileMetricValue(scorePercentiles, bins, spec)
            if (value == null || value <= 0d) {
                complete = false
                return
            }
            row[spec.label as String] = latencyFormat.format(value)
        }

        if (complete) {
            rowsByReport[reportName] << row
        }
    }

    rowsByReport.values().each { List<Map<String, Object>> rowsForReport ->
        rowsForReport.sort { a, b ->
            (a['Engine'] ?: '').toString() <=> (b['Engine'] ?: '').toString()
        }
    }
    return rowsByReport
}

def buildDetailedThroughputMarkdown = { List<Map<String, Object>> rows ->
    StringBuilder markdown = new StringBuilder()
    boolean includeLatency = !rows.isEmpty() &&
            rows[0].containsKey("Mean [us/op]")
    if (includeLatency) {
        markdown.append("| Engine | Score [ops/s] | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | Occupied space | CPU Usage |\n")
        markdown.append("|:-------|--------------:|-------------:|------------:|------------:|------------:|---------------:|----------:|\n")
        rows.each { row ->
            String engine = (row["Engine"] ?: "").toString()
            String score = (row["Score [ops/s]"] ?: "").toString()
            String mean = (row["Mean [us/op]"] ?: "").toString()
            String p50 = (row["p50 [us/op]"] ?: "").toString()
            String p95 = (row["p95 [us/op]"] ?: "").toString()
            String p99 = (row["p99 [us/op]"] ?: "").toString()
            String occupied = (row["Occupied space"] ?: "").toString()
            String cpuUsage = (row["cpuUsage"] ?: "").toString()
            markdown.append("| ${engine} | ${score.padLeft(13)} | ${mean} | ${p50} | ${p95} | ${p99} | ${occupied} | ${cpuUsage} |\n")
        }
    } else {
        markdown.append("| Engine | Score [ops/s] | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |\n")
        markdown.append("|:-------|--------------:|-----------:|-----------------------------:|---------------:|----------:|\n")
        rows.each { row ->
            String engine = stringifyCellValue(row["Engine"])
            String score = stringifyCellValue(row["Score [ops/s]"])
            String error = stringifyCellValue(row["ScoreError"])
            String confidenceInterval = stringifyCellValue(row["Confidence Interval [ops/s]"])
            String occupied = stringifyCellValue(row["Occupied space"])
            String cpuUsage = stringifyCellValue(row["cpuUsage"])
            markdown.append("| ${engine} | ${score.padLeft(13)} | ${error.padLeft(9)} | ${confidenceInterval} | ${occupied} | ${cpuUsage} |\n")
        }
    }
    return markdown.toString()
}

def buildPercentileMarkdown = { List<Map<String, Object>> rows ->
    StringBuilder markdown = new StringBuilder()
    if (rows.isEmpty()) {
        markdown.append("_No latency percentile data available._\n")
        return markdown.toString()
    }

    markdown.append("| Engine")
    percentileSpecs.each { spec ->
        markdown.append(" | ${spec.label}")
    }
    markdown.append(" |\n")
    markdown.append("|:-------")
    percentileSpecs.each {
        markdown.append("|-------------:")
    }
    markdown.append("|\n")
    rows.each { row ->
        markdown.append("| ${(row['Engine'] ?: '').toString()}")
        percentileSpecs.each { spec ->
            markdown.append(" | ${(row[spec.label as String] ?: '').toString()}")
        }
        markdown.append(" |\n")
    }
    return markdown.toString()
}

def buildDetailedMultithreadMarkdown = { List<Map<String, Object>> rows ->
    StringBuilder markdown = new StringBuilder()
    markdown.append("| Engine | Threads | Throughput [ops/s] | CPU Usage |\n")
    markdown.append("|:-------|--------:|-------------------:|----------:|\n")
    rows.each { row ->
        String engine = (row["Engine"] ?: "").toString()
        String threads = (row["Threads"] ?: "").toString()
        String throughput = (row["Throughput [ops/s]"] ?: "").toString()
        String cpuUsage = (row["cpuUsage"] ?: "").toString()
        markdown.append("| ${engine} | ${threads} | ${throughput} | ${cpuUsage} |\n")
    }
    return markdown.toString()
}

Map<String, List<Map<String, Object>>> percentileRowsByReport = collectPercentileRowsByReport()

Path writeOutput = outputDir.resolve("out-write-single-thread-table.md")
Path readOutput = outputDir.resolve("out-read-single-thread-table.md")
Path sequentialOutput = outputDir.resolve("out-sequential-read-table.md")
Path readMultiThreadOutput = outputDir.resolve("out-read-multi-thread-table.md")
Path writeMultiThreadOutput = outputDir.resolve("out-write-multi-thread-table.md")
Path writeOutput2 = outputDir.resolve("out-write-single-thread-table2.md")
Path readOutput2 = outputDir.resolve("out-read-single-thread-table2.md")
Path sequentialOutput2 = outputDir.resolve("out-sequential-read-table2.md")
Path readMultiThreadOutput2 = outputDir.resolve("out-read-multi-thread-table2.md")
Path writeMultiThreadOutput2 = outputDir.resolve("out-write-multi-thread-table2.md")

def deleteIfExists = { Path output ->
    if (Files.exists(output)) {
        Files.delete(output)
    }
}

if (!writeRows.isEmpty()) {
    Files.writeString(writeOutput, buildDetailedThroughputMarkdown(writeRows))
    println("Wrote ${writeOutput}")
    Files.writeString(writeOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-write-single-thread'] ?: []))
    println("Wrote ${writeOutput2}")
} else {
    deleteIfExists(writeOutput)
    deleteIfExists(writeOutput2)
}

if (!readRows.isEmpty()) {
    Files.writeString(readOutput, buildDetailedThroughputMarkdown(readRows))
    println("Wrote ${readOutput}")
    Files.writeString(readOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-read-single-thread'] ?: []))
    println("Wrote ${readOutput2}")
} else {
    deleteIfExists(readOutput)
    deleteIfExists(readOutput2)
}

if (!sequentialRows.isEmpty()) {
    Files.writeString(sequentialOutput, buildDetailedThroughputMarkdown(sequentialRows))
    println("Wrote ${sequentialOutput}")
    Files.writeString(sequentialOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-sequential-read'] ?: []))
    println("Wrote ${sequentialOutput2}")
} else {
    deleteIfExists(sequentialOutput)
    deleteIfExists(sequentialOutput2)
}

if (!readMultiThreadRows.isEmpty()) {
    Files.writeString(readMultiThreadOutput,
            buildDetailedMultithreadMarkdown(readMultiThreadRows))
    println("Wrote ${readMultiThreadOutput}")
    Files.writeString(readMultiThreadOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-read-multi-thread'] ?: []))
    println("Wrote ${readMultiThreadOutput2}")
} else {
    deleteIfExists(readMultiThreadOutput)
    deleteIfExists(readMultiThreadOutput2)
}

if (!writeMultiThreadRows.isEmpty()) {
    Files.writeString(writeMultiThreadOutput,
            buildDetailedMultithreadMarkdown(writeMultiThreadRows))
    println("Wrote ${writeMultiThreadOutput}")
    Files.writeString(writeMultiThreadOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-write-multi-thread'] ?: []))
    println("Wrote ${writeMultiThreadOutput2}")
} else {
    deleteIfExists(writeMultiThreadOutput)
    deleteIfExists(writeMultiThreadOutput2)
}
