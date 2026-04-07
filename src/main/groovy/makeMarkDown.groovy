#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.Field
import java.nio.charset.StandardCharsets
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
        cur = cur.getParent()
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

@Field final String TABLE_PLACEHOLDER = '{{TABLE}}'
@Field final String TABLE1_PLACEHOLDER = '{{TABLE1}}'

Path cwd = Paths.get('.')
Path rootDir = findProjectRoot(cwd)
Path rawResultsDir = resolveDirectoryFromEnv('BENCHMARK_RESULTS_DIR',
        rootDir.resolve('results'))
Path templatesDir = resolveDirectoryFromEnv('BENCHMARK_TEMPLATES_DIR',
        rawResultsDir)
Path buildDir = resolveDirectoryFromEnv('REPORT_BUILD_DIR', rawResultsDir)
Path docsDir = resolveDirectoryFromEnv('REPORT_DOCS_DIR', rawResultsDir)

if (!Files.isDirectory(rawResultsDir)) {
    System.err.println("Results directory not found: ${rawResultsDir}")
    System.exit(1)
}
if (!Files.isDirectory(templatesDir)) {
    System.err.println("Template directory not found: ${templatesDir}")
    System.exit(1)
}
Files.createDirectories(docsDir)
ObjectMapper mapper = new ObjectMapper()

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

Path resolveMarkdownTable(Path resultsDir, String reportName, String suffix) {
    Path candidate = resultsDir.resolve("${reportName}${suffix}")
    return Files.exists(candidate) ? candidate : null
}

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

def collectPercentileRowsByReport = { Path targetResultsDir ->
    Map<String, List<Map<String, Object>>> rowsByReport = [:].withDefault { [] }

    selectLatencyResultFiles(targetResultsDir).each { Path file ->
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

String buildThroughputTableMarkdown(List<Map<String, Object>> rows) {
    StringBuilder out = new StringBuilder()
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n')
    } else {
        boolean includeLatency = rows[0].containsKey('Mean [us/op]')
        if (includeLatency) {
            out.append('| Engine | Score [ops/s] | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | Occupied space | CPU Usage |\n')
            out.append('|:-------|--------------:|-------------:|------------:|------------:|------------:|---------------:|----------:|\n')
            rows.each { row ->
                def engine = (row['Engine'] ?: '').toString()
                def score = (row['Score [ops/s]'] ?: '').toString()
                def mean = (row['Mean [us/op]'] ?: '').toString()
                def p50 = (row['p50 [us/op]'] ?: '').toString()
                def p95 = (row['p95 [us/op]'] ?: '').toString()
                def p99 = (row['p99 [us/op]'] ?: '').toString()
                def occupied = (row['Occupied space'] ?: '').toString()
                def cpuUsage = (row['cpuUsage'] ?: '').toString()
                out.append("| ${engine} | ${score.padLeft(13)} | ${mean} | ${p50} | ${p95} | ${p99} | ${occupied} | ${cpuUsage} |\n")
            }
        } else {
            out.append('| Engine | Score [ops/s] | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |\n')
            out.append('|:-------|--------------:|-----------:|-----------------------------:|---------------:|----------:|\n')
            rows.each { row ->
                def engine = (row['Engine'] ?: '').toString()
                def score = (row['Score [ops/s]'] ?: '').toString()
                def error = (row['ScoreError'] ?: '').toString()
                def ci = (row['Confidence Interval [ops/s]'] ?: '').toString()
                def occupied = (row['Occupied space'] ?: '').toString()
                def cpuUsage = (row['cpuUsage'] ?: '').toString()
                out.append("| ${engine} | ${score.padLeft(13)} | ${error.padLeft(9)} | ${ci} | ${occupied} | ${cpuUsage} |\n")
            }
        }
    }
    return out.toString()
}

String buildMultithreadTableMarkdown(List<Map<String, Object>> rows) {
    StringBuilder out = new StringBuilder()
    if (rows.isEmpty()) {
        out.append('_No summary rows available._\n')
    } else {
        out.append('| Engine | Threads | Throughput [ops/s] | CPU Usage |\n')
        out.append('|:-------|--------:|-------------------:|----------:|\n')
        rows.each { row ->
            def engine = (row['Engine'] ?: '').toString()
            def threads = (row['Threads'] ?: '').toString()
            def throughput = (row['Throughput [ops/s]'] ?: '').toString()
            def cpuUsage = (row['cpuUsage'] ?: '').toString()
            out.append("| ${engine} | ${threads} | ${throughput} | ${cpuUsage} |\n")
        }
    }
    return out.toString()
}

String buildPercentileTableMarkdown(List<Map<String, Object>> rows, List<Map<String, Object>> specs) {
    StringBuilder out = new StringBuilder()
    if (rows.isEmpty()) {
        out.append('_No latency percentile data available._\n')
        return out.toString()
    }

    out.append('| Engine')
    specs.each { spec ->
        out.append(" | ${spec.label}")
    }
    out.append(' |\n')
    out.append('|:-------')
    specs.each {
        out.append('|-------------:')
    }
    out.append('|\n')
    rows.each { row ->
        out.append("| ${(row['Engine'] ?: '').toString()}")
        specs.each { spec ->
            out.append(" | ${(row[spec.label as String] ?: '').toString()}")
        }
        out.append(' |\n')
    }
    return out.toString()
}

boolean isMultithreadReport(List<Map<String, Object>> rows, String reportName) {
    return reportName in ['out-read-multi-thread', 'out-write-multi-thread'] ||
            (!rows.isEmpty() &&
                    rows[0].containsKey('Threads') &&
                    rows[0].containsKey('Throughput [ops/s]'))
}

String renderTemplate(String template, String tableSection, String table1Section) {
    if (!template.contains(TABLE_PLACEHOLDER)) {
        throw new IllegalArgumentException(
                "Template must contain ${TABLE_PLACEHOLDER}")
    }
    String rendered = template.replace(TABLE_PLACEHOLDER, tableSection.trim())
    if (template.contains(TABLE1_PLACEHOLDER)) {
        rendered = rendered.replace(TABLE1_PLACEHOLDER, table1Section.trim())
    }
    return rendered.endsWith('\n') ? rendered : rendered + '\n'
}

if (args.length == 0) {
    System.err.println('Usage: makeMarkDown.groovy REPORT_NAME [REPORT_NAME ...]')
    System.exit(1)
}

Map<String, List<Map<String, Object>>> percentileRowsByReportCache = null

args.each { String reportName ->
    Path templatePath = resolveTemplate(templatesDir, reportName)
    String template = Files.readString(templatePath, StandardCharsets.UTF_8)
    Path outputPath = docsDir.resolve("${reportName}.md")

    Path tablePath = resolveMarkdownTable(buildDir, reportName, '-table.md')
    Path table1Path = template.contains(TABLE1_PLACEHOLDER)
            ? resolveMarkdownTable(buildDir, reportName, '-table2.md')
            : null

    List<Map<String, Object>> rows = tablePath == null
            ? mapper.readValue(resolveSummaryJson(buildDir, reportName).toFile(), List)
            : []
    boolean multithreadReport = isMultithreadReport(rows, reportName)

    String tableSection = tablePath != null
            ? Files.readString(tablePath, StandardCharsets.UTF_8).trim()
            : (multithreadReport
                    ? buildMultithreadTableMarkdown(rows)
                    : buildThroughputTableMarkdown(rows)).trim()

    String table1Section = ''
    if (template.contains(TABLE1_PLACEHOLDER)) {
        if (table1Path != null) {
            table1Section = Files.readString(table1Path, StandardCharsets.UTF_8).trim()
        } else {
            if (percentileRowsByReportCache == null) {
                percentileRowsByReportCache = collectPercentileRowsByReport(rawResultsDir)
            }
            table1Section = buildPercentileTableMarkdown(
                    percentileRowsByReportCache[reportName] ?: [],
                    percentileSpecs).trim()
        }
    }

    String rendered = renderTemplate(template, tableSection, table1Section)
    Files.writeString(outputPath, rendered, StandardCharsets.UTF_8)
    println "Wrote ${outputPath}"
}
