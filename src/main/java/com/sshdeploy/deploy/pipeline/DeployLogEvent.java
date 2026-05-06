package com.sshdeploy.deploy.pipeline;

import java.time.Instant;

public final class DeployLogEvent {
    private final Instant timestamp;
    private final DeployStage stage;
    private final DeployLogLevel level;
    private final String message;

    public DeployLogEvent(DeployStage stage, DeployLogLevel level, String message) {
        this.timestamp = Instant.now();
        this.stage = stage;
        this.level = level;
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public DeployStage getStage() {
        return stage;
    }

    public DeployLogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
