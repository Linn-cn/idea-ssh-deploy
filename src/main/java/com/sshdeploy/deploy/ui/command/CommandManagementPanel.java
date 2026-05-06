package com.sshdeploy.deploy.ui.command;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
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
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

public final class CommandManagementPanel extends JPanel {
    private final DeployPluginStateService stateService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameSearchField;

    public CommandManagementPanel(DeployPluginStateService stateService) {
        super(new BorderLayout());
        this.stateService = stateService;
        this.tableModel = new DefaultTableModel(new Object[]{
                MyMessageBundle.message("command.manager.col.name"),
                MyMessageBundle.message("command.manager.col.command"),
                MyMessageBundle.message("command.manager.col.operation")
        }, 0);
        this.table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);
        table.setSelectionBackground(table.getBackground());
        table.setSelectionForeground(table.getForeground());
        nameSearchField = new JTextField();
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
        gbc.weightx = 1;
        topPanel.add(nameSearchField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton searchButton = new JButton(MyMessageBundle.message("command.manager.searchBtn"));
        JButton resetButton = new JButton(MyMessageBundle.message("command.manager.search.reset"));
        JButton addButton = new JButton(MyMessageBundle.message("command.manager.add"));
        topPanel.add(searchButton, gbc);
        gbc.gridx = 3;
        topPanel.add(resetButton, gbc);
        gbc.gridx = 4;
        topPanel.add(addButton, gbc);
        add(topPanel, BorderLayout.NORTH);

        add(new JBScrollPane(table), BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));

        configureTableAppearance();
        table.getColumnModel().getColumn(2).setCellRenderer(new OperationCellRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new OperationCellEditor());

        searchButton.addActionListener(e -> refreshTable());
        resetButton.addActionListener(e -> {
            nameSearchField.setText("");
            refreshTable();
        });
        addButton.addActionListener(e -> openEditDialog(null));
        refreshTable();
    }

    private void configureTableAppearance() {
        table.setRowHeight(44);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getTableHeader().setReorderingAllowed(false);
        table.setIntercellSpacing(new java.awt.Dimension(8, 4));
        table.setShowGrid(false);

        javax.swing.table.DefaultTableCellRenderer leftCellRenderer = new javax.swing.table.DefaultTableCellRenderer();
        leftCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftCellRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        for (int i = 0; i < 2; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(leftCellRenderer);
        }

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer((tbl, value, selected, focus, row, column) -> {
                Component component = headerRenderer.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
                if (component instanceof JLabel label) {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
                }
                return component;
            });
        }
    }

    private void refreshTable() {
        String keyword = nameSearchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (CommandTemplate command : stateService.getCommands()) {
            if (!contains(command.getName(), keyword)) {
                continue;
            }
            tableModel.addRow(new Object[]{
                    command.getName(),
                    command.getContent(),
                    MyMessageBundle.message("command.manager.col.operation")
            });
        }
    }

    private static boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(keyword);
    }

    private CommandTemplate commandAtRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return null;
        }
        String name = Objects.toString(tableModel.getValueAt(row, 0), "");
        for (CommandTemplate command : stateService.getCommands()) {
            if (Objects.equals(name, command.getName())) {
                return command;
            }
        }
        return null;
    }

    private void openEditDialog(CommandTemplate existing) {
        CommandFormDialog dialog = new CommandFormDialog(existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
        }
    }

    private final class OperationCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JButton editButton;
        private final JButton deleteButton;
        private int row = -1;

        private OperationCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            editButton = new JButton(MyMessageBundle.message("command.manager.edit"));
            deleteButton = new JButton(MyMessageBundle.message("command.manager.delete"));
            panel.add(editButton);
            panel.add(deleteButton);
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

    private static final class OperationCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final JButton editButton = new JButton(MyMessageBundle.message("command.manager.edit"));
        private final JButton deleteButton = new JButton(MyMessageBundle.message("command.manager.delete"));

        private OperationCellRenderer() {
            panel.add(editButton);
            panel.add(deleteButton);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            Color bg = UIManager.getColor("Button.background");
            Color fg = UIManager.getColor("Button.foreground");
            if (bg != null) {
                editButton.setBackground(bg);
                deleteButton.setBackground(bg);
            }
            if (fg != null) {
                editButton.setForeground(fg);
                deleteButton.setForeground(fg);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            panel.setBackground(table.getBackground());
            return panel;
        }
    }

    private final class CommandFormDialog extends JDialog {
        private final JTextField nameField = new JTextField();
        private final JTextArea commandArea = new JTextArea(8, 0);
        private final JLabel nameError = errorLabel();
        private final JLabel commandError = errorLabel();
        private final CommandTemplate existing;
        private boolean saved;

        private CommandFormDialog(CommandTemplate existing) {
            super((java.awt.Frame) null, true);
            this.existing = existing;
            setTitle(existing == null ? MyMessageBundle.message("command.manager.add") : MyMessageBundle.message("command.manager.edit"));
            setLayout(new BorderLayout());

            commandArea.setLineWrap(true);
            commandArea.setWrapStyleWord(true);
            JBScrollPane commandScroll = new JBScrollPane(commandArea);
            commandScroll.setPreferredSize(new java.awt.Dimension(520, 180));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 2, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            int row = 0;
            addField(form, gbc, row++, MyMessageBundle.message("command.manager.col.name"), nameField, nameError);
            addField(form, gbc, row, MyMessageBundle.message("command.manager.col.command"), commandScroll, commandError);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveBtn = new JButton(MyMessageBundle.message("command.manager.save"));
            JButton cancelBtn = new JButton(MyMessageBundle.message("command.manager.cancel"));
            buttons.add(saveBtn);
            buttons.add(cancelBtn);

            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);

            if (existing != null) {
                nameField.setText(existing.getName());
                commandArea.setText(existing.getContent());
            }

            applyIdeaFont(nameField, commandArea);

            saveBtn.addActionListener(e -> saveCommand());
            cancelBtn.addActionListener(e -> dispose());

            setSize(560, 420);
            setMinimumSize(new java.awt.Dimension(520, 360));
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(CommandManagementPanel.this));
        }

        private boolean isSaved() {
            return saved;
        }

        private void saveCommand() {
            clearErrors();
            if (!validateInputs()) {
                return;
            }
            CommandTemplate command = existing == null ? new CommandTemplate() : existing;
            command.setName(nameField.getText().trim());
            command.setContent(commandArea.getText().trim());
            stateService.upsertCommand(command);
            saved = true;
            dispose();
        }

        private boolean validateInputs() {
            boolean ok = true;
            String name = nameField.getText().trim();
            String content = commandArea.getText().trim();
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
            gbc.gridwidth = 1;
            form.add(new JLabel(title), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            form.add(input, gbc);

            gbc.gridx = 1;
            gbc.gridy = row * 2 + 1;
            gbc.weightx = 1;
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
}
