package de.ppi.fakesftpserver.extension;

import lombok.NonNull;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class provides a small logic to authenticate users. It can be used by SSHD-Auth.
 */
class InMemoryAuthenticator implements PasswordAuthenticator {

    private final Map<String, String> usernamesAndPasswords = new HashMap<>();

    @Override
    public boolean authenticate(final String user, final String pass, final ServerSession serverSession)
    throws PasswordChangeRequiredException, AsyncAuthException {
        return this.usernamesAndPasswords.isEmpty()
            || user != null && Objects.equals(this.usernamesAndPasswords.get(user), pass);
    }

    /**
     * Adds a new user to the in-memory user-database.
     *
     * @param user Username of the new user
     * @param pass Password of the new user
     */
    void putUser(@NonNull final String user, @NonNull final String pass) {
        this.usernamesAndPasswords.put(user, pass);
    }

}
