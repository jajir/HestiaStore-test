package org.hestiastore.index.integration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.utils.AbstractIndexCli;

public class Main extends AbstractIndexCli {

    protected final static Option OPTION_HELP = Option.builder()//
            .longOpt("help")//
            .hasArg(false)//
            .desc("display help")//
            .build();

    protected final static Option OPTION_TEST_1 = Option.builder()//
            .longOpt("test1")//
            .hasArg(false)//
            .desc("test1 - consistency test")//
            .build();

    // Write and related parameters
    protected final static Option OPTION_WRITE = Option.builder()//
            .longOpt("write")//
            .hasArg(false)//
            .desc("write new random data into index.")//
            .build();

    protected final static Option OPTION_COUNT = Option.builder("c")//
            .longOpt("count")//
            .hasArg(true)//
            .required(false)//
            .desc("How many key will be written").build();

    // Search and related parameters
    protected final static Option OPTION_SEARCH = Option.builder()//
            .longOpt("search")//
            .hasArg(false)//
            .desc("search random data from index.")//
            .build();

    protected final static Option OPTION_MAX_KEY = Option.builder()//
            .longOpt("max-key")//
            .hasArg(true)//
            .required(false)//
            .desc("Max key value to search").build();

    public static void main(final String[] args) throws Exception {
        new Main(args);
    }

    Main(final String[] args) throws ParseException {
        super(args);
    }

    @Override
    public Options addOptions(final Options options) {
        options.addOption(OPTION_MAX_KEY);
        options.addOption(OPTION_HELP);
        options.addOption(OPTION_TEST_1);
        options.addOption(OPTION_DIRECTORY);
        options.addOption(OPTION_COUNT);
        options.addOption(OPTION_WRITE);
        options.addOption(OPTION_SEARCH);
        return options;
    }

    @Override
    public void processCommandLile(final CommandLine cmd,
            final Options options) {
        if (cmd.hasOption(OPTION_HELP)) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(120);
            formatter.printHelp(
                    "java -jar target/load-test.jar com.coroptis.index.loadtest.Main",
                    options);
        } else if (cmd.hasOption(OPTION_TEST_1)) {
            final ConsistencyCheck consistencyCheck = new ConsistencyCheck();
            consistencyCheck.test();
        } else if (cmd.hasOption(OPTION_TEST_1)) {
            final ConsistencyCheck consistencyCheck = new ConsistencyCheck();
            consistencyCheck.test();
        } else if (cmd.hasOption(OPTION_WRITE)) {
            handleWriteOption(cmd);
        } else if (cmd.hasOption(OPTION_SEARCH)) {
            handleSearchOption(cmd);
        } else {
            throw new IllegalArgumentException(
                    "Unknown command. There must be --help, "
                            + "--search or --write");
        }
    }

    protected void handleWriteOption(final CommandLine cmd) {
        final long count = extractCountOption(cmd);
        final Index<String, Long> index = createIndex(cmd);
        final WriteData writeData = new WriteData(index);
        writeData.write(count);
    }

    protected void handleSearchOption(final CommandLine cmd) {
        final long count = extractCountOption(cmd);
        final long maxKey = extractMaxKeyOption(cmd);
        final Index<String, Long> index = createIndex(cmd);
        SearchData searchData = new SearchData(index);
        searchData.search(count, maxKey);
    }

    protected long extractCountOption(final CommandLine cmd) {
        if (cmd.hasOption(OPTION_COUNT)) {
            return parseLong(cmd.getOptionValue(OPTION_COUNT));
        } else {
            throw new IllegalArgumentException(
                    "When you select write task then you must specify count of keys");
        }
    }

    protected long extractMaxKeyOption(final CommandLine cmd) {
        if (cmd.hasOption(OPTION_MAX_KEY)) {
            return parseLong(cmd.getOptionValue(OPTION_MAX_KEY));
        } else {
            throw new IllegalArgumentException(
                    "When you select this task then you must specify max key value");
        }
    }

}
