package de.ppi.fakesftpserver.utils;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Util-Methods for the Sftp-Fake-Server
 */
public final class SftpServerUtil {

    private SftpServerUtil() {
        throw new UnsupportedOperationException("Util-Class can't be instantiated");
    }


    /**
     * Creates all directories of a Path.
     * @param path Path for directories
     */
    @SneakyThrows
    public static void ensureDirectoryOfPathExists(final Path path) {
        final Path directory = path.getParent();
        if (directory != null && !directory.equals(path.getRoot())) {
            Files.createDirectories(directory);
        }
    }
}
