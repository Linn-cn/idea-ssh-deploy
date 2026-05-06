package com.sshdeploy.deploy.security;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;

@Service(Service.Level.APP)
public final class PasswordSafeCredentialStore implements CredentialStore {
    private static final String SERVICE_NAME = "com.sshdeploy.serverdeploy.credentials";

    @Override
    public void savePassword(String credentialKey, String username, String password) {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME + ":" + credentialKey);
        Credentials credentials = new Credentials(username, password);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    @Override
    public String readPassword(String credentialKey) {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME + ":" + credentialKey);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials == null ? null : credentials.getPasswordAsString();
    }

    @Override
    public void delete(String credentialKey) {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME + ":" + credentialKey);
        PasswordSafe.getInstance().set(attributes, null);
    }
}
