package com.sshdeploy.deploy.settings;

import com.sshdeploy.MyMessageBundle;
import com.google.gson.JsonSyntaxException;
import com.sshdeploy.deploy.backup.PluginConfigBackupService;
import com.sshdeploy.deploy.importer.ActImportDialog;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.intellij.openapi.ui.Messages;

public final class ServerDeploySettingsConfigurable implements Configurable {
    private JPanel panel;
    private JSpinner timeoutSpinner;
    private JTextField encodingField;
    private JComboBox<PluginSettingsService.LanguageMode> languageModeCombo;

    @Override
    public @Nls String getDisplayName() {
        return MyMessageBundle.message("settings.title");
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(MyMessageBundle.message("settings.language.label")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        languageModeCombo = new JComboBox<>(PluginSettingsService.LanguageMode.values());
        languageModeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                PluginSettingsService.LanguageMode mode = value instanceof PluginSettingsService.LanguageMode
                        ? (PluginSettingsService.LanguageMode) value
                        : PluginSettingsService.LanguageMode.FOLLOW_IDE;
                label.setText(getLanguageModeLabel(mode));
                return label;
            }
        });
        applyCompactControlWidth(languageModeCombo);
        panel.add(languageModeCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(MyMessageBundle.message("settings.timeout.label")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 3600, 1));
        applyCompactControlWidth(timeoutSpinner);
        panel.add(timeoutSpinner, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(MyMessageBundle.message("settings.encoding.label")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        encodingField = new JTextField("UTF-8");
        applyCompactControlWidth(encodingField);
        panel.add(encodingField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(JBUI.scale(14), JBUI.scale(6), JBUI.scale(4), JBUI.scale(6));
        panel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(6, 6, 6, 6);

        JButton exportBackupBtn = new JButton(MyMessageBundle.message("settings.backup.export"));
        JButton importBackupBtn = new JButton(MyMessageBundle.message("settings.backup.import"));
        exportBackupBtn.addActionListener(e -> exportPluginConfig());
        importBackupBtn.addActionListener(e -> importPluginConfig());
        JPanel backupSection = createSettingsSection(
                MyMessageBundle.message("settings.backup.sectionTitle"),
                createWrappingHint(MyMessageBundle.message("settings.backup.hint")),
                exportBackupBtn,
                importBackupBtn);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(backupSection, gbc);

        JButton importActBtn = new JButton(MyMessageBundle.message("settings.importAct.button"));
        importActBtn.addActionListener(e -> new ActImportDialog(resolveProjectForImport()).show());
        JPanel actSection = createSettingsSection(
                MyMessageBundle.message("settings.act.sectionTitle"),
                createWrappingHint(MyMessageBundle.message("settings.importAct.hint")),
                importActBtn);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        panel.add(actSection, gbc);

        reset();
        return panel;
    }

    private static Project resolveProjectForImport() {
        Project[] open = ProjectManager.getInstance().getOpenProjects();
        if (open.length > 0) {
            return open[0];
        }
        return ProjectManager.getInstance().getDefaultProject();
    }

    private void exportPluginConfig() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle(MyMessageBundle.message("settings.backup.export.title"));
        chooser.setSelectedFile(new java.io.File("ssh-deploy-config.json"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showSaveDialog(panel) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = chooser.getSelectedFile();
        if (file.getName().indexOf('.') < 0) {
            file = new java.io.File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            DeployPluginStateService state = DeployPluginStateService.getInstance();
            PasswordSafeCredentialStore store = new PasswordSafeCredentialStore();
            String json = PluginConfigBackupService.exportToJson(state, store);
            Path path = file.toPath();
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Messages.showInfoMessage(panel, MyMessageBundle.message("settings.backup.export.success", path), MyMessageBundle.message("settings.backup.export.title"));
        } catch (IOException ex) {
            Messages.showErrorDialog(panel, MyMessageBundle.message("settings.backup.error.writeFailed", ex.getMessage()), MyMessageBundle.message("settings.backup.export.title"));
        }
    }

    private void importPluginConfig() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle(MyMessageBundle.message("settings.backup.import.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showOpenDialog(panel) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = chooser.getSelectedFile();
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            DeployPluginStateService state = DeployPluginStateService.getInstance();
            PasswordSafeCredentialStore store = new PasswordSafeCredentialStore();
            PluginConfigBackupService.ImportResult result = PluginConfigBackupService.importFromJson(json, state, store);
            ApplicationManager.getApplication().saveSettings();
            Messages.showInfoMessage(
                    panel,
                    MyMessageBundle.message(
                            "settings.backup.import.success",
                            result.serversAdded(),
                            result.serversSkipped(),
                            result.commandsAdded(),
                            result.commandsSkipped(),
                            result.rulesAdded(),
                            result.rulesSkipped()),
                    MyMessageBundle.message("settings.backup.import.title"));
        } catch (JsonSyntaxException ex) {
            Messages.showErrorDialog(panel, MyMessageBundle.message("settings.backup.error.invalidJson", ex.getMessage()), MyMessageBundle.message("settings.backup.import.title"));
        } catch (IOException ex) {
            Messages.showErrorDialog(panel, MyMessageBundle.message("settings.backup.error.readFailed", ex.getMessage()), MyMessageBundle.message("settings.backup.import.title"));
        }
    }

    @Override
    public boolean isModified() {
        PluginSettingsService service = PluginSettingsService.getInstance();
        if (languageModeCombo.getSelectedItem() != service.getLanguageMode()) {
            return true;
        }
        if (((Integer) timeoutSpinner.getValue()) != service.getDefaultCommandTimeoutSeconds()) {
            return true;
        }
        return !encodingField.getText().trim().equals(service.getDefaultEncoding());
    }

    @Override
    public void apply() {
        PluginSettingsService service = PluginSettingsService.getInstance();
        service.setLanguageMode((PluginSettingsService.LanguageMode) languageModeCombo.getSelectedItem());
        service.setDefaultCommandTimeoutSeconds((Integer) timeoutSpinner.getValue());
        service.setDefaultEncoding(encodingField.getText());
    }

    @Override
    public void reset() {
        PluginSettingsService service = PluginSettingsService.getInstance();
        if (languageModeCombo != null) {
            languageModeCombo.setSelectedItem(service.getLanguageMode());
        }
        if (timeoutSpinner != null) {
            timeoutSpinner.setValue(service.getDefaultCommandTimeoutSeconds());
        }
        if (encodingField != null) {
            encodingField.setText(service.getDefaultEncoding());
        }
    }

    private static String getLanguageModeLabel(PluginSettingsService.LanguageMode mode) {
        return switch (mode) {
            case ZH_CN -> MyMessageBundle.message("settings.language.zhCN");
            case EN_US -> MyMessageBundle.message("settings.language.enUS");
            case FOLLOW_IDE -> MyMessageBundle.message("settings.language.followIDE");
        };
    }

    /**
     * 分组块：标题边框 + 说明（自动换行）+ 下方一行操作按钮（左对齐），JSON 与 ACT 两区块结构一致。
     */
    private static JPanel createSettingsSection(String title, JTextArea hintBody, JButton... actionButtons) {
        JPanel section = new JPanel(new GridBagLayout());
        TitledBorder titled = javax.swing.BorderFactory.createTitledBorder(title);
        java.awt.Font labelFont = javax.swing.UIManager.getFont("Label.font");
        if (labelFont != null) {
            titled.setTitleFont(labelFont.deriveFont(Font.BOLD));
        }
        section.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                titled,
                JBUI.Borders.empty(4, 8, 8, 8)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = JBUI.insetsBottom(8);
        section.add(hintBody, c);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        buttonRow.setOpaque(false);
        for (JButton b : actionButtons) {
            buttonRow.add(b);
        }
        c.gridy = 1;
        c.insets = JBUI.emptyInsets();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        section.add(buttonRow, c);
        return section;
    }

    private static JTextArea createWrappingHint(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFocusable(false);
        area.setColumns(58);
        area.setRows(0);
        Font base = javax.swing.UIManager.getFont("Label.font");
        if (base != null) {
            area.setFont(base);
        }
        area.setBorder(JBUI.Borders.empty());
        return area;
    }

    /**
     * 设置页内控件不再横向铺满：约为原先常见拉伸宽度的一半量级。
     */
    private static void applyCompactControlWidth(javax.swing.JComponent component) {
        int minW = JBUI.scale(100);
        int prefW = JBUI.scale(200);
        java.awt.Dimension min = new java.awt.Dimension(minW, component.getMinimumSize().height);
        java.awt.Dimension pref = new java.awt.Dimension(prefW, component.getPreferredSize().height);
        component.setMinimumSize(min);
        component.setPreferredSize(pref);
        if (!(component instanceof JSpinner)) {
            component.setMaximumSize(new java.awt.Dimension(prefW + JBUI.scale(40), pref.height));
        }
    }
}
