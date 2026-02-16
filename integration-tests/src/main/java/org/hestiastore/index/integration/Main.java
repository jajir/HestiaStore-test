package org.hestiastore.index.integration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hestiastore.index.segmentindex.IndexConfiguration;
import org.hestiastore.index.utils.AbstractIndexCli;

public class Main extends AbstractIndexCli {

    private final static Option OPTION_HELP = Option.builder()//
            .longOpt("help")//
            .hasArg(false)//
            .desc("display help")//
            .build();

    public static final String OPTION_OUT_OF_MEMORY_TEST_NAME = "outOfMemoryTest";
    private final static Option OPTION_OUT_OF_MEMORY_TEST = Option.builder()//
            .longOpt(OPTION_OUT_OF_MEMORY_TEST_NAME)//
            .hasArg(false)//
            .desc("Verify index data consistency during OutOfMemmoryException")//
            .build();

    public static final String OPTION_GRACEFULL_DEGRADATION_TEST_NAME = "outOfMemoryTest";
    private final static Option OPTION_GRACEFULL_DEGRADATION_TEST = Option
            .builder()//
            .longOpt(OPTION_GRACEFULL_DEGRADATION_TEST_NAME)//
            .hasArg(false)//
            .desc("Verify index data consistency are kept during CTRL+C of process")//
            .build();

    public static void main(final String[] args) throws Exception {
        new Main(args);
    }

    Main(final String[] args) throws ParseException {
        super(args);
    }

    @Override
    public Options addOptions(final Options options) {
        options.addOption(OPTION_OUT_OF_MEMORY_TEST);
        options.addOption(OPTION_GRACEFULL_DEGRADATION_TEST);
        options.addOption(OPTION_HELP);
        return options;
    }

    @Override
    public void processCommandLile(final CommandLine cmd,
            final Options options) {
        if (cmd.hasOption(OPTION_HELP)) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(120);
            formatter.printHelp(
                    "Detail are at class com.coroptis.index.integration.MainRunConf",
                    options);
        } else if (cmd.hasOption(OPTION_OUT_OF_MEMORY_TEST)) {
            final String directory = extractDirectoryOption(cmd);
            final IndexConfiguration<String, Long> conf = createIndexConfiguration(
                    cmd);
            outOfMemoryTest(conf, directory);
        } else if (cmd.hasOption(OPTION_GRACEFULL_DEGRADATION_TEST)) {
            final String directory = extractDirectoryOption(cmd);
            final IndexConfiguration<String, Long> conf = createIndexConfiguration(
                    cmd);
            gracefullDegradatonTest(conf, directory);
        } else {
            throw new IllegalArgumentException(
                    "Unknown command. There should be --help "
                            + "option to display help.");
        }
    }

    private void outOfMemoryTest(final IndexConfiguration<String, Long> index,
            final String directoryName) {
        final TestOutOfMemory test = new TestOutOfMemory(index, directoryName);
        test.startTest();
    }

    private void gracefullDegradatonTest(
            final IndexConfiguration<String, Long> index,
            final String directoryName) {
        final TestGracefullDegradation test = new TestGracefullDegradation(
                index, directoryName);
        test.startTest();
    }

}
