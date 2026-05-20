package com.sshdeploy.deploy.ui.common;

import com.sshdeploy.MyMessageBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Multiline command editor ({@link EditorTextField}) and {@code ${fileName}} placeholder insertion,
 * shared by Run configuration and command management dialogs.
 */
public final class CommandEditorSupport {

    private CommandEditorSupport() {
    }

    public static EditorTextField createMultilineField(int rows) {
        EditorTextField field = new EditorTextField();
        field.setOneLineMode(false);
        int lineHeight = 22;
        int height = Math.max(88, rows * lineHeight + 12);
        java.awt.Dimension pref = field.getPreferredSize();
        int minWidth = JBUI.scale(200);
        field.setMinimumSize(new java.awt.Dimension(minWidth, height));
        field.setPreferredSize(new java.awt.Dimension(Math.max(minWidth, pref.width), height));
        field.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, height));
        return field;
    }

    public static JPanel wrapWithPlaceholderButton(EditorTextField field, Component dialogParent) {
        JButton placeholderBtn = createPlaceholderButton(dialogParent, field);
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        panel.add(field, BorderLayout.CENTER);
        panel.add(placeholderBtn, BorderLayout.EAST);
        return panel;
    }

    public static JButton createPlaceholderButton(Component parent, EditorTextField targetField) {
        JButton button = new JButton(AllIcons.Actions.ListFiles);
        button.setToolTipText(MyMessageBundle.message("runconfig.placeholder.button.tooltip"));
        button.addActionListener(e -> openPlaceholderDialog(parent, targetField));
        return button;
    }

    private static void openPlaceholderDialog(Component parent, EditorTextField targetField) {
        PlaceholderItem[] items = new PlaceholderItem[]{
                new PlaceholderItem("${fileName}", MyMessageBundle.message("runconfig.placeholder.fileName.desc"))
        };
        JList<PlaceholderItem> list = new JList<>(items);
        list.setSelectedIndex(0);
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new java.awt.Dimension(JBUI.scale(210), JBUI.scale(90)));
        int result = JOptionPane.showConfirmDialog(
                parent,
                scrollPane,
                MyMessageBundle.message("runconfig.placeholder.dialog.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            PlaceholderItem selected = list.getSelectedValue();
            if (selected != null) {
                insertPlaceholder(targetField, selected.token);
            }
        }
    }

    public static void insertPlaceholder(EditorTextField area, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String content = area.getText();
        int start = content.length();
        int end = start;
        Editor editor = area.getEditor();
        if (editor != null) {
            int selStart = editor.getSelectionModel().getSelectionStart();
            int selEnd = editor.getSelectionModel().getSelectionEnd();
            if (selStart >= 0 && selEnd >= selStart) {
                start = selStart;
                end = selEnd;
            } else {
                start = editor.getCaretModel().getOffset();
                end = start;
            }
        }
        String updated = content.substring(0, start) + token + content.substring(end);
        area.setText(updated);
        Editor updatedEditor = area.getEditor();
        if (updatedEditor != null) {
            updatedEditor.getCaretModel().moveToOffset(start + token.length());
        }
    }

    private static final class PlaceholderItem {
        private final String token;
        private final String description;

        private PlaceholderItem(String token, String description) {
            this.token = token;
            this.description = description;
        }

        @Override
        public String toString() {
            return token + " - " + description;
        }
    }
}
