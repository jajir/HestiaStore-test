package org.hestiastore.index.benchmark.plainload;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final static String PROPERTY_ENGINE = "engine";

    private static final Map<String, Class<?>> ENGINE_TO_BENCHMARK = Map
            .ofEntries(
                    Map.entry("HestiaStoreBasic",
                            TestHestiaStoreBasicWrite.class),
                    Map.entry("HestiaStoreBasicRead",
                            TestHestiaStoreBasicRead.class),
                    Map.entry("HestiaStoreCompress",
                            TestHestiaStoreCompressWrite.class),
                    Map.entry("HestiaStoreCompressRead",
                            TestHestiaStoreCompressRead.class),
                    Map.entry("MapDB", TestMapDBWrite.class),
                    Map.entry("MapDBRead", TestMapDBRead.class),
                    Map.entry("H2", TestH2Write.class),
                    Map.entry("H2Read", TestH2Read.class),
                    Map.entry("ChronicleMap", TestChronicleMapWrite.class),
                    Map.entry("ChronicleMapRead", TestChronicleMapRead.class),
                    Map.entry("RocksDB", TestRocksDBWrite.class),
                    Map.entry("RocksDBRead", TestRocksDBRead.class),
                    Map.entry("LevelDB", TestLevelDBWrite.class),
                    Map.entry("LevelDBRead", TestLevelDBRead.class));

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

        final Options opt = new OptionsBuilder()
                .include(".*" + includePattern + "")//
                .forks(1)//
                .resultFormat(ResultFormatType.JSON)//
                .result("./results/results-" + engine + ".json")//
                .build()//
        ;

        for (RunResult results : new Runner(opt).run()) {
            LOGGER.debug(
                    "JMH result: " + results.getPrimaryResult().getScore());
        }
        DiskInfoMaker diskInfoMaker = new DiskInfoMaker();
        diskInfoMaker.setState(state);
        setAfterCpu(state);
        setMemUsage(state);
        writeAsJson(state, "./results/results-" + engine + "-my.json");
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
