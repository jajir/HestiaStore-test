#!/usr/bin/env bash
#
# before running this script, ensure you have built the project with:
#   mvn -Dmaven.compiler.useIncrementalCompilation=falseclean package
#

run(){
  java \
    -Ddir=/Volumes/ponrava/test-index \
	-Xmx10000m \
    -DtestClassName=$1 \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.microbenchmarks.Main
}

echo "==> Starting"

#mvn -q -DskipTests=true package

echo "==> Project was built"

#run DiffKeyReaderBenchmark
#run UniqueCacheBenchmark
#run SingleChunkEntryWriterBenchmark

#run RandomReadsInputStreamBenchmark
#run RandomReadsRandomFileAccessBenchmark
run RandomReadsChannelsBenchmark

echo "==> Done"

