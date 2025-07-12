package org.hestiastore.index.integration;

import java.io.File;
import java.util.Objects;

import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.hestiastore.index.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class initialize index starts some operations and wait for
 * OutOfMemoryException.
 * 
 * 
 */
public class TestOutOfMemory {

    private static final long WRITE_PREPARE_KEYS = 5_000L;
    private static final long WRITE_KEYS = 9_000_000L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IndexConfiguration<String, Long> conf;
    private final String directoryName;
    private final Index<String, Long> index;

    TestOutOfMemory(final IndexConfiguration<String, Long> conf,
            final String directoryName) {
        this.conf = Objects.requireNonNull(conf);
        this.directoryName = Objects.requireNonNull(directoryName);

        final File directoryFile = new File(directoryName);
        FileUtils.deleteFileRecursively(directoryFile);
        final Directory dir = new FsDirectory(directoryFile);
        this.index = Index.create(dir, conf);
    }

    void startTest() {
        logger.info("Starting test for OutOfMemoryError. "
                + "In some time OutOfMemmoryException should come. "
                + "It's in directory '{}'", directoryName);

        TestStatus.reset();
        writeKeys(0, WRITE_PREPARE_KEYS);
        index.flush();
        TestStatus.setReadyToTest(true);

        writeKeys(WRITE_PREPARE_KEYS, WRITE_KEYS);
        index.flush();
        index.close();

        throw new IllegalStateException(
                "OutOfMemoryError should be thrown, but it was not.");
    }

    private void writeKeys(final long start, final long count) {
        for (long i = 0; i < count; i++) {
            String key = String.valueOf(start + i);
            Long value = i;
            index.put(key, value);
        }
    }

}
