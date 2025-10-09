package org.hestiastore.index.integration;

import org.hestiastore.index.sst.IndexConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Helper class for integration tests. In some casess it's usefull to call
 * directly test in a same thread.
 */
class HelperTest {

    @Test
    void test_out_of_memory() {
        final String directoryName = "target/test-out-of-memmory";
        final IndexConfiguration<String, Long> conf = IndexConfiguration
                .<String, Long>builder()//
                .withKeyClass(String.class)//
                .withValueClass(Long.class)//
                .withName("indexicek") //
                .withMaxNumberOfKeysInSegment((int) 500_000) //
                .withMaxNumberOfKeysInSegmentCache(100_000L) //
                .withMaxNumberOfKeysInSegmentCacheDuringFlushing(200_000L) //
                .withMaxNumberOfKeysInSegmentChunk((int) 1_000) //
                .withMaxNumberOfKeysInCache((int) 500_000) //
                .withBloomFilterIndexSizeInBytes((int) 500_000) //
                .withBloomFilterNumberOfHashFunctions((int) 3) //
                .withLogEnabled(false) //
                .build();

        final TestOutOfMemory test = new TestOutOfMemory(conf, directoryName);
        test.startTest();

    }
}
