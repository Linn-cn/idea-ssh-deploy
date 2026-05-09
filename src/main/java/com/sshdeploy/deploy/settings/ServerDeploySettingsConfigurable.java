package com.sshdeploy.deploy.settings;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.importer.ActImportDialog;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel importPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        importPanel.add(new JLabel(MyMessageBundle.message("settings.importAct.hint")));
        JButton importActBtn = new JButton(MyMessageBundle.message("settings.importAct.button"));
        importActBtn.addActionListener(e -> new ActImportDialog(resolveProjectForImport()).show());
        importPanel.add(importActBtn);
        panel.add(importPanel, gbc);

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
