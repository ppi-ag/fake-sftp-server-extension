package de.ppi.fakesftpserver.outside;

import de.ppi.fakesftpserver.extension.FakeSftpServerExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TestForPublicAccess {

    public static final int RANDOM_PORT = 4344;

    @RegisterExtension
    private final FakeSftpServerExtension extension = new FakeSftpServerExtension();

    @Test
    void testThatMethodsAreAccessible() {
        assertDoesNotThrow(() -> this.extension.addUser("test", "test")
            .setManualPort(RANDOM_PORT));

        assertDoesNotThrow(this.extension::getPort);
        assertDoesNotThrow(() -> this.extension.existsFile("gfdsg"));
        assertDoesNotThrow(() ->
            this.extension.putFile("test.txt", IOUtils.toInputStream("test", StandardCharsets.UTF_8)));
    }
}
