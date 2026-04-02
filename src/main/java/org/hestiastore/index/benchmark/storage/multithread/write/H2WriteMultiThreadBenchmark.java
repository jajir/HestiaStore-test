package org.hestiastore.index.benchmark.storage.multithread.write;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.StringDataType;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class H2WriteMultiThreadBenchmark extends AbstractWriteMultiThreadBenchmark {

    private MVStore store;
    private MVMap<String, String> map;

    @Override
    protected void createStorage(final File dir) {
        store = new MVStore.Builder()
                .fileName(
                        new File(dir, "test-multithread-write.dat")
                                .getAbsolutePath())
                .cacheSize(4096)
                .autoCommitDisabled()
                .open();

        final MVMap.Builder<String, String> builder = new MVMap.Builder<String, String>()
                .keyType(StringDataType.INSTANCE)
                .valueType(StringDataType.INSTANCE);
        final Map<String, Object> config = new HashMap<>();
        map = builder.create(store, config);
    }

    @Override
    protected void writeValue(final String key, final String value) {
        map.put(key, value);
    }

    @Override
    protected void closeStorage() {
        if (store != null) {
            store.close();
        }
    }
}
