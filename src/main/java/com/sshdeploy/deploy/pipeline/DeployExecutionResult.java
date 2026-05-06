package com.sshdeploy.deploy.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeployExecutionResult {
    private final boolean success;
    private final List<DeployLogEvent> logs;

    public DeployExecutionResult(boolean success, List<DeployLogEvent> logs) {
        this.success = success;
        this.logs = new ArrayList<>(logs);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<DeployLogEvent> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
