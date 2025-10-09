#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.*
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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

println '# Benchmark Results'
println ''
// Table header printed at the beginning
println '| Engine       | Score     | Error     | Confidence Interval        |'
println '|--------------|-----------|-----------|----------------------------|'

files.each { Path file ->
    def engine = file.fileName.toString().replace('results-', '').replace('.json', '')
    def benchmarks = mapper.readValue(file.toFile(), List)

    // Helper: get nested value by path like "primaryMetric.scoreConfidence[0]"
    def getByPath
    getByPath = { Object root, String path ->
        def cur = root
        if (path == null || path.isEmpty()) return cur
        path.split('\n') // avoid accidental newlines
        path.split(/\./).each { part ->
            if (cur == null) return null
            def m = (part =~ /(.*?)\[(\d+)]$/)
            if (m.matches()) {
                def name = m[0][1]
                def idx = Integer.parseInt(m[0][2])
                if (name) {
                    cur = (cur instanceof Map) ? cur[name] : null
                }
                cur = (cur instanceof List) ? cur.getAt(idx) : null
            } else {
                cur = (cur instanceof Map) ? cur[part] : null
            }
        }
        return cur
    }

    // Helper: try multiple alternative paths for compatibility
    def firstOf = { obj, List<String> paths ->
        for (p in paths) {
            def v = getByPath(obj, p)
            if (v != null) return v
        }
        return null
    }

    // number formatter: group thousands with space, no decimals
    def dfs = new DecimalFormatSymbols(Locale.US)
    dfs.groupingSeparator = ' ' as char
    def df = new DecimalFormat('#,##0', dfs)
    def fmtNum = { Object v ->
        if (!(v instanceof Number)) return ''
        return df.format((v as Number).doubleValue())
    }

    benchmarks.each { benchmark ->
        def scoreVal = firstOf(benchmark, ['primaryMetric.score', 'score'])
        def errorVal = firstOf(benchmark, ['primaryMetric.scoreError', 'scoreError'])
        def lo = firstOf(benchmark, ['primaryMetric.scoreConfidence[0]', 'scoreConfidence[0]'])
        def hi = firstOf(benchmark, ['primaryMetric.scoreConfidence[1]', 'scoreConfidence[1]'])

        def scoreStr = fmtNum(scoreVal).toString().padLeft(9)
        def errorStr = fmtNum(errorVal).toString().padLeft(9)
        def ci = (lo instanceof Number && hi instanceof Number) ? (fmtNum(lo) + ' .. ' + fmtNum(hi)) : ''

        println String.format('| %-12s | %s | %s | %s |', engine, scoreStr, errorStr, ci)
    }
}

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
