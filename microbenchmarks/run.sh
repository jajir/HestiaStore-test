#!/usr/bin/env bash
#
# before running this script, ensure you have built the project with:
#   mvn clean package
#

run(){
  java \
    -Ddir=/Volumes/ponrava/test-index \
    -DtestClassName=$1 \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.microbenchmarks.Main
}

echo "==> Starting"

#mvn -q -DskipTests=true package

echo "==> Project was built"

#run TreeMapCacheBenchmark
run UniqueCacheBenchmark

echo "==> Done"


