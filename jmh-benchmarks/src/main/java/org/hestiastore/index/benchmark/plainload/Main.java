package org.hestiastore.index.benchmark.plainload;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final static String PROPERTY_ENGINE = "engine";
    private final static String ENGINE_HESTIASTORE = "HestiaStoreBasic";
    private final static String ENGINE_HESTIASTORE_COMPRESS = "HestiaStoreCompress";
    private final static String PROPERTY_MAPDB = "MapDB";
    private final static String PROPERTY_H2 = "H2";
    private final static String PROPERTY_CHRONICLE_MAP = "ChronicleMap";

    /**
     * Main entry that runs the selected JMH benchmark class.
     */
    public static void main(final String[] args) throws Exception {

        final String engine = System.getProperty(PROPERTY_ENGINE);
        LOGGER.debug("Property 'engine' is '" + engine + "'");
        if (engine == null || engine.isEmpty()) {
            throw new IllegalStateException("Property 'engine' is not set");
        }

        final String includePattern;
        if (ENGINE_HESTIASTORE.equals(engine)) {
            includePattern = TestHestiaStoreBasic.class.getSimpleName();
        } else if (ENGINE_HESTIASTORE_COMPRESS.equals(engine)) {
            includePattern = TestHestiaStoreCompress.class.getSimpleName();
        } else if (PROPERTY_MAPDB.equals(engine)) {
            includePattern = TestMapDB.class.getSimpleName();
        } else if (PROPERTY_H2.equals(engine)) {
            includePattern = TestH2.class.getSimpleName();
        } else if (PROPERTY_CHRONICLE_MAP.equals(engine)) {
            includePattern = TestChronicleMap.class.getSimpleName();
        } else {
            throw new IllegalStateException("Unknown engine '" + engine + "'");
        }

        final Options opt = new OptionsBuilder()
                .include(".*" + includePattern + "")//
                .forks(1)//
                .resultFormat(ResultFormatType.JSON)//
                .result("./results/results-" + engine + ".json")//
                .build()//
        ;

        for (RunResult r : new Runner(opt).run()) {
            LOGGER.info("Benchmark: {} -> {} ops/s",
                    r.getParams().getBenchmark(),
                    r.getPrimaryResult().getScore());
        }
    }
}
