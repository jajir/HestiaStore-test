package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.StringDataType;
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
public class TestH2Sequential extends AbstractSequentialReadTest {

    private MVStore store;
    private MVMap<String, String> map;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readSequential() {
        final String key = nextSequentialKey();
        final String value = map.get(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public void setup() {
        final File dir = prepareDirectory();
        store = new MVStore.Builder()//
                .fileName(new File(dir, "test-seq.dat").getAbsolutePath())//
                .cacheSize(4096)//
                .autoCommitDisabled()//
                .open();

        final MVMap.Builder<String, String> builder = new MVMap.Builder<String, String>()//
                .keyType(StringDataType.INSTANCE)//
                .valueType(StringDataType.INSTANCE);//
        final Map<String, Object> config = new HashMap<>();
        map = builder.create(store, config);

        preloadDataset((key, value) -> map.put(key, value));
        resetSequentialCursor();
    }

    @Setup(Level.Iteration)
    public void iterationSetup() {
        resetSequentialCursor();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }
}

