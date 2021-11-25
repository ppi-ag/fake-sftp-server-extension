package de.ppi.fakesftpserver.extension;


import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static com.github.stefanbirkner.fishbowl.Fishbowl.exceptionThrownBy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* Wording according to the draft:
 * http://tools.ietf.org/html/draft-ietf-secsh-filexfer-13
 */
@SuppressWarnings("ClassWithTooManyMethods")
class FakeSftpServerExtensionTest {

    static final String DUMMY_PASSWORD = "dummy password";
    static final String DUMMY_USER = "dummy user";

    private static final byte[] DUMMY_CONTENT = new byte[]{1, 4, 2, 4, 2, 4};
    private static final int DUMMY_PORT = 46354;
    static final int TIMEOUT = 500;

    private static final int HIGHEST_PORT = 65535;
    private static final int LOWEST_PORT = 1024;


    @RegisterExtension
    private final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();


    @Test
    void a_file_that_is_written_to_the_SFTP_server_can_be_read() throws Exception {
        final Session session = AssertionHelperUtil.connectToServer(this.sftpServer);
        final ChannelSftp channel = AssertionHelperUtil.connectSftpChannel(session);
        channel.put(new ByteArrayInputStream("dummy content".getBytes(UTF_8)), "dummy_file.txt");
        final InputStream file = channel.get("dummy_file.txt");

        assertThat(IOUtils.toString(file, UTF_8)).isEqualTo("dummy content");

        channel.disconnect();
        session.disconnect();
    }

    @Test
    void multiple_connections_to_the_server_are_possible() {
        assertDoesNotThrow(() -> AssertionHelperUtil.connectAndDisconnect(this.sftpServer));
        assertDoesNotThrow(() -> AssertionHelperUtil.connectAndDisconnect(this.sftpServer));
    }

    @Test
    void a_client_can_connect_to_the_server_at_a_user_specified_port() throws Exception {
        final var port = 8394;
        try (var sftpServer = new FakeSftpServerExtension().setManualPort(port)) {
            sftpServer.beforeEach(null);
            assertDoesNotThrow(() -> AssertionHelperUtil.connectToServerAtPort(port));
        }
    }

    @Test
    void the_server_accepts_connections_with_password() throws Exception {
        final Session session = AssertionHelperUtil.createSessionWithCredentials(this.sftpServer, DUMMY_PASSWORD);
        assertDoesNotThrow(() -> session.connect(TIMEOUT));
    }

    @Test
    void the_server_rejects_connections_with_wrong_password() throws JSchException {
        this.sftpServer.addUser(DUMMY_USER, "correct password");
        final Session session = AssertionHelperUtil.createSessionWithCredentials(this.sftpServer, "wrong password");
        AssertionHelperUtil.assertAuthenticationFails(() -> session.connect(TIMEOUT));
    }

    @Test
    void the_last_password_is_effective_if_addUser_is_called_multiple_times() throws JSchException {
        this.sftpServer.addUser(DUMMY_USER, "first password").addUser(DUMMY_USER, "second password");
        final Session session = AssertionHelperUtil.createSessionWithCredentials(this.sftpServer, "second password");
        assertDoesNotThrow(() -> session.connect(TIMEOUT));
    }

    @Test
    void the_server_accepts_connections_with_correct_password() throws JSchException {
        this.sftpServer.addUser(DUMMY_USER, DUMMY_PASSWORD);
        final Session session = AssertionHelperUtil.createSessionWithCredentials(this.sftpServer, DUMMY_PASSWORD);
        assertDoesNotThrow(() -> session.connect(TIMEOUT));
    }

    @Test
    void that_is_put_to_root_directory_via_the_rule_can_be_read_from_server() throws Exception {
        this.sftpServer.putFile("/dummy_file.txt", IOUtils.toInputStream("dummy content with umlaut ü", UTF_8));
        final byte[] file = AssertionHelperUtil.downloadFile(this.sftpServer, "/dummy_file.txt");

        assertThat(new String(file, UTF_8))
            .isEqualTo("dummy content with umlaut ü");
    }

    @Test
    void that_is_put_to_directory_via_the_rule_can_be_read_from_server() throws Exception {
        this.sftpServer.putFile("/dummy_directory/dummy_file.txt", "dummy content with umlaut ü", UTF_8);
        final byte[] file = AssertionHelperUtil.downloadFile(this.sftpServer, "/dummy_directory/dummy_file.txt");

        assertThat(new String(file, UTF_8))
            .isEqualTo("dummy content with umlaut ü");
    }

    @Test
    void cannot_be_put_before_the_test_is_started() {
        final var sftpServer = new FakeSftpServerExtension();
        final Throwable exception = exceptionThrownBy(() ->
            sftpServer.putFile("/dummy_file.txt", "dummy content", UTF_8));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to upload file because test has not been started or is already finished.");
    }

    @Test
    void cannot_be_put_after_the_test_is_finished() throws Exception {
        this.sftpServer.afterEach(null);

        final Throwable exception = exceptionThrownBy(() ->
            this.sftpServer.putFile("/dummy_file.txt", "dummy content", UTF_8));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to upload file because test has not been started or is already finished.");
    }

    @Test
    void that_is_created_with_the_rule_can_be_read_by_a_client() throws Exception {
        this.sftpServer.createDirectory("/a/directory");
        AssertionHelperUtil.assertEmptyDirectory(this.sftpServer, "/a/directory");
    }

    @Test
    void cannot_be_created_before_the_test_is_started() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();
        final Throwable exception = exceptionThrownBy(() -> sftpServer.createDirectory("/a/directory"));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to create directory because test has not been started or is already finished.");
    }

    @Test
    void cannot_be_created_after_the_test_is_finished() throws Exception {
        this.sftpServer.afterEach(null);

        final Throwable exception = exceptionThrownBy(() -> this.sftpServer.createDirectory("/a/directory"));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to create directory because test has not been started or is already finished.");
    }

    @Test
    void that_are_created_with_the_rule_can_be_read_by_a_client() throws Exception {
        this.sftpServer.createDirectories("/a/directory", "/another/directory");

        AssertionHelperUtil.assertEmptyDirectory(this.sftpServer, "/a/directory");
        AssertionHelperUtil.assertEmptyDirectory(this.sftpServer, "/another/directory");
    }

    @Test
    void that_is_written_to_the_server_can_be_retrieved_with_the_rule() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer,
            "/dummy_directory/dummy_file.txt",
            "dummy content with umlaut ü".getBytes(UTF_8));

        final String fileContent = this.sftpServer.getFileContent("/dummy_directory/dummy_file.txt", UTF_8);

        assertThat(fileContent)
            .isEqualTo("dummy content with umlaut ü");
    }

    @Test
    void cannot_be_retrieved_before_the_test_is_started() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();
        final Throwable exception = exceptionThrownBy(() ->
            sftpServer.getFileContent("/dummy_directory/dummy_file.txt"));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to download file because test has not been started or is already finished.");
    }

    @Test
    void cannot_be_retrieved_after_the_test_is_finished() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer, "/dummy_directory/dummy_file.txt", "dummy content".getBytes(UTF_8));

        this.sftpServer.afterEach(null);

        final Throwable exception = exceptionThrownBy(() ->
            this.sftpServer.getFileContent("/dummy_directory/dummy_file.txt"));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to download file because test has not been started or is already finished.");
    }

    @Test
    void exists_returns_true_for_a_file_that_exists_on_the_server() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer, "/dummy_directory/dummy_file.bin", DUMMY_CONTENT);

        final boolean exists = this.sftpServer.existsFile("/dummy_directory/dummy_file.bin");
        assertThat(exists).isTrue();
    }

    @Test
    void exists_returns_false_for_a_file_that_does_not_exists_on_the_server() {
        assertThat(this.sftpServer.existsFile("/dummy_directory/dummy_file.bin")).isFalse();
    }

    @Test
    void exists_returns_false_for_a_directory_that_exists_on_the_server() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer, "/dummy_directory/dummy_file.bin", DUMMY_CONTENT);
        assertThat(this.sftpServer.existsFile("/dummy_directory")).isFalse();
    }

    @Test
    void existence_of_a_file_cannot_be_checked_before_the_test_is_started() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();
        final Throwable exception = exceptionThrownBy(() -> sftpServer.existsFile("/dummy_file.bin"));

        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to check existence of file because test has not been started or is already finished.");
    }

    @Test
    void existence_of_a_file_cannot_be_checked_after_the_test_is_finished() throws Exception {
        this.sftpServer.afterEach(null);

        final Throwable exception = exceptionThrownBy(() -> this.sftpServer.existsFile("/dummy_file.bin"));
        assertThat(exception)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to check existence of file because test has not been started or is already finished.");
    }

    @Test
    void after_a_successful_test_SFTP_server_is_shutdown() throws Exception {
        this.sftpServer.setManualPort(DUMMY_PORT).afterEach(null);
        AssertionHelperUtil.assertConnectionToSftpServerNotPossible(DUMMY_PORT);
    }

    @Test
    void after_a_test_first_SFTP_server_is_shutdown_when_port_was_changed_during_test() {
        this.sftpServer.setManualPort(DUMMY_PORT - 1);
        this.sftpServer.setManualPort(DUMMY_PORT);
        AssertionHelperUtil.assertConnectionToSftpServerNotPossible(DUMMY_PORT - 1);
    }

    @Test
    void by_default_two_rules_run_servers_at_different_ports() throws Exception {
        final FakeSftpServerExtension secondSftpServer = new FakeSftpServerExtension();
        secondSftpServer.beforeEach(null);

        assertThat(this.sftpServer.getPort()).isNotEqualTo(secondSftpServer.getPort());
    }

    @Test
    void the_port_can_be_changed_during_the_test() {
        this.sftpServer.setManualPort(DUMMY_PORT);
        assertDoesNotThrow(() -> AssertionHelperUtil.connectToServerAtPort(DUMMY_PORT));
    }

    @Test
    void testing_border_port_values() {
        final var invalidPorts = List.of(-1, 0, HIGHEST_PORT + 1);

        invalidPorts.forEach(port -> assertThatThrownBy(() -> this.sftpServer.setManualPort(port))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Port cannot be set to " + port + " because only ports between 1 and 65535 are valid."));
    }

    @Test
    void the_port_can_be_set_to_1() {
        // test must run as root to use a port <1024
        // this code should not run as root
        assertThatThrownBy(() -> this.sftpServer.setManualPort(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The SFTP server cannot be restarted.");
    }

    @Test
    void the_server_can_be_run_at_highest_port() {
        this.sftpServer.setManualPort(HIGHEST_PORT);
        assertDoesNotThrow(() -> AssertionHelperUtil.connectToServerAtPort(HIGHEST_PORT));
    }

    @Test
    void cannot_be_read_before_the_test() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();
        AssertionHelperUtil.assertPortCannotBeRead(sftpServer);
    }

    @Test
    void can_be_read_during_the_test() {
        assertThat(this.sftpServer.getPort())
            .isBetween(LOWEST_PORT, HIGHEST_PORT);
    }

    @Test
    void cannot_be_read_after_the_test() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();
        AssertionHelperUtil.assertPortCannotBeRead(sftpServer);
    }

    @Test
    void cant_read_port_before_the_test() {
        final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension().setManualPort(DUMMY_PORT);
        assertThrows(IllegalStateException.class, sftpServer::getPort);
    }

    @Test
    void cant_port_read_after_the_test() throws Exception {
        this.sftpServer.setManualPort(DUMMY_PORT).afterEach(null);
        assertThrows(IllegalStateException.class, this.sftpServer::getPort);
    }

    @Test
    void deletes_file_in_root_directory() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer, "/dummy_file.bin", DUMMY_CONTENT);
        this.sftpServer.deleteAllFilesAndDirectories();
        AssertionHelperUtil.assertFileDoesNotExist(this.sftpServer, "/dummy_file.bin");
    }

    @Test
    void deletes_file_in_directory() throws Exception {
        AssertionHelperUtil.uploadFile(this.sftpServer, "/dummy_directory/dummy_file.bin", DUMMY_CONTENT);
        this.sftpServer.deleteAllFilesAndDirectories();
        AssertionHelperUtil.assertFileDoesNotExist(this.sftpServer, "/dummy_directory/dummy_file.bin");
    }

    @Test
    void deletes_directory() throws Exception {
        this.sftpServer.createDirectory("/dummy_directory");
        this.sftpServer.deleteAllFilesAndDirectories();
        AssertionHelperUtil.assertDirectoryDoesNotExist(this.sftpServer);
    }

    @Test
    void works_on_an_empty_filesystem() {
        assertDoesNotThrow(this.sftpServer::deleteAllFilesAndDirectories);
    }
}
