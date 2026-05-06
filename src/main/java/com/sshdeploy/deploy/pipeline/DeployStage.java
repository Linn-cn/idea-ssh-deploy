package com.sshdeploy.deploy.pipeline;

public enum DeployStage {
    PREPARE,
    BUILD,
    UPLOAD,
    BEFORE_COMMANDS,
    AFTER_COMMANDS,
    OPEN_TERMINAL
}
