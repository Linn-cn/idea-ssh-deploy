package com.sshdeploy.deploy.ui.common;

/**
 * Formats multi-line command text for HTML table cells and combo box previews.
 */
public final class CommandContentPreview {

    private CommandContentPreview() {
    }

    public static int displayLineCount(String raw, int maxLines) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        return Math.min(maxLines, normalizeLines(raw).length);
    }

    public static String toHtml(String raw, int maxLines) {
        String body = toHtmlBody(raw, maxLines);
        if (body.isEmpty()) {
            return "";
        }
        return "<html><body style='margin:0;padding:0'>" + body + "</body></html>";
    }

    public static String toHtmlBody(String raw, int maxLines) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] lines = normalizeLines(raw);
        boolean truncated = lines.length > maxLines;
        int show = Math.min(maxLines, lines.length);
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < show; i++) {
            if (i > 0) {
                html.append("<br>");
            }
            String line = escapeHtml(lines[i]);
            if (truncated && i == show - 1) {
                line = line + " …";
            }
            html.append(line);
        }
        return html.toString();
    }

    private static String[] normalizeLines(String raw) {
        return raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
