package org.hestiastore.microbenchmarks.collections;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hestiastore.index.Entry;
import org.hestiastore.index.cache.UniqueCache;
import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.directory.async.AsyncDirectory;
import org.hestiastore.index.directory.async.AsyncDirectoryAdapter;
import org.hestiastore.index.sorteddatafile.SortedDataFile;
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

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@State(Scope.Benchmark)
public class UniqueCacheBenchmark extends AbstractBenchmark {

    private FsDirectory directory;
    private AsyncDirectory asyncDirectory;
    private UniqueCache<String, String> cache;
    private SortedDataFile<String, String> cacheFile;

    private static final TypeDescriptor<String> keyTypeDescriptor = new TypeDescriptorString();

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 10, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String put() {
        final String key = dataProvider.generateRandomString(20);

        cache.put(Entry.of(key, VALUE));
        return key;
    }

    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 10, time = MEASUREMENT_TIME, timeUnit = TimeUnit.SECONDS)
    public String get() {
        final String key = dataProvider.generateRandomString(20);
        cache.get(key);
        return key;
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        this.directory = new FsDirectory(prepareDirectory());
        this.asyncDirectory = AsyncDirectoryAdapter.wrap(directory);
        cacheFile = SortedDataFile.<String, String>builder()//
                .withKeyTypeDescriptor(keyTypeDescriptor)//
                .withValueTypeDescriptor(keyTypeDescriptor)//
                .withAsyncDirectory(asyncDirectory)//
                .withFileName("segment-file")//
                .build();
        this.cache = UniqueCache.<String, String>builder()//
                .withKeyComparator(keyTypeDescriptor.getComparator())//
                .withDataFile(cacheFile)//
                .build();

        // Fill chache with initial data
        for (int i = 0; i < 10_000_000; i++) {
            final String key = dataProvider.generateRandomString(20);
            cache.put(Entry.of(key, VALUE));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        System.out.println("Cache size: " + cache.size());
        cache = null;
        cacheFile = null;
        directory = null;
        if (asyncDirectory != null) {
            asyncDirectory.close();
            asyncDirectory = null;
        }
    }

}
