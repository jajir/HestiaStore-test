package org.hestiastore.index.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is used to create a {@link ProcessBuilder} for running the
 * {@link org.hestiastore.index.integration.Main} class with specified
 * arguments. It allows setting the memory size and test name as command line
 * arguments.
 * 
 * Class justs simplify calling of Main class.
 */
public class CommandLineConf {

    private static final String GENERATED_CLASS_PATH_FILE = "target/test.classpath";

    /**
     * This is file representing this package. Test that run in separet thread
     * still need call this package. So it have to at class path
     */
    private final String thisPackageFileLocation;

    private List<String> args;

    public CommandLineConf(final String thisPackageFileLocation,
            final String mainClassName, final String testName,
            final String memSize) {
        this.thisPackageFileLocation = Objects
                .requireNonNull(thisPackageFileLocation);
        args = new ArrayList<>();
        args.add("java");
        args.add("-Xmx" + memSize);
        addParameter("-cp", loadClassParh());
        args.add(mainClassName);
        args.add("--" + testName);
    }

    public CommandLineConf addParameter(final String paramName,
            final String paramValue) {
        args.add(paramName);
        args.add(paramValue);
        return this;
    }

    public String toString() {
        final StringBuilder buff = new StringBuilder();
        buff.append("Call it manually in case problems: ");
        args.forEach(part -> {
            buff.append(part);
            buff.append(" ");
        });
        return buff.toString();
    }

    private String loadClassParh() {
        try {
            return Files.readString(Paths.get(GENERATED_CLASS_PATH_FILE)).trim()
                    + ":" + getClassPathToThisPackage();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String getClassPathToThisPackage() {
        final Path jarFile = Paths.get(thisPackageFileLocation);
        return jarFile.toFile().getAbsolutePath();
    }

    public ProcessBuilder createProcessBuilder() {
        final ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        return builder;
    }

}
