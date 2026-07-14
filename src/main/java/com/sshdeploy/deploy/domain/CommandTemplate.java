package com.sshdeploy.deploy.domain;

import java.util.UUID;

public class CommandTemplate {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    private String content = "";
    private int timeoutSeconds = 60;
    private boolean failFast = true;
    private CommandExecutionType executionType = CommandExecutionType.GENERAL;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public CommandExecutionType getExecutionType() {
        return CommandExecutionType.normalize(executionType);
    }

    public void setExecutionType(CommandExecutionType executionType) {
        this.executionType = CommandExecutionType.normalize(executionType);
    }
}
