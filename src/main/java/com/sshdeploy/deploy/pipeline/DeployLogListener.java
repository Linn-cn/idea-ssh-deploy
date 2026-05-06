package com.sshdeploy.deploy.pipeline;

@FunctionalInterface
public interface DeployLogListener {
    void onLog(DeployLogEvent event);
}
