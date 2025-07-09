package org.hestiastore.index.integration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hestiastore.index.sst.Index;
import org.hestiastore.index.utils.AbstractIndexCli;

public class Main extends AbstractIndexCli {

    public static void main(final String[] args) throws Exception {
        new Main(args);
    }

    Main(final String[] args) throws ParseException {
        super(args);
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

}
