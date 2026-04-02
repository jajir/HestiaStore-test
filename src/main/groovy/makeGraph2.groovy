#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

Path cwd = Paths.get('.').toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)
Path rawResultsDir = resolveDirectoryFromEnv('BENCHMARK_RESULTS_DIR',
        rootDir.resolve('results'))
Path imagesDir = resolveDirectoryFromEnv('REPORT_IMAGES_DIR', rawResultsDir)
def graphShared = new GroovyShell().evaluate(
        rootDir.resolve('src/main/groovy/graphShared.groovy').toFile())

if (!Files.isDirectory(rawResultsDir)) {
    System.err.println("Results directory not found: ${rawResultsDir}")
    System.exit(1)
}
Files.createDirectories(imagesDir)

def percentileSpecs = [
        [label: 'p50', percentile: 50d, keys: ['50.0', '50']],
        [label: 'p75', percentile: 75d, keys: ['75.0', '75']],
        [label: 'p90', percentile: 90d, keys: ['90.0', '90']],
        [label: 'p95', percentile: 95d, keys: ['95.0', '95']],
        [label: 'p99', percentile: 99d, keys: ['99.0', '99']],
        [label: 'p99.5', percentile: 99.5d, keys: ['99.5']],
        [label: 'p99.9', percentile: 99.9d, keys: ['99.9']],
        [label: 'p99.99', percentile: 99.99d, keys: ['99.99']]
]

def scenarioMeta = [
        WriteSingleThread: [stem: 'out-write-single-thread-percentiles.svg',
                            title: 'Single-thread Write Latency Percentiles'],
        ReadSingleThread : [stem: 'out-read-single-thread-percentiles.svg',
                            title: 'Single-thread Read Latency Percentiles'],
        SequentialRead   : [stem: 'out-sequential-read-percentiles.svg',
                            title: 'Sequential Read Latency Percentiles'],
        ReadMultiThread  : [stem: 'out-read-multi-thread-percentiles.svg',
                            title: 'Multi-thread Read Latency Percentiles'],
        WriteMultiThread : [stem: 'out-write-multi-thread-percentiles.svg',
                            title: 'Multi-thread Write Latency Percentiles']
]

def colorForEngine = { String engine ->
    graphShared.colorForEngine.call(engine) as String
}

def darker = graphShared.darker

def describeResultFile = { Path file ->
    String rawEngine = file.fileName.toString()
            .replace('results-', '')
            .replace('.json', '')
    if (rawEngine.endsWith('-my')) {
        return null
    }

    def matcher = rawEngine =~ /^(write-single-thread|read-single-thread|sequential-read|write-multi-thread|read-multi-thread)-(.+?)(?:-threads\d+)?$/
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
    [scenario: scenarioNames[matcher.group(1)], engine: matcher.group(2)]
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

def percentileValue = { Map scorePercentiles, List<Map<String, Object>> bins,
        Map spec ->
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

def loadLatencySeries = { Path file ->
    byte[] bytes = Files.readAllBytes(file)
    if (bytes.length == 0) {
        return null
    }

    List entries
    try {
        entries = new JsonSlurper().parse(bytes) as List
    } catch (Exception e) {
        System.err.println("Skipping unreadable result file ${file.fileName}: ${e.message}")
        return null
    }

    if (!entries) {
        return null
    }

    Map sampleEntry = entries.find {
        (it['mode'] ?: '').toString() == 'sample' &&
                ((it['primaryMetric'] ?: [:])['scoreUnit'] ?: '').toString() == 'us/op'
    } as Map

    if (sampleEntry == null) {
        return null
    }

    Map primaryMetric = sampleEntry['primaryMetric'] as Map ?: [:]
    Map scorePercentiles = primaryMetric['scorePercentiles'] as Map ?: [:]
    List<Map<String, Object>> bins =
            flattenHistogramBins(primaryMetric['rawDataHistogram'])

    List<Double> values = percentileSpecs.collect { spec ->
        percentileValue(scorePercentiles, bins, spec) as Double
    }

    if (values.any { it == null || it <= 0d }) {
        return null
    }

    values
}

def dfs = new DecimalFormatSymbols(Locale.US)
dfs.groupingSeparator = ' ' as char
def latencyFormat = new DecimalFormat('#,##0.###', dfs)

def formatLatency = { double value ->
    "${latencyFormat.format(value)} us"
}

def buildLogTicks = { double minValue, double maxValue ->
    List<Double> ticks = []
    int minExp = (int) Math.floor(Math.log10(minValue))
    int maxExp = (int) Math.ceil(Math.log10(maxValue))
    for (int exp = minExp; exp <= maxExp; exp++) {
        [1d, 2d, 5d].each { factor ->
            double tick = factor * Math.pow(10d, exp)
            if (tick >= minValue && tick <= maxValue) {
                ticks << tick
            }
        }
    }
    if (ticks.isEmpty()) {
        ticks = [minValue, maxValue].unique()
    }
    ticks.unique().sort()
}

def renderChart = { String scenario, List<Map<String, Object>> series ->
    if (series.isEmpty()) {
        println "Skipping ${scenario}: no usable latency percentile data."
        return
    }

    def meta = scenarioMeta[scenario]
    if (meta == null) {
        println "Skipping ${scenario}: unsupported scenario."
        return
    }

    int legendColumns = Math.min(4, Math.max(1, series.size()))
    int legendRows = (int) Math.ceil(series.size() / (double) legendColumns)
    int width = 1120
    int xTickLabelOffset = 28
    int axisTitleOffset = 62
    int legendTopOffset = 104
    int legendRowHeight = 34
    int legendBottomPadding = 24
    int height = 760 + Math.max(0, legendRows - 1) * legendRowHeight
    int marginTop = 90
    int marginRight = 60
    int marginBottom = legendTopOffset + (legendRows * legendRowHeight) + legendBottomPadding
    int marginLeft = 110
    int plotWidth = width - marginLeft - marginRight
    int plotHeight = height - marginTop - marginBottom

    List<Double> allValues = series.collectMany { it.values as List<Double> }
    double minValue = allValues.min()
    double maxValue = allValues.max()
    double minLog = Math.floor(Math.log10(minValue))
    double maxLog = Math.ceil(Math.log10(maxValue))
    if (minLog == maxLog) {
        maxLog = minLog + 1d
    }

    def xForIndex = { int idx ->
        marginLeft + ((plotWidth / (double) (percentileSpecs.size() - 1)) * idx)
    }
    def yForValue = { double value ->
        double valueLog = Math.log10(value)
        marginTop + plotHeight -
                (((valueLog - minLog) / (maxLog - minLog)) * plotHeight)
    }

    List<Double> ticks = buildLogTicks(minValue, maxValue)

    StringWriter buffer = new StringWriter()
    MarkupBuilder svg = new MarkupBuilder(buffer)
    svg.doubleQuotes = true
    svg.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')

    svg.svg(xmlns: 'http://www.w3.org/2000/svg', width: width, height: height,
            viewBox: "0 0 ${width} ${height}") {
        style('''
        text { fill: currentColor; dominant-baseline: middle; }
        .title { font: 600 26px "Inter", "Helvetica Neue", Arial, sans-serif; }
        .subtitle { font: 16px "Inter", "Helvetica Neue", Arial, sans-serif; fill: #5B667A; }
        .axis-label { font: 500 16px "Inter", "Helvetica Neue", Arial, sans-serif; }
        .tick { font: 14px "IBM Plex Mono", "Courier New", monospace; fill: #5B667A; }
        .legend { font: 500 16px "Inter", "Helvetica Neue", Arial, sans-serif; }
        .grid { stroke: #D7DCE5; stroke-width: 1; fill: none; }
        .axis { stroke: #8A94A6; stroke-width: 1.5; fill: none; }
    ''')

        text(meta.title, class: 'title', x: marginLeft, y: 36)
        text('X axis: percentile, Y axis: latency (log scale)', class: 'subtitle',
                x: marginLeft, y: 66)

        ticks.each { tick ->
            double y = yForValue(tick)
            line(class: 'grid', x1: marginLeft, y1: y, x2: marginLeft + plotWidth, y2: y)
            text(formatLatency(tick), class: 'tick', x: marginLeft - 14, y: y,
                    'text-anchor': 'end')
        }

        line(class: 'axis', x1: marginLeft, y1: marginTop + plotHeight,
                x2: marginLeft + plotWidth, y2: marginTop + plotHeight)
        line(class: 'axis', x1: marginLeft, y1: marginTop,
                x2: marginLeft, y2: marginTop + plotHeight)

        percentileSpecs.eachWithIndex { spec, idx ->
            double x = xForIndex(idx)
            line(class: 'grid', x1: x, y1: marginTop, x2: x, y2: marginTop + plotHeight)
            text(spec.label as String, class: 'tick', x: x,
                    y: marginTop + plotHeight + xTickLabelOffset, 'text-anchor': 'middle')
        }

        text('Percentile', class: 'axis-label', x: marginLeft + (plotWidth / 2),
                y: marginTop + plotHeight + axisTitleOffset, 'text-anchor': 'middle')
        text('Latency (us/op)', class: 'axis-label', x: 28,
                y: marginTop + (plotHeight / 2),
                transform: "rotate(-90 28 ${marginTop + (plotHeight / 2)})",
                'text-anchor': 'middle')

        series.each { item ->
            String color = colorForEngine(item.engine as String)
            List<Double> values = item.values as List<Double>
            String points = values.withIndex().collect { entry ->
                double value = (entry[0] as Number).doubleValue()
                int idx = (entry[1] as Number).intValue()
                String.format(Locale.US, '%.2f,%.2f', xForIndex(idx), yForValue(value))
            }.join(' ')

            polyline(points: points, fill: 'none', stroke: color,
                    'stroke-width': 3, 'stroke-linejoin': 'round',
                    'stroke-linecap': 'round')

            values.eachWithIndex { double value, int idx ->
                circle(cx: xForIndex(idx), cy: yForValue(value), r: 4.5,
                        fill: color, stroke: darker(color), 'stroke-width': 1.5)
            }
        }

        double legendStartY = marginTop + plotHeight + legendTopOffset
        double legendColumnWidth = plotWidth / (double) legendColumns
        series.eachWithIndex { item, idx ->
            int row = (int) (idx / legendColumns)
            int col = idx % legendColumns
            double cellX = marginLeft + (legendColumnWidth * col)
            double y = legendStartY + (row * legendRowHeight)
            String color = colorForEngine(item.engine as String)
            line(x1: cellX + 10, y1: y, x2: cellX + 40,
                    y2: y, stroke: color, 'stroke-width': 4,
                    'stroke-linecap': 'round')
            text(item.engine as String, class: 'legend', x: cellX + 52,
                    y: y)
        }
    }

    Path outputPath = imagesDir.resolve(meta.stem as String)
    Files.writeString(outputPath, buffer.toString())
    println "Graph written to ${outputPath}"
}

Map<String, List<Map<String, Object>>> seriesByScenario = [:].withDefault { [] }

Files.newDirectoryStream(rawResultsDir, 'results-*.json').each { Path file ->
    String fileName = file.fileName.toString()
    if (fileName.endsWith('-my.json')) {
        return
    }

    def description = describeResultFile(file)
    if (description == null) {
        return
    }

    List<Double> values = loadLatencySeries(file)
    if (values == null) {
        println "Skipping ${description.engine} for ${description.scenario}: not enough latency percentile data."
        return
    }

    seriesByScenario[description.scenario] << [
            engine: description.engine,
            values: values
    ]
}

scenarioMeta.keySet().each { String scenario ->
    List<Map<String, Object>> series = (seriesByScenario[scenario] ?: [])
            .sort { a, b -> (a.engine as String) <=> (b.engine as String) }
    renderChart(scenario, series)
}
