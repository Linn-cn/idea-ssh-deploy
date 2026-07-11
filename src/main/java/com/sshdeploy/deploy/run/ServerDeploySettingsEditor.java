package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.BuiltinFileMatchRules;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.FileMatchRule;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.importer.ActWorkspaceXmlParser;
import com.sshdeploy.deploy.pipeline.CredentialResolver;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.ui.common.ComboPreviewHtml;
import com.sshdeploy.deploy.ui.common.CommandContentPreview;
import com.sshdeploy.deploy.ui.common.CommandEditorSupport;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ServerDeploySettingsEditor extends SettingsEditor<ServerDeployRunConfiguration> {
    private static final String MODE_DIRECT = "DIRECT_FILE";
    private static final String MODE_REGEX = "DIR_REGEX";
    private static final int COMMAND_COMBO_PREVIEW_LINES = 3;
    private static final int REGEX_COMBO_PREVIEW_LINES = 2;
    private static final javax.swing.border.Border DEFAULT_BORDER = JBUI.Borders.customLine(JBColor.border(), 1);
    private static final javax.swing.border.Border ERROR_BORDER = JBUI.Borders.customLine(JBColor.RED, 1);
    private static final javax.swing.border.Border DEFAULT_PAD_SINGLE = paddedBorder(DEFAULT_BORDER, 0, 6, 0, 6);
    private static final javax.swing.border.Border DEFAULT_PAD_MULTI = paddedBorder(DEFAULT_BORDER, 4, 6, 4, 6);
    private static final javax.swing.border.Border ERROR_PAD_SINGLE = paddedBorder(ERROR_BORDER, 0, 6, 0, 6);
    private static final javax.swing.border.Border ERROR_PAD_MULTI = paddedBorder(ERROR_BORDER, 4, 6, 4, 6);

    private final DeployPluginStateService stateService = DeployPluginStateService.getInstance();
    private final Project project;
    private final String defaultChooserDir;
    private final JPanel panel;
    private final JComboBox<ServerItem> serverCombo;
    private final JComboBox<ModeItem> uploadModeCombo;
    private final JTextField uploadFileField;
    private final JTextField uploadDirectoryField;
    private final JComboBox<FileRegexRuleItem> regexRuleCombo;
    private final JTextField regexField;
    private final JButton regexRuleApplyBtn;
    private final EditorTextField preCommandsArea;
    private final JComboBox<CommandItem> preCommandCombo;
    private final JButton preCommandAddBtn;
    private final JTextField remoteDirField;
    private final EditorTextField postCommandsArea;
    private final JComboBox<CommandItem> postCommandCombo;
    private final JButton postCommandAddBtn;
    private final JComboBox<CommandItem> terminalCommandCombo;
    private final JButton terminalCommandUseBtn;
    private final JTextField terminalCommandField;
    private final JPanel directFileRow;
    private final JPanel regexRow1;
    private final JPanel regexRow2;
    private final JPanel regexRow4;

    public ServerDeploySettingsEditor(Project project) {
        this.project = project;
        this.defaultChooserDir = project.getBasePath() == null ? "." : project.getBasePath();
        panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(JBUI.Borders.empty(8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        serverCombo = new JComboBox<>();
        uploadModeCombo = new JComboBox<>(new ModeItem[]{
                new ModeItem(MODE_DIRECT, MyMessageBundle.message("runconfig.upload.mode.direct")),
                new ModeItem(MODE_REGEX, MyMessageBundle.message("runconfig.upload.mode.regex"))
        });
        uploadFileField = new JTextField();
        uploadDirectoryField = new JTextField();
        regexRuleCombo = new JComboBox<>();
        regexField = new JTextField();

        preCommandsArea = CommandEditorSupport.createMultilineField(4);
        preCommandCombo = new JComboBox<>();
        preCommandAddBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        regexRuleApplyBtn = new JButton(MyMessageBundle.message("runconfig.regex.apply"));

        remoteDirField = new JTextField();
        postCommandsArea = CommandEditorSupport.createMultilineField(5);
        postCommandCombo = new JComboBox<>();
        postCommandAddBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        terminalCommandCombo = new JComboBox<>();
        terminalCommandUseBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        terminalCommandField = new JTextField();

        applyCompactRunConfigFieldWidths(
                serverCombo, uploadModeCombo, uploadFileField, uploadDirectoryField,
                regexRuleCombo, regexField, preCommandCombo, postCommandCombo,
                terminalCommandCombo, remoteDirField, terminalCommandField);

        applyIdeaFont(uploadFileField, uploadDirectoryField, regexField, preCommandsArea, remoteDirField, postCommandsArea, terminalCommandField);
        applyInputPadding();
        configureComboPreviewRenderers();

        JButton chooseFileBtn = new JButton(MyMessageBundle.message("runconfig.choose.file"));
        chooseFileBtn.addActionListener(e -> chooseFile(uploadFileField, false));
        directFileRow = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadFile"), uploadFileField, chooseFileBtn);

        JButton chooseDirBtn = new JButton(MyMessageBundle.message("runconfig.choose.dir"));
        chooseDirBtn.addActionListener(e -> chooseFile(uploadDirectoryField, true));
        regexRow1 = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadDir"), uploadDirectoryField, chooseDirBtn);
        regexRow2 = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadRegexRuleSelect"), regexRuleCombo, regexRuleApplyBtn);
        regexRow4 = rowOnly(MyMessageBundle.message("runconfig.editor.uploadRegex"), regexField);

        JButton chooseRemoteDirBtn = new JButton(MyMessageBundle.message("runconfig.choose.remoteDir"));
        chooseRemoteDirBtn.addActionListener(e -> chooseRemoteDirectory());
        JPanel preRow = commandRow(preCommandCombo, preCommandAddBtn);
        JPanel postRow = commandRow(postCommandCombo, postCommandAddBtn);
        JPanel terminalRow = commandRow(terminalCommandCombo, terminalCommandUseBtn);
        JButton prePlaceholderBtn = CommandEditorSupport.createPlaceholderButton(panel, preCommandsArea);
        JButton postPlaceholderBtn = CommandEditorSupport.createPlaceholderButton(panel, postCommandsArea);

        int row = 0;
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.server.single"), serverCombo);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.buildType"), uploadModeCombo);
        addRow(form, gbc, row++, "", directFileRow);
        addRow(form, gbc, row++, "", regexRow1);
        addRow(form, gbc, row++, "", regexRow2);
        addRow(form, gbc, row++, "", regexRow4);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.preCommands.select"), preRow);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.preCommands"), rowWithButton("", preCommandsArea, prePlaceholderBtn));
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.remoteDir"),
                rowWithButton("", remoteDirField, chooseRemoteDirBtn));
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.postCommands.select"), postRow);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.postCommands"), rowWithButton("", postCommandsArea, postPlaceholderBtn));
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.terminalCommand.select"), terminalRow);
        addRow(form, gbc, row, MyMessageBundle.message("runconfig.editor.terminalCommand"), terminalCommandField);

        panel.add(new JBScrollPane(form), BorderLayout.CENTER);

        refreshServersInternal();
        refreshCommandCombos();
        refreshFileRegexRuleCombo();
        serverCombo.setSelectedItem(null);
        uploadModeCombo.addActionListener(e -> refreshUploadModeVisibility());
        preCommandAddBtn.addActionListener(e -> appendSelectedCommand(preCommandCombo, preCommandsArea));
        postCommandAddBtn.addActionListener(e -> appendSelectedCommand(postCommandCombo, postCommandsArea));
        terminalCommandUseBtn.addActionListener(e -> setTextFromSelectedCommand(terminalCommandCombo, terminalCommandField));
        regexRuleApplyBtn.addActionListener(e -> {
            FileRegexRuleItem item = (FileRegexRuleItem) regexRuleCombo.getSelectedItem();
            if (item == null || item.regex == null || item.regex.isBlank()) {
                return;
            }
            replaceRegexField(item.regex);
        });
        refreshUploadModeVisibility();
    }

    @Override
    protected void resetEditorFrom(ServerDeployRunConfiguration configuration) {
        refreshServersInternal();
        refreshCommandCombos();
        refreshFileRegexRuleCombo();
        selectServer(configuration.getServerId());
        selectMode(configuration.getUploadMode());
        uploadFileField.setText(configuration.getUploadFilePath());
        uploadDirectoryField.setText(configuration.getUploadDirectoryPath());
        regexField.setText(configuration.getUploadFileRegex());
        selectFileRegexRuleByPattern(configuration.getUploadFileRegex());
        preCommandsArea.setText(configuration.getPreDeployCommands());
        remoteDirField.setText(configuration.getRemoteUploadDir());
        postCommandsArea.setText(configuration.getPostDeployCommands());
        selectCommandById(terminalCommandCombo, configuration.getTerminalCommandRef());
        terminalCommandField.setText(configuration.getTerminalCommand());
        resetFieldBorders();
        refreshUploadModeVisibility();
    }

    @Override
    protected void applyEditorTo(ServerDeployRunConfiguration configuration) throws ConfigurationException {
        resetFieldBorders();
        ServerItem selectedServer = (ServerItem) serverCombo.getSelectedItem();
        String serverId = selectedServer == null ? "" : selectedServer.id;
        ModeItem mode = (ModeItem) uploadModeCombo.getSelectedItem();
        String modeValue = mode == null ? MODE_DIRECT : mode.value;
        String directFile = uploadFileField.getText().trim();
        String dir = uploadDirectoryField.getText().trim();
        String finalRegex = regexField.getText().trim();
        String remoteDir = remoteDirField.getText().trim();

        if (serverId.isBlank()) {
            markError(serverCombo);
            throw new ConfigurationException(MyMessageBundle.message("runconfig.error.server.required"));
        }
        if (MODE_DIRECT.equals(modeValue) && directFile.isBlank()) {
            markError(uploadFileField);
            throw new ConfigurationException(MyMessageBundle.message("runconfig.error.uploadFile.required"));
        }
        if (MODE_REGEX.equals(modeValue)) {
            if (dir.isBlank()) {
                markError(uploadDirectoryField);
                throw new ConfigurationException(MyMessageBundle.message("runconfig.error.uploadDir.required"));
            }
            if (finalRegex.isBlank()) {
                markError(regexField);
                throw new ConfigurationException(MyMessageBundle.message("runconfig.error.uploadRegex.required"));
            }
            try {
                Pattern.compile(finalRegex);
            } catch (PatternSyntaxException ex) {
                markError(regexField);
                throw new ConfigurationException(MyMessageBundle.message("runconfig.error.uploadRegex.invalid"));
            }
        }
        if (remoteDir.isBlank()) {
            markError(remoteDirField);
            throw new ConfigurationException(MyMessageBundle.message("runconfig.error.remoteDir.required"));
        }

        configuration.setServerId(serverId);
        configuration.setUploadMode(modeValue);
        configuration.setUploadFilePath(directFile);
        configuration.setUploadDirectoryPath(dir);
        configuration.setUploadFileRegex(finalRegex);
        configuration.setPreDeployCommands(preCommandsArea.getText().trim());
        configuration.setRemoteUploadDir(remoteDir);
        configuration.setPostDeployCommands(postCommandsArea.getText().trim());
        CommandItem terminalItem = (CommandItem) terminalCommandCombo.getSelectedItem();
        configuration.setTerminalCommandRef(terminalItem == null ? "" : terminalItem.id);
        configuration.setTerminalCommand(terminalCommandField.getText().trim());
    }

    @Override
    protected JComponent createEditor() {
        return panel;
    }

    private void refreshServersInternal() {
        serverCombo.removeAllItems();
        for (ServerProfile server : stateService.getServers()) {
            serverCombo.addItem(new ServerItem(
                    server.getId(),
                    server.getName(),
                    server.getHost(),
                    server.getPort(),
                    server.getUsername(),
                    server.getDescription()));
        }
    }

    private void refreshCommandCombos() {
        preCommandCombo.removeAllItems();
        postCommandCombo.removeAllItems();
        terminalCommandCombo.removeAllItems();
        CommandItem empty = new CommandItem("", MyMessageBundle.message("runconfig.command.none"), "");
        preCommandCombo.addItem(empty);
        postCommandCombo.addItem(empty);
        terminalCommandCombo.addItem(empty);
        for (CommandTemplate command : stateService.getCommands()) {
            CommandItem item = new CommandItem(command.getId(), command.getName(), command.getContent());
            preCommandCombo.addItem(item);
            postCommandCombo.addItem(item);
            terminalCommandCombo.addItem(item);
        }
        preCommandCombo.setSelectedIndex(0);
        postCommandCombo.setSelectedIndex(0);
        terminalCommandCombo.setSelectedIndex(0);
    }

    private void refreshFileRegexRuleCombo() {
        regexRuleCombo.removeAllItems();
        regexRuleCombo.addItem(new FileRegexRuleItem(
                MyMessageBundle.message("runconfig.upload.regex.none"),
                ""));
        regexRuleCombo.addItem(new FileRegexRuleItem(
                MyMessageBundle.message("runconfig.upload.regex.springboot"),
                BuiltinFileMatchRules.DEFAULT_SPRING_BOOT_PATTERN));
        java.util.ArrayList<FileMatchRule> users = new java.util.ArrayList<>(stateService.getUserFileMatchRules());
        users.sort(java.util.Comparator.comparing(FileMatchRule::getName, String.CASE_INSENSITIVE_ORDER));
        for (FileMatchRule rule : users) {
            regexRuleCombo.addItem(new FileRegexRuleItem(rule.getName(), rule.getPattern()));
        }
        regexRuleCombo.setSelectedIndex(0);
    }

    private void selectFileRegexRuleByPattern(String regex) {
        regexRuleCombo.setSelectedIndex(0);
        if (regex == null || regex.isBlank()) {
            return;
        }
        for (int i = 0; i < regexRuleCombo.getItemCount(); i++) {
            FileRegexRuleItem item = regexRuleCombo.getItemAt(i);
            if (item != null && item.regex.equals(regex)) {
                regexRuleCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void appendSelectedCommand(JComboBox<CommandItem> combo, EditorTextField area) {
        CommandItem item = (CommandItem) combo.getSelectedItem();
        if (item == null || item.content.isBlank()) {
            return;
        }
        String existing = area.getText().trim();
        area.setText(existing.isBlank() ? item.content : existing + System.lineSeparator() + item.content);
    }

    private static void setTextFromSelectedCommand(JComboBox<CommandItem> combo, JTextField field) {
        CommandItem item = (CommandItem) combo.getSelectedItem();
        if (item == null || item.content.isBlank()) {
            return;
        }
        field.setText(item.content);
    }

    private void selectServer(String id) {
        if (id == null || id.isBlank()) {
            serverCombo.setSelectedItem(null);
            return;
        }
        for (int i = 0; i < serverCombo.getItemCount(); i++) {
            ServerItem item = serverCombo.getItemAt(i);
            if (item.id.equals(id)) {
                serverCombo.setSelectedIndex(i);
                return;
            }
        }
        serverCombo.setSelectedItem(null);
    }

    private void selectCommandById(JComboBox<CommandItem> combo, String id) {
        if (id == null || id.isBlank()) {
            combo.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            CommandItem item = combo.getItemAt(i);
            if (id.equals(item.id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private void refreshUploadModeVisibility() {
        ModeItem mode = (ModeItem) uploadModeCombo.getSelectedItem();
        boolean direct = mode == null || MODE_DIRECT.equals(mode.value);
        directFileRow.setVisible(direct);
        regexRow1.setVisible(!direct);
        regexRow2.setVisible(!direct);
        regexRow4.setVisible(!direct);
    }

    private void selectMode(String value) {
        for (int i = 0; i < uploadModeCombo.getItemCount(); i++) {
            ModeItem item = uploadModeCombo.getItemAt(i);
            if (item.value.equals(value)) {
                uploadModeCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static JPanel rowOnly(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private static JPanel rowWithButton(String label, JComponent field, JButton button) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private static JPanel commandRow(JComboBox<CommandItem> combo, JButton addButton) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(combo, BorderLayout.CENTER);
        row.add(addButton, BorderLayout.EAST);
        return row;
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label.isEmpty() ? new JLabel() : new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    /**
     * Only constrains minimum widths so fields stay usable in small dialogs; horizontal growth
     * comes from {@link #addRow} GridBag {@code weightx}.
     */
    private static void applyCompactRunConfigFieldWidths(JComboBox<ServerItem> serverCombo,
                                                         JComboBox<ModeItem> uploadModeCombo,
                                                         JTextField uploadFileField,
                                                         JTextField uploadDirectoryField,
                                                         JComboBox<FileRegexRuleItem> regexRuleCombo,
                                                         JTextField regexField,
                                                         JComboBox<CommandItem> preCommandCombo,
                                                         JComboBox<CommandItem> postCommandCombo,
                                                         JComboBox<CommandItem> terminalCommandCombo,
                                                         JTextField remoteDirField,
                                                         JTextField terminalCommandField) {
        int minW = JBUI.scale(160);
        applyGrowableCombo(serverCombo, minW);
        applyGrowableCombo(uploadModeCombo, minW);
        applyGrowableCombo(regexRuleCombo, minW);
        applyGrowableCombo(preCommandCombo, minW);
        applyGrowableCombo(postCommandCombo, minW);
        applyGrowableCombo(terminalCommandCombo, minW);
        for (JTextField field : new JTextField[]{
                uploadFileField, uploadDirectoryField, regexField, remoteDirField, terminalCommandField}) {
            java.awt.Dimension h = field.getPreferredSize();
            field.setMinimumSize(new java.awt.Dimension(minW, h.height));
            field.setPreferredSize(null);
            field.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, h.height));
        }
    }

    private static <T> void applyGrowableCombo(JComboBox<T> combo, int minW) {
        java.awt.Dimension h = combo.getPreferredSize();
        combo.setMinimumSize(new java.awt.Dimension(minW, h.height));
        combo.setPreferredSize(null);
        combo.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, h.height));
    }

    private void markError(JComponent comp) {
        if (comp == regexField) {
            comp.setBorder(ERROR_PAD_SINGLE);
            return;
        }
        if (comp == preCommandsArea || comp == postCommandsArea) {
            comp.setBorder(ERROR_BORDER);
            return;
        }
        if (comp == uploadFileField || comp == uploadDirectoryField || comp == remoteDirField) {
            comp.setBorder(ERROR_PAD_SINGLE);
            return;
        }
        comp.setBorder(ERROR_BORDER);
    }

    private void resetFieldBorders() {
        serverCombo.setBorder(UIManager.getBorder("ComboBox.border"));
        uploadFileField.setBorder(DEFAULT_PAD_SINGLE);
        uploadDirectoryField.setBorder(DEFAULT_PAD_SINGLE);
        regexRuleCombo.setBorder(UIManager.getBorder("ComboBox.border"));
        regexField.setBorder(DEFAULT_PAD_SINGLE);
        remoteDirField.setBorder(DEFAULT_PAD_SINGLE);
        preCommandsArea.setBorder(UIManager.getBorder("TextField.border"));
        postCommandsArea.setBorder(UIManager.getBorder("TextField.border"));
    }

    private void chooseRemoteDirectory() {
        ServerItem selected = (ServerItem) serverCombo.getSelectedItem();
        if (selected == null || selected.id.isBlank()) {
            JOptionPane.showMessageDialog(
                    panel,
                    MyMessageBundle.message("runconfig.choose.remoteDir.serverRequired"),
                    MyMessageBundle.message("runconfig.choose.remoteDir.title"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ServerProfile server = stateService.findServerById(selected.id).orElse(null);
        if (server == null) {
            JOptionPane.showMessageDialog(
                    panel,
                    MyMessageBundle.message("runconfig.error.server.notfound"),
                    MyMessageBundle.message("runconfig.choose.remoteDir.title"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        PasswordSafeCredentialStore credentialStore = new PasswordSafeCredentialStore();
        RemoteCredentials credentials = new CredentialResolver().resolve(server, credentialStore);
        Optional<String> chosen = RemoteDirectoryPickerDialog.choose(
                panel, server, credentials, remoteDirField.getText());
        chosen.ifPresent(remoteDirField::setText);
    }

    private void chooseFile(JTextComponent targetField, boolean directoryOnly) {
        // macOS 下 AWT FileDialog 可选目录；Windows/Linux 上该 API 无目录模式，改用平台 FileChooser。
        File selected = SystemInfo.isMac ? chooseByAwtFileDialog(directoryOnly) : chooseByIntelliJFileChooser(directoryOnly);
        if (selected != null) {
            targetField.setText(selected.getAbsolutePath());
        }
    }

    private File chooseByIntelliJFileChooser(boolean directoryOnly) {
        FileChooserDescriptor descriptor = directoryOnly
                ? FileChooserDescriptorFactory.singleDir()
                : FileChooserDescriptorFactory.singleFile();
        descriptor.setTitle(directoryOnly
                ? MyMessageBundle.message("runconfig.choose.dir")
                : MyMessageBundle.message("runconfig.choose.file"));
        VirtualFile initial = resolveInitialVirtualFile();
        VirtualFile chosen = FileChooser.chooseFile(descriptor, panel, project, initial);
        if (chosen == null) {
            return null;
        }
        return VfsUtilCore.virtualToIoFile(chosen);
    }

    private VirtualFile resolveInitialVirtualFile() {
        File base = new File(defaultChooserDir);
        String path = base.isAbsolute() ? base.getAbsolutePath() : base.getPath();
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path.replace(File.separatorChar, '/'));
        if (vf == null) {
            vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(base);
        }
        return vf;
    }

    private File chooseByAwtFileDialog(boolean directoryOnly) {
        Frame owner = null;
        java.awt.Window window = SwingUtilities.getWindowAncestor(panel);
        if (window instanceof Frame frame) {
            owner = frame;
        }

        String oldDirProp = System.getProperty("apple.awt.fileDialogForDirectories");
        if (directoryOnly) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
        }
        try {
            FileDialog dialog = new FileDialog(owner, directoryOnly
                    ? MyMessageBundle.message("runconfig.choose.dir")
                    : MyMessageBundle.message("runconfig.choose.file"), FileDialog.LOAD);
            dialog.setDirectory(defaultChooserDir);
            dialog.setVisible(true);
            String directory = dialog.getDirectory();
            String file = dialog.getFile();
            if (directory == null) {
                return null;
            }
            if (directoryOnly) {
                if (file == null || file.isBlank()) {
                    File dir = new File(directory);
                    return dir.isDirectory() && dir.exists() ? dir : null;
                }
                File picked = new File(directory, file);
                return picked.isDirectory() ? picked : null;
            }
            if (file == null || file.isBlank()) {
                return null;
            }
            return new File(directory, file);
        } finally {
            if (directoryOnly) {
                if (oldDirProp == null) {
                    System.clearProperty("apple.awt.fileDialogForDirectories");
                } else {
                    System.setProperty("apple.awt.fileDialogForDirectories", oldDirProp);
                }
            }
        }
    }

    private static void applyIdeaFont(JComponent... components) {
        Font font = UIManager.getFont("TextField.font");
        if (font == null) {
            return;
        }
        for (JComponent component : components) {
            component.setFont(font);
        }
    }

    private void applyInputPadding() {
        // 两层保障：Border 提供“文本-边框”间距，Margin/Alignment 负责竖向观感
        uploadFileField.setMargin(new Insets(0, 6, 0, 6));
        uploadFileField.setAlignmentY(0.5f);
        uploadDirectoryField.setMargin(new Insets(0, 6, 0, 6));
        uploadDirectoryField.setAlignmentY(0.5f);

        regexField.setMargin(new Insets(0, 6, 0, 6));
        regexField.setAlignmentY(0.5f);

        remoteDirField.setMargin(new Insets(0, 6, 0, 6));
        remoteDirField.setAlignmentY(0.5f);
    }

    private static javax.swing.border.Border paddedBorder(javax.swing.border.Border base,
                                                           int top,
                                                           int left,
                                                           int bottom,
                                                           int right) {
        return new javax.swing.border.CompoundBorder(base, JBUI.Borders.empty(top, left, bottom, right));
    }

    private void configureComboPreviewRenderers() {
        int maxRows = 12;
        serverCombo.setMaximumRowCount(maxRows);
        regexRuleCombo.setMaximumRowCount(maxRows);
        preCommandCombo.setMaximumRowCount(maxRows);
        postCommandCombo.setMaximumRowCount(maxRows);
        terminalCommandCombo.setMaximumRowCount(maxRows);

        serverCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                                                                   Object value,
                                                                   int index,
                                                                   boolean isSelected,
                                                                   boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!(value instanceof ServerItem item)) {
                    return label;
                }
                if (index < 0) {
                    label.setText(item.name);
                    label.setToolTipText(null);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                } else {
                    label.setText(ComboPreviewHtml.titledPreview(item.name, serverPreviewDetailHtml(item)));
                    label.setVerticalAlignment(SwingConstants.TOP);
                }
                return label;
            }
        });

        preCommandCombo.setRenderer(createCommandPreviewRenderer());
        postCommandCombo.setRenderer(createCommandPreviewRenderer());
        terminalCommandCombo.setRenderer(createCommandPreviewRenderer());

        regexRuleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                                                                   Object value,
                                                                   int index,
                                                                   boolean isSelected,
                                                                   boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!(value instanceof FileRegexRuleItem item)) {
                    return label;
                }
                if (index < 0) {
                    label.setText(item.label);
                    label.setToolTipText(null);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                } else {
                    String detail = item.regex.isBlank()
                            ? ""
                            : CommandContentPreview.toHtmlBody(item.regex, REGEX_COMBO_PREVIEW_LINES);
                    label.setText(ComboPreviewHtml.titledPreview(item.label, detail));
                    label.setVerticalAlignment(SwingConstants.TOP);
                }
                return label;
            }
        });
    }

    private static String serverPreviewDetailHtml(ServerItem item) {
        StringBuilder detail = new StringBuilder();
        String connection = serverConnectionSummary(item);
        if (!connection.isBlank()) {
            detail.append(ComboPreviewHtml.escape(connection));
        }
        String description = previewableServerDescription(item.description);
        if (!description.isBlank()) {
            if (!detail.isEmpty()) {
                detail.append("<br>");
            }
            detail.append(ComboPreviewHtml.escape(description));
        }
        return detail.toString();
    }

    private static String previewableServerDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String trimmed = description.trim();
        if (ActWorkspaceXmlParser.IMPORTED_SERVER_DESCRIPTION.equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    private static String serverConnectionSummary(ServerItem item) {
        if (item.host == null || item.host.isBlank()) {
            return "";
        }
        String user = item.username == null ? "" : item.username.trim();
        String prefix = user.isBlank() ? "" : user + "@";
        return prefix + item.host.trim() + ":" + item.port;
    }

    private static DefaultListCellRenderer createCommandPreviewRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                                                                   Object value,
                                                                   int index,
                                                                   boolean isSelected,
                                                                   boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!(value instanceof CommandItem item)) {
                    return label;
                }
                if (index < 0) {
                    label.setText(item.name);
                    label.setToolTipText(null);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                } else {
                    String detail = CommandContentPreview.toHtmlBody(item.content, COMMAND_COMBO_PREVIEW_LINES);
                    label.setText(ComboPreviewHtml.titledPreview(item.name, detail));
                    label.setVerticalAlignment(SwingConstants.TOP);
                }
                return label;
            }
        };
    }

    private void replaceRegexField(String regex) {
        if (regex == null) {
            return;
        }
        String trimmed = regex.trim();
        if (trimmed.isBlank()) {
            return;
        }
        regexField.setText(trimmed);
    }

    private static final class ModeItem {
        private final String value;
        private final String label;

        private ModeItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class FileRegexRuleItem {
        private final String label;
        private final String regex;

        private FileRegexRuleItem(String label, String regex) {
            this.label = label;
            this.regex = regex == null ? "" : regex;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ServerItem {
        private final String id;
        private final String name;
        private final String host;
        private final int port;
        private final String username;
        private final String description;

        private ServerItem(String id,
                           String name,
                           String host,
                           int port,
                           String username,
                           String description) {
            this.id = id;
            this.name = name;
            this.host = host == null ? "" : host;
            this.port = port;
            this.username = username == null ? "" : username;
            this.description = description == null ? "" : description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class CommandItem {
        private final String id;
        private final String name;
        private final String content;

        private CommandItem(String id, String name, String content) {
            this.id = id;
            this.name = name;
            this.content = content == null ? "" : content.trim();
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
