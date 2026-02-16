package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.Random;

import org.hestiastore.index.segmentindex.IndexConfigurationBuilder;
import org.hestiastore.index.utils.HashDataProvider;
import org.hestiastore.index.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base for simple "plain load" tests that insert many key/value pairs
 * into different storage engines while periodically printing progress.
 */
abstract class AbstractWriteTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Warmup processs definition.
     */
    protected final static int WARM_UP_ITERACTIONS = 10;
    protected final static int WARM_UP_TIME = 20;

    /**
     * Measurement processs definition.
     */
    protected final static int MEASUREMENT_ITERACTIONS = 25;
    protected final static int MEASUREMENT_TIME = 20;

    public static final String PROPERTY_DIRECTORY = "dir";
    protected static final String VALUE = "opice skace po stromech";
    protected static final HashDataProvider HASH_DATA_PROVIDER = new HashDataProvider();
    protected static final long RANDOM_SEED = 324432L;
    protected static final Random RANDOM = new Random(RANDOM_SEED);

    protected String directoryFileName;

    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT = 500_000;
    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE = 100_000;
    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_WRITE_CACHE = 100_000;
    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_WRITE_CACHE_DURING_MAINTENANCE = 200_000;
    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CHUNK = 1_000;
    protected static final int INDEX_MAX_NUMBER_OF_KEYS_IN_CACHE = 500_000;
    protected static final int INDEX_BLOOM_FILTER_INDEX_SIZE_IN_BYTES = 500_000;
    protected static final int INDEX_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS = 3;

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

    protected <K, V> IndexConfigurationBuilder<K, V> applySegmentIndexTuning(
            final IndexConfigurationBuilder<K, V> builder) {
        return builder.withContextLoggingEnabled(false)
                .withMaxNumberOfKeysInSegment(
                        INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT)
                .withMaxNumberOfKeysInSegmentCache(
                        INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE)
                .withMaxNumberOfKeysInSegmentWriteCache(
                        INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_WRITE_CACHE)
                .withMaxNumberOfKeysInSegmentWriteCacheDuringMaintenance(
                        INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_WRITE_CACHE_DURING_MAINTENANCE)
                .withMaxNumberOfKeysInSegmentChunk(
                        INDEX_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CHUNK)
                .withMaxNumberOfKeysInCache(INDEX_MAX_NUMBER_OF_KEYS_IN_CACHE)
                .withBloomFilterIndexSizeInBytes(
                        INDEX_BLOOM_FILTER_INDEX_SIZE_IN_BYTES)
                .withBloomFilterNumberOfHashFunctions(
                        INDEX_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS);
    }

}
