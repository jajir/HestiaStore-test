package org.hestiastore.index.benchmark.multithread;

import java.io.File;

import org.hestiastore.index.chunkstore.ChunkFilterCrc32Validation;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Writing;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberValidation;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberWriting;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.hestiastore.index.segmentindex.SegmentIndex;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class TestHestiaStoreBasicMultithreadWrite
        extends AbstractMultithreadWriteBenchmark {

    private SegmentIndex<String, String> index;

    @Override
    protected void createStorage(final File dir) {
        final Directory directory = new FsDirectory(dir);
        final IndexConfiguration<String, String> conf = applySegmentIndexTuning(
                IndexConfiguration.<String, String>builder()
                        .withName("test-index-multithread-write-basic")
                        .withKeyClass(String.class)
                        .withValueClass(String.class))
                .addEncodingFilter(new ChunkFilterMagicNumberWriting())
                .addEncodingFilter(new ChunkFilterCrc32Writing())
                .addDecodingFilter(new ChunkFilterCrc32Validation())
                .addDecodingFilter(new ChunkFilterMagicNumberValidation())
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
