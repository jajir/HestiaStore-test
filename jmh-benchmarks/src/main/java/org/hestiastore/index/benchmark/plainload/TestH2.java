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
public class TestH2 extends AbstractPlainLoadTest {

    private MVStore store;
    private MVMap<String, String> map;

    @Benchmark()
    @Warmup(iterations = WARM_UP_ITERACTIONS, time = WARM_UP_TIME, timeUnit = TimeUnit.SECONDS)
    public String write() {
        final long rnd = RANDOM.nextLong();
        final String hash = HASH_DATA_PROVIDER.makeHash(rnd);
        map.put(hash, VALUE);
        return hash;
    }

    @Setup(Level.Trial)
    public void setup() {
        File dir = prepareDirectory();
        store = new MVStore.Builder()
                .fileName(dir.getAbsolutePath() + "/test.dat").cacheSize(4096)
                .autoCommitDisabled().open();

        MVMap.Builder<String, String> builder = new MVMap.Builder<String, String>()//
                .keyType(StringDataType.INSTANCE)//
                .valueType(StringDataType.INSTANCE);//
        Map<String, Object> config = new HashMap<>();
        map = builder.create(store, config);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Closing index and directory, number of written keys: ");
        store.close();
    }

}
