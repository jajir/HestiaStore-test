package org.hestiastore.index.benchmark.plainload;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for read-focused benchmarks. It preloads a fixed number of entries
 * and provides helpers to pick keys for hit or miss scenarios.
 */
abstract class AbstractReadTest extends AbstractWriteTest {

    protected static final long PRELOAD_ENTRY_COUNT = 10_000_000L;
    private static final double MISS_PROBABILITY = 0.2d; // 20% misses

    @FunctionalInterface
    protected interface DataWriter {
        void write(String key, String value) throws Exception;
    }

    /**
     * Creates deterministic key based on numeric index.
     */
    protected String keyForIndex(final long index) {
        return HASH_DATA_PROVIDER.makeHash(index);
    }

    /**
     * Preloads the dataset by delegating to the supplied writer.
     */
    protected void preloadDataset(final DataWriter writer) {
        for (long i = 0; i < PRELOAD_ENTRY_COUNT; i++) {
            final String key = keyForIndex(i);
            try {
                writer.write(key, VALUE);
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Unable to preload dataset at index " + i, e);
            }
        }
    }

    protected String randomExistingKey() {
        // Always pick an index from the preloaded range [0,
        // PRELOAD_ENTRY_COUNT).
        // ThreadLocalRandom avoids contention between benchmark threads and
        // keeps
        // the hit-rate stable.
        final long index = ThreadLocalRandom.current()
                .nextLong(PRELOAD_ENTRY_COUNT);
        return keyForIndex(index);
    }

    protected String randomMissingKey() {
        // Pick an index strictly outside the preloaded range to force a miss.
        final long index = PRELOAD_ENTRY_COUNT
                + ThreadLocalRandom.current().nextLong(PRELOAD_ENTRY_COUNT);
        return keyForIndex(index);
    }

    protected String pickReadKey() {
        return ThreadLocalRandom.current().nextDouble() < MISS_PROBABILITY
                ? randomMissingKey()
                : randomExistingKey();
    }
}
