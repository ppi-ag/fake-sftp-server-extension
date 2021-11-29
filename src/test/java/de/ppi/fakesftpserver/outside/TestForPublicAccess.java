package de.ppi.fakesftpserver.outside;

import de.ppi.fakesftpserver.extension.FakeSftpServerExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class TestForPublicAccess {

    @RegisterExtension
    private FakeSftpServerExtension extension = new FakeSftpServerExtension();

    @Test
    void testThatMethodsAreAccessible() throws IOException {
        this.extension.addUser("test", "test")
            .setManualPort(4344);

        this.extension.getPort();
        this.extension.existsFile("gfdsg");
        this.extension.putFile("test.txt", IOUtils.toInputStream("test", StandardCharsets.UTF_8));
    }
}
