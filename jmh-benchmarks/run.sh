#!/bin/bash

#mvn clean package


run(){
  java \
    -Ddir=/Volumes/ponrava/test-index \
    -Dengine=$1 \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.index.benchmark.plainload.Main
}

run H2
run MapDB
run HestiaStoreBasic
run HestiaStoreCompress

