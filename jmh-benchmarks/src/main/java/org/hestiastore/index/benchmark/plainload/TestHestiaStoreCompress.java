package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Pair;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Validation;
import org.hestiastore.index.chunkstore.ChunkFilterCrc32Writing;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberValidation;
import org.hestiastore.index.chunkstore.ChunkFilterMagicNumberWriting;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyCompress;
import org.hestiastore.index.chunkstore.ChunkFilterSnappyDecompress;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
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
public class TestHestiaStoreCompress extends AbstractPlainLoadTest {
    private Index<String, String> index;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        final Pair<String, String> pair = Pair.of(hash, VALUE);
        index.put(pair);
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() {
        final File dirFile = prepareDirectory();
        final Directory directory = new FsDirectory(dirFile);

        final IndexConfiguration<String, String> conf = IndexConfiguration
                .<String, String>builder()//
                .withName("test-index")//
                .withKeyClass(String.class)//
                .withValueClass(String.class)//
                .addEncodingFilter(new ChunkFilterMagicNumberWriting())//
                .addEncodingFilter(new ChunkFilterCrc32Writing())//
                .addEncodingFilter(new ChunkFilterSnappyCompress())//
                // .addEncodingFilter(new ChunkFilterXorEncrypt())//
                // .addDecodingFilter(new ChunkFilterXorDecrypt())//
                .addDecodingFilter(new ChunkFilterSnappyDecompress())//
                .addDecodingFilter(new ChunkFilterCrc32Validation())//
                .addDecodingFilter(new ChunkFilterMagicNumberValidation())//
                .build();

        index = Index.create(directory, conf);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: ");
        index.close();
    }
}
