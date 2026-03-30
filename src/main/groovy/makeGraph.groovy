#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.util.Locale

File baseDir = new File(System.getProperty('user.dir'))
def datasets = []
def slurper = new JsonSlurper()

def datasetConfigs = [
        [
                input: new File(baseDir, 'results/out-write-table.json'),
                output: new File(baseDir, 'results/out-write.svg'),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: new File(baseDir, 'results/out-read-table.json'),
                output: new File(baseDir, 'results/out-read.svg'),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: new File(baseDir, 'results/out-sequential-table.json'),
                output: new File(baseDir, 'results/out-sequential.svg'),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: new File(baseDir,
                        'results/out-multithread-read-table.json'),
                output: new File(baseDir, 'results/out-multithread-read.svg'),
                valueKey: 'Throughput [ops/s]',
                labelSuffix: ' ops/s'
        ],
        [
                input: new File(baseDir,
                        'results/out-multithread-write-table.json'),
                output: new File(baseDir, 'results/out-multithread-write.svg'),
                valueKey: 'Throughput [ops/s]',
                labelSuffix: ' ops/s'
        ]
]

datasetConfigs.each { config ->
    File inputFile = config.input as File
    if (inputFile.exists()) {
        def rows = slurper.parse(inputFile) as List
        if (!rows.isEmpty()) {
            datasets << [
                    rows: rows,
                    output: config.output,
                    valueKey: config.valueKey,
                    labelSuffix: config.labelSuffix
            ]
        }
    }
}

if (datasets.isEmpty()) {
    println "No benchmark summary files found in results/. Skipping SVG generation."
    System.exit(0)
}

def palette = [
        '#4E79A7', '#F28E2B', '#E15759', '#76B7B2', '#59A14F',
        '#EDC948', '#B07AA1', '#FF9DA7', '#9C755F', '#BAB0AC'
]

def canonicalEngineName = { String engine ->
    String value = engine?.trim()
    if (!value) {
        return 'Unknown'
    }
    if (value.startsWith('HestiaStoreBasic')) {
        return 'HestiaStoreBasic'
    }
    if (value.startsWith('HestiaStoreCompress')) {
        return 'HestiaStoreCompress'
    }
    if (value.startsWith('HestiaStoreStream')) {
        return 'HestiaStoreStream'
    }
    return value
}

def fixedEngineColors = [
        ChronicleMap       : '#4E79A7',
        H2                 : '#F28E2B',
        HestiaStoreBasic   : '#E15759',
        HestiaStoreCompress: '#E15759',
        HestiaStoreStream  : '#E15759',
        LevelDB            : '#59A14F',
        MapDB              : '#B07AA1',
        RocksDB            : '#9299fb',
        Unknown            : '#9C755F'
]

def fallbackColorFor = { String engine ->
    int index = Math.floorMod(engine.hashCode(), palette.size())
    palette[index]
}

def colorForEngine = { String engine ->
    String canonical = canonicalEngineName(engine)
    fixedEngineColors[canonical] ?: fallbackColorFor(canonical)
}

def darker = { String hex ->
    int rgb = Integer.parseInt(hex.substring(1), 16)
    int r = ((rgb >> 16) & 0xFF)
    int g = ((rgb >> 8) & 0xFF)
    int b = (rgb & 0xFF)
    r = Math.max(0, (int) (r * 0.85))
    g = Math.max(0, (int) (g * 0.85))
    b = Math.max(0, (int) (b * 0.85))
    String.format('#%02X%02X%02X', r, g, b)
}

def renderChart = { List data, File outputFile, String valueKey,
        String labelSuffix ->
    if (data.isEmpty()) {
        return
    }
    def processed = data.collect { row ->
        def rawScore = row[valueKey]?.toString()?.replaceAll('[^0-9.]', '')
        long scoreValue = rawScore ? rawScore.toLong() : 0L
        [engine: row.Engine?.toString() ?: 'Unknown', score: scoreValue]
    }
    def maxScore = processed*.score.max() ?: 1L

    int width = 1000
    int margin = 0
    int engineLeftMargin = margin + 20
    int rowHeight = 60
    int legendWidth = 250
    int barHeight = 26
    int barMaxWidth = width - (margin * 2) - legendWidth - 160
    int height = (processed.size() * rowHeight) + margin

    StringWriter buffer = new StringWriter()
    MarkupBuilder svg = new MarkupBuilder(buffer)
    svg.doubleQuotes = true
    svg.mkp.xmlDeclaration(version: '1.0', encoding: 'UTF-8')

    svg.svg(xmlns: 'http://www.w3.org/2000/svg', width: width, height: height,
            viewBox: "0 0 ${width} ${height}") {
        style('''
        text { fill: currentColor; dominant-baseline: middle; }
        .label { font: 500 20px "Inter", "Helvetica Neue", Arial, sans-serif; }
        .value { font: 18px "IBM Plex Mono", "Courier New", monospace; }
    ''')

        processed.eachWithIndex { item, idx ->
            int rowTop = idx * rowHeight
            int rowCenter = rowTop + (rowHeight / 2)
            float ratio = item.score / (float) maxScore
            int barWidth = Math.max(8, (int) (barMaxWidth * ratio))
            int barX = margin + legendWidth
            int barY = rowCenter - (barHeight / 2)

            rect(x: margin / 2, y: rowTop + 10, width: width - margin,
                    height: rowHeight - 20, fill: 'rgba(255,255,255,0.15)')

            text(item.engine, class: 'label', x: engineLeftMargin, y: rowCenter)

            String barColor = colorForEngine(item.engine)
            rect(x: barX, y: barY, width: barWidth, height: barHeight,
                    rx: 6, ry: 6, fill: barColor, stroke: darker(barColor),
                    'stroke-width': 2)

            String formattedScore = String.format(Locale.US, '%,d%s',
                    item.score, labelSuffix)
            text(formattedScore, class: 'value', x: barX + barWidth + 16, y: rowCenter)
        }
    }

    outputFile.parentFile.mkdirs()
    outputFile.setText(buffer.toString(), 'UTF-8')
    println "Graph written to ${outputFile.absolutePath}"
}

datasets.each { dataset ->
    renderChart(dataset.rows as List, dataset.output as File,
            dataset.valueKey as String, dataset.labelSuffix as String)
}
