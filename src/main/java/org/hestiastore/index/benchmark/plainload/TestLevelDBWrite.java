package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
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
public class TestLevelDBWrite extends AbstractWriteTest {
    private DB storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        storage.put(hash.getBytes(StandardCharsets.UTF_8),
                VALUE.getBytes(StandardCharsets.UTF_8));
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        File dir = prepareDirectory();

        Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(100 * 1024 * 1024L); // 100 MB cache

        File dbFile = new File(dir.getAbsolutePath());
        dbFile.mkdirs();

        // Use the pure Java factory
        storage = Iq80DBFactory.factory.open(dbFile, options);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        logger.info("Closing index and directory, number of written keys: ");
        storage.close();
    }
}
