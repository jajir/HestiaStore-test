#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
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

Path cwd = Paths.get('.').toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)
Path buildDir = resolveDirectoryFromEnv('REPORT_BUILD_DIR', rootDir.resolve('results'))
Path imagesDir = resolveDirectoryFromEnv('REPORT_IMAGES_DIR', rootDir.resolve('results'))
Files.createDirectories(imagesDir)

def datasets = []
def slurper = new JsonSlurper()
def graphShared = new GroovyShell().evaluate(
        rootDir.resolve('src/main/groovy/graphShared.groovy').toFile())

def datasetConfigs = [
        [
                input: buildDir.resolve('out-write-single-thread-table.json').toFile(),
                output: imagesDir.resolve('out-write-single-thread.svg').toFile(),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: buildDir.resolve('out-read-single-thread-table.json').toFile(),
                output: imagesDir.resolve('out-read-single-thread.svg').toFile(),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: buildDir.resolve('out-sequential-read-table.json').toFile(),
                output: imagesDir.resolve('out-sequential-read.svg').toFile(),
                valueKey: 'Score [ops/s]',
                labelSuffix: ''
        ],
        [
                input: buildDir.resolve('out-read-multi-thread-table.json').toFile(),
                output: imagesDir.resolve('out-read-multi-thread.svg').toFile(),
                valueKey: 'Throughput [ops/s]',
                labelSuffix: ' ops/s'
        ],
        [
                input: buildDir.resolve('out-write-multi-thread-table.json').toFile(),
                output: imagesDir.resolve('out-write-multi-thread.svg').toFile(),
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
    println "No benchmark summary files found in ${buildDir}. Skipping SVG generation."
    System.exit(0)
}

def colorForEngine = { String engine ->
    graphShared.colorForEngine.call(engine) as String
}

def darker = graphShared.darker

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
