package org.hestiastore.index.benchmark.storage.multithread.write;

import java.io.File;

import org.hestiastore.index.chunkstore.ChunkFilterCrc32Validation;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Writing;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberValidation;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberWriting;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyCompress;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyDecompress;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.hestiastore.index.segmentindex.SegmentIndex;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class HestiaStoreCompressWriteMultiThreadBenchmark
        extends AbstractWriteMultiThreadBenchmark {

    private SegmentIndex<String, String> index;

    @Override
    protected void createStorage(final File dir) {
        final Directory directory = new FsDirectory(dir);
        final IndexConfiguration<String, String> conf = applySegmentIndexTuning(
                IndexConfiguration.<String, String>builder()
                        .withName("test-index-multithread-write-compress")
                        .withKeyClass(String.class)
                        .withValueClass(String.class))
                .addEncodingFilter(new ChunkFilterMagicNumberWriting())
                .addEncodingFilter(new ChunkFilterCrc32Writing())
                .addEncodingFilter(new ChunkFilterSnappyCompress())
                .addDecodingFilter(new ChunkFilterSnappyDecompress())
                .addDecodingFilter(new ChunkFilterCrc32Validation())
                .addDecodingFilter(new ChunkFilterMagicNumberValidation())
                .withIndexWorkerThreadCount(10)
                .withNumberOfSegmentIndexMaintenanceThreads(10)
                .withMaxNumberOfKeysInSegment(10_000_000)
                .withMaxNumberOfKeysInSegmentCache(1_000_000)
                .withMaxNumberOfKeysInSegmentWriteCache(300_000)
                .withMaxNumberOfKeysInSegmentWriteCacheDuringMaintenance(
                        600_000)
                .withMaxNumberOfSegmentsInCache(10)
                .build();
        index = SegmentIndex.create(directory, conf);
    }

    @Override
    protected void writeValue(final String key, final String value) {
        index.put(key, value);
    }

    @Override
    protected void closeStorage() {
        if (index != null) {
            index.close();
        }
    }
}
