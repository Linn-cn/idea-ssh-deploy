package com.sshdeploy.deploy.ui.command;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.CommandExecutionTypeLabels;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.common.CommandEditorSupport;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

/**
 * Shared add/edit command form used by tool-window management and deploy run configuration.
 */
public final class CommandFormDialog extends JDialog {
    private final DeployPluginStateService stateService;
    private final JTextField nameField = new JTextField();
    private final JComboBox<TypeItem> typeCombo = new JComboBox<>();
    private final EditorTextField commandField = CommandEditorSupport.createMultilineField(8);
    private final JLabel nameError = errorLabel();
    private final JLabel commandError = errorLabel();
    private final CommandTemplate existing;
    private boolean saved;
    private CommandTemplate savedCommand;

    public CommandFormDialog(Component parent,
                             DeployPluginStateService stateService,
                             CommandTemplate existing) {
        this(parent, stateService, existing, null, null);
    }

    /**
     * Prefill content and default type when saving from the deploy configuration editor.
     */
    public CommandFormDialog(Component parent,
                             DeployPluginStateService stateService,
                             String prefilledContent,
                             CommandExecutionType defaultType) {
        this(parent, stateService, null, prefilledContent, defaultType);
    }

    private CommandFormDialog(Component parent,
                              DeployPluginStateService stateService,
                              CommandTemplate existing,
                              String prefilledContent,
                              CommandExecutionType defaultType) {
        super(SwingUtilities.getWindowAncestor(parent),
                existing == null
                        ? MyMessageBundle.message("command.manager.add")
                        : MyMessageBundle.message("command.manager.edit"),
                ModalityType.APPLICATION_MODAL);
        this.stateService = stateService;
        this.existing = existing;
        setLayout(new BorderLayout());

        for (CommandExecutionType type : CommandExecutionType.values()) {
            typeCombo.addItem(new TypeItem(type));
        }

        JPanel commandEditorPanel = CommandEditorSupport.wrapWithPlaceholderButton(commandField, this);
        commandEditorPanel.setPreferredSize(new java.awt.Dimension(JBUI.scale(460), JBUI.scale(200)));
        commandEditorPanel.setMinimumSize(new java.awt.Dimension(JBUI.scale(360), JBUI.scale(120)));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        addField(form, gbc, row++, MyMessageBundle.message("command.manager.col.name"), nameField, nameError);
        addField(form, gbc, row++, MyMessageBundle.message("command.manager.dialog.type"), typeCombo, errorLabel());
        addCommandField(form, gbc, row, MyMessageBundle.message("command.manager.dialog.command"), commandEditorPanel, commandError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton(MyMessageBundle.message("command.manager.save"));
        JButton cancelBtn = new JButton(MyMessageBundle.message("command.manager.cancel"));
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        if (existing != null) {
            nameField.setText(existing.getName());
            commandField.setText(existing.getContent());
            selectType(CommandExecutionType.normalize(existing.getExecutionType()));
        } else {
            if (prefilledContent != null) {
                commandField.setText(prefilledContent);
            }
            selectType(defaultType == null ? CommandExecutionType.GENERAL : defaultType);
        }

        applyIdeaFont(nameField, commandField);
        applyCompactNameField(nameField);

        saveBtn.addActionListener(e -> saveCommand());
        cancelBtn.addActionListener(e -> dispose());

        setSize(JBUI.scale(520), JBUI.scale(480));
        setMinimumSize(new java.awt.Dimension(JBUI.scale(480), JBUI.scale(400)));
        setLocationRelativeTo(parent);
    }

    public boolean isSaved() {
        return saved;
    }

    public CommandTemplate getSavedCommand() {
        return savedCommand;
    }

    private void selectType(CommandExecutionType type) {
        CommandExecutionType target = CommandExecutionType.normalize(type);
        for (int i = 0; i < typeCombo.getItemCount(); i++) {
            TypeItem item = typeCombo.getItemAt(i);
            if (item != null && item.type == target) {
                typeCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void saveCommand() {
        clearErrors();
        if (!validateInputs()) {
            return;
        }
        CommandTemplate command = existing == null ? new CommandTemplate() : existing;
        command.setName(nameField.getText().trim());
        command.setContent(commandField.getText().trim());
        TypeItem selected = (TypeItem) typeCombo.getSelectedItem();
        command.setExecutionType(selected == null ? CommandExecutionType.GENERAL : selected.type);
        stateService.upsertCommand(command);
        savedCommand = command;
        saved = true;
        dispose();
    }

    private boolean validateInputs() {
        boolean ok = true;
        String name = nameField.getText().trim();
        String content = commandField.getText().trim();
        if (name.isBlank()) {
            nameError.setText(MyMessageBundle.message("command.manager.error.nameRequired"));
            ok = false;
        } else {
            boolean duplicate = stateService.getCommands().stream().anyMatch(c ->
                    !Objects.equals(existing == null ? null : existing.getId(), c.getId())
                            && c.getName() != null
                            && c.getName().equalsIgnoreCase(name));
            if (duplicate) {
                nameError.setText(MyMessageBundle.message("command.manager.error.nameDuplicate"));
                ok = false;
            }
        }
        if (content.isBlank()) {
            commandError.setText(MyMessageBundle.message("command.manager.error.commandRequired"));
            ok = false;
        }
        return ok;
    }

    private void clearErrors() {
        nameError.setText(" ");
        commandError.setText(" ");
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(new Color(200, 65, 65));
        return label;
    }

    private static void addField(JPanel form,
                                 GridBagConstraints gbc,
                                 int row,
                                 String title,
                                 Component input,
                                 JLabel error) {
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(title), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, gbc);

        gbc.gridx = 1;
        gbc.gridy = row * 2 + 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 6, 4);
        form.add(error, gbc);
        gbc.insets = new Insets(4, 4, 2, 4);
    }

    private static void addCommandField(JPanel form,
                                        GridBagConstraints gbc,
                                        int row,
                                        String title,
                                        Component commandEditor,
                                        JLabel error) {
        gbc.gridx = 0;
        gbc.gridy = row * 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel(title), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        form.add(commandEditor, gbc);

        gbc.gridx = 1;
        gbc.gridy = row * 2 + 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 6, 4);
        form.add(error, gbc);
        gbc.insets = new Insets(4, 4, 2, 4);
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

    private static void applyCompactNameField(JTextField field) {
        int minW = JBUI.scale(100);
        int prefW = JBUI.scale(200);
        java.awt.Dimension h = field.getPreferredSize();
        field.setMinimumSize(new java.awt.Dimension(minW, h.height));
        field.setPreferredSize(new java.awt.Dimension(prefW, h.height));
        field.setMaximumSize(new java.awt.Dimension(prefW + JBUI.scale(40), h.height));
    }

    private record TypeItem(CommandExecutionType type) {
        @Override
        public String toString() {
            return CommandExecutionTypeLabels.label(type);
        }
    }
}
