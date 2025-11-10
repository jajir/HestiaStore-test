package org.hestiastore.index.benchmark.plainload;

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
        final long index = Math.floorMod(RANDOM.nextLong(),
                PRELOAD_ENTRY_COUNT);
        return keyForIndex(index);
    }

    protected String randomMissingKey() {
        final long index = PRELOAD_ENTRY_COUNT
                + Math.floorMod(RANDOM.nextLong(), PRELOAD_ENTRY_COUNT);
        return keyForIndex(index);
    }

    protected String pickReadKey() {
        return RANDOM.nextDouble() < MISS_PROBABILITY ? randomMissingKey()
                : randomExistingKey();
    }
}
