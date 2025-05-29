package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.Random;

import org.hestiastore.index.benchmark.FileUtils;
import org.hestiastore.index.benchmark.load.HashDataProvider;
import org.hestiastore.index.benchmark.load.IndexWritingBenchmark;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMapDB {
    private final Logger logger = LoggerFactory.getLogger(IndexWritingBenchmark.class);
    private final static long RANDOM_SEED = 324432L;
    private final static String PROPERTY_DIRECTORY = "dir";
    private final static String VALUE = "opice skace po stromech";
    private final static HashDataProvider HASH_DATA_PROVIDER = new HashDataProvider();
    private final static Random RANDOM = new Random(RANDOM_SEED);
    private String directoryFileName;
    private HTreeMap<String, String> storage;

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
        storage.put(hash, VALUE);
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

        // 1) Open (or create) a file-backed database.
        // transactionEnable() gives you full ACID with commit()/rollback()
        // fileChannelEnable() uses java.nio for slightly better performance
        final DB db = DBMaker
                .fileDB("data.db") // file on disk
                .fileChannelEnable() // use FileChannel
                // .transactionEnable() // enable transactions
                .checksumHeaderBypass() // enable checksums
                .make();

        // 2) Create or open a HashMap named "users" with String→User serialization
        storage = db
                .hashMap("users", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING)
                .createOrOpen();

        startMs = System.currentTimeMillis();
        print(startMs);
    }

    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: " + cx);
        storage.close();
    }
}
