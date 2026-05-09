package com.sshdeploy.deploy.ui.server;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.storage.DeployPluginStateService;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

public final class ServerManagerDialog {
    private final DeployPluginStateService stateService;

    public ServerManagerDialog(DeployPluginStateService stateService) {
        this.stateService = stateService;
    }

    public void show() {
        JDialog dialog = new JDialog((Frame) null, MyMessageBundle.message("server.manager.title"), true);
        dialog.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new Object[]{
                MyMessageBundle.message("server.manager.col.name"),
                MyMessageBundle.message("server.manager.col.host"),
                MyMessageBundle.message("server.manager.col.port"),
                MyMessageBundle.message("server.manager.col.user")
        }, 0);
        JTable table = new JTable(model);
        refresh(model);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton(MyMessageBundle.message("server.manager.add"));
        JButton deleteBtn = new JButton(MyMessageBundle.message("server.manager.delete"));
        JButton closeBtn = new JButton(MyMessageBundle.message("server.manager.close"));

        addBtn.addActionListener(e -> {
            ServerProfile profile = promptServer();
            if (profile != null) {
                stateService.upsertServer(profile);
                refresh(model);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                return;
            }
            String name = (String) model.getValueAt(row, 0);
            for (ServerProfile server : stateService.getServers()) {
                if (server.getName().equals(name)) {
                    stateService.deleteServerById(server.getId());
                    break;
                }
            }
            refresh(model);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        buttons.add(addBtn);
        buttons.add(deleteBtn);
        buttons.add(closeBtn);

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(350, 380);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private ServerProfile promptServer() {
        JTextField nameField = new JTextField();
        JTextField hostField = new JTextField();
        JTextField portField = new JTextField("22");
        JTextField userField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));
        panel.add(new JLabel(MyMessageBundle.message("server.manager.col.name")));
        panel.add(nameField);
        panel.add(new JLabel(MyMessageBundle.message("server.manager.col.host")));
        panel.add(hostField);
        panel.add(new JLabel(MyMessageBundle.message("server.manager.col.port")));
        panel.add(portField);
        panel.add(new JLabel(MyMessageBundle.message("server.manager.col.user")));
        panel.add(userField);

        int result = JOptionPane.showConfirmDialog(
                null, panel, MyMessageBundle.message("server.manager.add"), JOptionPane.OK_CANCEL_OPTION
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        ServerProfile profile = new ServerProfile();
        profile.setName(nameField.getText().trim());
        profile.setHost(hostField.getText().trim());
        try {
            profile.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (Exception ex) {
            profile.setPort(22);
        }
        profile.setUsername(userField.getText().trim());
        return profile;
    }

    private void refresh(DefaultTableModel model) {
        model.setRowCount(0);
        for (ServerProfile server : stateService.getServers()) {
            model.addRow(new Object[]{server.getName(), server.getHost(), server.getPort(), server.getUsername()});
        }
    }
}
