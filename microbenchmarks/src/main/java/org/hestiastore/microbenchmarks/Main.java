package org.hestiastore.microbenchmarks;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class Main {

    private final static String TEST_CLASS_NAME_NAME = "testClassName";

    public static void main(String[] args) throws Exception {

        final String benchmarkTestName = System
                .getProperty(TEST_CLASS_NAME_NAME);
        final String resultPath = "results/" + benchmarkTestName + ".json";
        final String includePattern = "org.hestiastore.microbenchmarks.collections."
                + benchmarkTestName + "";

        System.out.println("==> Benchmark test name: " + includePattern);
        System.out.println("==> Results file       : " + resultPath);
        System.out.println("==> Include pattern    : " + includePattern);

        final Options opt = new OptionsBuilder()//
                .include(includePattern)//
                .forks(1)//
                .resultFormat(ResultFormatType.JSON)//
                .result(resultPath)//
                .verbosity(VerboseMode.NORMAL)//
                .build();

        new Runner(opt).run();
    }
}
