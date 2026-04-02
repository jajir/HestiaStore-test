package org.hestiastore.index.benchmark.storage.multithread.read;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LevelDBReadMultiThreadBenchmark
        extends AbstractReadMultiThreadBenchmark {

    private DB storage;

    @Override
    protected void createStorage(final File dir) throws IOException {
        final Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(100 * 1024 * 1024L);

        final File dbDir = new File(dir, "leveldb-multithread-read");
        dbDir.mkdirs();
        storage = Iq80DBFactory.factory.open(dbDir, options);
    }

    @Override
    protected void writeValue(final String key, final String value) {
        storage.put(key.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected String readValue(final String key) {
        final byte[] value = storage.get(key.getBytes(StandardCharsets.UTF_8));
        return value != null ? VALUE : null;
    }

    @Override
    protected void closeStorage() throws IOException {
        if (storage != null) {
            storage.close();
        }
    }
}
