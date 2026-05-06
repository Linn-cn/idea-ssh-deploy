package com.sshdeploy.deploy.importer;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;

public final class ActImportDialog {
    private final Project project;
    private VirtualFile selectedFile;

    public ActImportDialog(Project project) {
        this.project = project;
    }

    public void show() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(com.intellij.util.ui.JBUI.Borders.empty(10));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(new JLabel(MyMessageBundle.message("import.wizard.description")));

        JButton btnChooseFile = new JButton(MyMessageBundle.message("import.wizard.chooseFile"));
        JTextArea previewArea = new JTextArea(3, 60);
        previewArea.setEditable(false);
        topPanel.add(btnChooseFile);

        JButton btnImport = new JButton(MyMessageBundle.message("import.wizard.import"));
        btnImport.setEnabled(false);
        btnChooseFile.addActionListener(e -> chooseFile(previewArea, btnImport));
        JTextArea resultArea = new JTextArea(8, 60);
        resultArea.setEditable(false);

        btnImport.addActionListener(e -> {
            btnImport.setEnabled(false);
            resultArea.setText("Importing...");
            doImport(btnImport, resultArea);
        });

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JBScrollPane(previewArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(btnImport, BorderLayout.NORTH);
        bottomPanel.add(new JBScrollPane(resultArea), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        int option = JOptionPane.showOptionDialog(
                null,
                mainPanel,
                MyMessageBundle.message("import.wizard.title"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[]{MyMessageBundle.message("import.wizard.close")},
                0
        );
    }

    private void chooseFile(JTextArea previewArea, JButton btnImport) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true, false, false, false, false, false);
        descriptor.setTitle(MyMessageBundle.message("import.wizard.title"));
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file != null) {
            selectedFile = file;
            previewArea.setText(file.getPath());
            btnImport.setEnabled(true);
        }
    }

    private void doImport(JButton btnImport, JTextArea resultArea) {
        if (selectedFile == null) {
            resultArea.setText("No file selected.");
            btnImport.setEnabled(true);
            return;
        }

        new Thread(() -> {
            try {
                DeployPluginStateService stateService = DeployPluginStateService.getInstance();
                PasswordSafeCredentialStore credentialStore = new PasswordSafeCredentialStore();
                List<ActConfigParser> parsers = Collections.singletonList(new ActWorkspaceXmlParser());
                ActConfigImporter importer = new ActConfigImporter(stateService, credentialStore, parsers);

                java.io.File localFile = new java.io.File(selectedFile.getPath());
                ImportResult result = importer.importFromFile(localFile);

                StringBuilder sb = new StringBuilder();
                if (result.hasErrors()) {
                    sb.append("Errors:\n");
                    for (String error : result.getErrors()) {
                        sb.append("  - ").append(error).append("\n");
                    }
                }
                if (result.hasWarnings()) {
                    sb.append("Warnings:\n");
                    for (String warning : result.getWarnings()) {
                        sb.append("  - ").append(warning).append("\n");
                    }
                }
                sb.append("Imported: ")
                        .append(result.getServers().size()).append(" servers, ")
                        .append(result.getUploads().size()).append(" upload configs, ")
                        .append(result.getBeforeCommands().size() + result.getAfterCommands().size()).append(" commands, ")
                        .append(result.getDeployProfiles().size()).append(" deploy profiles.");

                String finalResult = sb.toString();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    resultArea.setText(finalResult);
                    btnImport.setEnabled(true);
                });
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    resultArea.setText("Import failed: " + ex.getMessage());
                    btnImport.setEnabled(true);
                });
            }
        }, "SshDeploy-Import").start();
    }
}
