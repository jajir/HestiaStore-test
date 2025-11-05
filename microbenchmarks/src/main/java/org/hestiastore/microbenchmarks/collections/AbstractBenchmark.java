package org.hestiastore.microbenchmarks.collections;

import java.io.File;
import java.util.Random;

import org.hestiastore.index.utils.DataProvider;
import org.hestiastore.index.utils.FileUtils;

public class AbstractBenchmark {
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
    protected static final long RANDOM_SEED = 324432L;
    protected static final Random RANDOM = new Random(RANDOM_SEED);
    protected DataProvider dataProvider = new DataProvider();
    protected String directoryFileName;

    /**
     * Prepare target directory based on system property and return it.
     */
    protected File prepareDirectory() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }
        final File dirFile = new File(directoryFileName);
        FileUtils.deleteFileRecursively(dirFile);
        dirFile.mkdirs();
        return dirFile;
    }
}
