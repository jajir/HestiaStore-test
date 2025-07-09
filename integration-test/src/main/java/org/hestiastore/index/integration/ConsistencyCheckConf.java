package org.hestiastore.index.integration;

import java.io.File;

import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorLong;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.sst.IndexConfiguration;
import org.hestiastore.index.utils.FileUtils;

public class ConsistencyCheckConf {

    public static final String DIRECTORY = "target/consistency-check";
    public static final String LOCK_FILE_NAME = ".lock";
    public static final File FILE_DIRECTORY = new File(DIRECTORY);
    private static final File LOCK_FILE = new File(
            ConsistencyCheckConf.FILE_DIRECTORY, LOCK_FILE_NAME);
    private final static TypeDescriptor<String> TYPE_DESCRIPTOR_STRING = new TypeDescriptorString();
    private final static TypeDescriptor<Long> TYPE_DESCRIPTOR_LONG = new TypeDescriptorLong();

    private final IndexConfiguration<String, Long> indexConfiguration;

    ConsistencyCheckConf() {
        indexConfiguration = IndexConfiguration.<String, Long>builder()//
                .withName("kachnicka")//
                .withKeyClass(String.class)//
                .withValueClass(Long.class)//
                .withKeyTypeDescriptor(TYPE_DESCRIPTOR_STRING) //
                .withValueTypeDescriptor(TYPE_DESCRIPTOR_LONG) //
                .withMaxNumberOfKeysInSegment(1_000_000) //
                .withMaxNumberOfKeysInSegmentCache(2_000L) //
                .withMaxNumberOfKeysInSegmentCacheDuringFlushing(5_000L) //
                .withMaxNumberOfKeysInSegmentIndexPage(1_000) //
                .withMaxNumberOfKeysInCache(10_000_000) //
                .withBloomFilterIndexSizeInBytes(0) //
                .withBloomFilterNumberOfHashFunctions(1) //
                .withLogEnabled(false) //
                .build();
    }

    public IndexConfiguration<String, Long> getIndexConfiguration() {
        return indexConfiguration;
    }

    public static void removeLockFile() {
        if (LOCK_FILE.exists()) {
            FileUtils.deleteFileRecursively(LOCK_FILE);
        } else {
            throw new IllegalStateException(
                    "Lock file does not exist: " + LOCK_FILE.getAbsolutePath());
        }
    }

}
