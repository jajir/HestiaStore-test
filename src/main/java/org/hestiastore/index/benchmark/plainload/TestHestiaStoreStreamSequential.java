package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.hestiastore.index.Entry;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Validation;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Writing;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberValidation;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberWriting;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyCompress;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyDecompress;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.segmentindex.SegmentIndex;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TestHestiaStoreStreamSequential extends AbstractReadTest {

    private SegmentIndex<String, String> index;
    private Stream<Entry<String, String>> currentStream;
    private Iterator<Entry<String, String>> streamIterator;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readSequentialStream() {
        ensureIterator();
        if (!streamIterator.hasNext()) {
            resetStream();
        }
        if (!streamIterator.hasNext()) {
            return VALUE;
        }
        final Entry<String, String> entry = streamIterator.next();
        return entry.getValue();
    }

    private void ensureIterator() {
        if (streamIterator == null) {
            resetStream();
        }
    }

    private void resetStream() {
        closeStream();
        currentStream = index.getStream();
        streamIterator = currentStream.iterator();
    }

    private void closeStream() {
        if (currentStream != null) {
            currentStream.close();
            currentStream = null;
            streamIterator = null;
        }
    }

    @Setup(Level.Trial)
    public void setup() {
        final File dirFile = prepareDirectory();
        final Directory directory = new FsDirectory(dirFile);

        final IndexConfiguration<String, String> conf = applySegmentIndexTuning(
                IndexConfiguration.<String, String>builder()//
                .withName("test-index-seq-stream")//
                .withKeyClass(String.class)//
                .withValueClass(String.class))//
                .addEncodingFilter(new ChunkFilterMagicNumberWriting())//
                .addEncodingFilter(new ChunkFilterCrc32Writing())//
                .addEncodingFilter(new ChunkFilterSnappyCompress())//
                .addDecodingFilter(new ChunkFilterSnappyDecompress())//
                .addDecodingFilter(new ChunkFilterCrc32Validation())//
                .addDecodingFilter(new ChunkFilterMagicNumberValidation())//
                .build();

        index = SegmentIndex.create(directory, conf);
        preloadDataset((key, value) -> index.put(key, value));
        index.flush();
        resetStream();
    }

    @Setup(Level.Iteration)
    public void iterationSetup() {
        resetStream();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        closeStream();
        if (index != null) {
            index.close();
        }
    }
}
