package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.hestiastore.index.benchmark.multithread.TestChronicleMapMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestChronicleMapMultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestH2MultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestH2MultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestHestiaStoreBasicMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestHestiaStoreBasicMultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestHestiaStoreCompressMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestHestiaStoreCompressMultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestLevelDBMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestLevelDBMultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestMapDBMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestMapDBMultithreadWrite;
import org.hestiastore.index.benchmark.multithread.TestRocksDBMultithreadRead;
import org.hestiastore.index.benchmark.multithread.TestRocksDBMultithreadWrite;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final static String PROPERTY_ENGINE = "engine";
    private final static String PROPERTY_BENCHMARK_THREADS = "benchmarkThreads";
    private final static String MULTITHREAD_READ_SUFFIX = "MultithreadRead";
    private final static String MULTITHREAD_WRITE_SUFFIX = "MultithreadWrite";

    private static final Map<String, Class<?>> ENGINE_TO_BENCHMARK = Map
            .ofEntries(
                    Map.entry("HestiaStoreBasic",
                            TestHestiaStoreBasicWrite.class),
                    Map.entry("HestiaStoreBasicRead",
                            TestHestiaStoreBasicRead.class),
                    Map.entry("HestiaStoreBasicSequential",
                            TestHestiaStoreBasicSequential.class),
                    Map.entry("HestiaStoreCompressWrite",
                            TestHestiaStoreCompressWrite.class),
                    Map.entry("HestiaStoreCompressRead",
                            TestHestiaStoreCompressRead.class),
                    Map.entry("HestiaStoreCompressSequential",
                            TestHestiaStoreCompressSequential.class),
                    Map.entry("HestiaStoreCompressSequential2",
                            TestHestiaStoreStreamSequential.class),
                    Map.entry("MapDB", TestMapDBWrite.class),
                    Map.entry("MapDBRead", TestMapDBRead.class),
                    Map.entry("MapDBSequential", TestMapDBSequential.class),
                    Map.entry("H2", TestH2Write.class),
                    Map.entry("H2Read", TestH2Read.class),
                    Map.entry("H2Sequential", TestH2Sequential.class),
                    Map.entry("ChronicleMap", TestChronicleMapWrite.class),
                    Map.entry("ChronicleMapRead", TestChronicleMapRead.class),
                    Map.entry("ChronicleMapSequential",
                            TestChronicleMapSequential.class),
                    Map.entry("RocksDB", TestRocksDBWrite.class),
                    Map.entry("RocksDBRead", TestRocksDBRead.class),
                    Map.entry("RocksDBSequential", TestRocksDBSequential.class),
                    Map.entry("LevelDB", TestLevelDBWrite.class),
                    Map.entry("LevelDBRead", TestLevelDBRead.class),
                    Map.entry("LevelDBSequential", TestLevelDBSequential.class),
                    Map.entry("HestiaStoreBasic" + MULTITHREAD_READ_SUFFIX,
                            TestHestiaStoreBasicMultithreadRead.class),
                    Map.entry("HestiaStoreBasic" + MULTITHREAD_WRITE_SUFFIX,
                            TestHestiaStoreBasicMultithreadWrite.class),
                    Map.entry("HestiaStoreCompress" + MULTITHREAD_READ_SUFFIX,
                            TestHestiaStoreCompressMultithreadRead.class),
                    Map.entry("HestiaStoreCompress" + MULTITHREAD_WRITE_SUFFIX,
                            TestHestiaStoreCompressMultithreadWrite.class),
                    Map.entry("MapDB" + MULTITHREAD_READ_SUFFIX,
                            TestMapDBMultithreadRead.class),
                    Map.entry("MapDB" + MULTITHREAD_WRITE_SUFFIX,
                            TestMapDBMultithreadWrite.class),
                    Map.entry("H2" + MULTITHREAD_READ_SUFFIX,
                            TestH2MultithreadRead.class),
                    Map.entry("H2" + MULTITHREAD_WRITE_SUFFIX,
                            TestH2MultithreadWrite.class),
                    Map.entry("ChronicleMap" + MULTITHREAD_READ_SUFFIX,
                            TestChronicleMapMultithreadRead.class),
                    Map.entry("ChronicleMap" + MULTITHREAD_WRITE_SUFFIX,
                            TestChronicleMapMultithreadWrite.class),
                    Map.entry("RocksDB" + MULTITHREAD_READ_SUFFIX,
                            TestRocksDBMultithreadRead.class),
                    Map.entry("RocksDB" + MULTITHREAD_WRITE_SUFFIX,
                            TestRocksDBMultithreadWrite.class),
                    Map.entry("LevelDB" + MULTITHREAD_READ_SUFFIX,
                            TestLevelDBMultithreadRead.class),
                    Map.entry("LevelDB" + MULTITHREAD_WRITE_SUFFIX,
                            TestLevelDBMultithreadWrite.class));

    /**
     * Main entry that runs the selected JMH benchmark class.
     */
    public static void main(final String[] args) throws Exception {
        final SystemState state = new SystemState();
        setBeforeCpu(state);
        final String engine = System.getProperty(PROPERTY_ENGINE);
        LOGGER.debug("Property 'engine' is '" + engine + "'");
        if (engine == null || engine.isEmpty()) {
            throw new IllegalStateException("Property 'engine' is not set");
        }

        final Class<?> benchmarkClass = ENGINE_TO_BENCHMARK.get(engine);
        if (benchmarkClass == null) {
            throw new IllegalStateException("Unknown engine '" + engine + "'");
        }
        final String includePattern = benchmarkClass.getSimpleName();

        final boolean isMultithreadReadVariant = engine
                .endsWith(MULTITHREAD_READ_SUFFIX);
        final boolean isMultithreadWriteVariant = engine
                .endsWith(MULTITHREAD_WRITE_SUFFIX);
        final boolean isMultithreadVariant = isMultithreadReadVariant
                || isMultithreadWriteVariant;
        final boolean isReadVariant = !isMultithreadVariant
                && engine.endsWith("Read");
        final boolean isSequentialVariant = engine.endsWith("Sequential")
                || engine.endsWith("Sequential2");
        final String engineBase;
        final String resultPrefix;
        final int threadCount = resolveThreadCount();
        if (isMultithreadReadVariant) {
            engineBase = engine.substring(
                    0, engine.length() - MULTITHREAD_READ_SUFFIX.length());
            resultPrefix = "./results/results-multithread-read-" + engineBase
                    + "-threads" + threadCount;
        } else if (isMultithreadWriteVariant) {
            engineBase = engine.substring(
                    0, engine.length() - MULTITHREAD_WRITE_SUFFIX.length());
            resultPrefix = "./results/results-multithread-write-" + engineBase
                    + "-threads" + threadCount;
        } else if (isReadVariant) {
            engineBase = engine.substring(0, engine.length() - "Read".length());
            resultPrefix = "./results/results-read-" + engineBase;
        } else if (isSequentialVariant) {
            final String suffix = engine.endsWith("Sequential2") ? "Sequential2"
                    : "Sequential";
            engineBase = engine.substring(0, engine.length() - suffix.length());
            resultPrefix = "./results/results-sequential-" + engineBase;
        } else {
            engineBase = engine;
            resultPrefix = "./results/results-write-" + engine;
        }

        final ChainedOptionsBuilder builder = new OptionsBuilder()
                .include(".*" + includePattern + "")//
                .forks(1)//
                .resultFormat(ResultFormatType.JSON)//
                .result(resultPrefix + ".json");
        if (isMultithreadVariant) {
            builder.threads(threadCount);
            LOGGER.info("Running {} with {} benchmark threads", engine,
                    threadCount);
        }
        final Options opt = builder.build();

        for (RunResult results : new Runner(opt).run()) {
            LOGGER.debug(
                    "JMH result: " + results.getPrimaryResult().getScore());
        }
        DiskInfoMaker diskInfoMaker = new DiskInfoMaker();
        diskInfoMaker.setState(state);
        setAfterCpu(state);
        setMemUsage(state);
        writeAsJson(state, resultPrefix + "-my.json");
    }

    private static void setBeforeCpu(final SystemState state) {
        state.setCpuBefore(getProcessCpuTime());
        state.setStartTime(System.nanoTime());
    }

    private static void setAfterCpu(final SystemState state) {
        state.setCpuAfter(getProcessCpuTime());
        state.setEndTime(System.nanoTime());
        long cpuTime = state.getCpuAfter() - state.getCpuBefore();
        long wallTime = state.getEndTime() - state.getStartTime();
        state.setCpuUsage((double) cpuTime / (double) wallTime * 100D);
    }

    private static void setMemUsage(final SystemState state) {
        state.setUsedMemoryBytes(Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory());
    }

    private static long getProcessCpuTime() {
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();
        return os.getProcessCpuTime(); // nanoseconds
    }

    private static int resolveThreadCount() {
        final String raw = System.getProperty(PROPERTY_BENCHMARK_THREADS, "1");
        try {
            final int value = Integer.parseInt(raw);
            if (value <= 0) {
                throw new IllegalStateException("Property '"
                        + PROPERTY_BENCHMARK_THREADS + "' must be > 0");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Property '" + PROPERTY_BENCHMARK_THREADS
                            + "' must be an integer",
                    e);
        }
    }

    public static void writeAsJson(Object results, String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(fileName), results);
        } catch (Exception e) {
            throw new RuntimeException("Cannot write results as JSON", e);
        }
    }
}
