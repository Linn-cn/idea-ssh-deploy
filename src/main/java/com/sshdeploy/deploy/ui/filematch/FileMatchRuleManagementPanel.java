package com.sshdeploy.deploy.ui.filematch;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.backup.ConfigSection;
import com.sshdeploy.deploy.domain.BuiltinFileMatchRules;
import com.sshdeploy.deploy.domain.FileMatchRule;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.common.ConfigSectionIoSupport;
import com.sshdeploy.deploy.ui.common.MasterCheckboxColumnHeaderSupport;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FileMatchRuleManagementPanel extends JPanel {
    private final DeployPluginStateService stateService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameSearchField;
    private final List<String> visibleRuleIds = new ArrayList<>();

    public FileMatchRuleManagementPanel(DeployPluginStateService stateService) {
        super(new BorderLayout());
        this.stateService = stateService;
        this.tableModel = new DefaultTableModel(new Object[]{
                MyMessageBundle.message("fileMatch.manager.col.select"),
                MyMessageBundle.message("fileMatch.manager.col.name"),
                MyMessageBundle.message("fileMatch.manager.col.regex"),
                MyMessageBundle.message("fileMatch.manager.col.operation")
        }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return Object.class;
            }
        };
        this.table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 0) {
                    return row >= 0
                            && row < visibleRuleIds.size()
                            && !BuiltinFileMatchRules.isBuiltinId(visibleRuleIds.get(row));
                }
                return column == 3;
            }
        };
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setFocusable(true);
        nameSearchField = new JTextField();
        applyIdeaFont(nameSearchField);

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel(MyMessageBundle.message("fileMatch.manager.search.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        topPanel.add(nameSearchField, gbc);
        applyCompactSearchField(nameSearchField);
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 3;
        gbc.weightx = 0;
        JButton searchButton = new JButton(MyMessageBundle.message("fileMatch.manager.searchBtn"));
        JButton resetButton = new JButton(MyMessageBundle.message("fileMatch.manager.search.reset"));
        JButton addButton = new JButton(MyMessageBundle.message("fileMatch.manager.add"));
        JButton exportButton = new JButton(MyMessageBundle.message("fileMatch.manager.export"));
        JButton importButton = new JButton(MyMessageBundle.message("fileMatch.manager.import"));
        topPanel.add(searchButton, gbc);
        gbc.gridx = 4;
        topPanel.add(resetButton, gbc);
        gbc.gridx = 5;
        topPanel.add(addButton, gbc);
        gbc.gridx = 6;
        JButton batchDeleteButton = new JButton(MyMessageBundle.message("fileMatch.manager.batchDelete"));
        topPanel.add(batchDeleteButton, gbc);
        gbc.gridx = 7;
        topPanel.add(exportButton, gbc);
        gbc.gridx = 8;
        topPanel.add(importButton, gbc);
        add(topPanel, BorderLayout.NORTH);

        add(new JBScrollPane(table), BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));

        configureTableAppearance();
        JCheckBox checkPrototype = new JCheckBox();
        checkPrototype.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(checkPrototype));
        table.getColumnModel().getColumn(0).setCellRenderer(new BuiltinAwareCheckboxRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new OperationCellRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new OperationCellEditor());

        searchButton.addActionListener(e -> refreshTable());
        resetButton.addActionListener(e -> {
            nameSearchField.setText("");
            refreshTable();
        });
        addButton.addActionListener(e -> openEditDialog(null));
        batchDeleteButton.addActionListener(e -> batchDeleteSelected());
        exportButton.addActionListener(e ->
                ConfigSectionIoSupport.exportSection(this, ConfigSection.FILE_MATCH_RULES, stateService, null));
        importButton.addActionListener(e ->
                ConfigSectionIoSupport.importSection(this, ConfigSection.FILE_MATCH_RULES, stateService, null, this::refreshTable));
        refreshTable();
    }

    private void configureTableAppearance() {
        table.setRowHeight(44);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(44));
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(56));
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(420);
        table.getColumnModel().getColumn(3).setPreferredWidth(320);
        table.getTableHeader().setReorderingAllowed(false);
        table.setIntercellSpacing(new java.awt.Dimension(8, 4));
        table.setShowGrid(false);

        javax.swing.table.DefaultTableCellRenderer leftCellRenderer = new javax.swing.table.DefaultTableCellRenderer();
        leftCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftCellRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        for (int i = 1; i < 3; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(leftCellRenderer);
        }

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        for (int i = 1; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer((tbl, value, selected, focus, row, column) -> {
                Component component = headerRenderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
                if (component instanceof JLabel label) {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
                }
                return component;
            });
        }
        MasterCheckboxColumnHeaderSupport.install(
                table,
                0,
                headerRenderer,
                MyMessageBundle.message("fileMatch.manager.header.masterSelect.tooltip"));
    }

    private void refreshTable() {
        String keyword = nameSearchField.getText().trim().toLowerCase();
        visibleRuleIds.clear();
        tableModel.setRowCount(0);
        for (FileMatchRule rule : stateService.getAllFileMatchRulesForDisplay()) {
            String displayName = BuiltinFileMatchRules.isBuiltinId(rule.getId())
                    ? BuiltinFileMatchRules.displayNameForId(rule.getId())
                    : rule.getName();
            String regex = rule.getPattern() == null ? "" : rule.getPattern();
            if (!matchesKeyword(displayName, regex, keyword)) {
                continue;
            }
            visibleRuleIds.add(rule.getId());
            tableModel.addRow(new Object[]{
                    BuiltinFileMatchRules.isBuiltinId(rule.getId()) ? null : Boolean.FALSE,
                    displayName,
                    regex,
                    MyMessageBundle.message("fileMatch.manager.col.operation")
            });
        }
    }

    private static boolean matchesKeyword(String name, String regex, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String n = name == null ? "" : name.toLowerCase();
        String r = regex == null ? "" : regex.toLowerCase();
        return n.contains(keyword) || r.contains(keyword);
    }

    private void batchDeleteSelected() {
        List<String> ids = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(r, 0))) {
                String id = visibleRuleIds.get(r);
                if (!BuiltinFileMatchRules.isBuiltinId(id)) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    MyMessageBundle.message("fileMatch.manager.batchDelete.none"),
                    MyMessageBundle.message("fileMatch.manager.batchDelete"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                MyMessageBundle.message("fileMatch.manager.confirm.batchDelete", ids.size()),
                MyMessageBundle.message("fileMatch.manager.batchDelete"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        for (String id : ids) {
            stateService.deleteUserFileMatchRuleById(id);
        }
        refreshTable();
    }

    private FileMatchRule ruleAtRow(int row) {
        if (row < 0 || row >= visibleRuleIds.size()) {
            return null;
        }
        String id = visibleRuleIds.get(row);
        if (BuiltinFileMatchRules.isBuiltinId(id)) {
            return BuiltinFileMatchRules.builtinRules().stream()
                    .filter(r -> Objects.equals(id, r.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return stateService.findUserFileMatchRuleById(id).orElse(null);
    }

    private void openEditDialog(FileMatchRule existingUserRule) {
        RuleFormDialog dialog = new RuleFormDialog(existingUserRule);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
        }
    }

    private final class OperationCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panelBuiltin;
        private final JPanel panelUser;
        private final JButton builtinCopyButton;
        private final JButton editButton;
        private final JButton deleteButton;
        private final JButton copyButton;
        private int row = -1;

        private OperationCellEditor() {
            panelBuiltin = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            builtinCopyButton = new JButton(MyMessageBundle.message("common.copy"));
            panelBuiltin.add(new JLabel(MyMessageBundle.message("fileMatch.manager.builtin.readonly")));
            panelBuiltin.add(builtinCopyButton);
            panelBuiltin.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));

            panelUser = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            editButton = new JButton(MyMessageBundle.message("fileMatch.manager.edit"));
            deleteButton = new JButton(MyMessageBundle.message("fileMatch.manager.delete"));
            copyButton = new JButton(MyMessageBundle.message("common.copy"));
            panelUser.add(editButton);
            panelUser.add(deleteButton);
            panelUser.add(copyButton);
            panelUser.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            editButton.addActionListener(e -> {
                stopCellEditing();
                FileMatchRule rule = ruleAtRow(row);
                if (rule != null && !BuiltinFileMatchRules.isBuiltinId(rule.getId())) {
                    openEditDialog(rule);
                }
            });
            deleteButton.addActionListener(e -> {
                stopCellEditing();
                FileMatchRule rule = ruleAtRow(row);
                if (rule == null || BuiltinFileMatchRules.isBuiltinId(rule.getId())) {
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(
                        FileMatchRuleManagementPanel.this,
                        MyMessageBundle.message("fileMatch.manager.confirm.delete", rule.getName()),
                        MyMessageBundle.message("fileMatch.manager.delete"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.OK_OPTION) {
                    stateService.deleteUserFileMatchRuleById(rule.getId());
                    refreshTable();
                }
            });
            copyButton.addActionListener(e -> {
                stopCellEditing();
                copyRowAt(row, 1, 2);
            });
            builtinCopyButton.addActionListener(e -> {
                stopCellEditing();
                copyRowAt(row, 1, 2);
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            String id = visibleRuleIds.get(row);
            return BuiltinFileMatchRules.isBuiltinId(id) ? panelBuiltin : panelUser;
        }
    }

    private final class OperationCellRenderer implements TableCellRenderer {
        private final JPanel panelBuiltin = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JPanel panelUser = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton builtinCopyButton = new JButton(MyMessageBundle.message("common.copy"));
        private final JButton editButton = new JButton(MyMessageBundle.message("fileMatch.manager.edit"));
        private final JButton deleteButton = new JButton(MyMessageBundle.message("fileMatch.manager.delete"));
        private final JButton copyButton = new JButton(MyMessageBundle.message("common.copy"));

        private OperationCellRenderer() {
            panelBuiltin.add(new JLabel(MyMessageBundle.message("fileMatch.manager.builtin.readonly")));
            panelBuiltin.add(builtinCopyButton);
            panelUser.add(editButton);
            panelUser.add(deleteButton);
            panelUser.add(copyButton);
            panelBuiltin.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            panelUser.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            Color bg = UIManager.getColor("Button.background");
            Color fg = UIManager.getColor("Button.foreground");
            if (bg != null) {
                builtinCopyButton.setBackground(bg);
                editButton.setBackground(bg);
                deleteButton.setBackground(bg);
                copyButton.setBackground(bg);
            }
            if (fg != null) {
                builtinCopyButton.setForeground(fg);
                editButton.setForeground(fg);
                deleteButton.setForeground(fg);
                copyButton.setForeground(fg);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            panelUser.setBackground(table.getBackground());
            panelBuiltin.setBackground(table.getBackground());
            if (row >= 0 && row < visibleRuleIds.size()
                    && BuiltinFileMatchRules.isBuiltinId(visibleRuleIds.get(row))) {
                return panelBuiltin;
            }
            return panelUser;
        }
    }

    private final class RuleFormDialog extends JDialog {
        private final JTextField nameField = new JTextField();
        private final JTextField regexField = new JTextField();
        private final JLabel nameError = errorLabel();
        private final JLabel regexError = errorLabel();
        private final FileMatchRule existing;
        private boolean saved;

        private RuleFormDialog(FileMatchRule existing) {
            super((java.awt.Frame) null, true);
            this.existing = existing;
            setTitle(existing == null ? MyMessageBundle.message("fileMatch.manager.add") : MyMessageBundle.message("fileMatch.manager.edit"));
            setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 2, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            int row = 0;
            addField(form, gbc, row++, MyMessageBundle.message("fileMatch.manager.col.name"), nameField, nameError);
            addField(form, gbc, row, MyMessageBundle.message("fileMatch.manager.col.regex"), regexField, regexError);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveBtn = new JButton(MyMessageBundle.message("fileMatch.manager.save"));
            JButton cancelBtn = new JButton(MyMessageBundle.message("fileMatch.manager.cancel"));
            buttons.add(saveBtn);
            buttons.add(cancelBtn);

            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);

            if (existing != null) {
                nameField.setText(existing.getName());
                regexField.setText(existing.getPattern());
            }

            applyIdeaFont(nameField, regexField);
            applyCompactSearchField(nameField);
            applyCompactSearchField(regexField);

            saveBtn.addActionListener(e -> saveRule());
            cancelBtn.addActionListener(e -> dispose());

            setSize(JBUI.scale(520), JBUI.scale(220));
            setMinimumSize(new java.awt.Dimension(JBUI.scale(480), JBUI.scale(180)));
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(FileMatchRuleManagementPanel.this));
        }

        private boolean isSaved() {
            return saved;
        }

        private void saveRule() {
            clearErrors();
            if (!validateInputs()) {
                return;
            }
            FileMatchRule rule = existing == null ? new FileMatchRule() : existing;
            rule.setName(nameField.getText().trim());
            rule.setPattern(regexField.getText().trim());
            stateService.upsertUserFileMatchRule(rule);
            saved = true;
            dispose();
        }

        private boolean validateInputs() {
            boolean ok = true;
            String name = nameField.getText().trim();
            String patternStr = regexField.getText().trim();
            if (name.isBlank()) {
                nameError.setText(MyMessageBundle.message("fileMatch.manager.error.nameRequired"));
                ok = false;
            } else {
                boolean duplicate = stateService.getUserFileMatchRules().stream().anyMatch(r ->
                        !Objects.equals(existing == null ? null : existing.getId(), r.getId())
                                && r.getName() != null
                                && r.getName().equalsIgnoreCase(name));
                if (duplicate) {
                    nameError.setText(MyMessageBundle.message("fileMatch.manager.error.nameDuplicate"));
                    ok = false;
                }
            }
            if (patternStr.isBlank()) {
                regexError.setText(MyMessageBundle.message("fileMatch.manager.error.regexRequired"));
                ok = false;
            } else {
                try {
                    Pattern.compile(patternStr);
                } catch (PatternSyntaxException ex) {
                    regexError.setText(MyMessageBundle.message("fileMatch.manager.error.regexInvalid"));
                    ok = false;
                }
            }
            return ok;
        }

        private void clearErrors() {
            nameError.setText(" ");
            regexError.setText(" ");
        }

        private JLabel errorLabel() {
            JLabel label = new JLabel(" ");
            label.setForeground(new Color(200, 65, 65));
            return label;
        }

        private void addField(JPanel form,
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

    private static void applyCompactSearchField(JTextField field) {
        int minW = JBUI.scale(100);
        int prefW = JBUI.scale(360);
        java.awt.Dimension h = field.getPreferredSize();
        field.setMinimumSize(new java.awt.Dimension(minW, h.height));
        field.setPreferredSize(new java.awt.Dimension(prefW, h.height));
        field.setMaximumSize(new java.awt.Dimension(prefW + JBUI.scale(120), h.height));
    }

    private void copyRowAt(int row, int startColumn, int endColumn) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int col = startColumn; col <= endColumn; col++) {
            String header = Objects.toString(tableModel.getColumnName(col), "");
            String value = Objects.toString(tableModel.getValueAt(row, col), "");
            if (!sb.isEmpty()) {
                sb.append(System.lineSeparator());
            }
            sb.append(header).append("：").append(value);
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }

    private final class BuiltinAwareCheckboxRenderer extends JCheckBox implements TableCellRenderer {
        private BuiltinAwareCheckboxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            boolean builtin = row >= 0
                    && row < visibleRuleIds.size()
                    && BuiltinFileMatchRules.isBuiltinId(visibleRuleIds.get(row));
            setEnabled(!builtin);
            setSelected(!builtin && Boolean.TRUE.equals(value));
            setBackground(table.getBackground());
            return this;
        }
    }
}
