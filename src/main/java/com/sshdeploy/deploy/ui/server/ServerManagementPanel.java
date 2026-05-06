package com.sshdeploy.deploy.ui.server;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.pipeline.CredentialResolver;
import com.sshdeploy.deploy.remote.DefaultRemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.security.CredentialRefManager;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

public final class ServerManagementPanel extends JPanel {
    private final DeployPluginStateService stateService;
    private final CredentialStore credentialStore;
    private final CredentialRefManager credentialRefManager = new CredentialRefManager();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameSearchField;
    private final JTextField hostSearchField;

    public ServerManagementPanel(DeployPluginStateService stateService, CredentialStore credentialStore) {
        super(new BorderLayout());
        this.stateService = stateService;
        this.credentialStore = credentialStore;
        this.tableModel = new DefaultTableModel(new Object[]{
                MyMessageBundle.message("server.manager.col.name"),
                MyMessageBundle.message("server.manager.col.host"),
                MyMessageBundle.message("server.manager.col.port"),
                MyMessageBundle.message("server.manager.col.user"),
                MyMessageBundle.message("server.manager.col.password"),
                MyMessageBundle.message("server.manager.col.operation")
        }, 0);
        this.table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
        this.table.setRowSelectionAllowed(false);
        this.table.setColumnSelectionAllowed(false);
        this.table.setCellSelectionEnabled(false);
        this.table.setFocusable(false);
        this.table.setSelectionBackground(this.table.getBackground());
        this.table.setSelectionForeground(this.table.getForeground());
        this.nameSearchField = new JTextField();
        this.hostSearchField = new JTextField();
        applyIdeaFont(nameSearchField, hostSearchField);

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel(MyMessageBundle.message("server.manager.search.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        topPanel.add(nameSearchField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel(MyMessageBundle.message("server.manager.search.host")), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        topPanel.add(hostSearchField, gbc);
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton searchButton = new JButton(MyMessageBundle.message("server.manager.searchBtn"));
        JButton resetButton = new JButton(MyMessageBundle.message("server.manager.search.reset"));
        JButton addButton = new JButton(MyMessageBundle.message("server.manager.add"));
        topPanel.add(searchButton, gbc);
        gbc.gridx = 5;
        topPanel.add(resetButton, gbc);
        gbc.gridx = 6;
        topPanel.add(addButton, gbc);
        add(topPanel, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));

        configureTableAppearance();
        table.getColumnModel().getColumn(5).setCellRenderer(new OperationCellRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new OperationCellEditor());

        searchButton.addActionListener(e -> refreshTable());
        resetButton.addActionListener(e -> {
            nameSearchField.setText("");
            hostSearchField.setText("");
            refreshTable();
        });
        addButton.addActionListener(e -> openEditDialog(null));
        refreshTable();
    }

    private void openEditDialog(ServerProfile existing) {
        ServerFormDialog dialog = new ServerFormDialog(existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
        }
    }

    private void refreshTable() {
        String nameKeyword = nameSearchField.getText().trim().toLowerCase();
        String hostKeyword = hostSearchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (ServerProfile profile : stateService.getServers()) {
            if (!contains(profile.getName(), nameKeyword)) {
                continue;
            }
            if (!contains(profile.getHost(), hostKeyword)) {
                continue;
            }
            String password = profile.getCredentialRef() == null ? "" : Objects.toString(credentialStore.readPassword(profile.getCredentialRef()), "");
            tableModel.addRow(new Object[]{
                    profile.getName(),
                    profile.getHost(),
                    profile.getPort(),
                    profile.getUsername(),
                    password,
                    MyMessageBundle.message("server.manager.col.operation")
            });
        }
    }

    private ServerProfile serverAtRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return null;
        }
        String name = Objects.toString(tableModel.getValueAt(row, 0), "");
        for (ServerProfile profile : stateService.getServers()) {
            if (name.equals(profile.getName())) {
                return profile;
            }
        }
        return null;
    }

    private static int parsePort(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return 22;
        }
    }

    private static boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(keyword);
    }

    private void configureTableAppearance() {
        table.setRowHeight(44);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(180);
        table.getColumnModel().getColumn(5).setPreferredWidth(220);
        table.getTableHeader().setReorderingAllowed(false);
        table.setIntercellSpacing(new java.awt.Dimension(8, 4));
        table.setShowGrid(false);

        javax.swing.table.DefaultTableCellRenderer leftCellRenderer = new javax.swing.table.DefaultTableCellRenderer();
        leftCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftCellRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        for (int i = 0; i < 5; i++) {
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

    private record ServerSelectItem(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private interface RowAction {
        void run(int row);
    }

    private final class OperationCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JButton editButton;
        private final JButton deleteButton;
        private int row = -1;

        private OperationCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            editButton = new JButton(MyMessageBundle.message("server.manager.edit"));
            deleteButton = new JButton(MyMessageBundle.message("server.manager.delete"));
            panel.add(editButton);
            panel.add(deleteButton);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            editButton.addActionListener(e -> {
                stopCellEditing();
                ServerProfile profile = serverAtRow(row);
                if (profile != null) {
                    openEditDialog(profile);
                }
            });
            deleteButton.addActionListener(e -> {
                stopCellEditing();
                ServerProfile profile = serverAtRow(row);
                if (profile == null) {
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(
                        ServerManagementPanel.this,
                        MyMessageBundle.message("server.manager.confirm.delete", profile.getName()),
                        MyMessageBundle.message("server.manager.delete"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.OK_OPTION) {
                    return;
                }
                stateService.deleteServerById(profile.getId());
                if (profile.getCredentialRef() != null && !profile.getCredentialRef().isBlank()) {
                    credentialStore.delete(profile.getCredentialRef());
                }
                refreshTable();
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
        private final JButton editButton = new JButton(MyMessageBundle.message("server.manager.edit"));
        private final JButton deleteButton = new JButton(MyMessageBundle.message("server.manager.delete"));

        private OperationCellRenderer() {
            panel.add(editButton);
            panel.add(deleteButton);
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 0));
            java.awt.Color bg = UIManager.getColor("Button.background");
            java.awt.Color fg = UIManager.getColor("Button.foreground");
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

    private final class ServerFormDialog extends JDialog {
        private final JTextField nameField = new JTextField();
        private final JTextField hostField = new JTextField();
        private final JTextField portField = new JTextField("22");
        private final JTextField userField = new JTextField();
        private final JTextField passwordField = new JTextField();
        private final JCheckBox reusePasswordCheck = new JCheckBox(MyMessageBundle.message("server.manager.password.reuse"));
        private final JComboBox<ServerSelectItem> reusePasswordServerCombo = new JComboBox<>();
        private final JLabel nameError = errorLabel();
        private final JLabel hostError = errorLabel();
        private final JLabel portError = errorLabel();
        private final JLabel userError = errorLabel();
        private final JLabel passwordError = errorLabel();
        private final JLabel testStatus = new JLabel(" ");
        private final ServerProfile existing;
        private boolean saved;

        private ServerFormDialog(ServerProfile existing) {
            super((java.awt.Frame) null, true);
            this.existing = existing;
            setTitle(existing == null ? MyMessageBundle.message("server.manager.add") : MyMessageBundle.message("server.manager.edit"));
            setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 2, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            int row = 0;
            addField(form, gbc, row++, MyMessageBundle.message("server.manager.col.name"), nameField, nameError);
            addField(form, gbc, row++, MyMessageBundle.message("server.manager.col.host"), hostField, hostError);
            addField(form, gbc, row++, MyMessageBundle.message("server.manager.col.port"), portField, portError);
            addField(form, gbc, row++, MyMessageBundle.message("server.manager.col.user"), userField, userError);
            addField(form, gbc, row++, MyMessageBundle.message("server.manager.password"), passwordField, passwordError);

            for (ServerProfile server : stateService.getServers()) {
                if (existing != null && Objects.equals(existing.getId(), server.getId())) {
                    continue;
                }
                reusePasswordServerCombo.addItem(new ServerSelectItem(server.getId(), server.getName()));
            }
            reusePasswordServerCombo.setEnabled(false);
            reusePasswordCheck.addActionListener(e -> reusePasswordServerCombo.setEnabled(reusePasswordCheck.isSelected()));
            addField(form, gbc, row++, "", reusePasswordCheck, errorLabel());
            addField(form, gbc, row++, "", reusePasswordServerCombo, errorLabel());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton testBtn = new JButton(MyMessageBundle.message("server.manager.test"));
            JButton saveBtn = new JButton(MyMessageBundle.message("server.manager.save"));
            JButton cancelBtn = new JButton(MyMessageBundle.message("server.manager.cancel"));
            buttons.add(testStatus);
            buttons.add(testBtn);
            buttons.add(saveBtn);
            buttons.add(cancelBtn);
            add(new JBScrollPane(form), BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);

            if (existing != null) {
                nameField.setText(existing.getName());
                hostField.setText(existing.getHost());
                portField.setText(String.valueOf(existing.getPort()));
                userField.setText(existing.getUsername());
                String pwd = credentialStore.readPassword(existing.getCredentialRef());
                if (pwd != null) {
                    passwordField.setText(pwd);
                }
            }

            applyIdeaFont(nameField, hostField, portField, userField, passwordField);

            testBtn.addActionListener(e -> testConnectionFromDialog());
            saveBtn.addActionListener(e -> saveServer());
            cancelBtn.addActionListener(e -> dispose());

            setSize(480, 460);
            setMinimumSize(new java.awt.Dimension(480, 520));
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(ServerManagementPanel.this));
        }

        private boolean isSaved() {
            return saved;
        }

        private void testConnectionFromDialog() {
            clearErrors();
            if (!validateInputs(false)) {
                return;
            }
            String password = resolvePassword();
            ServerProfile temp = new ServerProfile();
            temp.setHost(hostField.getText().trim());
            temp.setPort(parsePort(portField.getText().trim()));
            temp.setUsername(userField.getText().trim());
            temp.setAuthType(AuthType.PASSWORD);
            testStatus.setText(MyMessageBundle.message("server.manager.test.running"));
            testStatus.setForeground(UIManager.getColor("Label.foreground"));
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try (RemoteClient remoteClient = new DefaultRemoteClientFactory().create()) {
                    RemoteCredentials creds = new RemoteCredentials(password, null, null);
                    remoteClient.testConnection(new RemoteConnectRequest(temp, creds));
                    SwingUtilities.invokeLater(() -> {
                        testStatus.setText(MyMessageBundle.message("server.manager.test.ok"));
                        testStatus.setForeground(new Color(76, 175, 80));
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        testStatus.setText(MyMessageBundle.message("server.manager.test.fail", ex.getMessage()));
                        testStatus.setForeground(new Color(229, 57, 53));
                    });
                }
            });
        }

        private void saveServer() {
            clearErrors();
            if (!validateInputs(true)) {
                return;
            }
            String name = nameField.getText().trim();
            String host = hostField.getText().trim();
            int port = parsePort(portField.getText().trim());
            String username = userField.getText().trim();
            String password = resolvePassword();

            ServerProfile profile = existing == null ? new ServerProfile() : existing;
            profile.setName(name);
            profile.setHost(host);
            profile.setPort(port);
            profile.setUsername(username);
            profile.setAuthType(AuthType.PASSWORD);
            if (profile.getCredentialRef() == null || profile.getCredentialRef().isBlank()) {
                profile.setCredentialRef(credentialRefManager.createServerCredentialRef(profile.getId()));
            }
            if (!password.isBlank()) {
                credentialStore.savePassword(profile.getCredentialRef(), profile.getUsername(), password);
            }
            stateService.upsertServer(profile);
            saved = true;
            dispose();
        }

        private String resolvePassword() {
            String pwd = passwordField.getText();
            if (reusePasswordCheck.isSelected()) {
                ServerSelectItem selected = (ServerSelectItem) reusePasswordServerCombo.getSelectedItem();
                if (selected != null && !selected.id.isBlank()) {
                    ServerProfile profile = stateService.findServerById(selected.id).orElse(null);
                    if (profile != null && profile.getCredentialRef() != null) {
                        String reused = credentialStore.readPassword(profile.getCredentialRef());
                        if (reused != null) {
                            return reused;
                        }
                    }
                }
            }
            return pwd;
        }

        private boolean validateInputs(boolean checkPasswordRequired) {
            boolean ok = true;
            String name = nameField.getText().trim();
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String user = userField.getText().trim();
            String password = resolvePassword();

            if (name.isBlank()) {
                nameError.setText(MyMessageBundle.message("server.manager.error.nameRequired"));
                ok = false;
            } else {
                boolean duplicateName = stateService.getServers().stream().anyMatch(server ->
                        !Objects.equals(existing == null ? null : existing.getId(), server.getId())
                                && server.getName() != null
                                && server.getName().equalsIgnoreCase(name));
                if (duplicateName) {
                    nameError.setText(MyMessageBundle.message("server.manager.error.nameDuplicate"));
                    ok = false;
                }
            }

            if (host.isBlank()) {
                hostError.setText(MyMessageBundle.message("server.manager.error.hostRequired"));
                ok = false;
            }
            try {
                int port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException("out of range");
                }
            } catch (Exception ex) {
                portError.setText(MyMessageBundle.message("server.manager.error.portInvalid"));
                ok = false;
            }
            if (user.isBlank()) {
                userError.setText(MyMessageBundle.message("server.manager.error.userRequired"));
                ok = false;
            }
            if (checkPasswordRequired && password.isBlank()) {
                passwordError.setText(MyMessageBundle.message("server.manager.error.passwordRequired"));
                ok = false;
            }
            return ok;
        }

        private void clearErrors() {
            nameError.setText(" ");
            hostError.setText(" ");
            portError.setText(" ");
            userError.setText(" ");
            passwordError.setText(" ");
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
            form.add(new JLabel(title), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            form.add(input, gbc);

            gbc.gridx = 1;
            gbc.gridy = row * 2 + 1;
            gbc.weightx = 1;
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
