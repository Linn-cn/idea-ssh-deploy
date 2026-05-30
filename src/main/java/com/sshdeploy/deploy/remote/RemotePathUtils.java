package com.sshdeploy.deploy.remote;

/**
 * Normalizes POSIX-style remote paths for SFTP navigation.
 */
public final class RemotePathUtils {

    private RemotePathUtils() {
    }

    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "/" : normalized;
    }

    public static String parent(String path) {
        String normalized = normalize(path);
        if ("/".equals(normalized)) {
            return "/";
        }
        int idx = normalized.lastIndexOf('/');
        if (idx <= 0) {
            return "/";
        }
        return normalized.substring(0, idx);
    }

    public static String child(String parentPath, String name) {
        String parent = normalize(parentPath);
        if (name == null || name.isBlank()) {
            return parent;
        }
        if ("/".equals(parent)) {
            return "/" + name;
        }
        return parent + "/" + name;
    }

    public static String expandHome(String path, String homeDirectory) {
        if (path == null || path.isBlank()) {
            return normalize(homeDirectory);
        }
        String trimmed = path.trim().replace('\\', '/');
        if ("~".equals(trimmed)) {
            return normalize(homeDirectory);
        }
        if (trimmed.startsWith("~/")) {
            String home = normalize(homeDirectory);
            if ("/".equals(home)) {
                return normalize(trimmed.substring(1));
            }
            return normalize(home + trimmed.substring(1));
        }
        return normalize(trimmed);
    }
}
