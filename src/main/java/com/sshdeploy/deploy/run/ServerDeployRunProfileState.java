package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.pipeline.CredentialResolver;
import com.sshdeploy.deploy.remote.DefaultRemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteCommandResult;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.remote.RemoteShellCommand;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.console.DeployConsoleService;
import com.sshdeploy.deploy.ui.ssh.terminal.JschTtyConnector;
import com.sshdeploy.deploy.ui.ssh.terminal.SshTerminalOpener;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerDeployRunProfileState implements RunProfileState {
    private static final String PLACEHOLDER_FILE_NAME = "${fileName}";
    private final Project project;
    private final ServerDeployRunConfiguration configuration;

    public ServerDeployRunProfileState(Project project, ServerDeployRunConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, ProgramRunner<?> runner) throws ExecutionException {
        ConsoleView console = new ConsoleViewImpl(project, true);
        DeployProcessHandler processHandler = new DeployProcessHandler();
        console.attachToProcess(processHandler);
        ProcessTerminatedListener.attach(processHandler);
        processHandler.startNotify();

        ApplicationManager.getApplication().executeOnPooledThread(() -> runDeployment(processHandler));
        return new DefaultExecutionResult(console, processHandler);
    }

    private void runDeployment(DeployProcessHandler processHandler) {
        DeployConsoleService consoleService = project.getService(DeployConsoleService.class);
        try {
            DeployPluginStateService stateService = DeployPluginStateService.getInstance();
            ServerProfile target = resolveTarget(stateService);
            File uploadSource = resolveUploadSource(processHandler, consoleService);

            PasswordSafeCredentialStore credentialStore = new PasswordSafeCredentialStore();
            RemoteCredentials credentials = new CredentialResolver().resolve(target, credentialStore);

            List<String> preCommands = parseCommands(configuration.getPreDeployCommands());
            List<String> postCommands = parseCommands(configuration.getPostDeployCommands());

            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.connecting", target.getHost()));
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.deploy.start"));

            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.step.checkRemoteUploadDir"));
            CompletableFuture<Void> checkFuture = CompletableFuture.runAsync(() -> {
                try (RemoteClient checkClient = new DefaultRemoteClientFactory().create()) {
                    checkClient.connect(new RemoteConnectRequest(target, credentials));
                    if (checkClient.execute("test -d '" + configuration.getRemoteUploadDir().replace("'", "'\"'\"'") + "'", 15).getExitCode() != 0) {
                        throw new IllegalStateException(MyMessageBundle.message("runconfig.error.remoteUploadDir.inaccessible"));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            try {
                checkFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                checkFuture.cancel(true);
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new IllegalStateException(
                        MyMessageBundle.message("runconfig.error.remoteUploadDir.checkFailed", cause.getMessage()), cause);
            }
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.step.checkRemoteUploadDirOk"));
            if (processHandler.isCancelled()) {
                throw new CancellationException(MyMessageBundle.message("runconfig.error.userCancelled"));
            }

            try (RemoteClient remoteClient = new DefaultRemoteClientFactory().create()) {
                remoteClient.connect(new RemoteConnectRequest(target, credentials));
                executeRemoteCommands(processHandler, consoleService, remoteClient, preCommands,
                        MyMessageBundle.message("runconfig.log.stage.beforeUpload"), uploadSource.getName());

                log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.step.startUploadFile"));
                String remotePath = configuration.getRemoteUploadDir();
                if (!remotePath.endsWith("/")) {
                    remotePath += "/";
                }
                remotePath += uploadSource.getName();
                final int[] lastPercent = new int[]{-1};
                remoteClient.uploadFile(uploadSource, remotePath, (transferred, total) -> {
                    if (processHandler.isCancelled()) {
                        throw new RuntimeException(MyMessageBundle.message("runconfig.error.userCancelled"));
                    }
                    if (total <= 0) {
                        return;
                    }
                    int percent = (int) Math.min(100, (transferred * 100L) / total);
                    if (percent <= lastPercent[0]) {
                        return;
                    }
                    lastPercent[0] = percent;
                    logProgressOverwrite(processHandler, buildProgressBar(percent));
                });
            }
            logProgressFinish(processHandler);
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.step.uploadFinished"));
            if (processHandler.isCancelled()) {
                throw new CancellationException(MyMessageBundle.message("runconfig.error.userCancelled"));
            }

            try (RemoteClient remoteClient = new DefaultRemoteClientFactory().create()) {
                remoteClient.connect(new RemoteConnectRequest(target, credentials));
                executeRemoteCommands(processHandler, consoleService, remoteClient, postCommands,
                        MyMessageBundle.message("runconfig.log.stage.afterUpload"), uploadSource.getName());
            }

            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.openTerminal"));
            String terminalCommand = resolveTerminalCommand(configuration.getTerminalCommandRef(), configuration.getTerminalCommand());
            final JschTtyConnector[] connectorHolder = new JschTtyConnector[1];
            ApplicationManager.getApplication().invokeAndWait(() ->
                    connectorHolder[0] = new SshTerminalOpener().open(project, target, credentials)
            );
            if (terminalCommand != null && !terminalCommand.isBlank()) {
                try {
                    JschTtyConnector connector = connectorHolder[0];
                    if (connector == null) {
                        throw new IllegalStateException(MyMessageBundle.message("runconfig.error.terminal.connectorFailed"));
                    }
                    connector.awaitInitiated(15_000);
                    Thread.sleep(250L);
                    String commandToRun = terminalCommand.trim();
                    connector.write(commandToRun + "\n");
                    log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.terminal.command.input", commandToRun));
                } catch (Exception ex) {
                    throw new IllegalStateException(
                            MyMessageBundle.message("runconfig.error.terminal.commandFailed", ex.getMessage()), ex);
                }
            }
            if (!processHandler.isCancelled()) {
                notifyResult(true, target.getName());
                processHandler.finish(0);
            } else {
                log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.cancelled"));
                processHandler.finish(1);
            }
        } catch (CancellationException ex) {
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.cancelled"));
            notifyResult(false, MyMessageBundle.message("runconfig.notify.cancelledUser"));
            processHandler.finish(1);
        } catch (Exception ex) {
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.notify.failed", ex.getMessage()));
            notifyResult(false, ex.getMessage());
            processHandler.finish(1);
        }
    }

    private ServerProfile resolveTarget(DeployPluginStateService stateService) {
        String serverId = configuration.getServerId();
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalStateException(MyMessageBundle.message("runconfig.error.server.required"));
        }
        return stateService.findServerById(serverId)
                .orElseThrow(() -> new IllegalStateException(MyMessageBundle.message("runconfig.error.server.notfound")));
    }

    private File resolveUploadSource(DeployProcessHandler processHandler, DeployConsoleService consoleService) {
        String mode = configuration.getUploadMode();
        if ("DIRECT_FILE".equals(mode)) {
            File file = new File(configuration.getUploadFilePath());
            if (!file.exists()) {
                throw new IllegalStateException(MyMessageBundle.message("runconfig.error.localPath.notfound", file.getAbsolutePath()));
            }
            return file;
        }
        if ("DIR_REGEX".equals(mode)) {
            File dir = new File(configuration.getUploadDirectoryPath());
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalStateException(MyMessageBundle.message("runconfig.error.localPath.notfound", dir.getAbsolutePath()));
            }
            java.util.regex.Pattern pattern;
            try {
                pattern = java.util.regex.Pattern.compile(configuration.getUploadFileRegex());
            } catch (Exception ex) {
                throw new IllegalStateException(MyMessageBundle.message("runconfig.error.uploadRegex.invalid"));
            }
            File matched = latestMatch(dir, pattern);
            if (matched == null) {
                throw new IllegalStateException(MyMessageBundle.message("runconfig.error.artifact.notfound", dir.getAbsolutePath()));
            }
            log(processHandler, consoleService, MyMessageBundle.message("runconfig.log.build.artifact", matched.getAbsolutePath()));
            return matched;
        }
        throw new IllegalStateException(MyMessageBundle.message("runconfig.error.uploadFile.required"));
    }

    private static File latestMatch(File dir, java.util.regex.Pattern pattern) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles((d, n) -> pattern.matcher(n).matches());
        if (files == null || files.length == 0) {
            return null;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files[0];
    }

    private static List<String> parseCommands(String text) {
        List<String> commands = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return commands;
        }
        for (String line : text.split("\\r?\\n")) {
            String command = line.trim();
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        return commands;
    }

    private String resolveTerminalCommand(String commandRef, String manualCommand) {
        if (manualCommand != null && !manualCommand.isBlank()) {
            return manualCommand.trim();
        }
        if (commandRef == null || commandRef.isBlank()) {
            return "";
        }
        return DeployPluginStateService.getInstance()
                .findCommandById(commandRef)
                .map(command -> command.getContent() == null ? "" : command.getContent().trim())
                .orElse("");
    }

    private void executeRemoteCommands(DeployProcessHandler processHandler,
                                       DeployConsoleService consoleService,
                                       RemoteClient remoteClient,
                                       List<String> commands,
                                       String stageName,
                                       String artifactName) throws Exception {
        for (String cmd : commands) {
            if (processHandler.isCancelled()) {
                throw new CancellationException(MyMessageBundle.message("runconfig.error.userCancelled"));
            }
            String resolved = replaceCommandPlaceholders(cmd, artifactName);
            String toRun = RemoteShellCommand.withRemoteWorkingDirectory(configuration.getRemoteUploadDir(), resolved);
            log(processHandler, consoleService,
                    MyMessageBundle.message("runconfig.log.executingRemoteCommand", stageName, toRun));
            RemoteCommandResult result = remoteClient.executeStreaming(
                    toRun,
                    300,
                    chunk -> logStreamChunk(processHandler, consoleService, chunk),
                    chunk -> logStreamChunk(processHandler, consoleService, chunk)
            );
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                        MyMessageBundle.message("runconfig.error.commandRemoteFailed", result.getExitCode(), toRun));
            }
        }
    }

    private static String replaceCommandPlaceholders(String command, String artifactName) {
        if (command == null || command.isBlank()) {
            return "";
        }
        String safeArtifact = artifactName == null ? "" : artifactName.trim();
        return command.replace(PLACEHOLDER_FILE_NAME, safeArtifact);
    }

    private static void logStreamChunk(DeployProcessHandler handler,
                                       DeployConsoleService consoleService,
                                       String chunk) {
        if (chunk == null || chunk.isEmpty() || handler.isProcessTerminated()) {
            return;
        }
        String normalized = chunk.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (String line : lines) {
            if (!line.isBlank()) {
                log(handler, consoleService, line);
            }
        }
    }

    private void notifyResult(boolean success, String value) {
        String title = MyMessageBundle.message("runconfig.name");
        String content = success
                ? MyMessageBundle.message("runconfig.notify.success", value)
                : MyMessageBundle.message("runconfig.notify.failed", value);
        NotificationType type = success ? NotificationType.INFORMATION : NotificationType.ERROR;
        NotificationGroupManager.getInstance()
                .getNotificationGroup("SSH Deploy Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }

    private static void log(ProcessHandler handler,
                            DeployConsoleService consoleService,
                            String message) {
        if (handler.isProcessTerminated()) {
            return;
        }
        handler.notifyTextAvailable(message + System.lineSeparator(), ProcessOutputType.STDOUT);
        consoleService.appendLine(message);
    }

    private static void logProgressOverwrite(ProcessHandler handler, String message) {
        if (handler.isProcessTerminated()) {
            return;
        }
        handler.notifyTextAvailable("\r" + message, ProcessOutputType.STDOUT);
    }

    private static void logProgressFinish(ProcessHandler handler) {
        if (handler.isProcessTerminated()) {
            return;
        }
        handler.notifyTextAvailable(System.lineSeparator(), ProcessOutputType.STDOUT);
    }

    private static String buildProgressBar(int percent) {
        int width = 20;
        int filled = Math.max(0, Math.min(width, (percent * width) / 100));
        String bar = "=".repeat(filled) + "-".repeat(width - filled);
        return "上传进度 [" + bar + "] " + percent + "%";
    }

    private static final class DeployProcessHandler extends ProcessHandler {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean completed = new AtomicBoolean(false);

        private boolean isCancelled() {
            return cancelled.get();
        }

        private void finish(int code) {
            if (completed.compareAndSet(false, true)) {
                notifyProcessTerminated(code);
            }
        }

        @Override
        protected void destroyProcessImpl() {
            cancelled.set(true);
            finish(1);
        }

        @Override
        protected void detachProcessImpl() {
            notifyProcessDetached();
        }

        @Override
        public boolean detachIsDefault() {
            return false;
        }

        @Override
        public @Nullable java.io.OutputStream getProcessInput() {
            return null;
        }
    }
}
