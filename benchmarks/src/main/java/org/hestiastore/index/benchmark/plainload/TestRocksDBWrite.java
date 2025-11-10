package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.io.IOException;
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
public class TestRocksDBWrite extends AbstractWriteTest {

    private Options options;
    private RocksDB storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() throws RocksDBException {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        storage.put(hash.getBytes(), VALUE.getBytes());
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException, RocksDBException {
        File dir = prepareDirectory();

        options = new Options()//
                .setCreateIfMissing(true)//
                .setUseFsync(false)//
        ;
        storage = RocksDB.open(options, dir.getAbsolutePath() + "/rocks");

    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Closing index and directory");
        storage.close();
        options.close();
        // generateSecondaryMetrics();
    }

}
