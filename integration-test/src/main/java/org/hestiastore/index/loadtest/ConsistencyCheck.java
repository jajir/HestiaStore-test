package org.hestiastore.index.loadtest;

import org.hestiastore.index.benchmark.FileUtils;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;

public class ConsistencyCheck {
    private final Index<String, Long> index;

    ConsistencyCheck() {
        FileUtils.deleteFileRecursively(ConsistencyCheckConf.FILE_DIRECTORY);
        final Directory dir = new FsDirectory(
                ConsistencyCheckConf.FILE_DIRECTORY);
        // Constructor logic if needed

        IndexConfiguration<String, Long> conf = new ConsistencyCheckConf()//
                .getIndexConfiguration();

        this.index = Index.create(dir, conf);
    }

    public Index<String, Long> getIndex() {
        return index;
    }

    private final static long WRITE_KEYS = 9_000_000L;

    void test() {
        TestStatus.reset();
        // Test logic to check consistency
        // This is a placeholder for the actual test logic
        System.out.println("Consistency check - preparing data");
        for (long i = 0; i < WRITE_KEYS; i++) {
            String key = String.valueOf(i);
            Long value = i;
            index.put(key, value);
        }
        System.out.println("Consistency check - ready to test");
        TestStatus.setReadyToTest(true);
        index.flush();
        index.close();
        System.out.println(
                "Consistency check - shoudl not be  here. It shoudl be interrupret to veryfy consistency");
    }

}
