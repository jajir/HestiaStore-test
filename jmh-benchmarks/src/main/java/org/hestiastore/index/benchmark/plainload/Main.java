package org.hestiastore.index.benchmark.plainload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final static String PROPERTY_ENGINE = "engine";
    private final static String ENGINE_HESTIASTORE = "HestiaStore";
    private final static String PROPERTY_MAPDB = "MapDB";
    private final static String PROPERTY_H2 = "H2";

    private final static long TEST_ITERATIOSN = 1_000_000L;

    public static void main(final String[] args) throws Exception {

        String engine = System.getProperty(PROPERTY_ENGINE);
        LOGGER.debug("Property 'engine' is '" + engine + "'");
        if (engine == null || engine.isEmpty()) {
            throw new IllegalStateException("Property 'engine' is not set");
        }

        if (ENGINE_HESTIASTORE.equals(engine)) {
            final TestHestiaStore test = new TestHestiaStore();
            test.setup();
            test.test(TEST_ITERATIOSN);
            test.tearDown();
        } else if (PROPERTY_MAPDB.equals(engine)) {
            final TestMapDB test = new TestMapDB();
            test.setup();
            test.test(TEST_ITERATIOSN);
            test.tearDown();
        } else if (PROPERTY_H2.equals(engine)) {
            final TestH2 test = new TestH2();
            test.setup();
            test.test(TEST_ITERATIOSN);
            test.tearDown();
        } else {
            throw new IllegalStateException("Unknown engine '" + engine + "'");
        }

    }
}
