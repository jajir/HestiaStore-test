package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.Random;

import org.hestiastore.index.Pair;
import org.hestiastore.index.benchmark.FileUtils;
import org.hestiastore.index.benchmark.load.HashDataProvider;
import org.hestiastore.index.benchmark.load.IndexWritingBenchmark;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHestiaStore {
    private final Logger logger = LoggerFactory.getLogger(IndexWritingBenchmark.class);
    private final static long RANDOM_SEED = 324432L;
    private final static String PROPERTY_DIRECTORY = "dir";
    private final static String VALUE = "opice skace po stromech";
    private final static HashDataProvider HASH_DATA_PROVIDER = new HashDataProvider();
    private final static Random RANDOM = new Random(RANDOM_SEED);
    private String directoryFileName;
    private Directory directory;
    private Index<String, String> index;

    private long cx = 0;

    private final static int TEST_COUNT = 100_000;
    long startMs;

    public void test(final long howMuch) {
        for (cx = 1; cx < howMuch; cx++) {
            if ((cx % TEST_COUNT) == 0) {
                print(startMs);
            }
            test_writing();
        }
    }

    private void print(long startMs) {
        long currentMs = System.currentTimeMillis();
        final long ElapsedMili = currentMs - startMs;
        final long elapsedMs = ElapsedMili / 1000;
        final long elapsedMm = ElapsedMili % 1000;
        System.out.println("Written, " + cx + ", \"" + elapsedMs + "." + elapsedMm + "\", ");
    }

    public String test_writing() {
        final long rnd = RANDOM.nextLong(cx);
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        final Pair<String, String> pair = Pair.of(hash, VALUE);
        index.put(pair);
        return hash;
    }

    public void setup() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        logger.debug("Property 'dir' is '" + directoryFileName + "'");
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }

        final File dirFile = new File(directoryFileName);
        FileUtils.deleteFileRecursively(dirFile);
        directory = new FsDirectory(dirFile);

        final IndexConfiguration<String, String> conf = IndexConfiguration.<String, String>builder()//
                .withName("test-index")//
                .withKeyClass(String.class)//
                .withValueClass(String.class)//
                .build();

        index = Index.create(directory, conf);
        startMs = System.currentTimeMillis();
        print(startMs);
    }

    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: " + cx);
        index.close();
    }
}
