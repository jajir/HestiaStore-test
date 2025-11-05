package org.hestiastore.index.benchmark.load;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class Main {

    public static void main(final String[] args) throws Exception {

        final Options opt = new OptionsBuilder()//
                .include(IndexWritingBenchmark.class.getSimpleName())//
                .forks(1)//
                .resultFormat(ResultFormatType.CSV)//
                .result("target/index-writing-benchmark.csv")//
                .verbosity(VerboseMode.NORMAL)//
                .build();

        new Runner(opt).run();

    }
}
