package org.hestiastore.index.benchmark.load;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Pair;
import org.hestiastore.index.benchmark.FileUtils;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread) // Each thread gets its own state
@Warmup(iterations = 0, time = 1) // 0 warm-up iterations
@Fork(1) // Use 1 fork (JVM instance)
@Threads(1)
public class IndexWritingBenchmark {

    private final Logger logger = LoggerFactory.getLogger(IndexWritingBenchmark.class);
    private final static long RANDOM_SEED = 324432L;
    private final static String PROPERTY_DIRECTORY = "dir";
    private final static String VALUE = "opice skace po stromech";
    private final static HashDataProvider HASH_DATA_PROVIDER = new HashDataProvider();
    private final static Random RANDOM = new Random(RANDOM_SEED);
    private String directoryFileName;
    private Directory directory;
    private Index<String, String> index;

    private long cx = 1;

    /**
     * Measure number of operation per some time period. Annotations,
     * like @OperationsPerInvocation may tell that a single @Benchmark invocation
     * means N operations.
     * 
     * One operation is one @Benchmark invocation.
     * 
     * Ø
     * 
     * @return
     */
    @Measurement(iterations = 40, time = 2, timeUnit = TimeUnit.SECONDS)
    @Benchmark
    public String test_writing() {
        final long rnd = RANDOM.nextLong(cx++);
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        final Pair<String, String> pair = Pair.of(hash, VALUE);
        index.put(pair);
        return hash;
    }

    @Setup
    public void setup() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        logger.debug("Property 'dir' is '" + directoryFileName + "'");
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }

        final File dirFile = new File(directoryFileName);
        FileUtils.deleteFileRecursively(dirFile);
        directory = new FsDirectory(dirFile);

        final IndexConfiguration<String, String> conf = IndexConfiguration.<String, String>builder()//
                .withName("test-index")//
                .withKeyClass(String.class)//
                .withValueClass(String.class)//
                .build();

        index = Index.create(directory, conf);
    }

    @TearDown
    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: " + cx);
        index.close();
    }

}
