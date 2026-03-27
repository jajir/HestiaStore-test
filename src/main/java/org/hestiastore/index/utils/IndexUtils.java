package org.hestiastore.index.utils;

import java.io.File;

public class IndexUtils {

    public static final String LOCK_FILE_NAME = ".lock";

    private IndexUtils() {
        // Utility class, no instantiation allowed
    }

    public static void optionalyRemoveLockFile(final String directoryName) {
        if (directoryName == null || directoryName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Directory name cannot be null or empty");
        }
        final File lockFile = new File(directoryName, LOCK_FILE_NAME);
        if (lockFile.exists()) {
            if (!lockFile.delete()) {
                throw new IllegalStateException("Failed to delete lock file: "
                        + lockFile.getAbsolutePath());
            }
        }
    }

}
