package org.hestiastore.index.benchmark.plainload;

public class Main {

    public static void main(final String[] args) throws Exception {
        final SimpleLoadTest test = new SimpleLoadTest();
        test.setup();
        final long howMuch = 1_000_000L;
        test.test(howMuch);
        test.tearDown();
    }
}
