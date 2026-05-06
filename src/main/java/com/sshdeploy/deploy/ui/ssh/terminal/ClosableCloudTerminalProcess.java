package com.sshdeploy.deploy.ui.ssh.terminal;

import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess;

public final class ClosableCloudTerminalProcess extends CloudTerminalProcess {
    private final JschTtyConnector connector;

    public ClosableCloudTerminalProcess(JschTtyConnector connector) {
        super(connector.getOutputStream(), connector.getInputStream());
        this.connector = connector;
    }

    @Override
    public void destroy() {
        super.destroy();
        connector.close();
    }
}

