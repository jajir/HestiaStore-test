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

Path cwd = Paths.get(".").toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)

List<Path> candidateDirs = [
        rootDir.resolve("results"),
        cwd.resolve("results")
]

Path resultsDir = candidateDirs.find { Files.exists(it) && Files.isDirectory(it) }

if (resultsDir == null) {
    System.err.println("Results directory not found. Checked: ${candidateDirs*.toString().join(', ')}")
    System.exit(1)
}

Path writeTable = resultsDir.resolve("out-write-table.json")
Path readTable = resultsDir.resolve("out-read-table.json")
Path sequentialTable = resultsDir.resolve("out-sequential-table.json")
Path multithreadReadTable = resultsDir.resolve("out-multithread-read-table.json")
Path multithreadWriteTable = resultsDir.resolve("out-multithread-write-table.json")

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

def reportNameForScenario = { String scenario ->
    switch (scenario) {
        case 'Write':
            return 'out-write'
        case 'Read':
            return 'out-read'
        case 'Sequential':
            return 'out-sequential'
        case 'MultithreadRead':
            return 'out-multithread-read'
        case 'MultithreadWrite':
            return 'out-multithread-write'
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

    String engineBase = rawEngine
    String scenario = 'Write'

    if (rawEngine.startsWith('multithread-read-')) {
        scenario = 'MultithreadRead'
        engineBase = rawEngine.substring('multithread-read-'.length())
        def matcher = engineBase =~ /^(.*)-threads\d+$/
        if (matcher.matches()) {
            engineBase = matcher.group(1)
        }
    } else if (rawEngine.startsWith('multithread-write-')) {
        scenario = 'MultithreadWrite'
        engineBase = rawEngine.substring('multithread-write-'.length())
        def matcher = engineBase =~ /^(.*)-threads\d+$/
        if (matcher.matches()) {
            engineBase = matcher.group(1)
        }
    } else if (rawEngine.startsWith('read-')) {
        scenario = 'Read'
        engineBase = rawEngine.substring('read-'.length())
    } else if (rawEngine.endsWith('Read')) {
        scenario = 'Read'
        engineBase = rawEngine.substring(0, rawEngine.length() - 'Read'.length())
    } else if (rawEngine.startsWith('sequential-')) {
        scenario = 'Sequential'
        engineBase = rawEngine.substring('sequential-'.length())
    } else if (rawEngine.startsWith('write-')) {
        scenario = 'Write'
        engineBase = rawEngine.substring('write-'.length())
    }

    [scenario: scenario, engine: engineBase]
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
List<Map<String, Object>> multithreadReadRows = Files.exists(multithreadReadTable)
        ? mapper.readValue(multithreadReadTable.toFile(), List)
        : []
List<Map<String, Object>> multithreadWriteRows = Files.exists(multithreadWriteTable)
        ? mapper.readValue(multithreadWriteTable.toFile(), List)
        : []

if (writeRows.isEmpty() && readRows.isEmpty() && sequentialRows.isEmpty()
        && multithreadReadRows.isEmpty() && multithreadWriteRows.isEmpty()) {
    System.err.println("No summary JSON files found in ${resultsDir}")
    System.exit(1)
}

def collectPercentileRowsByReport = {
    Map<String, List<Map<String, Object>>> rowsByReport = [:].withDefault { [] }

    Files.newDirectoryStream(resultsDir, 'results-*.json').each { Path file ->
        if (file.fileName.toString().endsWith('-my.json')) {
            return
        }

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
            String engine = (row["Engine"] ?: "").toString()
            String score = (row["Score [ops/s]"] ?: "").toString()
            String error = (row["ScoreError"] ?: "").toString()
            String confidenceInterval = (row["Confidence Interval [ops/s]"] ?: "").toString()
            String occupied = (row["Occupied space"] ?: "").toString()
            String cpuUsage = (row["cpuUsage"] ?: "").toString()
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

Path writeOutput = resultsDir.resolve("out-write-table.md")
Path readOutput = resultsDir.resolve("out-read-table.md")
Path sequentialOutput = resultsDir.resolve("out-sequential-table.md")
Path multithreadReadOutput = resultsDir.resolve("out-multithread-read-table.md")
Path multithreadWriteOutput = resultsDir.resolve("out-multithread-write-table.md")
Path writeOutput2 = resultsDir.resolve("out-write-table2.md")
Path readOutput2 = resultsDir.resolve("out-read-table2.md")
Path sequentialOutput2 = resultsDir.resolve("out-sequential-table2.md")
Path multithreadReadOutput2 = resultsDir.resolve("out-multithread-read-table2.md")
Path multithreadWriteOutput2 = resultsDir.resolve("out-multithread-write-table2.md")

def deleteIfExists = { Path output ->
    if (Files.exists(output)) {
        Files.delete(output)
    }
}

if (!writeRows.isEmpty()) {
    Files.writeString(writeOutput, buildDetailedThroughputMarkdown(writeRows))
    println("Wrote ${writeOutput}")
    Files.writeString(writeOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-write'] ?: []))
    println("Wrote ${writeOutput2}")
} else {
    deleteIfExists(writeOutput)
    deleteIfExists(writeOutput2)
}

if (!readRows.isEmpty()) {
    Files.writeString(readOutput, buildDetailedThroughputMarkdown(readRows))
    println("Wrote ${readOutput}")
    Files.writeString(readOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-read'] ?: []))
    println("Wrote ${readOutput2}")
} else {
    deleteIfExists(readOutput)
    deleteIfExists(readOutput2)
}

if (!sequentialRows.isEmpty()) {
    Files.writeString(sequentialOutput, buildDetailedThroughputMarkdown(sequentialRows))
    println("Wrote ${sequentialOutput}")
    Files.writeString(sequentialOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-sequential'] ?: []))
    println("Wrote ${sequentialOutput2}")
} else {
    deleteIfExists(sequentialOutput)
    deleteIfExists(sequentialOutput2)
}

if (!multithreadReadRows.isEmpty()) {
    Files.writeString(multithreadReadOutput,
            buildDetailedMultithreadMarkdown(multithreadReadRows))
    println("Wrote ${multithreadReadOutput}")
    Files.writeString(multithreadReadOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-multithread-read'] ?: []))
    println("Wrote ${multithreadReadOutput2}")
} else {
    deleteIfExists(multithreadReadOutput)
    deleteIfExists(multithreadReadOutput2)
}

if (!multithreadWriteRows.isEmpty()) {
    Files.writeString(multithreadWriteOutput,
            buildDetailedMultithreadMarkdown(multithreadWriteRows))
    println("Wrote ${multithreadWriteOutput}")
    Files.writeString(multithreadWriteOutput2,
            buildPercentileMarkdown(percentileRowsByReport['out-multithread-write'] ?: []))
    println("Wrote ${multithreadWriteOutput2}")
} else {
    deleteIfExists(multithreadWriteOutput)
    deleteIfExists(multithreadWriteOutput2)
}
