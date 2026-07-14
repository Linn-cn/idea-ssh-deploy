package com.sshdeploy.deploy.ui.common;

import com.sshdeploy.MyMessageBundle;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modal confirm dialog that is resizable (unlike {@link javax.swing.JOptionPane}).
 */
public final class ResizableConfirmDialog {
    public static final int OK_OPTION = 0;
    public static final int CANCEL_OPTION = 1;

    private ResizableConfirmDialog() {
    }

    public static int show(Component parent,
                           String title,
                           JComponent content,
                           Dimension preferredSize,
                           Dimension minimumSize) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);

        AtomicInteger result = new AtomicInteger(CANCEL_OPTION);
        JPanel root = new JPanel(new BorderLayout(0, JBUI.scale(10)));
        root.setBorder(JBUI.Borders.empty(12));
        root.add(content, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
        JButton okBtn = new JButton(MyMessageBundle.message("common.dialog.ok"));
        JButton cancelBtn = new JButton(MyMessageBundle.message("common.dialog.cancel"));
        buttons.add(okBtn);
        buttons.add(cancelBtn);
        root.add(buttons, BorderLayout.SOUTH);

        okBtn.addActionListener(e -> {
            result.set(OK_OPTION);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(okBtn);

        dialog.setContentPane(root);
        if (minimumSize != null) {
            dialog.setMinimumSize(minimumSize);
        }
        if (preferredSize != null) {
            dialog.setPreferredSize(preferredSize);
            dialog.setSize(preferredSize);
        } else {
            dialog.pack();
        }
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result.get();
    }
}
