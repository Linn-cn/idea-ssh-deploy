package com.sshdeploy.deploy.remote;

public final class RemoteCredentials {
    private final String password;
    private final String privateKeyPath;
    private final String privateKeyPassphrase;

    public RemoteCredentials(String password, String privateKeyPath, String privateKeyPassphrase) {
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }
}
