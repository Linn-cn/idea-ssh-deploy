package com.sshdeploy.deploy.ui.common;

/**
 * HTML helpers for combo box list previews (title + gray detail).
 */
public final class ComboPreviewHtml {

    private ComboPreviewHtml() {
    }

    public static String titledPreview(String title, String detailHtml) {
        String safeTitle = escape(title == null ? "" : title);
        if (detailHtml == null || detailHtml.isBlank()) {
            return "<html><body style='margin:2px 4px;padding:0'><b>" + safeTitle + "</b></body></html>";
        }
        return "<html><body style='margin:2px 4px;padding:0'><b>"
                + safeTitle
                + "</b><br><span style='color:#888888'>"
                + detailHtml
                + "</span></body></html>";
    }

    public static String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
