package org.hestiastore.microbenchmarks.collections;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import org.hestiastore.index.chunkstore.ChunkFilterCrc32Validation;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Writing;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberValidation;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberWriting;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.segmentindex.SegmentIndex;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.hestiastore.index.utils.FileUtils;
import org.hestiastore.index.utils.HashDataProvider;

/**
 * Base class for random-read microbenchmarks.
 *
 * Responsibilities:
 * - Ensure the target directory exists.
 * - If the primary segment file is missing or undersized, build a fresh
 *   HestiaStore index populated with {@link #PRELOAD_ENTRY_COUNT} records using
 *   the same key/value generation as {@code TestHestiaStoreBasicRead}.
 * - Expose the segment file and its size for concrete benchmarks.
 */
abstract class AbstractRandomReadBenchmark extends AbstractBenchmark {

    protected static final long PRELOAD_ENTRY_COUNT = 10_000_000L;
    protected static final String SEGMENT_FILE_NAME = "segment-00000.index";
    protected static final long MIN_SEGMENT_SIZE_BYTES = 230L * 1024L * 1024L;

    private final HashDataProvider hashDataProvider = new HashDataProvider();

    private File segmentFile;
    private long segmentFileSize;

    protected final void ensureDataFileReady() {
        final File dir = resolveDirectory();
        final File target = new File(dir, SEGMENT_FILE_NAME);
        if (target.exists() && target.length() > MIN_SEGMENT_SIZE_BYTES) {
            this.segmentFile = target;
            this.segmentFileSize = target.length();
            return;
        }

        // Recreate the dataset from scratch.
        FileUtils.deleteFileRecursively(dir);
        dir.mkdirs();

        final Directory directory = new FsDirectory(dir);
        final IndexConfiguration<String, String> conf = IndexConfiguration
                .<String, String>builder()//
                .withName("random-read-index")//
                .withKeyClass(String.class)//
                .withValueClass(String.class)//
                .withContextLoggingEnabled(false)//
                .withMaxNumberOfKeysInReadCache(4_000_000)//
                .addEncodingFilter(new ChunkFilterMagicNumberWriting())//
                .addEncodingFilter(new ChunkFilterCrc32Writing())//
                .addDecodingFilter(new ChunkFilterCrc32Validation())//
                .addDecodingFilter(new ChunkFilterMagicNumberValidation())//
                .build();

        SegmentIndex<String, String> index = null;
        try {
            index = SegmentIndex.create(directory, conf);
            for (long i = 0; i < PRELOAD_ENTRY_COUNT; i++) {
                index.put(hashDataProvider.makeHash(i), VALUE);
            }
            index.flush();
        } finally {
            if (index != null) {
                index.close();
            }
        }

        this.segmentFile = new File(dir, SEGMENT_FILE_NAME);
        this.segmentFileSize = segmentFile.length();
    }

    protected final File getSegmentFile() {
        return segmentFile;
    }

    protected final long getSegmentFileSize() {
        return segmentFileSize;
    }

    protected final long nextRandomPosition(final int readLength) {
        final long maxStart = Math.max(0, segmentFileSize - readLength);
        // Align to 1 KB boundaries to mimic page-aligned access patterns.
        final long blocks = Math.max(1, (maxStart / 1024) + 1);
        final long blockIndex = ThreadLocalRandom.current().nextLong(blocks);
        return blockIndex * 1024;
    }

    private File resolveDirectory() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }
        return new File(directoryFileName);
    }

}
