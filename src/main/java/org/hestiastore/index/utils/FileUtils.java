package org.hestiastore.index.utils;

import java.io.File;

public class FileUtils {

    private FileUtils() {
        // Utility class, no instantiation allowed
    }

    /**
     * Recursively deletes the directory (and all files/subdirectories) at the
     * given location, if it exists.
     *
     * @param dir the {@link File} pointing to the directory to delete
     * @throws RuntimeException if an I/O error occurs or if the path exists but
     *                          is not a directory
     */
    public static void deleteFileRecursively(File dir) {
        if (!dir.exists()) {
            // Nothing to do if path doesn't exist
            return;
        }
        if (!dir.isDirectory()) {
            if (!dir.delete()) {
                throw new RuntimeException("Failed to delete file: " + dir);
            }
            return;
        }

        // List all entries (files and subdirectories)
        final File[] entries = dir.listFiles();
        if (entries != null) {
            for (final File entry : entries) {
                deleteFileRecursively(entry);
            }
        }

        // Finally delete the directory itself
        if (!dir.delete()) {
            throw new RuntimeException("Failed to delete directory: " + dir);
        }
    }

}
