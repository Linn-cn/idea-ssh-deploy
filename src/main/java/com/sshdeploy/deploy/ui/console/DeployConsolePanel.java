package com.sshdeploy.deploy.ui.console;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

public final class DeployConsolePanel extends JPanel implements Disposable {
    private final JBTextArea area;
    private final DeployConsoleService consoleService;
    private final Consumer<String> listener;

    public DeployConsolePanel(Project project) {
        super(new BorderLayout());
        this.area = new JBTextArea();
        this.area.setEditable(false);
        this.consoleService = project.getService(DeployConsoleService.class);
        this.listener = line -> ApplicationManager.getApplication().invokeLater(() -> {
            area.append(line + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
        this.consoleService.addListener(listener);
        add(new JBScrollPane(area), BorderLayout.CENTER);

        for (String line : consoleService.snapshot()) {
            area.append(line + "\n");
        }
    }

    @Override
    public void dispose() {
        consoleService.removeListener(listener);
    }
}
