package org.hestiastore.index.benchmark.storage.singlethread.sequential;

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
public class MapDBSequentialReadBenchmark extends AbstractSequentialReadBenchmark {

    private DB db;
    private HTreeMap<String, String> storage;

    @Override
    protected String performOperation() {
        final String key = nextSequentialKey();
        final String value = storage.get(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public void setup() {
        final File dir = prepareDirectory();
        db = DBMaker//
                .fileDB(new File(dir, "data-seq.db"))//
                .fileChannelEnable()//
                .checksumHeaderBypass()//
                .make();
        storage = db.hashMap("users-seq", org.mapdb.Serializer.STRING,
                org.mapdb.Serializer.STRING).createOrOpen();

        preloadDataset((key, value) -> storage.put(key, value));
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
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}
