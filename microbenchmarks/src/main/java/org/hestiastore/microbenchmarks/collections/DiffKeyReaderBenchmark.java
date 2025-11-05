package org.hestiastore.microbenchmarks.collections;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Entry;
import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.datatype.TypeReader;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FileReader;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sorteddatafile.DiffKeyReader;
import org.hestiastore.index.sorteddatafile.SortedDataFile;
import org.hestiastore.index.sorteddatafile.SortedDataFileWriterTx;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Microbenchmark focusing on reading keys using {@link DiffKeyReader}.
 *
 * Setup phase generates a sorted data file with String keys and String values,
 * targeting at least ~50MB of on-disk size. Keys are built using
 * {@code dataProvider.generateRandomString(20)} and then sorted to satisfy writer
 * ordering requirements. Values are constant {@code VALUE}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@State(Scope.Benchmark)
public class DiffKeyReaderBenchmark extends AbstractBenchmark {

    private static final String FILE_NAME = "diff-keys-segment";

    private static final TypeDescriptor<String> typeString = new TypeDescriptorString();

    private FsDirectory directory;
    private SortedDataFile<String, String> dataFile;

    // Reader state reused across benchmark iterations
    private FileReader fileReader;
    private DiffKeyReader<String> keyReader;
    private TypeReader<String> valueReader;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        this.directory = new FsDirectory(prepareDirectory());

        // Prepare file instance
        this.dataFile = SortedDataFile.<String, String>builder()//
                .withKeyTypeDescriptor(typeString)//
                .withValueTypeDescriptor(typeString)//
                .withDirectory(directory)//
                .withFileName(FILE_NAME)//
                .build();

        // Create at least ~50MB file by writing ~2,000,000 entries
        // Estimate per-entry size: diff-key header+key (~22B) + value (4+5=9B) ~= 31B
        // 2,000,000 * 31B ~= 62MB
        final int targetEntries = 2_000_000;

        generateDataFile(targetEntries);

        // Initialize readers for the benchmark runs
        openReaders();
    }

    private void openReaders() {
        // Open a fresh reader and reset keyReader state
        this.fileReader = directory.getFileReader(FILE_NAME);
        this.keyReader = new DiffKeyReader<>(typeString.getConvertorFromBytes());
        this.valueReader = typeString.getTypeReader();
    }

    private void closeReaders() {
        if (fileReader != null) {
            fileReader.close();
            fileReader = null;
        }
        keyReader = null;
        valueReader = null;
    }

    private void generateDataFile(final int targetEntries) throws IOException {
        // Collect unique random keys, then sort them to satisfy writer ordering
        final Set<String> unique = new HashSet<>(Math.max(16, targetEntries * 4 / 3));
        while (unique.size() < targetEntries) {
            unique.add(dataProvider.generateRandomString(20));
        }
        final List<String> keys = new ArrayList<>(unique);
        Collections.sort(keys);

        final SortedDataFileWriterTx<String, String> tx = dataFile.openWriterTx();
        try (var writer = tx.open()) {
            for (String k : keys) {
                writer.write(Entry.of(k, VALUE));
            }
        }
        tx.commit();

        // Sanity print for visibility when run via main harness
        final File f = new File(directoryFileName, FILE_NAME);
        if (f.exists()) {
            System.out.println("Created file: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
        }
    }

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 10, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String readKey() {
        // Read next key and consume its value to keep stream aligned
        String key = keyReader.read(fileReader);
        if (key == null) {
            // EOF reached; reopen and try again
            closeReaders();
            openReaders();
            key = keyReader.read(fileReader);
            if (key == null) {
                // Should not happen for a non-empty file, but return a constant to satisfy JMH
                return "";
            }
        }
        // consume the value to advance file position correctly
        valueReader.read(fileReader);
        return key;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        closeReaders();
        dataFile = null;
        directory = null;
    }
}
