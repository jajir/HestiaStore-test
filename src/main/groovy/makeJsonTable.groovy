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
        if (Files.exists(cur.resolve('pom.xml'))) {
            lastPom = cur
        }
        cur = cur.parent
    }
    return lastPom ?: start
}

Path cwd = Paths.get('.').toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)
List<Path> candidateResultsDirs = [
        rootDir.resolve('results'),
        cwd.resolve('results')
]
Path resultsDir = candidateResultsDirs.find { Files.exists(it) && Files.isDirectory(it) }

if (resultsDir == null) {
    System.err.println("Results directory not found. Checked: ${candidateResultsDirs*.toString().join(', ')}")
    System.exit(1)
}

def mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

def dfs = new DecimalFormatSymbols(Locale.US)
dfs.groupingSeparator = ' ' as char
def scoreFormat = new DecimalFormat('#,##0', dfs)
def latencyFormat = new DecimalFormat('#,##0.###', dfs)
def sizeFormat = new DecimalFormat('#,##0.##', dfs)
def percentFormat = new DecimalFormat('#,##0', dfs)

def normalizeNumber = { Object value ->
    if (value == null) {
        return null
    }
    if (value instanceof Number) {
        double d = value.doubleValue()
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return null
        }
        return d
    }
    if (value instanceof CharSequence) {
        try {
            double d = Double.parseDouble(value.toString())
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return null
            }
            return d
        } catch (NumberFormatException ignored) {
            return null
        }
    }
    return null
}

def formatOpsScore = { Object value ->
    Double number = normalizeNumber(value)
    if (number == null) {
        return ''
    }
    return scoreFormat.format(number)
}

def formatLatency = { Object value ->
    Double number = normalizeNumber(value)
    if (number == null) {
        return ''
    }
    return latencyFormat.format(number)
}

def humanReadableSize = { long bytes ->
    if (bytes < 1024) {
        return "${bytes} B"
    }
    String[] units = ['KB', 'MB', 'GB', 'TB', 'PB']
    double size = bytes
    int unitIdx = -1
    while (size >= 1024 && unitIdx < units.length - 1) {
        size /= 1024d
        unitIdx++
    }
    return "${sizeFormat.format(size)} ${units[unitIdx]}"
}

def percentileValue = { Map percentiles, String... keys ->
    for (String key : keys) {
        if (percentiles?.containsKey(key)) {
            return percentiles[key]
        }
    }
    return null
}

def findBenchmarkEntry = { List data, List<String> preferredModes,
        boolean fallbackToFirst = true ->
    for (String mode : preferredModes) {
        def entry = data.find { (it['mode'] ?: '').toString() == mode }
        if (entry != null) {
            return entry as Map
        }
    }
    return fallbackToFirst && data ? data[0] as Map : null
}

def buildConfidenceInterval = { Map primary, boolean latency ->
    List confidence = primary?.get('scoreConfidence') as List
    if (confidence?.size() != 2) {
        return ''
    }
    Double lo = normalizeNumber(confidence[0])
    Double hi = normalizeNumber(confidence[1])
    if (lo == null || hi == null) {
        return ''
    }
    return latency
            ? "${formatLatency(lo)} .. ${formatLatency(hi)}"
            : "${formatOpsScore(lo)} .. ${formatOpsScore(hi)}"
}

def files = []
Files.newDirectoryStream(resultsDir, 'results-*.json').each { Path file ->
    def name = file.fileName.toString()
    if (!name.endsWith('-my.json')) {
        files << file
    }
}
files.sort { a, b -> a.fileName.toString() <=> b.fileName.toString() }

def rows = []

files.each { Path file ->
    String rawEngine = file.fileName.toString()
            .replace('results-', '')
            .replace('.json', '')

    boolean isReadVariant = false
    boolean isSequentialVariant = false
    boolean isMultithreadReadVariant = false
    String engineBase = rawEngine
    String threads = ''

    if (rawEngine.startsWith('multithread-read-')) {
        isMultithreadReadVariant = true
        engineBase = rawEngine.substring('multithread-read-'.length())
        def matcher = engineBase =~ /^(.*)-threads(\d+)$/
        if (matcher.matches()) {
            engineBase = matcher.group(1)
            threads = matcher.group(2)
        }
    } else if (rawEngine.startsWith('read-')) {
        isReadVariant = true
        engineBase = rawEngine.substring('read-'.length())
    } else if (rawEngine.endsWith('Read')) {
        isReadVariant = true
        engineBase = rawEngine.substring(0, rawEngine.length() - 'Read'.length())
    } else if (rawEngine.startsWith('sequential-')) {
        isSequentialVariant = true
        engineBase = rawEngine.substring('sequential-'.length())
    } else if (rawEngine.startsWith('write-')) {
        engineBase = rawEngine.substring('write-'.length())
    }

    String scenario = isMultithreadReadVariant ? 'MultithreadRead'
            : (isReadVariant ? 'Read' : (isSequentialVariant ? 'Sequential' : 'Write'))
    String engine = engineBase

    def data = mapper.readValue(Files.readAllBytes(file), List)
    if (data.isEmpty()) {
        return
    }
    def entry = findBenchmarkEntry(data, ['thrpt', 'sample', 'avgt'])
    def primary = entry?.get('primaryMetric') as Map
    Double score = normalizeNumber(primary?.get('score'))
    Double scoreError = normalizeNumber(primary?.get('scoreError'))
    String confidenceInterval = buildConfidenceInterval(primary,
            isMultithreadReadVariant)

    Path myVariant = file.parent.resolve(file.fileName.toString().replace('.json', '-my.json'))
    String occupied = ''
    String usedMemoryStr = ''
    String cpuUsageStr = ''
    if (Files.exists(myVariant)) {
        def extra = mapper.readValue(Files.readAllBytes(myVariant), Map)
        Double totalSize = normalizeNumber(extra?.get('totalDirectorySize') ?: extra?.get('totalSize'))
        if (totalSize != null) {
            occupied = humanReadableSize(totalSize.longValue())
        }
        Double usedMemory = normalizeNumber(extra?.get('usedMemoryBytes'))
        if (usedMemory != null) {
            usedMemoryStr = humanReadableSize(usedMemory.longValue())
        }
        Double cpuUsage = normalizeNumber(extra?.get('cpuUsage'))
        if (cpuUsage != null) {
            cpuUsageStr = percentFormat.format(cpuUsage * 100d)
            if (!cpuUsageStr.isEmpty()) {
                cpuUsageStr = cpuUsageStr + '%'
            }
        }
    }

    if (isMultithreadReadVariant) {
        def latencyEntry = findBenchmarkEntry(data, ['sample', 'avgt'])
        def throughputEntry = findBenchmarkEntry(data, ['thrpt'], false)
        Map latencyPrimary = latencyEntry?.get('primaryMetric') as Map ?: [:]
        Map throughputPrimary = throughputEntry?.get('primaryMetric') as Map ?: [:]
        Map percentiles = latencyPrimary?.get('scorePercentiles') as Map ?: [:]
        rows << [
                'Engine': engine,
                'Variant': scenario,
                'Threads': threads,
                'Throughput [ops/s]': formatOpsScore(
                        normalizeNumber(throughputPrimary?.get('score'))),
                'Throughput Error [ops/s]': formatOpsScore(
                        normalizeNumber(throughputPrimary?.get('scoreError'))),
                'Throughput Confidence Interval [ops/s]': buildConfidenceInterval(
                        throughputPrimary, false),
                'Mean [us/op]': formatLatency(
                        normalizeNumber(latencyPrimary?.get('score'))),
                'Latency Error [us/op]': formatLatency(
                        normalizeNumber(latencyPrimary?.get('scoreError'))),
                'Confidence Interval [us/op]': buildConfidenceInterval(
                        latencyPrimary, true),
                'p50 [us/op]': formatLatency(percentileValue(percentiles, '50.0', '50')),
                'p95 [us/op]': formatLatency(percentileValue(percentiles, '95.0', '95')),
                'p99 [us/op]': formatLatency(percentileValue(percentiles, '99.0', '99')),
                'Occupied space': occupied,
                'usedMemoryBytes': usedMemoryStr,
                'cpuUsage': cpuUsageStr
        ]
    } else {
        rows << [
                'Engine': engine,
                'Variant': scenario,
                'Score [ops/s]': formatOpsScore(score),
                'ScoreError': formatOpsScore(scoreError),
                'Confidence Interval [ops/s]': confidenceInterval,
                'Occupied space': occupied,
                'usedMemoryBytes': usedMemoryStr,
                'cpuUsage': cpuUsageStr
        ]
    }
}

def writeRows = rows.findAll { (it['Variant'] ?: 'Write') == 'Write' }
def readRows = rows.findAll { (it['Variant'] ?: 'Write') == 'Read' }
def sequentialRows = rows.findAll { (it['Variant'] ?: 'Write') == 'Sequential' }
def multithreadReadRows = rows.findAll { (it['Variant'] ?: '') == 'MultithreadRead' }

Path writeOutput = resultsDir.resolve('out-write-table.json')
Path readOutput = resultsDir.resolve('out-read-table.json')
Path sequentialOutput = resultsDir.resolve('out-sequential-table.json')
Path multithreadReadOutput = resultsDir.resolve('out-multithread-read-table.json')

mapper.writeValue(writeOutput.toFile(), writeRows)
mapper.writeValue(readOutput.toFile(), readRows)
mapper.writeValue(sequentialOutput.toFile(), sequentialRows)
mapper.writeValue(multithreadReadOutput.toFile(), multithreadReadRows)

println "Written summaries to ${writeOutput}, ${readOutput}, ${sequentialOutput} and ${multithreadReadOutput}"
