package com.sshdeploy.deploy.remote;

public final class DefaultRemoteClientFactory implements RemoteClientFactory {
    @Override
    public RemoteClient create() {
        return new JschRemoteClient();
    }
}
