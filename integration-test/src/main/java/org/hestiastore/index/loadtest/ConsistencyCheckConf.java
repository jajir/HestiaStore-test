package org.hestiastore.index.loadtest;

import java.io.File;

import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorLong;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.sst.IndexConfiguration;

public class ConsistencyCheckConf {

    public static final String DIRECTORY = "target/consistency-check";
    public static final File FILE_DIRECTORY = new File(DIRECTORY);
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
                .withMaxNumberOfKeysInSegmentCache(200_000L) //
                .withMaxNumberOfKeysInSegmentCacheDuringFlushing(500_000L) //
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

}
