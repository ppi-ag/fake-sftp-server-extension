package de.ppi.fakesftpserver.extension;

import com.jcraft.jsch.*;
import org.assertj.core.api.ThrowableAssert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.*;

/**
 * Helper-Class for assertions.
 */
final class AssertionHelperUtil {

    private static final JSch JSCH = new JSch();

    private AssertionHelperUtil() {
        throw new UnsupportedOperationException("Util-Class can't be instantiated");
    }

    static void assertAuthenticationFails(final ThrowableAssert.ThrowingCallable connectToServer) {
        assertThatThrownBy(connectToServer)
            .isInstanceOf(JSchException.class)
            .hasMessage("Auth fail");
    }

    static void assertEmptyDirectory(final FakeSftpServerExtension sftpServer,
                                     final String directory) throws JSchException, SftpException {
        final Session session = connectToServer(sftpServer);
        final ChannelSftp channel = connectSftpChannel(session);
        final Vector<?> entries = channel.ls(directory);
        assertThat(entries).hasSize(2); //these are the entries "." and ".."
        channel.disconnect();
        session.disconnect();
    }

    static void assertFileDoesNotExist(final FakeSftpServerExtension sftpServer, final String path) {
        final boolean exists = sftpServer.existsFile(path);
        assertThat(exists).isFalse();
    }

    static void assertConnectionToSftpServerNotPossible(final int port) {
        final Throwable throwable = catchThrowable(() -> connectToServerAtPort(port));

        assertThat(throwable)
            .withFailMessage("SFTP server is still running on port %d.", port)
            .hasCauseInstanceOf(ConnectException.class);
    }

    static void assertPortCannotBeRead(final FakeSftpServerExtension sftpServer) {
        assertThatThrownBy(sftpServer::getPort)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to call getPort() because test has not been started or is already finished.");
    }

    static Session createSessionWithCredentials(final FakeSftpServerExtension sftpServer,
                                                final String password) throws JSchException {
        return createSessionWithCredentials(password, sftpServer.getPort());
    }

    static void assertDirectoryDoesNotExist(final FakeSftpServerExtension sftpServer) throws JSchException {
        final Session session = connectToServer(sftpServer);
        final ChannelSftp channel = connectSftpChannel(session);
        try {
            assertThatThrownBy(() -> channel.ls("/dummy_directory"))
                .isInstanceOf(SftpException.class)
                .hasMessage("No such file or directory");
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }

    static Session connectToServer(final FakeSftpServerExtension sftpServer) throws JSchException {
        return connectToServerAtPort(sftpServer.getPort());
    }

    static Session connectToServerAtPort(final int port) throws JSchException {
        final Session session = createSessionWithCredentials(FakeSftpServerExtensionTest.DUMMY_PASSWORD, port);
        session.connect(FakeSftpServerExtensionTest.TIMEOUT);
        return session;
    }

    static ChannelSftp connectSftpChannel(final Session session) throws JSchException {
        final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        return channel;
    }

    static void connectAndDisconnect(final FakeSftpServerExtension sftpServer) throws JSchException {
        final Session session = connectToServer(sftpServer);
        final ChannelSftp channel = connectSftpChannel(session);
        channel.disconnect();
        session.disconnect();
    }

    private static Session createSessionWithCredentials(final String password, final int port) throws JSchException {
        final Session session = JSCH.getSession(FakeSftpServerExtensionTest.DUMMY_USER, "127.0.0.1", port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(password);
        return session;
    }

    static byte[] downloadFile(final FakeSftpServerExtension server, final String path) throws Exception {
        final Session session = connectToServer(server);
        final ChannelSftp channel = connectSftpChannel(session);
        try {
            final InputStream is = channel.get(path);
            return toByteArray(is);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }

    static void uploadFile(final FakeSftpServerExtension server,
                           final String pathAsString,
                           final byte[] content) throws Exception {
        final Session session = connectToServer(server);
        final ChannelSftp channel = connectSftpChannel(session);
        try {
            final Path path = Paths.get(pathAsString);
            if (!path.getParent().equals(path.getRoot())) {
                channel.mkdir(path.getParent().toString());
            }
            channel.put(new ByteArrayInputStream(content), pathAsString);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }
}
