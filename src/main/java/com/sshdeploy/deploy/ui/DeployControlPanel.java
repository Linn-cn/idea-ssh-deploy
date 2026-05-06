package com.sshdeploy.deploy.ui;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.pipeline.DeployExecutionOptions;
import com.sshdeploy.deploy.pipeline.DeployExecutionResult;
import com.sshdeploy.deploy.pipeline.DeployLogLevel;
import com.sshdeploy.deploy.pipeline.DeployOrchestrator;
import com.sshdeploy.deploy.pipeline.DeployStage;
import com.sshdeploy.deploy.remote.RemoteTerminalLauncher;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.server.ServerManagerDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DeployControlPanel extends JBPanel<DeployControlPanel> {
    private final Project project;
    private final DeployPluginStateService stateService;
    private final CredentialStore credentialStore;
    private final DeployLogConsole logConsole;
    private final RemoteTerminalLauncher terminalLauncher;
    private final JComboBox<ProfileItem> profileComboBox;
    private final JCheckBox chkDryRun;
    private final JCheckBox chkTestConnection;
    private final JCheckBox chkSkipBuild;
    private final JCheckBox chkSkipUpload;
    private final JCheckBox chkAutoTerminal;
    private final JButton btnDeploy;
    private final JButton btnTestConnection;
    private final JButton btnDryRun;
    private final JButton btnOpenTerminal;
    private final JButton btnManageServers;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DeployControlPanel(Project project,
                              DeployPluginStateService stateService,
                              CredentialStore credentialStore) {
        this.project = project;
        this.stateService = stateService;
        this.credentialStore = credentialStore;
        this.logConsole = new DeployLogConsole();
        this.terminalLauncher = new RemoteTerminalLauncher();
        this.profileComboBox = new ComboBox<>();
        this.chkDryRun = new JCheckBox(MyMessageBundle.message("ui.option.dryRun"));
        this.chkTestConnection = new JCheckBox(MyMessageBundle.message("ui.option.testConnectionOnly"));
        this.chkSkipBuild = new JCheckBox(MyMessageBundle.message("ui.option.skipBuild"));
        this.chkSkipUpload = new JCheckBox(MyMessageBundle.message("ui.option.skipUpload"));
        this.chkAutoTerminal = new JCheckBox(MyMessageBundle.message("ui.option.autoTerminal"));
        this.btnDeploy = new JButton(MyMessageBundle.message("ui.button.deploy"));
        this.btnTestConnection = new JButton(MyMessageBundle.message("ui.button.test"));
        this.btnDryRun = new JButton(MyMessageBundle.message("ui.button.dryRun"));
        this.btnOpenTerminal = new JButton(MyMessageBundle.message("ui.button.openTerminal"));
        this.btnManageServers = new JButton(MyMessageBundle.message("ui.button.manageServers"));

        this.chkAutoTerminal.setSelected(false);

        init();
        refreshProfiles();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(JBUI.Borders.emptyBottom(10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(4, 4, 4, 4);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        topPanel.add(new JBLabel(MyMessageBundle.message("ui.label.profile")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        topPanel.add(profileComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        optionsPanel.add(chkDryRun);
        optionsPanel.add(chkTestConnection);
        optionsPanel.add(chkSkipBuild);
        optionsPanel.add(chkSkipUpload);
        optionsPanel.add(chkAutoTerminal);
        topPanel.add(optionsPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonsPanel.add(btnDeploy);
        buttonsPanel.add(btnTestConnection);
        buttonsPanel.add(btnDryRun);
        buttonsPanel.add(btnOpenTerminal);
        buttonsPanel.add(btnManageServers);
        topPanel.add(buttonsPanel, gbc);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logConsole.getComponent()), BorderLayout.CENTER);

        btnDeploy.addActionListener(e -> executeDeploy());
        btnTestConnection.addActionListener(e -> executeTestConnection());
        btnDryRun.addActionListener(e -> executeDryRun());
        btnOpenTerminal.addActionListener(e -> openTerminal());
        btnManageServers.addActionListener(e -> openServerManager());
    }

    public void refreshProfiles() {
        profileComboBox.removeAllItems();
        List<DeployProfile> profiles = stateService.getDeployProfiles();
        for (DeployProfile profile : profiles) {
            profileComboBox.addItem(new ProfileItem(profile));
        }
    }

    private void executeDeploy() {
        if (running.get()) {
            return;
        }

        ProfileItem selectedItem = (ProfileItem) profileComboBox.getSelectedItem();
        if (selectedItem == null) {
            Messages.showWarningDialog(project, MyMessageBundle.message("ui.error.selectProfile"), MyMessageBundle.message("ui.title.deploy"));
            return;
        }

        running.set(true);
        btnDeploy.setEnabled(false);
        logConsole.clear();
        logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.deploy.start"));

        DeployExecutionOptions options = DeployExecutionOptions.builder()
                .dryRun(chkDryRun.isSelected())
                .skipBuild(chkSkipBuild.isSelected())
                .skipUpload(chkSkipUpload.isSelected())
                .build();

        DeployOrchestrator orchestrator = new DeployOrchestrator(stateService, credentialStore);
        String projectBasePath = project.getBasePath() != null ? project.getBasePath() : ".";

        boolean autoTerminal = chkAutoTerminal.isSelected();
        DeployProfile profile = selectedItem.getProfile();

        new Thread(() -> {
            try {
                DeployExecutionResult result = orchestrator.execute(
                        profile.getId(),
                        projectBasePath,
                        options,
                        logConsole::appendLog
                );

                if (result.isSuccess()) {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.deploy.success"));
                    if (autoTerminal) {
                        openTerminalForProfile(profile);
                    }
                } else {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR, MyMessageBundle.message("ui.log.deploy.failed"));
                }
            } catch (Exception ex) {
                logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR,
                        MyMessageBundle.message("ui.log.deploy.error", ex.getMessage()));
            } finally {
                running.set(false);
                btnDeploy.setEnabled(true);
            }
        }, "SshDeploy-Executor").start();
    }

    private void executeTestConnection() {
        if (running.get()) {
            return;
        }

        ProfileItem selectedItem = (ProfileItem) profileComboBox.getSelectedItem();
        if (selectedItem == null) {
            Messages.showWarningDialog(project, MyMessageBundle.message("ui.error.selectProfile"), MyMessageBundle.message("ui.title.testConnection"));
            return;
        }

        running.set(true);
        btnTestConnection.setEnabled(false);
        logConsole.clear();
        logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.test.start"));

        DeployExecutionOptions options = DeployExecutionOptions.builder()
                .testConnectionOnly(true)
                .build();

        DeployOrchestrator orchestrator = new DeployOrchestrator(stateService, credentialStore);
        String projectBasePath = project.getBasePath() != null ? project.getBasePath() : ".";

        new Thread(() -> {
            try {
                DeployExecutionResult result = orchestrator.execute(
                        selectedItem.getProfile().getId(),
                        projectBasePath,
                        options,
                        logConsole::appendLog
                );

                if (result.isSuccess()) {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.test.success"));
                } else {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR, MyMessageBundle.message("ui.log.test.failed"));
                }
            } catch (Exception ex) {
                logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR,
                        MyMessageBundle.message("ui.log.test.error", ex.getMessage()));
            } finally {
                running.set(false);
                btnTestConnection.setEnabled(true);
            }
        }, "SshDeploy-ConnectionTest").start();
    }

    private void executeDryRun() {
        if (running.get()) {
            return;
        }

        ProfileItem selectedItem = (ProfileItem) profileComboBox.getSelectedItem();
        if (selectedItem == null) {
            Messages.showWarningDialog(project, MyMessageBundle.message("ui.error.selectProfile"), MyMessageBundle.message("ui.title.dryRun"));
            return;
        }

        running.set(true);
        btnDryRun.setEnabled(false);
        logConsole.clear();
        logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.dryRun.start"));

        DeployExecutionOptions options = DeployExecutionOptions.builder()
                .dryRun(true)
                .build();

        DeployOrchestrator orchestrator = new DeployOrchestrator(stateService, credentialStore);
        String projectBasePath = project.getBasePath() != null ? project.getBasePath() : ".";

        new Thread(() -> {
            try {
                DeployExecutionResult result = orchestrator.execute(
                        selectedItem.getProfile().getId(),
                        projectBasePath,
                        options,
                        logConsole::appendLog
                );

                if (result.isSuccess()) {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.INFO, MyMessageBundle.message("ui.log.dryRun.success"));
                } else {
                    logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR, MyMessageBundle.message("ui.log.dryRun.failed"));
                }
            } catch (Exception ex) {
                logConsole.appendLog(DeployStage.PREPARE, DeployLogLevel.ERROR,
                        MyMessageBundle.message("ui.log.dryRun.error", ex.getMessage()));
            } finally {
                running.set(false);
                btnDryRun.setEnabled(true);
            }
        }, "SshDeploy-DryRun").start();
    }

    private void openTerminal() {
        ProfileItem selectedItem = (ProfileItem) profileComboBox.getSelectedItem();
        if (selectedItem == null) {
            Messages.showWarningDialog(project, MyMessageBundle.message("ui.error.selectProfile"), MyMessageBundle.message("ui.title.openTerminal"));
            return;
        }
        openTerminalForProfile(selectedItem.getProfile());
    }

    private void openServerManager() {
        new ServerManagerDialog(stateService).show();
        refreshProfiles();
    }

    private void openTerminalForProfile(DeployProfile profile) {
        ServerProfile target = stateService.findServerById(profile.getServerRef()).orElse(null);
        if (target == null) {
            logConsole.appendLog(DeployStage.OPEN_TERMINAL, DeployLogLevel.ERROR, MyMessageBundle.message("ui.log.terminal.targetNotFound"));
            return;
        }

        try {
            terminalLauncher.openSshTerminal(project, target);
        } catch (Exception ex) {
            logConsole.appendLog(DeployStage.OPEN_TERMINAL, DeployLogLevel.ERROR,
                    MyMessageBundle.message("ui.log.terminal.openFailed", ex.getMessage()));
        }
    }

    private static final class ProfileItem {
        private final DeployProfile profile;

        private ProfileItem(DeployProfile profile) {
            this.profile = profile;
        }

        public DeployProfile getProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return profile.getName();
        }
    }
}
