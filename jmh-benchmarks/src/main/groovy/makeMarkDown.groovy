#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.*
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

def printFile(Path rootDir, String file) {
    println new String(Files.readAllBytes(rootDir.resolve(file)), StandardCharsets.UTF_8)
}


// Find project root (directory containing a pom.xml highest up from CWD)
def findProjectRoot(Path start) {
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

def cwd = Paths.get('.')
def rootDir = findProjectRoot(cwd)

def mapper = new ObjectMapper()

// Collect and sort all result JSON files from project's ./results directory
def files = []
def resultsDir = rootDir.resolve('results')
if (Files.exists(resultsDir)) {
    try {
        Files.newDirectoryStream(resultsDir, 'results-*.json').each { files << it }
    } catch (ignored) {
        // if glob not supported, fall back to list + filter
        Files.list(resultsDir).filter { it.fileName.toString().startsWith('results-') && it.fileName.toString().endsWith('.json') }
                .forEach { files << it }
    }
}
files.sort { a, b -> a.fileName.toString() <=> b.fileName.toString() }

println '# HestiaStore Benchmark Results'
println ''
printFile resultsDir, 'test-conditions.md'
println ''
println '## Benchmark Results'
println ''
def tablePath = resultsDir.resolve('table.json')
def tableRows = []
if (Files.exists(tablePath)) {
    tableRows = mapper.readValue(tablePath.toFile(), List)
} else {
    System.err.println("Warning: summary table file '${tablePath}' not found; table section will be empty.")
}

// Table header printed at the beginning
println '| Engine       | Score [ops/s]     | ScoreError | Confidence Interval [ops/s] | Occupied space | CPU Usage |'
println '|:--------------|-----------------:|-----------:|-----------------------------:|---------------:|---------:|'

tableRows.each { row ->
    def engine = (row['Engine'] ?: '').toString()
    def score = (row['Score [ops/s]'] ?: '').toString()
    def error = (row['ScoreError'] ?: '').toString()
    def ci = (row['Confidence Interval [ops/s]'] ?: '').toString()
    def occupied = (row['Occupied space'] ?: '').toString()
    def cpuUsage = (row['cpuUsage'] ?: '').toString()
    println "| ${engine.padRight(12)} | ${score.padLeft(15)} | ${error.padLeft(9)} | ${ci.padRight(27)} | ${occupied.padRight(14)} | ${cpuUsage.padRight(10)} |"
}

println ''
println 'meaning of columns:'
println ''
println '- Engine: name of the benchmarked engine (as derived from the JSON filename)'
println '- Score [ops/s]: number of operations per second (higher is better)'
println '- ScoreError: error margin of the score (lower is better). It\'s computed as `z * (stdev / sqrt(n)) where`'
println '  - `z` is the z-score for the desired confidence level (1.96 for 95%)'
println '  - `stdev` is the standard deviation of the measurements'
println '  - `n` is the number of measurements'
println '- Confidence Interval: 95% confidence interval of the score (lower and upper bound). This means that the true mean is likely between this interval of ops/sec. Negative values are possible if the error margin is larger than the score itself.'
println '- Occupied space : amount of disk space occupied by the engine\'s data structures (lower is better). It is measured after flushing last data to disk.'
println '- CPU Usage: average CPU usage during the benchmark (lower is better). Please note, that it includes all system processes, not only the benchmarked engine.'
println ''
println '## Raw JSON Files'
files.each { Path file ->
    println ''
    println '### ' + file.fileName.toString()
    println ''
    println '```json'
    println new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    println '```'
}
