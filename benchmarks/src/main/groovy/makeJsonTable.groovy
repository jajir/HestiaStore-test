#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Find project root (directory containing a pom.xml highest up from CWD)
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

def formatScore = { Object value ->
    Double number = normalizeNumber(value)
    if (number == null) {
        return ''
    }
    return scoreFormat.format(number)
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
    boolean isExplicitWrite = false
    String engineBase = rawEngine

    if (rawEngine.startsWith('read-')) {
        isReadVariant = true
        engineBase = rawEngine.substring('read-'.length())
    } else if (rawEngine.endsWith('Read')) { // fallback for legacy files
        isReadVariant = true
        engineBase = rawEngine.substring(0, rawEngine.length() - 'Read'.length())
    } else if (rawEngine.startsWith('write-')) {
        isExplicitWrite = true
        engineBase = rawEngine.substring('write-'.length())
    }

    String scenario = isReadVariant ? 'Read' : 'Write'
    // String engine = isReadVariant ? "${engineBase} (Read)" : engineBase
    String engine = engineBase

    def data = mapper.readValue(Files.readAllBytes(file), List)
    if (data.isEmpty()) {
        return
    }
    def entry = data[0] as Map
    def primary = entry['primaryMetric'] as Map
    Double score = normalizeNumber(primary?.get('score'))
    Double scoreError = normalizeNumber(primary?.get('scoreError'))
    List confidence = primary?.get('scoreConfidence') as List
    String confidenceInterval = ''
    if (confidence?.size() == 2) {
        Double lo = normalizeNumber(confidence[0])
        Double hi = normalizeNumber(confidence[1])
        if (lo != null && hi != null) {
            confidenceInterval = "${formatScore(lo)} .. ${formatScore(hi)}"
        }
    }

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

    rows << [
            'Engine': engine,
            'Variant': scenario,
            'Score [ops/s]': formatScore(score),
            'ScoreError': formatScore(scoreError),
            'Confidence Interval [ops/s]': confidenceInterval,
            'Occupied space': occupied,
            'usedMemoryBytes': usedMemoryStr,
            'cpuUsage': cpuUsageStr
    ]
}

def writeRows = rows.findAll { (it['Variant'] ?: 'Write') == 'Write' }
def readRows = rows.findAll { (it['Variant'] ?: 'Write') == 'Read' }

Path writeOutput = resultsDir.resolve('out-write-table.json')
Path readOutput = resultsDir.resolve('out-read-table.json')

mapper.writeValue(writeOutput.toFile(), writeRows)
mapper.writeValue(readOutput.toFile(), readRows)

println "Written summaries to ${writeOutput} and ${readOutput}"
