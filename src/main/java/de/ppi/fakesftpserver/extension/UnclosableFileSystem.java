package de.ppi.fakesftpserver.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

/**
 * This wrapper prevents, that a fileSystem can be closed.
 */
@Slf4j
@RequiredArgsConstructor
class UnclosableFileSystem extends FileSystem {

    final FileSystem fileSystem;

    @Override
    public FileSystemProvider provider() {
        return this.fileSystem.provider();
    }

    @Override
    public void close() {
        //will not be closed
    }

    @Override
    public boolean isOpen() {
        return this.fileSystem.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return this.fileSystem.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return this.fileSystem.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return this.fileSystem.getRootDirectories();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return this.fileSystem.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return this.fileSystem.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(final String first, final String... more) {
        return this.fileSystem.getPath(first, more);
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return this.fileSystem.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return this.fileSystem.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return this.fileSystem.newWatchService();
    }
}
