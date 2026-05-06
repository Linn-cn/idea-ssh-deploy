package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServerDeployRunConfiguration extends RunConfigurationBase<Element> {
    private String serverId = "";
    private String uploadMode = "DIRECT_FILE";
    private String uploadFilePath = "";
    private String uploadDirectoryPath = "";
    private String uploadFileRegex = "^(?!.*(?:-sources|\\.original)\\.jar$).+\\.jar$";
    private String preDeployCommands = "";
    private String remoteUploadDir = "";
    private String postDeployCommands = "";
    private String terminalCommandRef = "";
    private String terminalCommand = "";

    protected ServerDeployRunConfiguration(@NotNull Project project,
                                           @NotNull ConfigurationFactory factory,
                                           String name) {
        super(project, factory, name);
    }

    @Override
    public @NotNull ServerDeploySettingsEditor getConfigurationEditor() {
        return new ServerDeploySettingsEditor(getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (serverId == null || serverId.isBlank()) {
            throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.server.required"));
        }
        DeployPluginStateService service = DeployPluginStateService.getInstance();
        if (service.findServerById(serverId).isEmpty()) {
            throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.server.notfound"));
        }
        if ("DIRECT_FILE".equals(uploadMode) && (uploadFilePath == null || uploadFilePath.isBlank())) {
            throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.uploadFile.required"));
        }
        if ("DIR_REGEX".equals(uploadMode)) {
            if (uploadDirectoryPath == null || uploadDirectoryPath.isBlank()) {
                throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.uploadDir.required"));
            }
            if (uploadFileRegex == null || uploadFileRegex.isBlank()) {
                throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.uploadRegex.required"));
            }
        }
        if (remoteUploadDir == null || remoteUploadDir.isBlank()) {
            throw new RuntimeConfigurationError(MyMessageBundle.message("runconfig.error.remoteDir.required"));
        }
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor,
                                              @NotNull ExecutionEnvironment environment) {
        return new ServerDeployRunProfileState(getProject(), this);
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        serverId = element.getAttributeValue("serverId", "");
        uploadMode = element.getAttributeValue("uploadMode", "DIRECT_FILE");
        uploadFilePath = element.getAttributeValue("uploadFilePath", "");
        uploadDirectoryPath = element.getAttributeValue("uploadDirectoryPath", "");
        uploadFileRegex = element.getAttributeValue("uploadFileRegex", "^(?!.*(?:-sources|\\.original)\\.jar$).+\\.jar$");
        preDeployCommands = element.getAttributeValue("preDeployCommands", "");
        remoteUploadDir = element.getAttributeValue("remoteUploadDir", "");
        postDeployCommands = element.getAttributeValue("postDeployCommands", "");
        terminalCommandRef = element.getAttributeValue("terminalCommandRef", "");
        terminalCommand = element.getAttributeValue("terminalCommand", "");
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        element.setAttribute("serverId", safe(serverId));
        element.setAttribute("uploadMode", safe(uploadMode));
        element.setAttribute("uploadFilePath", safe(uploadFilePath));
        element.setAttribute("uploadDirectoryPath", safe(uploadDirectoryPath));
        element.setAttribute("uploadFileRegex", safe(uploadFileRegex));
        element.setAttribute("preDeployCommands", safe(preDeployCommands));
        element.setAttribute("remoteUploadDir", safe(remoteUploadDir));
        element.setAttribute("postDeployCommands", safe(postDeployCommands));
        element.setAttribute("terminalCommandRef", safe(terminalCommandRef));
        element.setAttribute("terminalCommand", safe(terminalCommand));
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getUploadMode() {
        return uploadMode;
    }

    public void setUploadMode(String uploadMode) {
        this.uploadMode = uploadMode;
    }

    public String getUploadFilePath() {
        return uploadFilePath;
    }

    public void setUploadFilePath(String uploadFilePath) {
        this.uploadFilePath = uploadFilePath;
    }

    public String getUploadDirectoryPath() {
        return uploadDirectoryPath;
    }

    public void setUploadDirectoryPath(String uploadDirectoryPath) {
        this.uploadDirectoryPath = uploadDirectoryPath;
    }

    public String getUploadFileRegex() {
        return uploadFileRegex;
    }

    public void setUploadFileRegex(String uploadFileRegex) {
        this.uploadFileRegex = uploadFileRegex;
    }

    public String getPreDeployCommands() {
        return preDeployCommands;
    }

    public void setPreDeployCommands(String preDeployCommands) {
        this.preDeployCommands = preDeployCommands;
    }

    public String getRemoteUploadDir() {
        return remoteUploadDir;
    }

    public void setRemoteUploadDir(String remoteUploadDir) {
        this.remoteUploadDir = remoteUploadDir;
    }

    public String getPostDeployCommands() {
        return postDeployCommands;
    }

    public void setPostDeployCommands(String postDeployCommands) {
        this.postDeployCommands = postDeployCommands;
    }

    public String getTerminalCommandRef() {
        return terminalCommandRef;
    }

    public void setTerminalCommandRef(String terminalCommandRef) {
        this.terminalCommandRef = terminalCommandRef;
    }

    public String getTerminalCommand() {
        return terminalCommand;
    }

    public void setTerminalCommand(String terminalCommand) {
        this.terminalCommand = terminalCommand;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
