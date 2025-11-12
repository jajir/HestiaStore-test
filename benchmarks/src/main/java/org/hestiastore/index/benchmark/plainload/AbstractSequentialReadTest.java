package org.hestiastore.index.benchmark.plainload;

/**
 * Base class for sequential read benchmarks. Sequential variants reuse the
 * preloaded dataset from {@link AbstractReadTest} but iterate over keys in a
 * deterministic order instead of random access.
 */
abstract class AbstractSequentialReadTest extends AbstractReadTest {

    private long sequentialCursor = 0L;

    protected String nextSequentialKey() {
        final long idx = sequentialCursor;
        sequentialCursor = (sequentialCursor + 1) % PRELOAD_ENTRY_COUNT;
        return keyForIndex(idx);
    }

    protected void resetSequentialCursor() {
        sequentialCursor = 0L;
    }
}

