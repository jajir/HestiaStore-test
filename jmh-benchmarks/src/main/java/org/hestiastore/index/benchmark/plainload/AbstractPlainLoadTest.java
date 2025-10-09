package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.benchmark.load.HashDataProvider;
import org.hestiastore.index.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base for simple "plain load" tests that insert many key/value pairs
 * into different storage engines while periodically printing progress.
 */
abstract class AbstractPlainLoadTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Warmup processs definition.
     */
    protected final static int WARM_UP_ITERACTIONS = 5;
    protected final static int WARM_UP_TIME = 20;

    protected static final String PROPERTY_DIRECTORY = "dir";
    protected static final String VALUE = "opice skace po stromech";
    protected static final HashDataProvider HASH_DATA_PROVIDER = new HashDataProvider();
    protected static final long RANDOM_SEED = 324432L;
    protected static final Random RANDOM = new Random(RANDOM_SEED);

    protected String directoryFileName;

    /**
     * Prepare target directory based on system property and return it.
     */
    protected File prepareDirectory() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        logger.debug("Property 'dir' is '" + directoryFileName + "'");
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }
        final File dirFile = new File(directoryFileName);
        FileUtils.deleteFileRecursively(dirFile);
        dirFile.mkdirs();
        return dirFile;
    }

}
