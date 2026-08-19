package com.sshdeploy.deploy.ui.ssh.terminal;

import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.remote.JschRetry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.agent.util.log.TerminalListener;
import com.jediterm.core.util.TermSize;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner;

import java.lang.reflect.Constructor;

/**
 * Opens a remote SSH shell as a tab in the IDE Terminal tool window.
 */
public final class SshTerminalOpener {
    private static final Class<?>[] RUNNER_CTOR_NEW = {
            Project.class, String.class, org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess.class,
            TerminalListener.TtyResizeHandler.class, boolean.class
    };
    private static final Class<?>[] RUNNER_CTOR_OLD = {
            Project.class, String.class, org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess.class,
            TerminalListener.TtyResizeHandler.class
    };

    /**
     * Connects over SSH using stored credentials, then attaches the PTY to the IDE Terminal.
     * SSH I/O may run off the EDT; the tab is created on the EDT.
     */
    public JschTtyConnector open(Project project,
                                 ServerProfile target,
                                 RemoteCredentials targetCredentials) {
        String title = target.getName() + " (" + target.getHost() + ":" + target.getPort() + ")";
        JschTtyConnector connected = connect(target, targetCredentials, title);
        Runnable attach = () -> attachToIdeTerminal(project, title, connected);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            attach.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(attach);
        }
        return connected;
    }

    private static JschTtyConnector connect(ServerProfile target,
                                            RemoteCredentials targetCredentials,
                                            String title) {
        JschTtyConnector connector = null;
        boolean ok = false;
        for (int attempt = 1; attempt <= JschRetry.MAX_ATTEMPTS; attempt++) {
            connector = new JschTtyConnector(target, targetCredentials, null);
            connector.setName(title);
            ok = connector.connect();
            if (ok) {
                break;
            }
            if (attempt < JschRetry.MAX_ATTEMPTS) {
                try {
                    Thread.sleep(JschRetry.DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying SSH terminal open", e);
                }
            }
        }
        if (!ok || connector == null) {
            throw new IllegalStateException("SSH terminal connector init failed.");
        }
        return connector;
    }

    private static void attachToIdeTerminal(Project project, String title, JschTtyConnector connected) {
        ClosableCloudTerminalProcess process = new ClosableCloudTerminalProcess(connected);
        TerminalListener.TtyResizeHandler resizeHandler =
                (w, h) -> connected.resize(new TermSize(w, h));
        CloudTerminalRunner runner = createRunner(project, title, process, resizeHandler);
        if (runner == null) {
            throw new IllegalStateException("Cannot create cloud terminal runner");
        }
        TerminalTabState state = new TerminalTabState();
        state.myTabName = title;
        TerminalToolWindowManager.getInstance(project).createNewSession(runner, state);
    }

    private static CloudTerminalRunner createRunner(Project project,
                                                    String title,
                                                    org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess process,
                                                    TerminalListener.TtyResizeHandler handler) {
        try {
            Constructor<?> c = CloudTerminalRunner.class.getConstructor(RUNNER_CTOR_NEW);
            return (CloudTerminalRunner) c.newInstance(project, title, process, handler, true);
        } catch (Exception ignored) {
        }
        try {
            Constructor<?> c = CloudTerminalRunner.class.getConstructor(RUNNER_CTOR_OLD);
            return (CloudTerminalRunner) c.newInstance(project, title, process, handler);
        } catch (Exception ignored) {
        }
        return null;
    }
}
