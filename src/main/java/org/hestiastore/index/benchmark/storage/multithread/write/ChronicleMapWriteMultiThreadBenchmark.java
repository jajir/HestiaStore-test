package org.hestiastore.index.benchmark.storage.multithread.write;

import java.io.File;
import java.io.IOException;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import net.openhft.chronicle.map.ChronicleMap;

@State(Scope.Benchmark)
public class ChronicleMapWriteMultiThreadBenchmark
        extends AbstractWriteMultiThreadBenchmark {

    private ChronicleMap<String, String> map;

    @Override
    protected void createStorage(final File dir) throws IOException {
        map = ChronicleMap.of(String.class, String.class)
                .name("benchmark-multithread-write")
                .entries(100_000_000)
                .averageKeySize(32)
                .averageValueSize(128)
                .maxBloatFactor(5.0)
                .createPersistedTo(
                        new File(dir, "test-multithread-write.dat"));
    }

    @Override
    protected void writeValue(final String key, final String value) {
        map.put(key, value);
    }

    @Override
    protected void closeStorage() {
        if (map != null) {
            map.close();
        }
    }
}
