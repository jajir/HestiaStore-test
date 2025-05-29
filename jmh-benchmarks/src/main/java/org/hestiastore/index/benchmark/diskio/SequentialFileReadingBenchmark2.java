package org.hestiastore.index.benchmark.diskio;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Pair;
import org.hestiastore.index.PairIterator;
import org.hestiastore.index.PairWriter;
import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorLong;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.directory.Directory.Access;
import org.hestiastore.index.unsorteddatafile.UnsortedDataFile;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BenchmarkMode(Mode.AverageTime) // Measures the average time per operation
@OutputTimeUnit(TimeUnit.MILLISECONDS) // Results in milliseconds
@State(Scope.Thread) // Each thread gets its own state
@Warmup(iterations = 0, time = 1) // 0 warm-up iterations
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1) // Use 1 fork (JVM instance)
@Threads(1)
public class SequentialFileReadingBenchmark2 {

    private final Logger logger = LoggerFactory
            .getLogger(SequentialFileReadingBenchmark2.class);
    private final static String PROPERTY_DIRECTORY = "dir";
    private final static String FILE_NAME = "test.unsorted";
    private final static Random RANDOM = new Random();
    private final static DataProvider dataProvider = new DataProvider();
    private final static int NUMBER_OF_TESTING_PAIRS = 800_000;
    private final static TypeDescriptor<String> TYPE_DESCRIPTOR_STRING = new TypeDescriptorString();
    private final static TypeDescriptor<Long> TYPE_DESCRIPTOR_LONG = new TypeDescriptorLong();

    private String directoryFileName;
    private Directory directory;
    private UnsortedDataFile<String, Long> testFile;

    @Param({ "1", "2", "4", "8", "16", "32" })
    private int diskIoBufferSize;

    @Setup
    public void setup() {
        directoryFileName = System.getProperty(PROPERTY_DIRECTORY);
        logger.debug("Property 'dir' is '" + directoryFileName + "'");
        if (directoryFileName == null || directoryFileName.isEmpty()) {
            throw new IllegalStateException("Property 'dir' is not set");
        }
        directory = new FsDirectory(new File(directoryFileName));

        testFile = getDataFile(diskIoBufferSize);

        // prepare data
        try (PairWriter<String, Long> pairWriter = testFile
                .openWriter(Access.OVERWRITE)) {
            for (int i = 0; i < NUMBER_OF_TESTING_PAIRS; i++) {
                pairWriter.put(dataProvider.generateRandomString(),
                        RANDOM.nextLong());
            }
        }
    }

    @Benchmark
    public String test_reading_buffer() {
        long result = 0;
        try (PairIterator<String, Long> pairIterator = testFile
                .openIterator()) {
            while (pairIterator.hasNext()) {
                final Pair<String, Long> pair = pairIterator.next();
                if (pair == null) {
                    throw new IllegalStateException("Pair is null");
                }
                if (pair.getKey() == null) {
                    throw new IllegalStateException("Key is null");
                }
                if (pair.getValue() == null) {
                    throw new IllegalStateException("Value is null");
                }
                result++;
            }
        }
        return String.valueOf(result);
    }

    private UnsortedDataFile<String, Long> getDataFile(int bufferSize) {
        return UnsortedDataFile.<String, Long>builder()//
                .withDirectory(directory)//
                .withFileName(FILE_NAME)//
                .withKeyWriter(TYPE_DESCRIPTOR_STRING.getTypeWriter())//
                .withKeyReader(TYPE_DESCRIPTOR_STRING.getTypeReader())//
                .withValueWriter(TYPE_DESCRIPTOR_LONG.getTypeWriter())//
                .withValueReader(TYPE_DESCRIPTOR_LONG.getTypeReader())//
                .withDiskIoBufferSize(bufferSize)//
                .build();
    }

}
