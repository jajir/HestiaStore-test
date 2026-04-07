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

Path resolveDirectoryFromEnv(String envName, Path fallback) {
    String raw = System.getenv(envName)
    if (raw == null || raw.trim().isEmpty()) {
        return fallback
    }
    return Paths.get(raw).toAbsolutePath().normalize()
}

Path cwd = Paths.get('.').toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)
Path rawResultsDir = resolveDirectoryFromEnv('BENCHMARK_RESULTS_DIR',
        rootDir.resolve('results'))
Path outputDir = resolveDirectoryFromEnv('REPORT_BUILD_DIR', rawResultsDir)

if (!Files.isDirectory(rawResultsDir)) {
    System.err.println("Results directory not found: ${rawResultsDir}")
    System.exit(1)
}
Files.createDirectories(outputDir)

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
    String lowerBound = latency
            ? formatLatency(lo)
            : formatOpsScore(lo)
    String upperBound = latency
            ? formatLatency(hi)
            : formatOpsScore(hi)
    if (lowerBound.isEmpty() || upperBound.isEmpty()) {
        return ''
    }
    return "${lowerBound} .. ${upperBound}".toString()
}

Map<String, String> scenarioNames = [
        'write-single-thread': 'WriteSingleThread',
        'read-single-thread' : 'ReadSingleThread',
        'sequential-read'    : 'SequentialRead',
        'write-multi-thread' : 'WriteMultiThread',
        'read-multi-thread'  : 'ReadMultiThread'
]

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

    String scenarioToken = matcher.group(1)
    String scenario = scenarioNames[scenarioToken]
    if (scenario == null) {
        return null
    }
    String engine = matcher.group(2)
    String threads = matcher.group(3) ?: ''
    String metric = matcher.group(4)
    [
            scenarioToken: scenarioToken,
            scenario     : scenario,
            engine       : engine,
            threads      : threads,
            metric       : metric,
            key          : "${scenarioToken}|${engine}|${threads}".toString()
    ]
}

def resultDataCache = [:]
def loadResultEntries = { Path file ->
    if (file == null) {
        return null
    }
    if (resultDataCache.containsKey(file)) {
        return resultDataCache[file]
    }

    byte[] rawBytes = Files.readAllBytes(file)
    if (rawBytes.length == 0) {
        System.err.println("Skipping empty result file: ${file.fileName}")
        resultDataCache[file] = null
        return null
    }

    try {
        List data = mapper.readValue(rawBytes, List)
        resultDataCache[file] = data
        return data
    } catch (Exception e) {
        System.err.println("Skipping unreadable result file ${file.fileName}: ${e.message}")
        resultDataCache[file] = null
        return null
    }
}

def metadataPathFor = { Path file ->
    file == null
            ? null
            : file.parent.resolve(file.fileName.toString().replace('.json', '-my.json'))
}

def metadataCache = [:]
def loadMetadata = { Path metadataFile ->
    if (metadataFile == null || !Files.exists(metadataFile)) {
        return null
    }
    if (metadataCache.containsKey(metadataFile)) {
        return metadataCache[metadataFile]
    }

    byte[] extraBytes = Files.readAllBytes(metadataFile)
    if (extraBytes.length == 0) {
        System.err.println("Skipping empty metadata file: ${metadataFile.fileName}")
        metadataCache[metadataFile] = null
        return null
    }

    try {
        Map extra = mapper.readValue(extraBytes, Map)
        metadataCache[metadataFile] = extra
        return extra
    } catch (Exception e) {
        System.err.println("Skipping unreadable metadata file ${metadataFile.fileName}: ${e.message}")
        metadataCache[metadataFile] = null
        return null
    }
}

def mergeMetadata = { List<Path> resultFiles ->
    List<Map> extras = resultFiles.findAll { it != null }
            .collect { metadataPathFor(it) }
            .findAll { it != null && Files.exists(it) }
            .unique()
            .collect { loadMetadata(it) }
            .findAll { it != null }

    List<Double> totalSizes = extras.collect {
        normalizeNumber(it?.get('totalDirectorySize') ?: it?.get('totalSize'))
    }.findAll { it != null }
    List<Double> usedMemories = extras.collect {
        normalizeNumber(it?.get('usedMemoryBytes'))
    }.findAll { it != null }
    List<Double> cpuUsages = extras.collect {
        normalizeNumber(it?.get('cpuUsage'))
    }.findAll { it != null }

    String occupied = totalSizes.isEmpty()
            ? ''
            : humanReadableSize(totalSizes.max().longValue())
    String usedMemoryStr = usedMemories.isEmpty()
            ? ''
            : humanReadableSize(usedMemories.max().longValue())
    String cpuUsageStr = ''
    if (!cpuUsages.isEmpty()) {
        double averageCpuUsage = cpuUsages.sum(0d) / cpuUsages.size()
        cpuUsageStr = percentFormat.format(averageCpuUsage * 100d)
        if (!cpuUsageStr.isEmpty()) {
            cpuUsageStr = cpuUsageStr + '%'
        }
    }

    [
            occupied     : occupied,
            usedMemoryStr: usedMemoryStr,
            cpuUsageStr  : cpuUsageStr
    ]
}

def groupedFiles = [:]
Files.newDirectoryStream(rawResultsDir, 'results-*.json').each { Path file ->
    String fileName = file.fileName.toString()
    if (fileName.endsWith('-my.json')) {
        return
    }

    Map description = describeResultFile(file) as Map
    if (description == null) {
        System.err.println("Skipping result file with unsupported name: ${file.fileName}")
        return
    }

    Map group = groupedFiles[description.key] ?: [
            description   : description,
            latencyFile   : null,
            throughputFile: null
    ]
    switch (description.metric) {
        case 'latency':
            group.latencyFile = file
            break
        case 'throughput':
            group.throughputFile = file
            break
    }
    groupedFiles[description.key] = group
}

def groups = groupedFiles.values().sort { a, b ->
    String left = [
            a.description.scenarioToken,
            a.description.engine,
            a.description.threads
    ].join('|')
    String right = [
            b.description.scenarioToken,
            b.description.engine,
            b.description.threads
    ].join('|')
    left <=> right
}

def rows = []

groups.each { Map group ->
    Map description = group.description as Map
    String scenarioToken = description.scenarioToken as String
    String engine = description.engine as String
    String threads = description.threads as String
    boolean isMultithreadVariant = scenarioToken in [
            'write-multi-thread',
            'read-multi-thread'
    ]
    String scenario = description.scenario as String

    Path latencyFile = group.latencyFile as Path
    Path throughputFile = group.throughputFile as Path

    List latencyData = loadResultEntries(latencyFile)
    List throughputData = throughputFile == latencyFile
            ? latencyData
            : loadResultEntries(throughputFile)

    def latencyEntry = findBenchmarkEntry(latencyData ?: [], ['sample', 'avgt'],
            false)
    def throughputEntry = findBenchmarkEntry(throughputData ?: [], ['thrpt'],
            false)
    if (latencyEntry == null && throughputEntry == null) {
        System.err.println("Skipping result group without a usable benchmark entry: ${engine} / ${scenario}")
        return
    }

    Map latencyPrimary = latencyEntry?.get('primaryMetric') as Map ?: [:]
    Map throughputPrimary = throughputEntry?.get('primaryMetric') as Map ?: [:]
    Map percentiles = latencyPrimary?.get('scorePercentiles') as Map ?: [:]
    Map metadata = mergeMetadata([latencyFile, throughputFile])
    String occupied = metadata.occupied as String
    String usedMemoryStr = metadata.usedMemoryStr as String
    String cpuUsageStr = metadata.cpuUsageStr as String

    if (isMultithreadVariant) {
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
                'Score [ops/s]': formatOpsScore(
                        normalizeNumber(throughputPrimary?.get('score'))),
                'ScoreError': formatOpsScore(
                        normalizeNumber(throughputPrimary?.get('scoreError'))),
                'Confidence Interval [ops/s]': buildConfidenceInterval(
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
    }
}

def writeSingleThreadRows = rows.findAll {
    (it['Variant'] ?: '') == 'WriteSingleThread'
}
def readSingleThreadRows = rows.findAll {
    (it['Variant'] ?: '') == 'ReadSingleThread'
}
def sequentialReadRows = rows.findAll {
    (it['Variant'] ?: '') == 'SequentialRead'
}
def readMultiThreadRows = rows.findAll {
    (it['Variant'] ?: '') == 'ReadMultiThread'
}
def writeMultiThreadRows = rows.findAll {
    (it['Variant'] ?: '') == 'WriteMultiThread'
}

Path writeOutput = outputDir.resolve('out-write-single-thread-table.json')
Path readOutput = outputDir.resolve('out-read-single-thread-table.json')
Path sequentialOutput = outputDir.resolve('out-sequential-read-table.json')
Path readMultiThreadOutput = outputDir.resolve('out-read-multi-thread-table.json')
Path writeMultiThreadOutput = outputDir.resolve('out-write-multi-thread-table.json')

mapper.writeValue(writeOutput.toFile(), writeSingleThreadRows)
mapper.writeValue(readOutput.toFile(), readSingleThreadRows)
mapper.writeValue(sequentialOutput.toFile(), sequentialReadRows)
mapper.writeValue(readMultiThreadOutput.toFile(), readMultiThreadRows)
mapper.writeValue(writeMultiThreadOutput.toFile(), writeMultiThreadRows)

println "Written summaries to ${writeOutput}, ${readOutput}, ${sequentialOutput}, ${readMultiThreadOutput} and ${writeMultiThreadOutput}"
