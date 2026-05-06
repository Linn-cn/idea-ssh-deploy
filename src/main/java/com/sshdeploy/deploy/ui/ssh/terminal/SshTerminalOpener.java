package com.sshdeploy.deploy.ui.ssh.terminal;

import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.agent.util.log.TerminalListener;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;
import org.jetbrains.plugins.terminal.cloud.CloudTerminalRunner;

import java.awt.Dimension;
import java.lang.reflect.Constructor;

public final class SshTerminalOpener {
    private static final Class<?>[] RUNNER_CTOR_NEW = {
            Project.class, String.class, org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess.class,
            TerminalListener.TtyResizeHandler.class, boolean.class
    };
    private static final Class<?>[] RUNNER_CTOR_OLD = {
            Project.class, String.class, org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess.class,
            TerminalListener.TtyResizeHandler.class
    };

    public JschTtyConnector open(Project project,
                                 ServerProfile target,
                                 RemoteCredentials targetCredentials) {
        String title = target.getName() + " (" + target.getHost() + ":" + target.getPort() + ")";
        JschTtyConnector connector = new JschTtyConnector(target, targetCredentials, null);
        connector.setName(title);
        // 主动初始化，避免依赖 Terminal 内部回调时序导致的初始化超时
        boolean ok = connector.init(null);
        if (!ok) {
            throw new IllegalStateException("SSH terminal connector init failed.");
        }
        ClosableCloudTerminalProcess process = new ClosableCloudTerminalProcess(connector);
        TerminalListener.TtyResizeHandler resizeHandler = (w, h) -> connector.resize(new Dimension(w, h), new Dimension(0, 0));
        CloudTerminalRunner runner = createRunner(project, title, process, resizeHandler);
        if (runner == null) {
            throw new IllegalStateException("Cannot create cloud terminal runner");
        }
        TerminalTabState state = new TerminalTabState();
        state.myTabName = title;
        TerminalView.getInstance(project).createNewSession(runner, state);
        return connector;
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
