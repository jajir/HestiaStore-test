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

import net.openhft.chronicle.map.ChronicleMap;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TestChronicleMapSequential extends AbstractSequentialReadTest {

    private ChronicleMap<String, String> map;

    @Benchmark
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readSequential() {
        final String key = nextSequentialKey();
        final String value = map.get(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final File dir = prepareDirectory();
        map = ChronicleMap.of(String.class, String.class)//
                .name("benchmark-seq")//
                .entries(PRELOAD_ENTRY_COUNT)//
                .averageKeySize(32)//
                .averageValueSize(128)//
                .maxBloatFactor(5.0)//
                .createPersistedTo(new File(dir, "test-seq.dat"));

        preloadDataset((key, value) -> map.put(key, value));
        resetSequentialCursor();
    }

    @Setup(Level.Iteration)
    public void iterationSetup() {
        resetSequentialCursor();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (map != null) {
            map.close();
        }
    }
}

