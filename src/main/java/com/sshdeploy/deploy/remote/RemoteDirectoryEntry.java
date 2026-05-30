package com.sshdeploy.deploy.remote;

/**
 * A directory entry returned from remote SFTP listing.
 */
public final class RemoteDirectoryEntry {
    private final String name;
    private final String path;

    public RemoteDirectoryEntry(String name, String path) {
        this.name = name == null ? "" : name;
        this.path = path == null ? "" : path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }
}
