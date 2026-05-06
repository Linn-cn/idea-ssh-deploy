package com.sshdeploy.deploy.security;

public interface CredentialStore {
    void savePassword(String credentialKey, String username, String password);

    String readPassword(String credentialKey);

    void delete(String credentialKey);
}
