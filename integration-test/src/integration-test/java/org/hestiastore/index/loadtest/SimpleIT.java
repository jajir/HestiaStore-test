package org.hestiastore.index.loadtest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.hestiastore.index.benchmark.FileUtils;
import org.hestiastore.index.benchmark.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleIT {

    private final Logger logger = Logger.getLogger(SimpleIT.class.getName());

    @Test
    void test_simple2() throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(//
                "java", //
                "-Xmx10000m", //
                "-cp", //
                loadClassParh(), //
                "org.hestiastore.index.loadtest.Main", //
                "--test1", //
                "")//
        ;
        // builder.inheritIO();
        builder.redirectErrorStream(true);

        log(builder);
        final Process process = builder.start();

        IoUtils.printInputStream(process.getInputStream());

        logger.info("Waiting for process finishing preparing data");
        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(TestStatus::isReadyToTest);
        assertTrue(process.isAlive(), "Process should be running");
        logger.info("Now it's ready to terminate");
        IoUtils.printInputStream(process.getInputStream());

        Thread.sleep(1000);
        process.destroy(); // graceful SIGTERM

        await().atMost(30, SECONDS).pollInterval(1, SECONDS)
                .until(() -> !process.isAlive());
        assertFalse(process.isAlive(), "Process should be terminated");
        System.out.println("" + process.exitValue());
    }

    @Test
    void test_simple3() throws Exception {
        logger.info("I'm testing");
    }

    @BeforeEach
    void setUp() {
        TestStatus.reset();
        FileUtils.deleteFileRecursively(new File(ConsistencyCheck.DIRECTORY));

    }

    @AfterEach
    void tearDown() {
        TestStatus.reset();
    }

    private void log(final ProcessBuilder builder) {
        final StringBuilder buff = new StringBuilder();
        buff.append("Call it manually in case problems: ");
        for (final String part : builder.command()) {
            buff.append(part);
            buff.append(" ");
        }
        logger.info(buff.toString());
    }

    private static final String GENERATED_CLASS_PATH_FILE = "target/test.classpath";

    private String loadClassParh() {
        try {
            return Files.readString(Paths.get(GENERATED_CLASS_PATH_FILE))
                    .trim();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
