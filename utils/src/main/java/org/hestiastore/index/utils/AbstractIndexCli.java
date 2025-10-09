package org.hestiastore.index.utils;

import java.io.File;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hestiastore.index.datatype.TypeDescriptor;
import org.hestiastore.index.datatype.TypeDescriptorLong;
import org.hestiastore.index.datatype.TypeDescriptorString;
import org.hestiastore.index.directory.Directory;
import org.hestiastore.index.directory.FsDirectory;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.sst.IndexConfiguration;

/**
 * Abstract class that all support command line interface for creating index.
 */
public abstract class AbstractIndexCli {

    protected final static TypeDescriptor<String> TYPE_DESCRIPTOR_STRING = new TypeDescriptorString();
    protected final static TypeDescriptor<Long> TYPE_DESCRIPTOR_LONG = new TypeDescriptorLong();

    // commmon index parameters

    private final static String STR_DIRECTORY = "directory";
    public final static String PARAM_DIRECTORY = "--" + STR_DIRECTORY;
    protected final static Option OPTION_DIRECTORY = Option.builder()//
            .longOpt(STR_DIRECTORY)//
            .hasArg(true)//
            .required(false)//
            .desc("directory where index lies, when user selects count or search task then this parameter is mandatory")//
            .build();

    private final static String STR_INDEX_NAME = "index-name";
    public final static String PARAM_INDEX_NAME = "--" + STR_INDEX_NAME;
    protected final static Option OPTION_INDEX_NAME = Option.builder()//
            .longOpt(STR_INDEX_NAME)//
            .hasArg(true)//
            .required(false)//
            .desc("directory where index lies, when user selects count or search task then this parameter is mandatory")//
            .build();

    private final static String STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT = "max-number-of-keys-in-segment";
    public final static String PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT = "--"
            + STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT;
    protected final static Option OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT = Option
            .builder()//
            .longOpt(STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT)//
            .hasArg(true)//
            .required(false)//
            .desc("Max number of keys in segment").build();

    private final static String STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE = "max-number-of-keys-in-segment-cache";
    public final static String PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE = "--"
            + STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE;
    protected final static Option OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE = Option
            .builder()//
            .longOpt(STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE)//
            .hasArg(true)//
            .required(false)//
            .desc("Max number of keys in segment cache").build();

    private final static String STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING = "max-number-of-keys-in-segment-cache-during-flushing";
    public final static String PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING = "--"
            + STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING;
    protected final static Option OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING = Option
            .builder()//
            .longOpt(STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING)//
            .hasArg(true)//
            .required(false)//
            .desc("Max number of keys in segment cache during flushing")
            .build();

    private final static String STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE = "max-number-of-keys-in-segment-index-page";
    public final static String PARAM_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE = "--"
            + STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE;
    protected final static Option OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE = Option
            .builder()//
            .longOpt(STR_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE)//
            .hasArg(true)//
            .required(false)//
            .desc("Max number of keys in segment index page").build();

    private final static String STR_MAX_NUMBER_OF_KEYS_IN_CACHE = "max-number-of-keys-in-cache";
    public final static String PARAM_MAX_NUMBER_OF_KEYS_IN_CACHE = "--"
            + STR_MAX_NUMBER_OF_KEYS_IN_CACHE;
    protected final static Option OPTION_MAX_NUMBER_OF_KEYS_IN_CACHE = Option
            .builder()//
            .longOpt(STR_MAX_NUMBER_OF_KEYS_IN_CACHE)//
            .hasArg(true)//
            .required(false)//
            .desc("Max number of keys in cache").build();

    private final static String STR_BLOOM_FILTER_INDEX_SIZE_IN_BYTES = "bloom-filter-index-size-in-bytes";
    public final static String PARAM_BLOOM_FILTER_INDEX_SIZE_IN_BYTES = "--"
            + STR_BLOOM_FILTER_INDEX_SIZE_IN_BYTES;
    protected final static Option OPTION_BLOOM_FILTER_INDEX_SIZE_IN_BYTES = Option
            .builder()//
            .longOpt(STR_BLOOM_FILTER_INDEX_SIZE_IN_BYTES)//
            .hasArg(true)//
            .required(false)//
            .desc("Bloom filter index size in bytes").build();

    private final static String STR_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS = "bloom-filter-number-of-hash-functions";
    public final static String PARAM_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS = "--"
            + STR_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS;
    protected final static Option OPTION_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS = Option
            .builder()//
            .longOpt(STR_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS)//
            .hasArg(true)//
            .required(false)//
            .desc("Bloom filter number of hash functions").build();

    protected Options makeOptions() {
        final Options options = new Options();
        options.addOption(OPTION_DIRECTORY);
        options.addOption(OPTION_INDEX_NAME);
        options.addOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT);
        options.addOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE);
        options.addOption(
                OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING);
        options.addOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE);
        options.addOption(OPTION_MAX_NUMBER_OF_KEYS_IN_CACHE);
        options.addOption(OPTION_BLOOM_FILTER_INDEX_SIZE_IN_BYTES);
        options.addOption(OPTION_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS);
        return addOptions(options);
    }

    public Options addOptions(final Options option) {
        return option;
    }

    public abstract void processCommandLile(final CommandLine cmd,
            final Options options);

    protected AbstractIndexCli(final String[] args) throws ParseException {
        final Options options = makeOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);
        processCommandLile(cmd, options);
    }

    protected IndexConfiguration<String, Long> createIndexConfiguration(
            final CommandLine cmd) {
        final long maxNumberOfKeysInSegment = extractMaxNumberOfKeysInSegmentOption(
                cmd);
        final long maxNumberOfKeysInSegmentCache = extractMaxNumberOfKeysInSegmentCacheOption(
                cmd);
        final long maxNumberOfKeysInSegmentCacheDuringFlushing = extractMaxNumberOfKeysInSegmentCacheDuringFlushingOption(
                cmd);
        final long maxNumberOfKeysInSegmentChunk = extractMaxNumberOfKeysInSegmentIndexPageOption(
                cmd);
        final long maxNumberOfKeysInCache = extractMaxNumberOfKeysInCacheOption(
                cmd);
        final long bloomFilterIndexSizeInBytes = extractBloomFilterIndexSizeInBytesOption(
                cmd);
        final long bloomFilterNumberOfHashFunctions = extractBloomFilterNumberOfHashFunctionsOption(
                cmd);
        final String indexName = extractIndexName(cmd);

        final IndexConfiguration<String, Long> conf = IndexConfiguration
                .<String, Long>builder()//
                .withKeyClass(String.class)//
                .withValueClass(Long.class)//
                .withName(indexName) //
                .withKeyTypeDescriptor(TYPE_DESCRIPTOR_STRING) //
                .withValueTypeDescriptor(TYPE_DESCRIPTOR_LONG) //
                .withMaxNumberOfKeysInSegment((int) maxNumberOfKeysInSegment) //
                .withMaxNumberOfKeysInSegmentCache(
                        maxNumberOfKeysInSegmentCache) //
                .withMaxNumberOfKeysInSegmentCacheDuringFlushing(
                        maxNumberOfKeysInSegmentCacheDuringFlushing) //
                .withMaxNumberOfKeysInSegmentChunk(
                        (int) maxNumberOfKeysInSegmentChunk) //
                .withMaxNumberOfKeysInCache((int) maxNumberOfKeysInCache) //
                .withBloomFilterIndexSizeInBytes(
                        (int) bloomFilterIndexSizeInBytes) //
                .withBloomFilterNumberOfHashFunctions(
                        (int) bloomFilterNumberOfHashFunctions) //
                .withLogEnabled(false) //
                .build();
        return conf;
    }

    protected Index<String, Long> createIndex(final CommandLine cmd) {
        final String directory = extractDirectoryOption(cmd);
        final Directory dir = new FsDirectory(new File(directory));
        final IndexConfiguration<String, Long> conf = createIndexConfiguration(
                cmd);
        return Index.create(dir, conf);
    }

    protected long extractMaxNumberOfKeysInSegmentOption(
            final CommandLine cmd) {
        if (cmd.hasOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT)) {
            return parseLong(
                    cmd.getOptionValue(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max number of keys in segment");
        }
    }

    protected long extractMaxNumberOfKeysInSegmentCacheOption(
            final CommandLine cmd) {
        if (cmd.hasOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE)) {
            return parseLong(cmd.getOptionValue(
                    OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max number of keys in segment cache");
        }
    }

    protected long extractMaxNumberOfKeysInSegmentCacheDuringFlushingOption(
            final CommandLine cmd) {
        if (cmd.hasOption(
                OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING)) {
            return parseLong(cmd.getOptionValue(
                    OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_CACHE_DURING_FLUSHING));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max number of keys in segment cache during flushing");
        }
    }

    protected long extractMaxNumberOfKeysInSegmentIndexPageOption(
            final CommandLine cmd) {
        if (cmd.hasOption(OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE)) {
            return parseLong(cmd.getOptionValue(
                    OPTION_MAX_NUMBER_OF_KEYS_IN_SEGMENT_INDEX_PAGE));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max number of keys in segment index page");
        }
    }

    protected long extractMaxNumberOfKeysInCacheOption(final CommandLine cmd) {
        if (cmd.hasOption(OPTION_MAX_NUMBER_OF_KEYS_IN_CACHE)) {
            return parseLong(
                    cmd.getOptionValue(OPTION_MAX_NUMBER_OF_KEYS_IN_CACHE));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max number of keys in cache");
        }
    }

    protected long extractBloomFilterIndexSizeInBytesOption(
            final CommandLine cmd) {
        if (cmd.hasOption(OPTION_BLOOM_FILTER_INDEX_SIZE_IN_BYTES)) {
            return parseLong(cmd
                    .getOptionValue(OPTION_BLOOM_FILTER_INDEX_SIZE_IN_BYTES));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify bloom filter index size in bytes");
        }
    }

    protected long extractBloomFilterNumberOfHashFunctionsOption(
            final CommandLine cmd) {
        if (cmd.hasOption(OPTION_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS)) {
            return parseLong(cmd.getOptionValue(
                    OPTION_BLOOM_FILTER_NUMBER_OF_HASH_FUNCTIONS));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify bloom filter number of hash functions");
        }
    }

    protected String extractIndexName(final CommandLine cmd) {
        if (cmd.hasOption(OPTION_INDEX_NAME)) {
            return cmd.getOptionValue(OPTION_INDEX_NAME);
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify index name");
        }
    }

    protected String extractDirectoryOption(final CommandLine cmd) {
        if (cmd.hasOption(OPTION_DIRECTORY)) {
            return cmd.getOptionValue(OPTION_DIRECTORY);
        } else {
            throw new IllegalArgumentException(
                    "When you select count or search task then you must specify directory");
        }
    }

    protected long parseLong(final String str) {
        Objects.requireNonNull(str);
        final String tmp = str.replace("_", "").replace("L", "").replace("l",
                "");
        final long out = Long.parseLong(tmp);
        return out;
    }

}
