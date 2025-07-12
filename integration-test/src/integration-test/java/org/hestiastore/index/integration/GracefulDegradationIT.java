package org.hestiastore.index.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.logging.Logger;

import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.hestiastore.index.utils.AbstractIndexCli;
import org.hestiastore.index.utils.CommandLineConf;
import org.hestiastore.index.utils.FileUtils;
import org.hestiastore.index.utils.IndexUtils;
import org.hestiastore.index.utils.IoUtils;
import org.hestiastore.index.utils.TestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GracefulDegradationIT {

    public static final int SIGTERM_EXIT_CODE = 128 + 15;
    public static final String MAIN_CLASS_NAME = "org.hestiastore.index.integration.Main";
    private final String THIS_PACKAGE_FILE_LOCATION = "target/integration-test-0.0.0-SNAPSHOT.jar";
    public static final String DIRECTORY = "target/consistency-check";
    public static final String INDEX_NAME = "test-index";

    private final Logger logger = Logger
            .getLogger(GracefulDegradationIT.class.getName());

    @Test
    void test_one_round() throws Exception {
        final CommandLineConf conf = new CommandLineConf(
                THIS_PACKAGE_FILE_LOCATION, MAIN_CLASS_NAME,
                Main.OPTION_GRACEFULL_DEGRADATION_TEST_NAME, "1000m")//
                .addParameter(AbstractIndexCli.PARAM_DIRECTORY, DIRECTORY) //
                .addParameter(AbstractIndexCli.PARAM_INDEX_NAME, INDEX_NAME) //
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
        // logger.info(conf.toString());
        final Process process = conf.createProcessBuilder().start();
        IoUtils.printInputStream(process.getInputStream());
        logger.info("Waiting for process finishing preparing data");

        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(TestStatus::isReadyToTest);
        assertTrue(process.isAlive(), "Process should be running");
        logger.info("Now it's ready to terminate");

        Thread.sleep(1 * 1000);
        process.destroy(); // graceful SIGTERM

        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(() -> !process.isAlive());
        assertFalse(process.isAlive(), "Process should be terminated");
        assertEquals(SIGTERM_EXIT_CODE, process.exitValue(),
                "Process should exit with SIGTERM code");
        process.destroyForcibly(); // in case it is still running

        // Validate index consistency
        IndexUtils.optionalyRemoveLockFile(DIRECTORY);
        final Directory dir = new FsDirectory(new File(DIRECTORY));
        final IndexConfiguration<String, Long> indexConfiguration = IndexConfiguration
                .<String, Long>builder()//
                .withKeyClass(String.class)//
                .withValueClass(Long.class)//
                .withName(INDEX_NAME) //
                .build();
        final Index<String, Long> index = Index.open(dir, indexConfiguration);
        index.checkAndRepairConsistency();
        final long cx = index.getStream().count();
        logger.info("Index size: " + cx);
        assertTrue(cx > 0,
                "Index should contain some data, but it is empty: " + cx);
        index.close();
    }

    @Test
    void test_multiple_rounds() throws Exception {
        for (int i = 0; i < 100; i++) {
            logger.info("Running test round " + i);
            test_one_round();
        }
    }

    @BeforeEach
    void setUp() {
        TestStatus.reset();
        FileUtils.deleteFileRecursively(new File(DIRECTORY));

    }

    @AfterEach
    void tearDown() {
        TestStatus.reset();
    }

}
