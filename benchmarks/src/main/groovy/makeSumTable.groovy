#!/usr/bin/env groovy
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

Path findProjectRoot(Path start) {
    Path cur = start
    Path lastPom = null
    while (cur != null) {
        if (Files.exists(cur.resolve("pom.xml"))) {
            lastPom = cur
        }
        cur = cur.parent
    }
    return lastPom ?: start
}

Path cwd = Paths.get(".").toAbsolutePath().normalize()
Path rootDir = findProjectRoot(cwd)

List<Path> candidateDirs = [
        rootDir.resolve("results"),
        cwd.resolve("results")
]

Path resultsDir = candidateDirs.find { Files.exists(it) && Files.isDirectory(it) }

if (resultsDir == null) {
    System.err.println("Results directory not found. Checked: ${candidateDirs*.toString().join(', ')}")
    System.exit(1)
}

Path tableJson = resultsDir.resolve("table.json")
if (!Files.exists(tableJson)) {
    System.err.println("Input file ${tableJson} not found.")
    System.exit(1)
}

ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
List<Map<String, Object>> rows = mapper.readValue(tableJson.toFile(), List)

Path output = resultsDir.resolve("sum-table.md")

StringBuilder markdown = new StringBuilder()
markdown.append("| Engine | Score [ops/s] | Occupied space | CPU Usage |\n")
markdown.append("|:-------|--------------:|---------------:|----------:|\n")

rows.each { row ->
    String engine = (row["Engine"] ?: "").toString()
    String score = (row["Score [ops/s]"] ?: "").toString()
    String occupied = (row["Occupied space"] ?: "").toString()
    // String usedMemory = (row["usedMemoryBytes"] ?: "").toString()
    String cpuUsage = (row["cpuUsage"] ?: "").toString()
    markdown.append("| ${engine} | ${score.padLeft(13)} | ${occupied} | ${cpuUsage} |\n")
}

Files.writeString(output, markdown.toString())
println("Wrote ${output}")
