package com.sshdeploy.deploy.ui.ssh;

import com.sshdeploy.MyMessageBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class SshInteractiveConsolePanel extends JPanel implements Disposable {
    private final JLabel tipLabel;

    public SshInteractiveConsolePanel(Project project) {
        super(new BorderLayout());
        tipLabel = new JLabel(MyMessageBundle.message("ssh.console.tip"));
        JPanel center = new JPanel(new BorderLayout());
        center.add(tipLabel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
    }
}

