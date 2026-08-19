package com.sshdeploy.deploy.ui.server;

import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.DiskPartition;
import com.sshdeploy.deploy.remote.ServerStatusCollector;
import com.sshdeploy.deploy.remote.ServerStatusSnapshot;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Dialog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Status panel that follows the IDE look and feel.
 */
public final class ServerStatusDialog extends JDialog {
    public ServerStatusDialog(Component parent, ServerProfile profile, ServerStatusSnapshot status) {
        super(SwingUtilities.getWindowAncestor(parent),
                MyMessageBundle.message("server.status.title") + " — " + profile.getName(),
                Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(panelBg());
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(section(MyMessageBundle.message("server.status.section.basic"), basicInfo(status)));
        body.add(Box.createVerticalStrut(18));
        body.add(section(MyMessageBundle.message("server.status.section.resource"), resourceGrid(status)));
        body.add(Box.createVerticalStrut(18));
        body.add(section(MyMessageBundle.message("server.status.section.disks"), diskTable(status.getDisks())));

        JBScrollPane scroll = new JBScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(panelBg());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton close = new JButton(MyMessageBundle.message("server.manager.close"));
        close.addActionListener(e -> dispose());
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        setMinimumSize(new Dimension(JBUI.scale(1020), JBUI.scale(640)));
        setSize(JBUI.scale(1120), JBUI.scale(760));
        setLocationRelativeTo(getOwner());
    }

    private static JPanel section(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading = new JLabel(title);
        heading.setForeground(textColor());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel basicInfo(ServerStatusSnapshot status) {
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        String[][] rows = {
                {MyMessageBundle.message("server.status.online"), onlineText(status),
                        MyMessageBundle.message("server.status.hostname"), dash(status.getHostName())},
                {MyMessageBundle.message("server.status.ip"), dash(status.getIpAddress()),
                        MyMessageBundle.message("server.status.sshUser"), dash(status.getSshUsername())},
                {MyMessageBundle.message("server.status.sshPort"), String.valueOf(status.getSshPort()),
                        MyMessageBundle.message("server.status.os"), dash(status.getOsName())},
                {MyMessageBundle.message("server.status.arch"), dash(status.getArchitecture()),
                        MyMessageBundle.message("server.status.kernel"), dash(status.getKernelVersion())},
                {MyMessageBundle.message("server.status.uptime"), formatUptime(status.getUptimeSeconds()),
                        MyMessageBundle.message("server.status.cpuModel"), dash(status.getCpuModel())},
                {MyMessageBundle.message("server.status.cpuCores"), String.valueOf(status.getCpuCores()),
                        MyMessageBundle.message("server.status.load"), formatLoad(status)}
        };
        for (int r = 0; r < rows.length; r++) {
            addDesc(table, r, 0, rows[r][0], rows[r][1], r == 0);
            addDesc(table, r, 2, rows[r][2], rows[r][3], false);
        }
        return table;
    }

    private void addDesc(JPanel table, int row, int col, String label, String value, boolean onlineTag) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = row;
        gbc.weighty = 0;
        gbc.gridx = col;
        gbc.weightx = 0.18;
        table.add(cell(label, true), gbc);
        gbc.gridx = col + 1;
        gbc.weightx = 0.32;
        if (onlineTag) {
            table.add(onlineCell(value), gbc);
        } else {
            table.add(cell(value, false), gbc);
        }
    }

    private static JComponent cell(String text, boolean label) {
        JLabel field = new JLabel(text);
        field.setForeground(textColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setOpaque(true);
        field.setBackground(label ? headerBg() : panelBg());
        field.setPreferredSize(new Dimension(label ? 160 : 220, 34));
        return field;
    }

    private static JComponent onlineCell(String text) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        wrap.setBackground(panelBg());
        wrap.setBorder(BorderFactory.createLineBorder(borderColor()));
        JLabel tag = new JLabel(text);
        tag.setForeground(usageColor(0));
        tag.setFont(tag.getFont().deriveFont(Font.BOLD));
        wrap.add(tag);
        return wrap;
    }

    private JPanel resourceGrid(ServerStatusSnapshot status) {
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);
        List<DiskPartition> disks = status.getDisks();
        DiskPartition maxDisk = maxDisk(disks);
        BigDecimal avg = averageUsage(disks);
        BigDecimal max = maxDisk == null ? BigDecimal.ZERO.setScale(2) : maxDisk.getUsageRate();
        grid.add(resourceCard(
                MyMessageBundle.message("server.status.cpuUsage"),
                formatPercent(status.getCpuUsage()),
                status.getCpuUsage(),
                dash(status.getCpuModel())));
        grid.add(resourceCard(
                MyMessageBundle.message("server.status.memUsage"),
                formatPercent(status.getMemoryUsage()),
                status.getMemoryUsage(),
                MyMessageBundle.message("server.status.memDetail",
                        ServerStatusCollector.size(status.getMemoryAvailableBytes()),
                        ServerStatusCollector.size(status.getMemoryTotalBytes()))));
        grid.add(resourceCard(
                MyMessageBundle.message("server.status.swapUsage"),
                formatPercent(status.getSwapUsage()),
                status.getSwapUsage(),
                MyMessageBundle.message("server.status.memDetail",
                        ServerStatusCollector.size(status.getSwapFreeBytes()),
                        ServerStatusCollector.size(status.getSwapTotalBytes()))));
        grid.add(resourceCard(
                MyMessageBundle.message("server.status.avgDisk"),
                formatPercent(avg),
                avg,
                MyMessageBundle.message("server.status.diskCount", disks.size())));
        grid.add(resourceCard(
                MyMessageBundle.message("server.status.maxDisk"),
                formatPercent(max),
                max,
                maxDisk == null ? "-" : MyMessageBundle.message("server.status.maxDiskPath", maxDisk.getMountPath())));
        return grid;
    }

    private JPanel resourceCard(String label, String value, BigDecimal percent, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(panelBg());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(mutedColor());
        JLabel pct = new JLabel(value);
        pct.setForeground(textColor());
        pct.setFont(pct.getFont().deriveFont(Font.BOLD));
        header.add(name, BorderLayout.WEST);
        header.add(pct, BorderLayout.EAST);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(8));
        UsageBar bar = new UsageBar(percent == null ? 0 : percent.doubleValue());
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        card.add(bar);
        card.add(Box.createVerticalStrut(8));
        JLabel desc = new JLabel(description);
        desc.setForeground(mutedColor());
        desc.setFont(desc.getFont().deriveFont(12f));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(desc);
        return card;
    }

    private JComponent diskTable(List<DiskPartition> disks) {
        String[] columns = {
                MyMessageBundle.message("server.status.disk.no"),
                MyMessageBundle.message("server.status.disk.fs"),
                MyMessageBundle.message("server.status.disk.mount"),
                MyMessageBundle.message("server.status.disk.total"),
                MyMessageBundle.message("server.status.disk.used"),
                MyMessageBundle.message("server.status.disk.free"),
                MyMessageBundle.message("server.status.disk.usage"),
                MyMessageBundle.message("server.status.disk.inodeTotal"),
                MyMessageBundle.message("server.status.disk.inodeUsed"),
                MyMessageBundle.message("server.status.disk.inodeFree"),
                MyMessageBundle.message("server.status.disk.inodeUsage")
        };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        int i = 1;
        for (DiskPartition disk : disks) {
            model.addRow(new Object[]{
                    i++,
                    disk.getFileSystem(),
                    disk.getMountPath(),
                    disk.getTotalText(),
                    disk.getUsedText(),
                    disk.getAvailableText(),
                    formatPercent(disk.getUsageRate()),
                    formatCount(disk.getInodeTotal()),
                    formatCount(disk.getInodeUsed()),
                    formatCount(disk.getInodeAvailable()),
                    formatPercent(disk.getInodeUsageRate())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setGridColor(borderColor());
        table.setBackground(panelBg());
        table.setForeground(textColor());
        table.setSelectionBackground(selectionBg());
        table.setSelectionForeground(selectionFg());
        table.getTableHeader().setBackground(headerBg());
        table.getTableHeader().setForeground(textColor());
        table.getTableHeader().setReorderingAllowed(false);
        Color even = panelBg();
        Color odd = UIUtil.getDecoratedRowColor();
        DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!selected) {
                    setBackground(row % 2 == 0 ? even : odd);
                    setForeground(textColor());
                }
                return c;
            }
        };
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(center);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(56);
        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 260));
        scroll.setBorder(BorderFactory.createLineBorder(borderColor()));
        scroll.getViewport().setBackground(panelBg());
        return scroll;
    }

    private static DiskPartition maxDisk(List<DiskPartition> disks) {
        DiskPartition max = null;
        for (DiskPartition disk : disks) {
            if (max == null || disk.getUsageRate().compareTo(max.getUsageRate()) > 0) {
                max = disk;
            }
        }
        return max;
    }

    private static BigDecimal averageUsage(List<DiskPartition> disks) {
        if (disks == null || disks.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (DiskPartition disk : disks) {
            sum = sum.add(disk.getUsageRate());
        }
        return sum.divide(BigDecimal.valueOf(disks.size()), 2, RoundingMode.HALF_UP);
    }

    private static String formatPercent(BigDecimal value) {
        if (value == null) {
            return "0.0%";
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String formatLoad(ServerStatusSnapshot status) {
        return status.getLoadAverage1().toPlainString()
                + " / " + status.getLoadAverage5().toPlainString()
                + " / " + status.getLoadAverage15().toPlainString();
    }

    private static String formatCount(long value) {
        return String.format("%,d", value);
    }

    private static String formatUptime(long seconds) {
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        return MyMessageBundle.message("server.status.uptimeFmt", days, hours, minutes);
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String onlineText(ServerStatusSnapshot status) {
        return status.isOnline()
                ? MyMessageBundle.message("server.status.online.yes")
                : MyMessageBundle.message("server.status.online.no");
    }

    private static Color panelBg() {
        return color("Panel.background", JBColor.PanelBackground);
    }

    private static Color headerBg() {
        return color("TableHeader.background", panelBg());
    }

    private static Color textColor() {
        return color("Label.foreground", JBColor.foreground());
    }

    private static Color mutedColor() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : JBColor.GRAY;
    }

    private static Color borderColor() {
        return JBColor.border();
    }

    private static Color selectionBg() {
        return color("Table.selectionBackground", UIUtil.getListSelectionBackground(true));
    }

    private static Color selectionFg() {
        return color("Table.selectionForeground", UIUtil.getListSelectionForeground(true));
    }

    private static Color color(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private static Color usageColor(double percent) {
        if (percent >= 90) {
            return JBColor.RED;
        }
        if (percent >= 70) {
            return JBColor.ORANGE;
        }
        return JBColor.GREEN;
    }

    private static final class UsageBar extends JComponent {
        private final double percent;

        private UsageBar(double percent) {
            this.percent = Math.max(0, Math.min(100, percent));
            setPreferredSize(new Dimension(120, 8));
            setMinimumSize(new Dimension(40, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(borderColor());
            g2.fillRoundRect(0, 0, w, h, 6, 6);
            int fill = (int) Math.round(w * percent / 100.0);
            if (fill > 0) {
                g2.setColor(usageColor(percent));
                g2.fillRoundRect(0, 0, Math.max(fill, 4), h, 6, 6);
            }
            g2.dispose();
        }
    }
}
