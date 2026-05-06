package com.sshdeploy.deploy.pipeline;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.security.CredentialStore;

public final class CredentialResolver {
    public RemoteCredentials resolve(ServerProfile serverProfile, CredentialStore credentialStore) {
        String secret = credentialStore.readPassword(serverProfile.getCredentialRef());
        if (serverProfile.getAuthType() == AuthType.PASSWORD) {
            return new RemoteCredentials(secret, null, null);
        }

        if (secret == null || secret.isBlank()) {
            return new RemoteCredentials(null, null, null);
        }

        String[] parts = secret.split("::", 2);
        String privateKeyPath = parts[0].trim();
        String passphrase = parts.length > 1 ? parts[1] : null;
        return new RemoteCredentials(null, privateKeyPath, passphrase);
    }
}
