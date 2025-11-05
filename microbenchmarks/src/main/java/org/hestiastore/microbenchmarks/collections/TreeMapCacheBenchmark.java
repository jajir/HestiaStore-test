package org.hestiastore.microbenchmarks.collections;

import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class TreeMapCacheBenchmark {

    private TreeMap<Integer, String> map;
    private Random rnd;

    @Setup(Level.Trial)
    public void setup() {
        map = new TreeMap<>();
        rnd = new Random(42);
        for (int i = 0; i < 100_000; i++) {
            map.put(i, "v" + i);
        }
    }

    @Benchmark
    public String testGet() {
        return map.get(rnd.nextInt(100_000));
    }

    @Benchmark
    public void testPut() {
        map.put(rnd.nextInt(100_000), "x");
    }
}
