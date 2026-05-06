package com.sshdeploy.deploy.remote;

import com.sshdeploy.deploy.domain.ServerProfile;

public final class RemoteConnectRequest {
    private final ServerProfile target;
    private final RemoteCredentials targetCredentials;

    public RemoteConnectRequest(ServerProfile target, RemoteCredentials targetCredentials) {
        this.target = target;
        this.targetCredentials = targetCredentials;
    }

    public ServerProfile getTarget() {
        return target;
    }

    public RemoteCredentials getTargetCredentials() {
        return targetCredentials;
    }
}
