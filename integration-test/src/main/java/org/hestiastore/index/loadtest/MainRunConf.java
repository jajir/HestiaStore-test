package org.hestiastore.index.loadtest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class is used to create a {@link ProcessBuilder} for running the
 * {@link org.hestiastore.index.loadtest.Main} class with specified arguments.
 * It allows setting the memory size and test name as command line arguments.
 * 
 * Class justs simplify calling of Main class.
 */
public class MainRunConf {

    private static final String GENERATED_CLASS_PATH_FILE = "target/test.classpath";

    /**
     * This is file representing this package. Test that run in separet thread
     * still need call this package. So it have to at class path
     */
    private static final String THIS_PACKAGE_FILE_LOCATION = "target/integration-test-0.0.0-SNAPSHOT.jar";

    private List<String> args;

    MainRunConf(String testName, String memSize) {
        args = List.of("java", //
                "-Xmx" + memSize, //
                "-cp", //
                loadClassParh(), //
                "org.hestiastore.index.loadtest.Main", //
                "--" + testName//
        );
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
        final Path jarFile = Paths.get(THIS_PACKAGE_FILE_LOCATION);
        return jarFile.toFile().getAbsolutePath();
    }

    public ProcessBuilder createProcessBuilder() {
        final ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        return builder;
    }

}
