package com.sshdeploy.deploy.ui;

import com.sshdeploy.deploy.pipeline.DeployLogEvent;
import com.sshdeploy.deploy.pipeline.DeployLogLevel;
import com.sshdeploy.deploy.pipeline.DeployStage;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.scale.JBUIScale;

import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

public final class DeployLogConsole extends JBPanel<DeployLogConsole> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea textArea;

    public DeployLogConsole() {
        this.textArea = new JTextArea();
        textArea.setEditable(false);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            textArea.setFont(baseFont.deriveFont(JBUIScale.scale(12f)));
        }
        init();
    }

    private void init() {
        setLayout(new java.awt.BorderLayout());
        add(textArea, java.awt.BorderLayout.CENTER);
    }

    public void clear() {
        textArea.setText("");
    }

    public void appendLog(DeployLogEvent event) {
        appendLog(event.getStage(), event.getLevel(), event.getMessage());
    }

    public void appendLog(DeployStage stage, DeployLogLevel level, String message) {
        String timestamp = TIME_FORMATTER.format(java.time.LocalDateTime.now());
        String prefix = String.format("[%s] [%s] [%s] ", timestamp, level.name(), stage.name());

        Document document = textArea.getDocument();
        try {
            document.insertString(document.getLength(), prefix, null);
            document.insertString(document.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            // ignore
        }
    }

    public JTextArea getComponent() {
        return textArea;
    }
}
