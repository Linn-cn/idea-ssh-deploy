package com.sshdeploy;

import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.command.CommandManagementPanel;
import com.sshdeploy.deploy.ui.console.DeployConsolePanel;
import com.sshdeploy.deploy.ui.server.ServerManagementPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.JTabbedPane;

public final class MyToolWindowFactory implements ToolWindowFactory {
    @Override
    public boolean shouldBeAvailable(Project project) {
        return true;
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        DeployPluginStateService stateService = DeployPluginStateService.getInstance();
        PasswordSafeCredentialStore credentialStore = new PasswordSafeCredentialStore();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(MyMessageBundle.message("toolwindow.tab.servers"),
                new ServerManagementPanel(stateService, credentialStore));
        tabs.addTab(MyMessageBundle.message("toolwindow.tab.commands"),
                new CommandManagementPanel(stateService));
        tabs.addTab(MyMessageBundle.message("toolwindow.tab.console"),
                new DeployConsolePanel(project));

        Content content = ContentFactory.getInstance()
                .createContent(tabs, MyMessageBundle.message("toolwindow.tab.deploy"), false);
        toolWindow.getContentManager().addContent(content);
    }
}
