package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
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
public class TestMapDB extends AbstractPlainLoadTest {
    private HTreeMap<String, String> storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        storage.put(hash, VALUE);
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() {
        File dir = prepareDirectory();

        // 1) Open (or create) a file-backed database.
        final DB db = DBMaker//
                .fileDB(dir.getAbsolutePath() + "/data.db")//
                .fileChannelEnable() //
                // .transactionEnable() // enable transactions
                .checksumHeaderBypass() //
                .make();

        // 2) Create or open a HashMap named "users" with String→User
        // serialization
        storage = db.hashMap("users", org.mapdb.Serializer.STRING,
                org.mapdb.Serializer.STRING).createOrOpen();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: ");
        storage.close();
    }

}
