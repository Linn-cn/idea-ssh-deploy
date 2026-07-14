package com.sshdeploy.deploy.ui.server;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.DefaultRemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.security.CredentialRefManager;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
 * Shared add/edit server form used by tool-window management and deploy run configuration.
 */
public final class ServerFormDialog extends JDialog {
    private final DeployPluginStateService stateService;
    private final CredentialStore credentialStore;
    private final CredentialRefManager credentialRefManager = new CredentialRefManager();

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
    private ServerProfile savedProfile;

    public ServerFormDialog(Component parent,
                            DeployPluginStateService stateService,
                            CredentialStore credentialStore,
                            ServerProfile existing) {
        super(SwingUtilities.getWindowAncestor(parent),
                existing == null
                        ? MyMessageBundle.message("server.manager.add")
                        : MyMessageBundle.message("server.manager.edit"),
                ModalityType.APPLICATION_MODAL);
        this.stateService = stateService;
        this.credentialStore = credentialStore;
        this.existing = existing;
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
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
        addField(form, gbc, row, "", reusePasswordServerCombo, errorLabel());

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
        applyGrowableFormField(nameField);
        applyGrowableFormField(hostField);
        applyGrowableFormField(userField);
        applyGrowableFormField(passwordField);
        applyCompactPortField(portField);
        applyGrowableFormField(reusePasswordServerCombo);

        testBtn.addActionListener(e -> testConnectionFromDialog());
        saveBtn.addActionListener(e -> saveServer());
        cancelBtn.addActionListener(e -> dispose());

        setMinimumSize(new java.awt.Dimension(JBUI.scale(420), JBUI.scale(400)));
        setSize(JBUI.scale(520), JBUI.scale(480));
        setLocationRelativeTo(parent);
    }

    public boolean isSaved() {
        return saved;
    }

    public ServerProfile getSavedProfile() {
        return savedProfile;
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
        savedProfile = profile;
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
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(title), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, gbc);

        gbc.gridx = 1;
        gbc.gridy = row * 2 + 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 6, 4);
        form.add(error, gbc);
        gbc.insets = new Insets(4, 4, 2, 4);
    }

    private static int parsePort(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return 22;
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

    private static void applyGrowableFormField(JComponent field) {
        int minW = JBUI.scale(220);
        java.awt.Dimension h = field.getPreferredSize();
        field.setMinimumSize(new java.awt.Dimension(minW, h.height));
        field.setPreferredSize(null);
        field.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, h.height));
    }

    private static void applyCompactPortField(JTextField portField) {
        int w = JBUI.scale(72);
        java.awt.Dimension h = portField.getPreferredSize();
        portField.setMinimumSize(new java.awt.Dimension(w, h.height));
        portField.setPreferredSize(new java.awt.Dimension(w, h.height));
        portField.setMaximumSize(new java.awt.Dimension(JBUI.scale(90), h.height));
    }

    private record ServerSelectItem(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
