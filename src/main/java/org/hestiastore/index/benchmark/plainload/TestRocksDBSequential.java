package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TestRocksDBSequential extends AbstractSequentialReadTest {

    private Options options;
    private RocksDB storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readSequential() throws RocksDBException {
        final String key = nextSequentialKey();
        final byte[] value = storage
                .get(key.getBytes(StandardCharsets.UTF_8));
        return value != null ? VALUE : key;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException, RocksDBException {
        final File dir = prepareDirectory();
        options = new Options()//
                .setCreateIfMissing(true)//
                .setUseFsync(false);
        final File rocksDir = new File(dir, "rocks-seq");
        rocksDir.mkdirs();
        storage = RocksDB.open(options, rocksDir.getAbsolutePath());

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
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
        if (options != null) {
            options.close();
        }
    }
}

