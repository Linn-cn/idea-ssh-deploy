package com.sshdeploy.deploy.pipeline;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.security.CredentialStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CredentialResolverTest {
    @Test
    public void resolvePassword_shouldMapSecretToPassword() {
        ServerProfile serverProfile = new ServerProfile();
        serverProfile.setAuthType(AuthType.PASSWORD);
        serverProfile.setCredentialRef("cred-1");

        CredentialResolver resolver = new CredentialResolver();
        RemoteCredentials credentials = resolver.resolve(serverProfile, new FixedCredentialStore("pwd123"));

        assertEquals("pwd123", credentials.getPassword());
        assertNull(credentials.getPrivateKeyPath());
    }

    @Test
    public void resolvePrivateKey_shouldSplitPathAndPassphrase() {
        ServerProfile serverProfile = new ServerProfile();
        serverProfile.setAuthType(AuthType.PRIVATE_KEY);
        serverProfile.setCredentialRef("cred-2");

        CredentialResolver resolver = new CredentialResolver();
        RemoteCredentials credentials = resolver.resolve(serverProfile, new FixedCredentialStore("D:/keys/id_rsa::abc"));

        assertEquals("D:/keys/id_rsa", credentials.getPrivateKeyPath());
        assertEquals("abc", credentials.getPrivateKeyPassphrase());
    }

    private static final class FixedCredentialStore implements CredentialStore {
        private final String value;

        private FixedCredentialStore(String value) {
            this.value = value;
        }

        @Override
        public void savePassword(String credentialKey, String username, String password) {
        }

        @Override
        public String readPassword(String credentialKey) {
            return value;
        }

        @Override
        public void delete(String credentialKey) {
        }
    }
}
