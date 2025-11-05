#!/bin/sh
#
#
#
cd `dirname $0`
cd ../..

DIR=./target/

RUN=java -jar target/benchmarks.jar com.coroptis.index.loadtest.Main
