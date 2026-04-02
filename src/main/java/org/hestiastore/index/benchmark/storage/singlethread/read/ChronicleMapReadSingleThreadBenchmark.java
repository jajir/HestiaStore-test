package org.hestiastore.index.benchmark.storage.singlethread.read;

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
public class ChronicleMapReadSingleThreadBenchmark extends AbstractReadSingleThreadBenchmark {

    private ChronicleMap<String, String> map;

    @Override
    protected String performOperation() {
        final String key = pickReadKey();
        final String value = map.get(key);
        return value != null ? value : key;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final File dir = prepareDirectory();
        map = ChronicleMap.of(String.class, String.class)//
                .name("benchmark-read")//
                .entries(PRELOAD_ENTRY_COUNT)//
                .averageKeySize(32)//
                .averageValueSize(128)//
                .maxBloatFactor(5.0)//
                .createPersistedTo(new File(dir, "test-read.dat"));

        preloadDataset((key, value) -> map.put(key, value));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (map != null) {
            map.close();
        }
    }
}
