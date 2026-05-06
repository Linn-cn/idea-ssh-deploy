package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ServerDeploySettingsEditor extends SettingsEditor<ServerDeployRunConfiguration> {
    private static final String MODE_DIRECT = "DIRECT_FILE";
    private static final String MODE_REGEX = "DIR_REGEX";
    private static final String DEFAULT_REGEX = "^(?!.*(?:-sources|\\.original)\\.jar$).+\\.jar$";
    private static final String BUILTIN_SPRINGBOOT = "SPRING_BOOT_JAR";
    private static final String BUILTIN_NONE = "";
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
    private final JComboBox<BuiltinRegexItem> regexBuiltinCombo;
    private final JTextField regexField;
    private final JButton regexBuiltinAddBtn;
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
        regexBuiltinCombo = new JComboBox<>(new BuiltinRegexItem[]{
                new BuiltinRegexItem(BUILTIN_NONE, MyMessageBundle.message("runconfig.upload.regex.none"), ""),
                new BuiltinRegexItem(BUILTIN_SPRINGBOOT, MyMessageBundle.message("runconfig.upload.regex.springboot"), DEFAULT_REGEX)
        });
        regexField = new JTextField();

        preCommandsArea = createCommandEditorField(4);
        preCommandCombo = new JComboBox<>();
        preCommandAddBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        regexBuiltinAddBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));

        remoteDirField = new JTextField();
        postCommandsArea = createCommandEditorField(5);
        postCommandCombo = new JComboBox<>();
        postCommandAddBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        terminalCommandCombo = new JComboBox<>();
        terminalCommandUseBtn = new JButton(MyMessageBundle.message("runconfig.command.add"));
        terminalCommandField = new JTextField();

        applyIdeaFont(uploadFileField, uploadDirectoryField, regexField, preCommandsArea, remoteDirField, postCommandsArea, terminalCommandField);
        applyInputPadding();

        JButton chooseFileBtn = new JButton(MyMessageBundle.message("runconfig.choose.file"));
        chooseFileBtn.addActionListener(e -> chooseFile(uploadFileField, false));
        directFileRow = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadFile"), uploadFileField, chooseFileBtn);

        JButton chooseDirBtn = new JButton(MyMessageBundle.message("runconfig.choose.dir"));
        chooseDirBtn.addActionListener(e -> chooseFile(uploadDirectoryField, true));
        regexRow1 = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadDir"), uploadDirectoryField, chooseDirBtn);
        regexRow2 = rowWithButton(MyMessageBundle.message("runconfig.editor.uploadRegexBuiltin"), regexBuiltinCombo, regexBuiltinAddBtn);
        regexRow4 = rowOnly(MyMessageBundle.message("runconfig.editor.uploadRegex"), regexField);

        JPanel preRow = commandRow(preCommandCombo, preCommandAddBtn);
        JPanel postRow = commandRow(postCommandCombo, postCommandAddBtn);
        JPanel terminalRow = commandRow(terminalCommandCombo, terminalCommandUseBtn);
        JButton prePlaceholderBtn = createPlaceholderButton(() -> insertPlaceholderIntoArea(preCommandsArea));
        JButton postPlaceholderBtn = createPlaceholderButton(() -> insertPlaceholderIntoArea(postCommandsArea));

        int row = 0;
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.server.single"), serverCombo);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.buildType"), uploadModeCombo);
        addRow(form, gbc, row++, "", directFileRow);
        addRow(form, gbc, row++, "", regexRow1);
        addRow(form, gbc, row++, "", regexRow2);
        addRow(form, gbc, row++, "", regexRow4);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.preCommands.select"), preRow);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.preCommands"), rowWithButton("", preCommandsArea, prePlaceholderBtn));
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.remoteDir"), remoteDirField);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.postCommands.select"), postRow);
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.postCommands"), rowWithButton("", postCommandsArea, postPlaceholderBtn));
        addRow(form, gbc, row++, MyMessageBundle.message("runconfig.editor.terminalCommand.select"), terminalRow);
        addRow(form, gbc, row, MyMessageBundle.message("runconfig.editor.terminalCommand"), terminalCommandField);

        panel.add(new JBScrollPane(form), BorderLayout.CENTER);

        refreshServersInternal();
        refreshCommandCombos();
        serverCombo.setSelectedItem(null);
        uploadModeCombo.addActionListener(e -> refreshUploadModeVisibility());
        preCommandAddBtn.addActionListener(e -> appendSelectedCommand(preCommandCombo, preCommandsArea));
        postCommandAddBtn.addActionListener(e -> appendSelectedCommand(postCommandCombo, postCommandsArea));
        terminalCommandUseBtn.addActionListener(e -> setTextFromSelectedCommand(terminalCommandCombo, terminalCommandField));
        regexBuiltinAddBtn.addActionListener(e -> {
            BuiltinRegexItem item = (BuiltinRegexItem) regexBuiltinCombo.getSelectedItem();
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
        selectServer(configuration.getServerId());
        selectMode(configuration.getUploadMode());
        uploadFileField.setText(configuration.getUploadFilePath());
        uploadDirectoryField.setText(configuration.getUploadDirectoryPath());
        regexField.setText(configuration.getUploadFileRegex());
        selectBuiltinByRegex(configuration.getUploadFileRegex());
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
            serverCombo.addItem(new ServerItem(server.getId(), server.getName()));
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

    private void selectBuiltinByRegex(String regex) {
        regexBuiltinCombo.setSelectedIndex(0);
        if (regex == null || regex.isBlank()) {
            return;
        }
        boolean matched = false;
        for (int i = 0; i < regexBuiltinCombo.getItemCount(); i++) {
            BuiltinRegexItem item = regexBuiltinCombo.getItemAt(i);
            if (item.regex.equals(regex)) {
                regexBuiltinCombo.setSelectedIndex(i);
                matched = true;
                break;
            }
        }
        // 未匹配内置规则时：保留下拉为“未选择”，最终规则从 regexField 读取。
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
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
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
        regexBuiltinCombo.setBorder(UIManager.getBorder("ComboBox.border"));
        regexField.setBorder(DEFAULT_PAD_SINGLE);
        remoteDirField.setBorder(DEFAULT_PAD_SINGLE);
        preCommandsArea.setBorder(UIManager.getBorder("TextField.border"));
        postCommandsArea.setBorder(UIManager.getBorder("TextField.border"));
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
                ? FileChooserDescriptorFactory.createSingleFolderDescriptor()
                : FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
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

    private static EditorTextField createCommandEditorField(int rows) {
        EditorTextField field = new EditorTextField();
        field.setOneLineMode(false);
        int lineHeight = 22;
        int height = Math.max(88, rows * lineHeight + 12);
        field.setPreferredSize(new java.awt.Dimension(10, height));
        return field;
    }

    private JButton createPlaceholderButton(Runnable insertAction) {
        JButton button = new JButton(AllIcons.Actions.ListFiles);
        button.setToolTipText(MyMessageBundle.message("runconfig.placeholder.button.tooltip"));
        button.addActionListener(e -> openPlaceholderDialog(insertAction));
        return button;
    }

    private void openPlaceholderDialog(Runnable insertAction) {
        PlaceholderItem[] items = new PlaceholderItem[]{
                new PlaceholderItem("${fileName}", MyMessageBundle.message("runconfig.placeholder.fileName.desc"))
        };
        JList<PlaceholderItem> list = new JList<>(items);
        list.setSelectedIndex(0);
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new java.awt.Dimension(420, 180));
        int result = JOptionPane.showConfirmDialog(
                panel,
                scrollPane,
                MyMessageBundle.message("runconfig.placeholder.dialog.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            PlaceholderItem selected = list.getSelectedValue();
            if (selected != null) {
                insertPlaceholder = selected.token;
                insertAction.run();
                insertPlaceholder = "";
            }
        }
    }

    private String insertPlaceholder = "";

    private void insertPlaceholderIntoArea(EditorTextField area) {
        if (insertPlaceholder == null || insertPlaceholder.isBlank()) {
            return;
        }
        String content = area.getText();
        int start = content.length();
        int end = start;
        Editor editor = area.getEditor();
        if (editor != null) {
            int selStart = editor.getSelectionModel().getSelectionStart();
            int selEnd = editor.getSelectionModel().getSelectionEnd();
            if (selStart >= 0 && selEnd >= selStart) {
                start = selStart;
                end = selEnd;
            } else {
                start = editor.getCaretModel().getOffset();
                end = start;
            }
        }
        String updated = content.substring(0, start) + insertPlaceholder + content.substring(end);
        area.setText(updated);
        Editor updatedEditor = area.getEditor();
        if (updatedEditor != null) {
            updatedEditor.getCaretModel().moveToOffset(start + insertPlaceholder.length());
        }
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

    private static final class BuiltinRegexItem {
        private final String key;
        private final String label;
        private final String regex;

        private BuiltinRegexItem(String key, String label, String regex) {
            this.key = key;
            this.label = label;
            this.regex = regex;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ServerItem {
        private final String id;
        private final String name;

        private ServerItem(String id, String name) {
            this.id = id;
            this.name = name;
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

    private static final class PlaceholderItem {
        private final String token;
        private final String description;

        private PlaceholderItem(String token, String description) {
            this.token = token;
            this.description = description;
        }

        @Override
        public String toString() {
            return token + " - " + description;
        }
    }
}
