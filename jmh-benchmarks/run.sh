#!/bin/bash

#mvn clean package


run(){
  java \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
    --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
    -Ddir=/Volumes/ponrava/test-index \
    -Dengine=$1 \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.index.benchmark.plainload.Main
}

#run H2
#run MapDB
#run HestiaStoreBasic
#run HestiaStoreCompress
run ChronicleMap

#run HestiaStoreCompress
