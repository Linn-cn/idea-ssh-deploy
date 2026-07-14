package com.sshdeploy.deploy.ui.command;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.backup.ConfigSection;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.CommandExecutionTypeLabels;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.common.CommandContentPreview;
import com.sshdeploy.deploy.ui.common.ConfigSectionIoSupport;
import com.sshdeploy.deploy.ui.common.MasterCheckboxColumnHeaderSupport;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandManagementPanel extends JPanel {
    private static final int COMMAND_PREVIEW_MAX_LINES = 5;
    private static final int COL_SELECT = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_COMMAND = 3;
    private static final int COL_OPERATION = 4;

    private final DeployPluginStateService stateService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameSearchField;
    private final JComboBox<TypeFilterItem> typeFilterCombo;
    private final List<String> visibleCommandIds = new ArrayList<>();

    public CommandManagementPanel(DeployPluginStateService stateService) {
        super(new BorderLayout());
        this.stateService = stateService;
        this.tableModel = new DefaultTableModel(new Object[]{
                MyMessageBundle.message("command.manager.col.select"),
                MyMessageBundle.message("command.manager.col.name"),
                MyMessageBundle.message("command.manager.col.type"),
                MyMessageBundle.message("command.manager.col.command"),
                MyMessageBundle.message("command.manager.col.operation")
        }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_SELECT) {
                    return Boolean.class;
                }
                return Object.class;
            }
        };
        this.table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_SELECT || column == COL_OPERATION;
            }
        };
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setFocusable(true);
        nameSearchField = new JTextField();
        typeFilterCombo = new JComboBox<>();
        typeFilterCombo.addItem(new TypeFilterItem(null, MyMessageBundle.message("command.manager.search.type.all")));
        for (CommandExecutionType type : CommandExecutionType.values()) {
            typeFilterCombo.addItem(new TypeFilterItem(type, CommandExecutionTypeLabels.label(type)));
        }
        applyIdeaFont(nameSearchField);

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel(MyMessageBundle.message("command.manager.search.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        topPanel.add(nameSearchField, gbc);
        applyCompactSearchField(nameSearchField);
        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel(MyMessageBundle.message("command.manager.search.type")), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0;
        topPanel.add(typeFilterCombo, gbc);
        applyCompactTypeFilter(typeFilterCombo);
        gbc.gridx = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(Box.createHorizontalGlue(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 5;
        gbc.weightx = 0;
        JButton searchButton = new JButton(MyMessageBundle.message("command.manager.searchBtn"));
        JButton resetButton = new JButton(MyMessageBundle.message("command.manager.search.reset"));
        JButton addButton = new JButton(MyMessageBundle.message("command.manager.add"));
        JButton exportButton = new JButton(MyMessageBundle.message("command.manager.export"));
        JButton importButton = new JButton(MyMessageBundle.message("command.manager.import"));
        topPanel.add(searchButton, gbc);
        gbc.gridx = 6;
        topPanel.add(resetButton, gbc);
        gbc.gridx = 7;
        topPanel.add(addButton, gbc);
        gbc.gridx = 8;
        JButton batchDeleteButton = new JButton(MyMessageBundle.message("command.manager.batchDelete"));
        topPanel.add(batchDeleteButton, gbc);
        gbc.gridx = 9;
        topPanel.add(exportButton, gbc);
        gbc.gridx = 10;
        topPanel.add(importButton, gbc);
        add(topPanel, BorderLayout.NORTH);

        add(new JBScrollPane(table), BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));

        configureTableAppearance();
        JCheckBox checkPrototype = new JCheckBox();
        checkPrototype.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_SELECT).setCellEditor(new DefaultCellEditor(checkPrototype));
        table.getColumnModel().getColumn(COL_OPERATION).setCellRenderer(new OperationCellRenderer());
        table.getColumnModel().getColumn(COL_OPERATION).setCellEditor(new OperationCellEditor());

        searchButton.addActionListener(e -> refreshTable());
        resetButton.addActionListener(e -> {
            nameSearchField.setText("");
            typeFilterCombo.setSelectedIndex(0);
            refreshTable();
        });
        exportButton.addActionListener(e ->
                ConfigSectionIoSupport.exportSection(this, ConfigSection.COMMANDS, stateService, null));
        importButton.addActionListener(e ->
                ConfigSectionIoSupport.importSection(this, ConfigSection.COMMANDS, stateService, null, this::refreshTable));
        addButton.addActionListener(e -> openEditDialog(null));
        batchDeleteButton.addActionListener(e -> batchDeleteSelectedCommands());
        refreshTable();
    }

    private void configureTableAppearance() {
        table.setRowHeight(44);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(COL_SELECT).setPreferredWidth(JBUI.scale(44));
        table.getColumnModel().getColumn(COL_SELECT).setMaxWidth(JBUI.scale(56));
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(180);
        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_COMMAND).setPreferredWidth(480);
        table.getColumnModel().getColumn(COL_OPERATION).setPreferredWidth(260);
        table.getTableHeader().setReorderingAllowed(false);
        table.setIntercellSpacing(new java.awt.Dimension(8, 4));
        table.setShowGrid(false);

        javax.swing.table.DefaultTableCellRenderer leftCellRenderer = new javax.swing.table.DefaultTableCellRenderer();
        leftCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftCellRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        table.getColumnModel().getColumn(COL_NAME).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(COL_TYPE).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(COL_COMMAND).setCellRenderer(new MultilineCommandCellRenderer());

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
                COL_SELECT,
                headerRenderer,
                MyMessageBundle.message("command.manager.header.masterSelect.tooltip"));
    }

    private void refreshTable() {
        String keyword = nameSearchField.getText().trim().toLowerCase();
        TypeFilterItem typeFilter = (TypeFilterItem) typeFilterCombo.getSelectedItem();
        CommandExecutionType requiredType = typeFilter == null ? null : typeFilter.type;
        visibleCommandIds.clear();
        tableModel.setRowCount(0);
        for (CommandTemplate command : stateService.getCommands()) {
            if (!contains(command.getName(), keyword)) {
                continue;
            }
            CommandExecutionType commandType = CommandExecutionType.normalize(command.getExecutionType());
            if (requiredType != null && commandType != requiredType) {
                continue;
            }
            visibleCommandIds.add(command.getId());
            tableModel.addRow(new Object[]{
                    Boolean.FALSE,
                    command.getName(),
                    CommandExecutionTypeLabels.label(command.getExecutionType()),
                    command.getContent(),
                    MyMessageBundle.message("command.manager.col.operation")
            });
        }
        adjustCommandColumnRowHeights();
    }

    private void adjustCommandColumnRowHeights() {
        if (tableModel.getRowCount() == 0) {
            return;
        }
        Font font = table.getFont();
        int lineHeight = table.getFontMetrics(font).getHeight();
        int verticalPad = JBUI.scale(12);
        int minHeight = JBUI.scale(44);
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String content = Objects.toString(tableModel.getValueAt(row, COL_COMMAND), "");
            int lines = CommandContentPreview.displayLineCount(content, COMMAND_PREVIEW_MAX_LINES);
            int height = Math.max(minHeight, lines * lineHeight + verticalPad);
            table.setRowHeight(row, height);
        }
    }

    private void batchDeleteSelectedCommands() {
        List<String> ids = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(r, COL_SELECT))) {
                ids.add(visibleCommandIds.get(r));
            }
        }
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    MyMessageBundle.message("command.manager.batchDelete.none"),
                    MyMessageBundle.message("command.manager.batchDelete"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                MyMessageBundle.message("command.manager.confirm.batchDelete", ids.size()),
                MyMessageBundle.message("command.manager.batchDelete"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        for (String id : ids) {
            stateService.deleteCommandById(id);
        }
        refreshTable();
    }

    private static boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(keyword);
    }

    private CommandTemplate commandAtRow(int row) {
        if (row < 0 || row >= visibleCommandIds.size()) {
            return null;
        }
        String id = visibleCommandIds.get(row);
        for (CommandTemplate command : stateService.getCommands()) {
            if (Objects.equals(id, command.getId())) {
                return command;
            }
        }
        return null;
    }

    private void openEditDialog(CommandTemplate existing) {
        CommandFormDialog dialog = new CommandFormDialog(this, stateService, existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
        }
    }

    private final class OperationCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JButton editButton;
        private final JButton deleteButton;
        private final JButton copyButton;
        private int row = -1;

        private OperationCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            editButton = new JButton(MyMessageBundle.message("command.manager.edit"));
            deleteButton = new JButton(MyMessageBundle.message("command.manager.delete"));
            copyButton = new JButton(MyMessageBundle.message("common.copy"));
            panel.add(editButton);
            panel.add(deleteButton);
            panel.add(copyButton);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            editButton.addActionListener(e -> {
                stopCellEditing();
                CommandTemplate command = commandAtRow(row);
                if (command != null) {
                    openEditDialog(command);
                }
            });
            deleteButton.addActionListener(e -> {
                stopCellEditing();
                CommandTemplate command = commandAtRow(row);
                if (command == null) {
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(
                        CommandManagementPanel.this,
                        MyMessageBundle.message("command.manager.confirm.delete", command.getName()),
                        MyMessageBundle.message("command.manager.delete"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.OK_OPTION) {
                    stateService.deleteCommandById(command.getId());
                    refreshTable();
                }
            });
            copyButton.addActionListener(e -> {
                stopCellEditing();
                copyRowAt(row, COL_NAME, COL_COMMAND);
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return panel;
        }
    }

    private static final class MultilineCommandCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private MultilineCommandCellRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.TOP);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(JBUI.scale(6), JBUI.scale(6), JBUI.scale(6), JBUI.scale(6)));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                         Object value,
                                                         boolean isSelected,
                                                         boolean hasFocus,
                                                         int row,
                                                         int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(CommandContentPreview.toHtml(Objects.toString(value, ""), COMMAND_PREVIEW_MAX_LINES));
            return this;
        }
    }

    private static final class OperationCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton editButton = new JButton(MyMessageBundle.message("command.manager.edit"));
        private final JButton deleteButton = new JButton(MyMessageBundle.message("command.manager.delete"));
        private final JButton copyButton = new JButton(MyMessageBundle.message("common.copy"));

        private OperationCellRenderer() {
            panel.add(editButton);
            panel.add(deleteButton);
            panel.add(copyButton);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            Color bg = UIManager.getColor("Button.background");
            Color fg = UIManager.getColor("Button.foreground");
            if (bg != null) {
                editButton.setBackground(bg);
                deleteButton.setBackground(bg);
                copyButton.setBackground(bg);
            }
            if (fg != null) {
                editButton.setForeground(fg);
                deleteButton.setForeground(fg);
                copyButton.setForeground(fg);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            panel.setBackground(table.getBackground());
            return panel;
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
        int prefW = JBUI.scale(200);
        java.awt.Dimension h = field.getPreferredSize();
        field.setMinimumSize(new java.awt.Dimension(minW, h.height));
        field.setPreferredSize(new java.awt.Dimension(prefW, h.height));
        field.setMaximumSize(new java.awt.Dimension(prefW + JBUI.scale(40), h.height));
    }

    private static void applyCompactTypeFilter(JComboBox<TypeFilterItem> combo) {
        int minW = JBUI.scale(100);
        int prefW = JBUI.scale(140);
        java.awt.Dimension h = combo.getPreferredSize();
        combo.setMinimumSize(new java.awt.Dimension(minW, h.height));
        combo.setPreferredSize(new java.awt.Dimension(prefW, h.height));
        combo.setMaximumSize(new java.awt.Dimension(prefW + JBUI.scale(40), h.height));
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

    private record TypeFilterItem(CommandExecutionType type, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
