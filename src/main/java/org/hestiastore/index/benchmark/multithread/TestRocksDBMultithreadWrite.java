package org.hestiastore.index.benchmark.multithread;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

@State(Scope.Benchmark)
public class TestRocksDBMultithreadWrite
        extends AbstractMultithreadWriteBenchmark {

    private Options options;
    private RocksDB storage;

    @Override
    protected void createStorage(final File dir) throws RocksDBException {
        options = new Options().setCreateIfMissing(true).setUseFsync(false);
        final File rocksDir = new File(dir, "rocks-multithread-write");
        rocksDir.mkdirs();
        storage = RocksDB.open(options, rocksDir.getAbsolutePath());
    }

    @Override
    protected void writeValue(final String key, final String value)
            throws RocksDBException {
        storage.put(key.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void closeStorage() {
        if (storage != null) {
            storage.close();
        }
        if (options != null) {
            options.close();
        }
    }
}
