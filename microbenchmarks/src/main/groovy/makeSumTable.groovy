#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import java.math.RoundingMode

/**
 * Generate a markdown comparison table from JMH JSON results.
 *
 * Usage:
 *   groovy src/main/groovy/GenerateBenchmarkComparison.groovy [resultsDir] [outputFile]
 *
 * - resultsDir: directory containing .json files (defaults to first existing of:
 *               ./result, ./results, ./microbenchmarks/results)
 * - outputFile: path for the generated markdown (defaults to <resultsDir>/benchmark-comparison.md)
 */

class Row {
    String fileBase
    String method
    String score
    String unit
    String error
}

static String fmt2(def v) {
    if (v == null) return ''
    try {
        def bd = new BigDecimal(v.toString())
        bd = bd.setScale(2, RoundingMode.HALF_UP)
        return bd.stripTrailingZeros().toPlainString()
    } catch (ignored) {
        return v.toString()
    }
}

static File resolveResultsDir(String[] args) {
    if (args && args[0]) {
        def dir = new File(args[0])
        if (dir.exists() && dir.isDirectory()) return dir
        System.err.println("Provided results directory does not exist: ${dir}")
        System.exit(2)
    }

    // Try common defaults
    def candidates = [
            new File("result"),
            new File("results"),
            new File("microbenchmarks/results")
    ]
    def found = candidates.find { it.exists() && it.isDirectory() }
    if (!found) {
        System.err.println("Could not locate results directory. Tried: ${candidates*.path.join(', ')}")
        System.exit(2)
    }
    return found
}

static List<File> findJsonFiles(File dir) {
    def files = dir.listFiles({ File f -> f.isFile() && (f.name.toLowerCase().endsWith('.json') || f.name.toLowerCase().endsWith('.josn')) } as FileFilter)
    return (files ?: []).sort { it.name.toLowerCase() }
}

static List<Row> extractRows(File jsonFile) {
    if (jsonFile.length() == 0L) {
        return []
    }
    def slurper = new JsonSlurper()
    def parsed
    try {
        parsed = slurper.parse(jsonFile)
    } catch (Exception ex) {
        System.err.println("Skipping unreadable JSON file: ${jsonFile.name} (${ex.class.simpleName}: ${ex.message?.split('\\n')?.first()})")
        return []
    }

    List<Map> entries
    if (parsed instanceof List) {
        entries = parsed as List<Map>
    } else if (parsed instanceof Map) {
        entries = [parsed as Map]
    } else {
        entries = []
    }

    def fname = jsonFile.name
    def lastDot = fname.lastIndexOf('.')
    def fileBase = lastDot > 0 ? fname.substring(0, lastDot) : fname

    return entries.collect { Map e ->
        def bench = (e.benchmark ?: '').toString()
        def method = bench.contains('.') ? bench.substring(bench.lastIndexOf('.') + 1) : bench
        def pm = (e.primaryMetric instanceof Map) ? (Map) e.primaryMetric : Collections.emptyMap()
        def score = pm.score != null ? fmt2(pm.score) : ''
        def unit = pm.scoreUnit != null ? pm.scoreUnit.toString() : ''
        def error = pm.scoreError != null ? fmt2(pm.scoreError) : ''
        new Row(fileBase: fileBase, method: method, score: score, unit: unit, error: error)
    }
}

static void writeMarkdown(File outFile, List<Row> rows) {
    outFile.parentFile?.mkdirs()
    outFile.withWriter('UTF-8') { w ->
        w.println("# Microbenchmark Comparison")
        w.println("")
        w.println("| File | Method | Score | Unit | Error |")
        w.println("|------|--------|-------|------|-------|")
        rows.each { r ->
            w.println("| ${r.fileBase} | ${r.method} | ${r.score} | ${r.unit} | ${r.error} |")
        }
    }
}

def resultsDir = resolveResultsDir(this.args as String[])
def jsonFiles = findJsonFiles(resultsDir)

if (!jsonFiles) {
    System.err.println("No .json files found in ${resultsDir}")
    System.exit(1)
}

def allRows = jsonFiles.collectMany { f -> extractRows(f) }
// Stable sort: by file then method
allRows = allRows.sort { a, b ->
    int c = a.fileBase <=> b.fileBase
    if (c != 0) return c
    return a.method <=> b.method
}

File outFile
if (this.args?.length >= 2 && this.args[1]) {
    outFile = new File(this.args[1])
} else {
    outFile = new File(resultsDir, 'benchmark-comparison.md')
}

writeMarkdown(outFile, allRows)
println("Wrote markdown comparison to: ${outFile}")
