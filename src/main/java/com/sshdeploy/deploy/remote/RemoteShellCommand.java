package com.sshdeploy.deploy.remote;

import org.jetbrains.annotations.Nullable;

/**
 * Helpers for running shell commands over non-interactive SSH ({@code exec} channel).
 * Remote commands do not start in the deploy directory; prefix with {@code cd} so scripts
 * that rely on relative paths behave like an interactive shell opened in that folder.
 */
public final class RemoteShellCommand {
    private RemoteShellCommand() {
    }

    /**
     * Single-quote a string for POSIX shells ({@code '…'} with embedded {@code '} escaped).
     */
    public static String shellSingleQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    /**
     * Runs {@code command} after changing to {@code remoteDir}, if non-blank.
     */
    public static String withRemoteWorkingDirectory(@Nullable String remoteDir, String command) {
        if (remoteDir == null || remoteDir.isBlank()) {
            return command;
        }
        if (command == null || command.isBlank()) {
            return command;
        }
        String normalized = remoteDir.trim().replace('\\', '/');
        return "cd " + shellSingleQuote(normalized) + " && " + command.trim();
    }
}
