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
public class TestMapDBRead extends AbstractReadTest {

    private DB db;
    private HTreeMap<String, String> storage;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String read() {
        final String key = pickReadKey();
        final String value = storage.get(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public void setup() {
        final File dir = prepareDirectory();
        db = DBMaker//
                .fileDB(new File(dir, "data-read.db"))//
                .fileChannelEnable()//
                .checksumHeaderBypass()//
                .make();
        storage = db.hashMap("users-read", org.mapdb.Serializer.STRING,
                org.mapdb.Serializer.STRING).createOrOpen();

        preloadDataset((key, value) -> storage.put(key, value));
        db.commit();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}
