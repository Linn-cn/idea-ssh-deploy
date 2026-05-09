package com.sshdeploy.deploy.pipeline;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.BuildToolType;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;
import com.sshdeploy.deploy.remote.DefaultRemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteCommandResult;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.remote.RemoteShellCommand;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.util.execution.ParametersListUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DeployOrchestrator {
    private final DeployPluginStateService stateService;
    private final CredentialStore credentialStore;
    private final CredentialResolver credentialResolver;
    private final RemoteClientFactory remoteClientFactory;

    public DeployOrchestrator(DeployPluginStateService stateService, CredentialStore credentialStore) {
        this(stateService, credentialStore, new CredentialResolver(), new DefaultRemoteClientFactory());
    }

    DeployOrchestrator(DeployPluginStateService stateService,
                       CredentialStore credentialStore,
                       CredentialResolver credentialResolver,
                       RemoteClientFactory remoteClientFactory) {
        this.stateService = stateService;
        this.credentialStore = credentialStore;
        this.credentialResolver = credentialResolver;
        this.remoteClientFactory = remoteClientFactory;
    }

    public DeployExecutionResult execute(String deployProfileId, String projectBasePath) {
        return execute(deployProfileId, projectBasePath, DeployExecutionOptions.defaults(), null);
    }

    public DeployExecutionResult execute(String deployProfileId,
                                         String projectBasePath,
                                         DeployExecutionOptions options) {
        return execute(deployProfileId, projectBasePath, options, null);
    }

    public DeployExecutionResult execute(String deployProfileId,
                                         String projectBasePath,
                                         DeployExecutionOptions options,
                                         DeployLogListener listener) {
        List<DeployLogEvent> logs = new ArrayList<>();
        try {
            log(logs, listener, DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("pipeline.log.starting"));

            DeployProfile profile = requireProfile(deployProfileId);
            ServerProfile target = requireServer(profile.getServerRef());
            UploadConfig upload = requireUpload(profile.getUploadConfigRef());
            String remoteWorkingDir = upload.getRemotePath();

            RemoteCredentials targetCreds = credentialResolver.resolve(target, credentialStore);

            if (!options.isSkipBuild()) {
                runBuildIfNeeded(profile, projectBasePath, options, logs, listener);
            } else {
                log(logs, listener, DeployStage.BUILD, DeployLogLevel.INFO, "Build skipped by options.");
            }

            try (RemoteClient remoteClient = remoteClientFactory.create()) {
                RemoteConnectRequest connectRequest = new RemoteConnectRequest(target, targetCreds);
                if (options.isTestConnectionOnly()) {
                    remoteClient.testConnection(connectRequest);
                } else {
                    remoteClient.connect(connectRequest);
                }
                log(logs, listener, DeployStage.PREPARE, DeployLogLevel.INFO, "Connected to remote host.");

                if (options.isTestConnectionOnly()) {
                    log(logs, listener, DeployStage.PREPARE, DeployLogLevel.INFO, "Connection test succeeded.");
                    return new DeployExecutionResult(true, logs);
                }

                if (profile.isBeforeCommandsEnabled() && !options.isSkipBeforeCommands()) {
                    runCommands(profile.getBeforeCommandRefs(), DeployStage.BEFORE_COMMANDS, remoteWorkingDir,
                            remoteClient, options, logs, listener);
                } else {
                    log(logs, listener, DeployStage.BEFORE_COMMANDS, DeployLogLevel.INFO,
                            MyMessageBundle.message("pipeline.log.beforeUploadCommandsSkipped"));
                }

                if (!options.isSkipUpload()) {
                    runUpload(upload, remoteClient, options, logs, listener);
                } else {
                    log(logs, listener, DeployStage.UPLOAD, DeployLogLevel.INFO, "Upload skipped by options.");
                }

                if (profile.isAfterCommandsEnabled() && !options.isSkipAfterCommands()) {
                    runCommands(profile.getAfterCommandRefs(), DeployStage.AFTER_COMMANDS, remoteWorkingDir,
                            remoteClient, options, logs, listener);
                } else {
                    log(logs, listener, DeployStage.AFTER_COMMANDS, DeployLogLevel.INFO,
                            MyMessageBundle.message("pipeline.log.afterUploadCommandsSkipped"));
                }

                if (profile.isTerminalCommandEnabled() && !options.isSkipTerminalCommand()) {
                    runTerminalCommand(profile, remoteWorkingDir, remoteClient, options, logs, listener);
                } else {
                    log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.INFO, "Terminal command skipped.");
                }
            }

            log(logs, listener, DeployStage.AFTER_COMMANDS, DeployLogLevel.INFO, MyMessageBundle.message("pipeline.log.finished"));
            return new DeployExecutionResult(true, logs);
        } catch (Exception ex) {
            log(logs, listener, DeployStage.PREPARE, DeployLogLevel.ERROR,
                    MyMessageBundle.message("pipeline.log.failed", ex.getMessage()));
            return new DeployExecutionResult(false, logs);
        }
    }

    private void runBuildIfNeeded(DeployProfile profile,
                                  String projectBasePath,
                                  DeployExecutionOptions options,
                                  List<DeployLogEvent> logs,
                                  DeployLogListener listener) throws Exception {
        if (profile.getBuildToolType() == BuildToolType.NONE) {
            return;
        }

        String command = profile.getBuildCommand();
        if (command == null || command.isBlank()) {
            if (profile.getBuildToolType() == BuildToolType.MAVEN) {
                command = "mvn clean package -DskipTests";
            } else {
                command = "gradlew.bat build";
            }
        }

        log(logs, listener, DeployStage.BUILD, DeployLogLevel.INFO, "Running build: " + command);
        if (options.isDryRun()) {
            log(logs, listener, DeployStage.BUILD, DeployLogLevel.INFO, "Dry-run mode, build command not executed.");
            return;
        }
        GeneralCommandLine commandLine = new GeneralCommandLine(ParametersListUtil.parse(command))
                .withCharset(StandardCharsets.UTF_8)
                .withWorkDirectory(projectBasePath);
        CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
        var output = handler.runProcess(10 * 60 * 1000);
        if (output.getExitCode() != 0) {
            throw new IllegalStateException("Build failed: " + output.getStderr());
        }
        if (!output.getStdout().isBlank()) {
            log(logs, listener, DeployStage.BUILD, DeployLogLevel.INFO, output.getStdout());
        }
    }

    private void runUpload(UploadConfig upload,
                           RemoteClient remoteClient,
                           DeployExecutionOptions options,
                           List<DeployLogEvent> logs,
                           DeployLogListener listener) throws Exception {
        File source = new File(upload.getLocalPath());
        if (!source.exists()) {
            throw new IllegalStateException("Upload source does not exist: " + upload.getLocalPath());
        }

        if (source.isDirectory() || upload.isDirectory()) {
            log(logs, listener, DeployStage.UPLOAD, DeployLogLevel.INFO, "Uploading directory: " + source.getAbsolutePath());
            if (options.isDryRun()) {
                log(logs, listener, DeployStage.UPLOAD, DeployLogLevel.INFO, "Dry-run mode, directory upload not executed.");
                return;
            }
            remoteClient.uploadDirectory(source, upload.getRemotePath(), upload.getFilters());
        } else {
            String remotePath = combineRemotePath(upload.getRemotePath(), source.getName());
            log(logs, listener, DeployStage.UPLOAD, DeployLogLevel.INFO, "Uploading file: " + source.getAbsolutePath());
            if (options.isDryRun()) {
                log(logs, listener, DeployStage.UPLOAD, DeployLogLevel.INFO, "Dry-run mode, file upload not executed.");
                return;
            }
            remoteClient.uploadFile(source, remotePath);
        }
    }

    private void runCommands(List<String> commandRefs,
                             DeployStage stage,
                             String remoteWorkingDir,
                             RemoteClient remoteClient,
                             DeployExecutionOptions options,
                             List<DeployLogEvent> logs,
                             DeployLogListener listener) throws Exception {
        for (String commandRef : commandRefs) {
            Optional<CommandTemplate> commandOptional = stateService.findCommandById(commandRef);
            if (commandOptional.isEmpty()) {
                log(logs, listener, stage, DeployLogLevel.WARN, "Command not found, skipped: " + commandRef);
                continue;
            }

            CommandTemplate command = commandOptional.get();
            log(logs, listener, stage, DeployLogLevel.INFO, "Executing command: " + command.getName());
            if (options.isDryRun()) {
                log(logs, listener, stage, DeployLogLevel.INFO, "Dry-run mode, command not executed: " + command.getContent());
                continue;
            }
            String remoteCmd = RemoteShellCommand.withRemoteWorkingDirectory(remoteWorkingDir, command.getContent());
            RemoteCommandResult result = remoteClient.execute(remoteCmd, command.getTimeoutSeconds());

            if (!result.getStdOut().isBlank()) {
                log(logs, listener, stage, DeployLogLevel.INFO, result.getStdOut());
            }
            if (!result.getStdErr().isBlank()) {
                log(logs, listener, stage, DeployLogLevel.WARN, result.getStdErr());
            }

            if (result.getExitCode() != 0 && command.isFailFast()) {
                throw new IllegalStateException("Command failed: " + command.getName() + ", exit code " + result.getExitCode());
            }
        }
    }

    private void runTerminalCommand(DeployProfile profile,
                                    String remoteWorkingDir,
                                    RemoteClient remoteClient,
                                    DeployExecutionOptions options,
                                    List<DeployLogEvent> logs,
                                    DeployLogListener listener) throws Exception {
        if (profile.getTerminalCommand() == null || profile.getTerminalCommand().isBlank()) {
            log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.WARN, "Terminal command is empty, skipped.");
            return;
        }

        log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.INFO, "Executing terminal command.");
        if (options.isDryRun()) {
            log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.INFO, "Dry-run mode, terminal command not executed.");
            return;
        }

        String remoteCmd = RemoteShellCommand.withRemoteWorkingDirectory(remoteWorkingDir, profile.getTerminalCommand());
        RemoteCommandResult result = remoteClient.execute(remoteCmd, 60);
        if (!result.getStdOut().isBlank()) {
            log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.INFO, result.getStdOut());
        }
        if (!result.getStdErr().isBlank()) {
            log(logs, listener, DeployStage.OPEN_TERMINAL, DeployLogLevel.WARN, result.getStdErr());
        }
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Terminal command failed, exit code " + result.getExitCode());
        }
    }

    private DeployProfile requireProfile(String deployProfileId) {
        return stateService.findDeployProfileById(deployProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Deploy profile not found: " + deployProfileId));
    }

    private ServerProfile requireServer(String serverId) {
        return stateService.findServerById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId));
    }

    private UploadConfig requireUpload(String uploadId) {
        return stateService.findUploadById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Upload config not found: " + uploadId));
    }

    private String combineRemotePath(String remoteDir, String fileName) {
        String normalized = remoteDir.endsWith("/") ? remoteDir : remoteDir + "/";
        return normalized + fileName;
    }

    private void log(List<DeployLogEvent> logs,
                     DeployLogListener listener,
                     DeployStage stage,
                     DeployLogLevel level,
                     String message) {
        DeployLogEvent event = new DeployLogEvent(stage, level, message);
        logs.add(event);
        if (listener != null) {
            listener.onLog(event);
        }
    }
}
