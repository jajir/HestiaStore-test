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

Path writeTable = resultsDir.resolve("out-write-table.json")
Path readTable = resultsDir.resolve("out-read-table.json")

ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
List<Map<String, Object>> writeRows = Files.exists(writeTable)
        ? mapper.readValue(writeTable.toFile(), List)
        : []
List<Map<String, Object>> readRows = Files.exists(readTable)
        ? mapper.readValue(readTable.toFile(), List)
        : []

if (writeRows.isEmpty() && readRows.isEmpty()) {
    System.err.println("No summary JSON files found in ${resultsDir}")
    System.exit(1)
}

def buildMarkdown = { List<Map<String, Object>> rows ->
    StringBuilder markdown = new StringBuilder()
    markdown.append("| Engine | Score [ops/s] | Occupied space | CPU Usage |\n")
    markdown.append("|:-------|--------------:|---------------:|----------:|\n")
    rows.each { row ->
        String engine = (row["Engine"] ?: "").toString()
        String score = (row["Score [ops/s]"] ?: "").toString()
        String occupied = (row["Occupied space"] ?: "").toString()
        String cpuUsage = (row["cpuUsage"] ?: "").toString()
        markdown.append("| ${engine} | ${score.padLeft(13)} | ${occupied} | ${cpuUsage} |\n")
    }
    return markdown.toString()
}

Path writeOutput = resultsDir.resolve("out-write-table.md")
Path readOutput = resultsDir.resolve("out-read-table.md")

if (!writeRows.isEmpty()) {
    Files.writeString(writeOutput, buildMarkdown(writeRows))
    println("Wrote ${writeOutput}")
} else if (Files.exists(writeOutput)) {
    Files.delete(writeOutput)
}

if (!readRows.isEmpty()) {
    Files.writeString(readOutput, buildMarkdown(readRows))
    println("Wrote ${readOutput}")
} else if (Files.exists(readOutput)) {
    Files.delete(readOutput)
}
