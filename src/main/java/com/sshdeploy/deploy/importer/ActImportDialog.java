package com.sshdeploy.deploy.importer;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;

public final class ActImportDialog {
    private static final int TEXT_COLS = 30;

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
        JTextArea previewArea = new JTextArea(2, TEXT_COLS);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        topPanel.add(btnChooseFile);

        JButton btnImport = new JButton(MyMessageBundle.message("import.wizard.import"));
        btnImport.setEnabled(false);
        btnChooseFile.addActionListener(e -> chooseFile(previewArea, btnImport));
        JTextArea resultArea = new JTextArea(12, TEXT_COLS);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JBScrollPane previewScroll = new JBScrollPane(previewArea);
        previewScroll.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(48)));
        JBScrollPane resultScroll = new JBScrollPane(resultArea);
        resultScroll.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(200)));

        btnImport.addActionListener(e -> {
            btnImport.setEnabled(false);
            resultArea.setText(MyMessageBundle.message("import.wizard.importing"));
            doImport(btnImport, resultArea);
        });

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(previewScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(btnImport, BorderLayout.NORTH);
        bottomPanel.add(resultScroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        JOptionPane.showOptionDialog(
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
            previewArea.setCaretPosition(0);
            btnImport.setEnabled(true);
        }
    }

    private void doImport(JButton btnImport, JTextArea resultArea) {
        if (selectedFile == null) {
            resultArea.setText(MyMessageBundle.message("import.wizard.noFile"));
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

                String finalResult = formatImportLog(result);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    resultArea.setText(finalResult);
                    resultArea.setCaretPosition(0);
                    btnImport.setEnabled(true);
                });
            } catch (Exception ex) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    resultArea.setText(MyMessageBundle.message("import.wizard.importFailed", ex.getMessage()));
                    btnImport.setEnabled(true);
                });
            }
        }, "SshDeploy-Import").start();
    }

    private static String formatImportLog(ImportResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.hasErrors()) {
            sb.append(MyMessageBundle.message("import.wizard.errors")).append('\n');
            for (String error : result.getErrors()) {
                sb.append("  - ").append(error).append('\n');
            }
            sb.append('\n');
        }
        if (result.hasWarnings()) {
            sb.append(MyMessageBundle.message("import.wizard.warnings")).append('\n');
            for (String warning : result.getWarnings()) {
                sb.append("  - ").append(warning).append('\n');
            }
            sb.append('\n');
        }

        sb.append(MyMessageBundle.message("import.result.section.servers", result.getServers().size())).append('\n');
        if (result.getServers().isEmpty()) {
            sb.append(MyMessageBundle.message("import.result.empty")).append('\n');
        } else {
            for (ServerProfile s : result.getServers()) {
                sb.append("  • ")
                        .append(s.getName()).append(" | ")
                        .append(s.getHost()).append(':').append(s.getPort())
                        .append(" | ").append(s.getUsername())
                        .append('\n');
            }
        }
        sb.append('\n');

        appendCommandBlock(sb, result.getBeforeCommands(),
                MyMessageBundle.message("import.result.section.commands.before", result.getBeforeCommands().size()));
        appendCommandBlock(sb, result.getAfterCommands(),
                MyMessageBundle.message("import.result.section.commands.after", result.getAfterCommands().size()));

        sb.append(MyMessageBundle.message("import.result.section.uploads", result.getUploads().size())).append('\n');
        if (result.getUploads().isEmpty()) {
            sb.append(MyMessageBundle.message("import.result.empty")).append('\n');
        } else {
            for (UploadConfig u : result.getUploads()) {
                String kind = u.isDirectory()
                        ? MyMessageBundle.message("import.result.upload.kind.dir")
                        : MyMessageBundle.message("import.result.upload.kind.file");
                sb.append("  • ").append(u.getLocalPath()).append(" → ").append(u.getRemotePath())
                        .append(" (").append(kind).append(")\n");
            }
        }
        sb.append('\n');

        sb.append(MyMessageBundle.message("import.result.section.profiles", result.getDeployProfiles().size())).append('\n');
        if (result.getDeployProfiles().isEmpty()) {
            sb.append(MyMessageBundle.message("import.result.empty")).append('\n');
        } else {
            for (DeployProfile p : result.getDeployProfiles()) {
                sb.append("  • ").append(p.getName()).append('\n');
            }
        }

        return sb.toString();
    }

    private static void appendCommandBlock(StringBuilder sb, List<CommandTemplate> commands, String sectionTitle) {
        sb.append(sectionTitle).append('\n');
        if (commands.isEmpty()) {
            sb.append(MyMessageBundle.message("import.result.empty")).append('\n');
        } else {
            for (CommandTemplate c : commands) {
                String name = c.getName() == null ? "" : c.getName().trim();
                if (name.isEmpty()) {
                    name = MyMessageBundle.message("import.result.command.unnamed");
                }
                sb.append("  • ").append(name).append('\n');
                String body = c.getContent() == null ? "" : c.getContent().trim();
                if (body.isEmpty()) {
                    sb.append("      ").append(MyMessageBundle.message("import.result.command.noBody")).append('\n');
                } else {
                    for (String line : body.split("\\R", -1)) {
                        sb.append("      ").append(line).append('\n');
                    }
                }
            }
        }
        sb.append('\n');
    }
}
