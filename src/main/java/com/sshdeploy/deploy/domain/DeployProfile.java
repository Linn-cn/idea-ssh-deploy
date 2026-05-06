package com.sshdeploy.deploy.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeployProfile {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    private String serverRef = "";
    private String uploadConfigRef = "";
    private List<String> beforeCommandRefs = new ArrayList<>();
    private List<String> afterCommandRefs = new ArrayList<>();
    private String terminalCommand = "";
    private BuildToolType buildToolType = BuildToolType.NONE;
    private String buildCommand = "";
    private boolean beforeCommandsEnabled = true;
    private boolean afterCommandsEnabled = true;
    private boolean terminalCommandEnabled;

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

    public String getServerRef() {
        return serverRef;
    }

    public void setServerRef(String serverRef) {
        this.serverRef = serverRef;
    }

    public String getUploadConfigRef() {
        return uploadConfigRef;
    }

    public void setUploadConfigRef(String uploadConfigRef) {
        this.uploadConfigRef = uploadConfigRef;
    }

    public List<String> getBeforeCommandRefs() {
        return beforeCommandRefs;
    }

    public void setBeforeCommandRefs(List<String> beforeCommandRefs) {
        this.beforeCommandRefs = beforeCommandRefs;
    }

    public List<String> getAfterCommandRefs() {
        return afterCommandRefs;
    }

    public void setAfterCommandRefs(List<String> afterCommandRefs) {
        this.afterCommandRefs = afterCommandRefs;
    }

    public String getTerminalCommand() {
        return terminalCommand;
    }

    public void setTerminalCommand(String terminalCommand) {
        this.terminalCommand = terminalCommand;
    }

    public BuildToolType getBuildToolType() {
        return buildToolType;
    }

    public void setBuildToolType(BuildToolType buildToolType) {
        this.buildToolType = buildToolType;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public boolean isBeforeCommandsEnabled() {
        return beforeCommandsEnabled;
    }

    public void setBeforeCommandsEnabled(boolean beforeCommandsEnabled) {
        this.beforeCommandsEnabled = beforeCommandsEnabled;
    }

    public boolean isAfterCommandsEnabled() {
        return afterCommandsEnabled;
    }

    public void setAfterCommandsEnabled(boolean afterCommandsEnabled) {
        this.afterCommandsEnabled = afterCommandsEnabled;
    }

    public boolean isTerminalCommandEnabled() {
        return terminalCommandEnabled;
    }

    public void setTerminalCommandEnabled(boolean terminalCommandEnabled) {
        this.terminalCommandEnabled = terminalCommandEnabled;
    }
}
