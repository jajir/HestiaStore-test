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
public class TestLevelDBSequential extends AbstractSequentialReadTest {

    private DB storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readSequential() {
        final String key = nextSequentialKey();
        final byte[] value = storage
                .get(key.getBytes(StandardCharsets.UTF_8));
        return value != null ? VALUE : key;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final File dir = prepareDirectory();
        final Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(100 * 1024 * 1024L);

        final File dbDir = new File(dir, "leveldb-seq");
        dbDir.mkdirs();
        storage = Iq80DBFactory.factory.open(dbDir, options);

        preloadDataset((key, value) -> storage.put(
                key.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8)));
        resetSequentialCursor();
    }

    @Setup(Level.Iteration)
    public void iterationSetup() {
        resetSequentialCursor();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        if (storage != null) {
            storage.close();
        }
    }
}

