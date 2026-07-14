package com.sshdeploy.deploy.ui.command;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.CommandExecutionTypeLabels;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.sshdeploy.deploy.ui.common.CommandContentPreview;
import com.sshdeploy.deploy.ui.common.ResizableConfirmDialog;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialog to pick a command template for a deploy-configuration slot.
 */
public final class CommandSelectSupport {
    private static final int PREVIEW_LINES = 4;

    private CommandSelectSupport() {
    }

    public static Optional<CommandTemplate> pickCommand(Component parent,
                                                        DeployPluginStateService stateService,
                                                        CommandExecutionType slot) {
        List<CommandTemplate> candidates = new ArrayList<>();
        for (CommandTemplate command : stateService.getCommands()) {
            CommandExecutionType type = CommandExecutionType.normalize(command.getExecutionType());
            if (type.matchesSlot(slot)) {
                candidates.add(command);
            }
        }
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    MyMessageBundle.message("runconfig.command.select.empty"),
                    MyMessageBundle.message("runconfig.command.select.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return Optional.empty();
        }

        JList<CommandTemplate> list = new JList<>(candidates.toArray(CommandTemplate[]::new));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(Math.min(10, Math.max(6, candidates.size())));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
                if (value instanceof CommandTemplate command) {
                    String name = command.getName() == null ? "" : command.getName();
                    String type = CommandExecutionTypeLabels.label(command.getExecutionType());
                    String content = command.getContent() == null ? "" : command.getContent();
                    String preview = CommandContentPreview.toHtmlBody(content, PREVIEW_LINES);
                    label.setText("<html><b>" + escape(name) + "</b> [" + escape(type) + "]"
                            + (preview.isEmpty() ? "" : "<br/>" + preview) + "</html>");
                    label.setVerticalAlignment(javax.swing.SwingConstants.TOP);
                }
                return label;
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(JBUI.scale(560), JBUI.scale(360)));
        int result = ResizableConfirmDialog.show(
                parent,
                MyMessageBundle.message("runconfig.command.select.title"),
                scrollPane,
                new Dimension(JBUI.scale(640), JBUI.scale(460)),
                new Dimension(JBUI.scale(480), JBUI.scale(320)));
        if (result != ResizableConfirmDialog.OK_OPTION) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getSelectedValue());
    }

    private static String escape(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
