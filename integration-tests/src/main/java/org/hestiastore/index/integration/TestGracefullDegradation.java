package org.hestiastore.index.integration;

import java.io.File;
import java.util.Objects;

import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.segmentindex.SegmentIndex;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.hestiastore.index.utils.FileUtils;
import org.hestiastore.index.utils.TestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGracefullDegradation {
    private static final long WRITE_PREPARE_KEYS = 5_000L;
    private static final long WRITE_KEYS = 9_000_000L;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SegmentIndex<String, Long> index;

    TestGracefullDegradation(final IndexConfiguration<String, Long> conf,
            final String directoryName) {
        Objects.requireNonNull(conf);
        Objects.requireNonNull(directoryName);

        final File directoryFile = new File(directoryName);
        FileUtils.deleteFileRecursively(directoryFile);
        final Directory dir = new FsDirectory(directoryFile);
        this.index = SegmentIndex.create(dir, conf);
    }

    void startTest() {
        TestStatus.reset();
        // Test logic to check consistency
        // This is a placeholder for the actual test logic
        logger.info("Consistency check - preparing data");
        writeKeys(0, WRITE_PREPARE_KEYS);
        logger.info("Consistency check - ready to test");
        index.flush();
        TestStatus.setReadyToTest(true);
        writeKeys(WRITE_PREPARE_KEYS, WRITE_KEYS);
        index.close();
        throw new IllegalStateException(
                "Graceful degradation should be tested, but it was not.");
    }

    private void writeKeys(final long start, final long count) {
        for (long i = 0; i < count; i++) {
            String key = String.valueOf(start + i);
            Long value = i;
            index.put(key, value);
        }
    }

}
