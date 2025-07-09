package org.hestiastore.index.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.logging.Logger;

import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.hestiastore.index.utils.FileUtils;
import org.hestiastore.index.utils.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class OutOfMemmoryIT {
    public static final int SIGTERM_EXIT_CODE = 1;

    private final Logger logger = Logger
            .getLogger(GracefulDegradationIT.class.getName());

    @Test
    void test_one_round() throws Exception {
        MainRunConf conf = new MainRunConf("test1", "400m")//
                .addParameter("--max-number-of-keys-in-segment-cache",
                        "1_000_000") //
                .addParameter(
                        "--max-number-of-keys-in-segment-cache-during-flushing",
                        "1_000_000") //
        ;
        final Process process = conf.createProcessBuilder().start();

        IoUtils.printInputStream(process.getInputStream());

        logger.info("Waiting for process finist at OutOfMemoryError");

        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(() -> !process.isAlive());

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
