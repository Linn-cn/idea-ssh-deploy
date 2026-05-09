package com.sshdeploy.deploy.ui.common;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Renders the boolean “select” column header as a master checkbox and toggles all visible rows
 * on click (select all when not all checked, clear when all checked).
 */
public final class MasterCheckboxColumnHeaderSupport {
    private MasterCheckboxColumnHeaderSupport() {
    }

    /**
     * @param table               target table; column 0 must be {@link Boolean} for the given model column
     * @param modelColumnIndex    model index of the checkbox column (usually 0)
     * @param delegateHeaderRenderer platform header renderer (e.g. {@link JTableHeader#getDefaultRenderer()})
     * @param tooltipText         shown when the pointer is over that header cell
     */
    public static void install(JTable table,
                               int modelColumnIndex,
                               TableCellRenderer delegateHeaderRenderer,
                               String tooltipText) {
        JTableHeader header = table.getTableHeader();
        int viewIndex = table.convertColumnIndexToView(modelColumnIndex);
        var column = table.getColumnModel().getColumn(viewIndex);

        column.setHeaderRenderer(new TableCellRenderer() {
            private final JPanel panel = new JPanel(new BorderLayout());
            private final JCheckBox box = new JCheckBox();

            {
                panel.setOpaque(true);
                box.setHorizontalAlignment(SwingConstants.CENTER);
                box.setEnabled(false);
                box.setFocusable(false);
                panel.add(box, BorderLayout.CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                TableModel model = table.getModel();
                int n = model.getRowCount();
                boolean all = n > 0 && allRowsChecked(model, modelColumnIndex);
                box.setSelected(all);
                Component ref = delegateHeaderRenderer.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                Color bg = header.getBackground();
                if (ref != null && ref.getBackground() != null) {
                    bg = ref.getBackground();
                }
                panel.setBackground(bg);
                box.setBackground(bg);
                if (ref instanceof JComponent jc) {
                    panel.setBorder(jc.getBorder());
                }
                return panel;
            }
        });

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int viewCol = header.columnAtPoint(e.getPoint());
                if (viewCol < 0) {
                    return;
                }
                if (table.convertColumnIndexToModel(viewCol) != modelColumnIndex) {
                    return;
                }
                toggleBooleanColumn(table.getModel(), modelColumnIndex);
                table.repaint();
                header.repaint();
            }
        });

        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int viewCol = header.columnAtPoint(e.getPoint());
                if (viewCol >= 0 && table.convertColumnIndexToModel(viewCol) == modelColumnIndex) {
                    header.setToolTipText(tooltipText);
                } else {
                    header.setToolTipText(null);
                }
            }
        });

        table.getModel().addTableModelListener(e -> {
            int c = e.getColumn();
            if (c != TableModelEvent.ALL_COLUMNS && c != modelColumnIndex) {
                return;
            }
            header.repaint();
        });
    }

    private static boolean allRowsChecked(TableModel model, int modelColumnIndex) {
        for (int r = 0; r < model.getRowCount(); r++) {
            if (!Boolean.TRUE.equals(model.getValueAt(r, modelColumnIndex))) {
                return false;
            }
        }
        return true;
    }

    private static void toggleBooleanColumn(TableModel model, int modelColumnIndex) {
        int n = model.getRowCount();
        if (n == 0) {
            return;
        }
        boolean all = allRowsChecked(model, modelColumnIndex);
        Boolean next = all ? Boolean.FALSE : Boolean.TRUE;
        for (int r = 0; r < n; r++) {
            model.setValueAt(next, r, modelColumnIndex);
        }
    }
}
