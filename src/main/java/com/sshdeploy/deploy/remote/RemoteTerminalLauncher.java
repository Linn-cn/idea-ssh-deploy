package com.sshdeploy.deploy.remote;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class RemoteTerminalLauncher {

    public void openSshTerminal(Project project, ServerProfile target) {
        String sshCommand = buildSshCommand(target, null, true);
        executeInTerminal(project, target.getName(), sshCommand);
    }

    public void openDeployTerminal(Project project,
                                   ServerProfile target,
                                   RemoteCredentials credentials,
                                   File uploadSource,
                                   String remoteDir,
                                   List<String> preCommands,
                                   List<String> postCommands) {
        String scpCommand = buildScpCommand(target, credentials, uploadSource, remoteDir);

        List<String> commands = new ArrayList<>();
        commands.add(buildSshCommand(target, "test -d " + shellQuote(remoteDir), false));
        if (!preCommands.isEmpty()) {
            commands.add(buildSshCommand(target, joinCommands(preCommands), false));
        }
        commands.add(scpCommand);
        if (!postCommands.isEmpty()) {
            commands.add(buildSshCommand(target, joinCommands(postCommands) + "; exec $SHELL -l", true));
        } else {
            commands.add(buildSshCommand(target, null, true));
        }
        executeInTerminal(project, target.getName(), String.join(" && ", commands));
    }

    private void executeInTerminal(Project project, String targetName, String command) {
        TerminalToolWindowManager terminalManager = TerminalToolWindowManager.getInstance(project);
        try {
            TerminalWidget widget = terminalManager.createShellWidget(
                    project.getBasePath(),
                    MyMessageBundle.message("terminal.tab.ssh", targetName),
                    true,
                    true
            );
            widget.sendCommandToExecute(command);
        } catch (Exception e) {
            throw new RuntimeException(MyMessageBundle.message("terminal.error.open", e.getMessage()), e);
        }
    }

    private String buildScpCommand(ServerProfile target,
                                   RemoteCredentials credentials,
                                   File uploadSource,
                                   String remoteDir) {
        StringBuilder sb = new StringBuilder("scp");
        appendSshOptions(sb, target, credentials, true);
        String remotePath = target.getUsername() + "@" + target.getHost() + ":" + quote(remoteDir + "/");
        sb.append(" ").append(quote(uploadSource.getAbsolutePath())).append(" ").append(remotePath);
        return sb.toString();
    }

    private String buildSshCommand(ServerProfile target,
                                   String remoteCommand,
                                   boolean interactiveTty) {
        return buildSshCommand(target, null, remoteCommand, interactiveTty);
    }

    private String buildSshCommand(ServerProfile target,
                                   RemoteCredentials credentials,
                                   String remoteCommand,
                                   boolean interactiveTty) {
        StringBuilder sb = new StringBuilder("ssh");
        appendSshOptions(sb, target, credentials, false);
        if (interactiveTty) {
            sb.append(" -t");
        }
        sb.append(" ").append(target.getUsername()).append("@").append(target.getHost());
        if (remoteCommand != null && !remoteCommand.isBlank()) {
            sb.append(" ").append(quote(remoteCommand));
        }
        return sb.toString();
    }

    private void appendSshOptions(StringBuilder sb,
                                  ServerProfile target,
                                  RemoteCredentials credentials,
                                  boolean scpStylePort) {
        if (target.getPort() != 22) {
            sb.append(scpStylePort ? " -P " : " -p ").append(target.getPort());
        }
        if (credentials != null && target.getAuthType() == com.sshdeploy.deploy.domain.AuthType.PRIVATE_KEY) {
            String keyPath = credentials.getPrivateKeyPath();
            if (keyPath != null && !keyPath.isBlank()) {
                sb.append(" -i ").append(quote(keyPath));
            }
        }
    }

    private static String joinCommands(List<String> commands) {
        return String.join(" && ", commands);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
