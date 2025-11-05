package org.hestiastore.index.benchmark.plainload;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskInfoMaker {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Prepare target directory based on system property and return it.
     */
    protected String getDirectoryFileName() {
        String directoryFileName = System
                .getProperty(AbstractPlainLoadTest.PROPERTY_DIRECTORY);
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }
        return directoryFileName;
    }

    /**
     * Recursively compute the number of files and total size under the
     * benchmark directory.
     *
     * @return aggregated file system state; zero values when the directory is
     *         missing
     */
    protected SystemState computeFileSystemState() {
        final SystemState state = new SystemState();

        if (getDirectoryFileName() == null
                || getDirectoryFileName().isEmpty()) {
            logger.debug(
                    "No directoryFileName set; returning empty file system state.");
            return state;
        }

        final Path root = Paths.get(getDirectoryFileName());
        if (!Files.exists(root)) {
            logger.debug(
                    "Directory {} does not exist; returning empty file system state.",
                    root);
            return state;
        }

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    state.setTotalDirectorySize(
                            state.getTotalDirectorySize() + attrs.size());
                    state.setFileCount(state.getFileCount() + 1);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file,
                        IOException exc) throws IOException {
                    logger.debug(
                            "Failed to access {} while computing file system state: {}",
                            file, exc.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            logger.warn("Unable to compute file system state for {}: {}", root,
                    ex.getMessage(), ex);
        }
        return state;
    }

    public static Collection<Result<?>> results;

    protected SystemState getSystemState() {
        return computeFileSystemState();
    }

    public SystemState setState(final SystemState state) {
        SystemState tmp = computeFileSystemState();
        state.setFileCount(tmp.getFileCount());
        state.setTotalDirectorySize(tmp.getTotalDirectorySize());
        return state;
    }

    protected Collection<Result<?>> generateSecondaryMetrics() {
        final SystemState fsState = computeFileSystemState();
        results = new ArrayList<>(2);
        results.add(new ScalarResult("diskUsedBytes",
                (double) fsState.getTotalDirectorySize(), "bytes",
                AggregationPolicy.MAX));
        results.add(
                new ScalarResult("fileCount", (double) fsState.getFileCount(),
                        "files", AggregationPolicy.MAX));
        System.out.println("Generated secondary metrics: " + results);
        return results;
    }
}
