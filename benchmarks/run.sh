#!/bin/bash

#mvn clean package


run(){
  java \
    -Ddir=/Volumes/ponrava/test-index \
	-agentpath:/Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=exceptions=disable,delay=10000,listen=all \
    -Dengine=$1 \
    -cp "target/classes:target/lib/*" \
    org.hestiastore.index.benchmark.plainload.Main
}

#run H2
#run MapDB
run HestiaStoreBasic
run HestiaStoreCompress
#run ChronicleMap
#run RocksDB
#run LevelDB

pok(){
    # it's just clumsy backup of all the --add-opens used in the jmh-maven-plugin
    # when it's needed, just copy-paste it into the java command above
    java \
	-agentpath:/Applications/YourKit-Java-Profiler-2024.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=exceptions=disable,delay=10000,listen=all \
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

}