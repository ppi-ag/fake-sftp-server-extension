package de.ppi.fakesftpserver.extension;


import lombok.RequiredArgsConstructor;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.SessionContext;

import java.nio.file.FileSystem;


/**
 * VirtualFileSystemFactory with a changeable underlying filesystem.
 */
@RequiredArgsConstructor
class CustomFileSystemFactory extends VirtualFileSystemFactory {

    private final FileSystem fileSystem;

    @Override
    public FileSystem createFileSystem(final SessionContext session) {
        return this.fileSystem;
    }
}
