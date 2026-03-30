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
Path sequentialTable = resultsDir.resolve("out-sequential-table.json")
Path multithreadReadTable = resultsDir.resolve("out-multithread-read-table.json")
Path multithreadWriteTable = resultsDir.resolve("out-multithread-write-table.json")

ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
List<Map<String, Object>> writeRows = Files.exists(writeTable)
        ? mapper.readValue(writeTable.toFile(), List)
        : []
List<Map<String, Object>> readRows = Files.exists(readTable)
        ? mapper.readValue(readTable.toFile(), List)
        : []
List<Map<String, Object>> sequentialRows = Files.exists(sequentialTable)
        ? mapper.readValue(sequentialTable.toFile(), List)
        : []
List<Map<String, Object>> multithreadReadRows = Files.exists(multithreadReadTable)
        ? mapper.readValue(multithreadReadTable.toFile(), List)
        : []
List<Map<String, Object>> multithreadWriteRows = Files.exists(multithreadWriteTable)
        ? mapper.readValue(multithreadWriteTable.toFile(), List)
        : []

if (writeRows.isEmpty() && readRows.isEmpty() && sequentialRows.isEmpty()
        && multithreadReadRows.isEmpty() && multithreadWriteRows.isEmpty()) {
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

def buildMultithreadMarkdown = { List<Map<String, Object>> rows ->
    StringBuilder markdown = new StringBuilder()
    markdown.append("| Engine | Threads | Throughput [ops/s] | Mean [us/op] | p50 [us/op] | p95 [us/op] | p99 [us/op] | CPU Usage |\n")
    markdown.append("|:-------|--------:|-------------------:|-------------:|------------:|------------:|------------:|----------:|\n")
    rows.each { row ->
        String engine = (row["Engine"] ?: "").toString()
        String threads = (row["Threads"] ?: "").toString()
        String throughput = (row["Throughput [ops/s]"] ?: "").toString()
        String mean = (row["Mean [us/op]"] ?: "").toString()
        String p50 = (row["p50 [us/op]"] ?: "").toString()
        String p95 = (row["p95 [us/op]"] ?: "").toString()
        String p99 = (row["p99 [us/op]"] ?: "").toString()
        String cpuUsage = (row["cpuUsage"] ?: "").toString()
        markdown.append("| ${engine} | ${threads} | ${throughput} | ${mean} | ${p50} | ${p95} | ${p99} | ${cpuUsage} |\n")
    }
    return markdown.toString()
}

Path writeOutput = resultsDir.resolve("out-write-table.md")
Path readOutput = resultsDir.resolve("out-read-table.md")
Path sequentialOutput = resultsDir.resolve("out-sequential-table.md")
Path multithreadReadOutput = resultsDir.resolve("out-multithread-read-table.md")
Path multithreadWriteOutput = resultsDir.resolve("out-multithread-write-table.md")

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

if (!sequentialRows.isEmpty()) {
    Files.writeString(sequentialOutput, buildMarkdown(sequentialRows))
    println("Wrote ${sequentialOutput}")
} else if (Files.exists(sequentialOutput)) {
    Files.delete(sequentialOutput)
}

if (!multithreadReadRows.isEmpty()) {
    Files.writeString(multithreadReadOutput,
            buildMultithreadMarkdown(multithreadReadRows))
    println("Wrote ${multithreadReadOutput}")
} else if (Files.exists(multithreadReadOutput)) {
    Files.delete(multithreadReadOutput)
}

if (!multithreadWriteRows.isEmpty()) {
    Files.writeString(multithreadWriteOutput,
            buildMultithreadMarkdown(multithreadWriteRows))
    println("Wrote ${multithreadWriteOutput}")
} else if (Files.exists(multithreadWriteOutput)) {
    Files.delete(multithreadWriteOutput)
}
