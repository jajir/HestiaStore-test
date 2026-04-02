package org.hestiastore.index.benchmark.runner;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.hestiastore.index.benchmark.common.DiskInfoMaker;
import org.hestiastore.index.benchmark.common.SystemState;
import org.hestiastore.index.benchmark.storage.multithread.read.ChronicleMapReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.H2ReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.HestiaStoreBasicReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.HestiaStoreCompressReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.LevelDBReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.MapDBReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.read.RocksDBReadMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.ChronicleMapWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.H2WriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.HestiaStoreBasicWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.HestiaStoreCompressWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.LevelDBWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.MapDBWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.multithread.write.RocksDBWriteMultiThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.ChronicleMapReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.H2ReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.HestiaStoreBasicReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.HestiaStoreCompressReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.LevelDBReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.MapDBReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.read.RocksDBReadSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.ChronicleMapSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.H2SequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.HestiaStoreBasicSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.HestiaStoreCompressSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.HestiaStoreStreamSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.LevelDBSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.MapDBSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.sequential.RocksDBSequentialReadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.ChronicleMapWriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.H2WriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.HestiaStoreBasicWriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.HestiaStoreCompressWriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.LevelDBWriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.MapDBWriteSingleThreadBenchmark;
import org.hestiastore.index.benchmark.storage.singlethread.write.RocksDBWriteSingleThreadBenchmark;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BenchmarkMain {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BenchmarkMain.class);
    private static final String PROPERTY_ENGINE = "engine";
    private static final String PROPERTY_BENCHMARK_THREADS = "benchmarkThreads";

    private record EngineDefinition(Class<?> benchmarkClass, String resultStem,
            boolean multithread) {
    }

    private static final Map<String, EngineDefinition> ENGINE_TO_BENCHMARK = Map
            .ofEntries(
                    singleThreadWrite("writeSingleThreadHestiaStoreBasic",
                            HestiaStoreBasicWriteSingleThreadBenchmark.class,
                            "HestiaStoreBasic"),
                    singleThreadWrite("writeSingleThreadHestiaStoreCompress",
                            HestiaStoreCompressWriteSingleThreadBenchmark.class,
                            "HestiaStoreCompress"),
                    singleThreadWrite("writeSingleThreadMapDB",
                            MapDBWriteSingleThreadBenchmark.class, "MapDB"),
                    singleThreadWrite("writeSingleThreadH2",
                            H2WriteSingleThreadBenchmark.class, "H2"),
                    singleThreadWrite("writeSingleThreadChronicleMap",
                            ChronicleMapWriteSingleThreadBenchmark.class, "ChronicleMap"),
                    singleThreadWrite("writeSingleThreadRocksDB",
                            RocksDBWriteSingleThreadBenchmark.class, "RocksDB"),
                    singleThreadWrite("writeSingleThreadLevelDB",
                            LevelDBWriteSingleThreadBenchmark.class, "LevelDB"),
                    singleThreadRead("readSingleThreadHestiaStoreBasic",
                            HestiaStoreBasicReadSingleThreadBenchmark.class,
                            "HestiaStoreBasic"),
                    singleThreadRead("readSingleThreadHestiaStoreCompress",
                            HestiaStoreCompressReadSingleThreadBenchmark.class,
                            "HestiaStoreCompress"),
                    singleThreadRead("readSingleThreadMapDB",
                            MapDBReadSingleThreadBenchmark.class, "MapDB"),
                    singleThreadRead("readSingleThreadH2",
                            H2ReadSingleThreadBenchmark.class, "H2"),
                    singleThreadRead("readSingleThreadChronicleMap",
                            ChronicleMapReadSingleThreadBenchmark.class, "ChronicleMap"),
                    singleThreadRead("readSingleThreadRocksDB",
                            RocksDBReadSingleThreadBenchmark.class, "RocksDB"),
                    singleThreadRead("readSingleThreadLevelDB",
                            LevelDBReadSingleThreadBenchmark.class, "LevelDB"),
                    sequentialRead("sequentialReadHestiaStoreBasic",
                            HestiaStoreBasicSequentialReadBenchmark.class,
                            "HestiaStoreBasic"),
                    sequentialRead("sequentialReadHestiaStoreCompress",
                            HestiaStoreCompressSequentialReadBenchmark.class,
                            "HestiaStoreCompress"),
                    sequentialRead("sequentialReadHestiaStoreStream",
                            HestiaStoreStreamSequentialReadBenchmark.class,
                            "HestiaStoreStream"),
                    sequentialRead("sequentialReadMapDB",
                            MapDBSequentialReadBenchmark.class, "MapDB"),
                    sequentialRead("sequentialReadH2",
                            H2SequentialReadBenchmark.class, "H2"),
                    sequentialRead("sequentialReadChronicleMap",
                            ChronicleMapSequentialReadBenchmark.class,
                            "ChronicleMap"),
                    sequentialRead("sequentialReadRocksDB",
                            RocksDBSequentialReadBenchmark.class, "RocksDB"),
                    sequentialRead("sequentialReadLevelDB",
                            LevelDBSequentialReadBenchmark.class, "LevelDB"),
                    multiThreadRead("readMultiThreadHestiaStoreBasic",
                            HestiaStoreBasicReadMultiThreadBenchmark.class,
                            "HestiaStoreBasic"),
                    multiThreadRead("readMultiThreadHestiaStoreCompress",
                            HestiaStoreCompressReadMultiThreadBenchmark.class,
                            "HestiaStoreCompress"),
                    multiThreadRead("readMultiThreadMapDB",
                            MapDBReadMultiThreadBenchmark.class, "MapDB"),
                    multiThreadRead("readMultiThreadH2",
                            H2ReadMultiThreadBenchmark.class, "H2"),
                    multiThreadRead("readMultiThreadChronicleMap",
                            ChronicleMapReadMultiThreadBenchmark.class,
                            "ChronicleMap"),
                    multiThreadRead("readMultiThreadRocksDB",
                            RocksDBReadMultiThreadBenchmark.class, "RocksDB"),
                    multiThreadRead("readMultiThreadLevelDB",
                            LevelDBReadMultiThreadBenchmark.class, "LevelDB"),
                    multiThreadWrite("writeMultiThreadHestiaStoreBasic",
                            HestiaStoreBasicWriteMultiThreadBenchmark.class,
                            "HestiaStoreBasic"),
                    multiThreadWrite("writeMultiThreadHestiaStoreCompress",
                            HestiaStoreCompressWriteMultiThreadBenchmark.class,
                            "HestiaStoreCompress"),
                    multiThreadWrite("writeMultiThreadMapDB",
                            MapDBWriteMultiThreadBenchmark.class, "MapDB"),
                    multiThreadWrite("writeMultiThreadH2",
                            H2WriteMultiThreadBenchmark.class, "H2"),
                    multiThreadWrite("writeMultiThreadChronicleMap",
                            ChronicleMapWriteMultiThreadBenchmark.class,
                            "ChronicleMap"),
                    multiThreadWrite("writeMultiThreadRocksDB",
                            RocksDBWriteMultiThreadBenchmark.class, "RocksDB"),
                    multiThreadWrite("writeMultiThreadLevelDB",
                            LevelDBWriteMultiThreadBenchmark.class, "LevelDB"));

    public static void main(final String[] args) throws Exception {
        final SystemState state = new SystemState();
        setBeforeCpu(state);

        final String engine = System.getProperty(PROPERTY_ENGINE);
        LOGGER.debug("Property 'engine' is '{}'", engine);
        if (engine == null || engine.isEmpty()) {
            throw new IllegalStateException("Property 'engine' is not set");
        }

        final EngineDefinition definition = ENGINE_TO_BENCHMARK.get(engine);
        if (definition == null) {
            throw new IllegalStateException("Unknown engine '" + engine + "'");
        }

        final String resultPrefix = buildResultPrefix(definition,
                resolveThreadCount());
        final ChainedOptionsBuilder builder = new OptionsBuilder()
                .include(".*" + definition.benchmarkClass().getSimpleName())
                .forks(1)
                .resultFormat(ResultFormatType.JSON)
                .result(resultPrefix + ".json");

        if (definition.multithread()) {
            final int threadCount = resolveThreadCount();
            builder.threads(threadCount);
            LOGGER.info("Running {} with {} benchmark threads", engine,
                    threadCount);
        }

        final Options options = builder.build();
        for (RunResult result : new Runner(options).run()) {
            LOGGER.debug("JMH result: {}",
                    result.getPrimaryResult().getScore());
        }

        DiskInfoMaker diskInfoMaker = new DiskInfoMaker();
        diskInfoMaker.setState(state);
        setAfterCpu(state);
        setMemUsage(state);
        writeAsJson(state, resultPrefix + "-my.json");
    }

    private static Map.Entry<String, EngineDefinition> singleThreadWrite(
            final String engine, final Class<?> benchmarkClass,
            final String engineBase) {
        return Map.entry(engine, new EngineDefinition(benchmarkClass,
                "write-single-thread-" + engineBase, false));
    }

    private static Map.Entry<String, EngineDefinition> singleThreadRead(
            final String engine, final Class<?> benchmarkClass,
            final String engineBase) {
        return Map.entry(engine, new EngineDefinition(benchmarkClass,
                "read-single-thread-" + engineBase, false));
    }

    private static Map.Entry<String, EngineDefinition> sequentialRead(
            final String engine, final Class<?> benchmarkClass,
            final String engineBase) {
        return Map.entry(engine, new EngineDefinition(benchmarkClass,
                "sequential-read-" + engineBase, false));
    }

    private static Map.Entry<String, EngineDefinition> multiThreadRead(
            final String engine, final Class<?> benchmarkClass,
            final String engineBase) {
        return Map.entry(engine, new EngineDefinition(benchmarkClass,
                "read-multi-thread-" + engineBase, true));
    }

    private static Map.Entry<String, EngineDefinition> multiThreadWrite(
            final String engine, final Class<?> benchmarkClass,
            final String engineBase) {
        return Map.entry(engine, new EngineDefinition(benchmarkClass,
                "write-multi-thread-" + engineBase, true));
    }

    private static String buildResultPrefix(final EngineDefinition definition,
            final int threadCount) {
        String prefix = "./results/results-" + definition.resultStem();
        if (definition.multithread()) {
            prefix = prefix + "-threads" + threadCount;
        }
        return prefix;
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
        return os.getProcessCpuTime();
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

    public static void writeAsJson(final Object results,
            final String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(fileName), results);
        } catch (Exception e) {
            throw new RuntimeException("Cannot write results as JSON", e);
        }
    }
}
