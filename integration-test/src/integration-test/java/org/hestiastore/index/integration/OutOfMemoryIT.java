package org.hestiastore.index.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;

import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.hestiastore.index.utils.AbstractIndexCli;
import org.hestiastore.index.utils.FileUtils;
import org.hestiastore.index.utils.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutOfMemoryIT {
    public static final int SIGTERM_EXIT_CODE = 1;
    public static final String DIRECTORY = "target/consistency-check";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void test_one_round() throws Exception {
        final MainRunConf conf = new MainRunConf(
                Main.OPTION_OUT_OF_MEMORY_TEST_NAME, "400m")//
                .addParameter(AbstractIndexCli.PARAM_DIRECTORY, DIRECTORY) //
                .addParameter(AbstractIndexCli.PARAM_INDEX_NAME, "test-index") //
                .addParameter(
                        AbstractIndexCli.PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT,
                        "500_000") //
                .addParameter(
                        AbstractIndexCli.PARAM_MAX_NUMBER_OF_KEYS_IN_CACHE,
                        "500_000") //
                .addParameter(
                        AbstractIndexCli.PARAM_BLOOM_FILTER_INDEX_SIZE_IN_BYTES,
                        "500_000") //
                .addParameter(
                        AbstractIndexCli.PARAM_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS,
                        "3") //
                .addParameter(
                        AbstractIndexCli.PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE,
                        "1_000") //
                .addParameter(
                        AbstractIndexCli.PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE,
                        "100_000") //
                .addParameter(
                        AbstractIndexCli.PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING,
                        "200_000") //
        ;
        final Process process = conf.createProcessBuilder().start();

        IoUtils.printInputStream(process.getInputStream());

        logger.info("Waiting for process finist at OutOfMemoryError");

        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(TestStatus::isReadyToTest);

        assertFalse(process.isAlive(), "Process should be terminated");
        assertEquals(SIGTERM_EXIT_CODE, process.exitValue(),
                "Process should exit with SIGTERM code");
        process.destroyForcibly(); // in case it is still running

        // Validate index consistency
        ConsistencyCheckConf.removeLockFile();
        final Directory dir = new FsDirectory(
                ConsistencyCheckConf.FILE_DIRECTORY);
        IndexConfiguration<String, Long> indexConfiguration = new ConsistencyCheckConf()//
                .getIndexConfiguration();
        Index<String, Long> index = Index.open(dir, indexConfiguration);
        index.checkAndRepairConsistency();
        final long cx = index.getStream().count();
        logger.info("Index size: " + cx);
    }

    @Test
    @Disabled
    void test_multiple_rounds() throws Exception {
        for (int i = 0; i < 100; i++) {
            logger.info("Running test round " + i);
            test_one_round();
        }
    }

    @BeforeEach
    void setUp() {
        TestStatus.reset();
        FileUtils.deleteFileRecursively(
                new File(ConsistencyCheckConf.DIRECTORY));

    }

    @AfterEach
    void tearDown() {
        TestStatus.reset();
    }
}
