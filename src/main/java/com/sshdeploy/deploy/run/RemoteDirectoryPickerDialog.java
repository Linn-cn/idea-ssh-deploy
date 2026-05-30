package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.DefaultRemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.sshdeploy.deploy.remote.RemoteDirectoryEntry;
import com.sshdeploy.deploy.remote.RemotePathUtils;
import com.intellij.util.ui.JBUI;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.intellij.ui.components.JBScrollPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Browses directories on a connected SSH host via SFTP and returns the chosen path.
 */
final class RemoteDirectoryPickerDialog extends JDialog {
    private static final String ROOT_PATH = "/";

    private final ServerProfile server;
    private final RemoteCredentials credentials;
    private final ExecutorService remoteExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ssh-deploy-remote-dir-picker");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private final AtomicBoolean executorShutdown = new AtomicBoolean();

    private final JTextField pathField = new JTextField();
    private final JLabel statusLabel = new JLabel(" ");
    private final JList<RemoteDirectoryEntry> directoryList = new JList<>();
    private final JButton parentButton = new JButton(MyMessageBundle.message("runconfig.choose.remoteDir.parent"));
    private final JButton refreshButton = new JButton(MyMessageBundle.message("runconfig.choose.remoteDir.refresh"));
    private final JButton selectButton = new JButton(MyMessageBundle.message("runconfig.choose.remoteDir.select"));
    private final JButton cancelButton = new JButton(MyMessageBundle.message("command.manager.cancel"));

    private RemoteClient client;
    private String homeDirectory = ROOT_PATH;
    private String currentPath = ROOT_PATH;
    private String selectedPath;
    private boolean confirmed;

    private RemoteDirectoryPickerDialog(Component parent,
                                        ServerProfile server,
                                        RemoteCredentials credentials,
                                        String initialPath) {
        super(resolveOwner(parent), MyMessageBundle.message("runconfig.choose.remoteDir.title"), true);
        this.server = server;
        this.credentials = credentials;
        setLayout(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        setMinimumSize(new java.awt.Dimension(JBUI.scale(520), JBUI.scale(420)));
        setSize(JBUI.scale(560), JBUI.scale(460));

        pathField.setText(initialPath == null ? "" : initialPath.trim());
        pathField.addActionListener(e -> navigateTo(pathField.getText()));

        directoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        directoryList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RemoteDirectoryEntry entry) {
                    label.setText(entry.getName());
                }
                return label;
            }
        });
        directoryList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    RemoteDirectoryEntry entry = directoryList.getSelectedValue();
                    if (entry != null) {
                        navigateTo(entry.getPath());
                    }
                }
            }
        });

        parentButton.addActionListener(e -> navigateTo(RemotePathUtils.parent(currentPath)));
        refreshButton.addActionListener(e -> navigateTo(pathField.getText()));
        selectButton.addActionListener(e -> confirmSelection());
        cancelButton.addActionListener(e -> dispose());

        JPanel toolbar = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        JPanel pathRow = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        pathRow.add(new JLabel(MyMessageBundle.message("runconfig.choose.remoteDir.path")), BorderLayout.WEST);
        pathRow.add(pathField, BorderLayout.CENTER);
        toolbar.add(pathRow, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        actionRow.add(parentButton);
        actionRow.add(refreshButton);
        toolbar.add(actionRow, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(JBUI.Borders.emptyTop(4));
        buttons.add(selectButton);
        buttons.add(cancelButton);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(JBUI.Borders.empty(8, 8, 0, 8));
        center.add(toolbar, BorderLayout.NORTH);
        center.add(new JBScrollPane(directoryList), BorderLayout.CENTER);
        statusLabel.setBorder(JBUI.Borders.empty(4, 8, 0, 8));
        center.add(statusLabel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownRemoteExecutor();
            }
        });

        setLocationRelativeTo(parent);
        setLoading(true, MyMessageBundle.message("runconfig.choose.remoteDir.connecting"));
        connectAndLoadInitial(pathField.getText());
    }

    static Optional<String> choose(Component parent,
                                   ServerProfile server,
                                   RemoteCredentials credentials,
                                   String initialPath) {
        RemoteDirectoryPickerDialog dialog = new RemoteDirectoryPickerDialog(parent, server, credentials, initialPath);
        dialog.setVisible(true);
        return dialog.confirmed ? Optional.of(dialog.selectedPath) : Optional.empty();
    }

    private void connectAndLoadInitial(String hintPath) {
        int generation = loadGeneration.incrementAndGet();
        remoteExecutor.submit(() -> {
            RemoteClient remoteClient = null;
            try {
                remoteClient = new DefaultRemoteClientFactory().create();
                remoteClient.connect(new RemoteConnectRequest(server, credentials));
                String home = remoteClient.resolveHomeDirectory();
                String startPath = resolveInitialPath(hintPath, home);
                LoadedDirectory loaded = loadDirectoryOrFallback(remoteClient, startPath, home);
                RemoteClient connectedClient = remoteClient;
                publishDirectoryLoaded(generation, connectedClient, home, loaded.path, loaded.entries, null);
            } catch (Exception ex) {
                if (remoteClient != null) {
                    try {
                        remoteClient.close();
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
                publishDirectoryLoaded(generation, null, homeDirectory, currentPath, List.of(), ex.getMessage());
            }
        });
    }

    private void navigateTo(String path) {
        if (client == null) {
            return;
        }
        int generation = loadGeneration.incrementAndGet();
        setLoading(true, MyMessageBundle.message("runconfig.choose.remoteDir.loading"));
        String target = RemotePathUtils.expandHome(path == null ? currentPath : path, homeDirectory);
        remoteExecutor.submit(() -> {
            try {
                List<RemoteDirectoryEntry> entries = client.listDirectory(target);
                publishDirectoryLoaded(generation, client, homeDirectory, target, entries, null);
            } catch (Exception ex) {
                publishDirectoryLoaded(generation, client, homeDirectory, currentPath, List.of(), ex.getMessage());
            }
        });
    }

    private void publishDirectoryLoaded(int generation,
                                        RemoteClient connectedClient,
                                        String home,
                                        String path,
                                        List<RemoteDirectoryEntry> entries,
                                        String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            if (generation != loadGeneration.get()) {
                return;
            }
            if (connectedClient != null && client == null) {
                client = connectedClient;
                homeDirectory = home;
            }
            onDirectoryLoaded(path, entries, errorMessage);
        });
    }

    private static String resolveInitialPath(String hintPath, String home) {
        if (hintPath == null || hintPath.isBlank()) {
            return ROOT_PATH;
        }
        if (hintPath.trim().startsWith("~")) {
            return RemotePathUtils.expandHome(hintPath.trim(), home);
        }
        return RemotePathUtils.normalize(hintPath);
    }

    private static LoadedDirectory loadDirectoryOrFallback(RemoteClient remoteClient,
                                                         String path,
                                                         String home) throws Exception {
        String normalized = RemotePathUtils.expandHome(path, home);
        try {
            return new LoadedDirectory(normalized, remoteClient.listDirectory(normalized));
        } catch (Exception first) {
            if (ROOT_PATH.equals(normalized)) {
                throw first;
            }
            return new LoadedDirectory(ROOT_PATH, remoteClient.listDirectory(ROOT_PATH));
        }
    }

    private void onDirectoryLoaded(String path, List<RemoteDirectoryEntry> entries, String errorMessage) {
        if (errorMessage != null) {
            currentPath = path;
            pathField.setText(path);
            directoryList.setListData(entries.toArray(RemoteDirectoryEntry[]::new));
            setLoading(false, MyMessageBundle.message("runconfig.choose.remoteDir.failed", errorMessage));
            return;
        }
        currentPath = RemotePathUtils.normalize(path);
        pathField.setText(currentPath);
        directoryList.setListData(entries.toArray(RemoteDirectoryEntry[]::new));
        if (entries.isEmpty()) {
            setLoading(false, MyMessageBundle.message("runconfig.choose.remoteDir.empty"));
        } else {
            setLoading(false, " ");
        }
        selectButton.setEnabled(true);
    }

    private void confirmSelection() {
        selectedPath = RemotePathUtils.expandHome(pathField.getText(), homeDirectory);
        confirmed = true;
        dispose();
    }

    private void setLoading(boolean loading, String status) {
        parentButton.setEnabled(!loading);
        refreshButton.setEnabled(!loading);
        selectButton.setEnabled(!loading && client != null);
        directoryList.setEnabled(!loading);
        pathField.setEnabled(!loading);
        statusLabel.setText(status == null ? " " : status);
        if (loading) {
            statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        } else {
            statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    private void shutdownRemoteExecutor() {
        if (!executorShutdown.compareAndSet(false, true)) {
            return;
        }
        remoteExecutor.submit(() -> {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {
                    // ignore
                }
                client = null;
            }
        });
        remoteExecutor.shutdown();
        try {
            if (!remoteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                remoteExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            remoteExecutor.shutdownNow();
        }
    }

    @Override
    public void dispose() {
        shutdownRemoteExecutor();
        super.dispose();
    }

    private static Frame resolveOwner(Component parent) {
        java.awt.Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        return window instanceof Frame frame ? frame : null;
    }

    private record LoadedDirectory(String path, List<RemoteDirectoryEntry> entries) {
    }
}
