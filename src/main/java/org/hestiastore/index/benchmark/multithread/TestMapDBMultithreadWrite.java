package org.hestiastore.index.benchmark.multithread;

import java.io.File;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class TestMapDBMultithreadWrite
        extends AbstractMultithreadWriteBenchmark {

    private DB db;
    private HTreeMap<String, String> storage;

    @Override
    protected void createStorage(final File dir) {
        db = DBMaker.fileDB(new File(dir, "data-multithread-write.db"))
                .fileChannelEnable()
                .checksumHeaderBypass()
                .make();
        storage = db.hashMap("users-multithread-write",
                org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING)
                .createOrOpen();
    }

    @Override
    protected void writeValue(final String key, final String value) {
        storage.put(key, value);
    }

    @Override
    protected void closeStorage() {
        if (storage != null) {
            storage.close();
        }
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}
