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
public class TestChronicleMapWrite extends AbstractWriteTest {

    private ChronicleMap<String, String> map;

    @Benchmark()
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = MEASUREMENT_ITERACTIONS, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        map.put(hash, VALUE);
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        File dir = prepareDirectory();

        map = ChronicleMap.of(String.class, String.class)//
                .name("benchmark")//
                .entries(100_000_000)//
                .averageKeySize(32) // ≈ bytes, include UTF-8 + length
                .averageValueSize(128) // ≈ bytes, include UTF-8 + length
                .maxBloatFactor(5.0) //
                .createPersistedTo(
                        new File(dir.getAbsolutePath() + "/test.dat"));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: ");
        map.close();
    }

}
