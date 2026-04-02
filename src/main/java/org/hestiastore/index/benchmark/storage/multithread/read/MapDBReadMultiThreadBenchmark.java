package org.hestiastore.index.benchmark.storage.multithread.read;

import java.io.File;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class MapDBReadMultiThreadBenchmark extends AbstractReadMultiThreadBenchmark {

    private DB db;
    private HTreeMap<String, String> storage;

    @Override
    protected void createStorage(final File dir) {
        db = DBMaker.fileDB(new File(dir, "data-multithread-read.db"))
                .fileChannelEnable()
                .checksumHeaderBypass()
                .make();
        storage = db.hashMap("users-multithread-read",
                org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING)
                .createOrOpen();
    }

    @Override
    protected void writeValue(final String key, final String value) {
        storage.put(key, value);
    }

    @Override
    protected String readValue(final String key) {
        return storage.get(key);
    }

    @Override
    protected void afterPreload() {
        db.commit();
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
