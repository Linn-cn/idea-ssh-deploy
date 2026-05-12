package com.sshdeploy.deploy.backup;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-serializable snapshot of user-managed deploy configuration (servers, commands, file match rules).
 */
public final class PluginConfigSnapshot {
    /** Schema version for forward-compatible import. */
    public int formatVersion = 1;
    public String exportedAt = "";
    public List<ServerBackupRow> servers = new ArrayList<>();
    public List<CommandBackupRow> commands = new ArrayList<>();
    public List<FileRuleBackupRow> fileMatchRules = new ArrayList<>();

    public static final class ServerBackupRow {
        public String name = "";
        public String host = "";
        public int port = 22;
        public String username = "";
        public String authType = "PASSWORD";
        public List<String> tags = new ArrayList<>();
        public String description = "";
        /**
         * For {@code PASSWORD}: plain password. For {@code PRIVATE_KEY}: {@code keyPath::passphrase} as stored internally.
         * May be null or empty if not available.
         */
        public String credentialSecret = "";
    }

    public static final class CommandBackupRow {
        public String name = "";
        public String content = "";
        public int timeoutSeconds = 60;
        public boolean failFast = true;
        public String executionType = "AFTER";
    }

    public static final class FileRuleBackupRow {
        public String name = "";
        public String pattern = "";
    }
}
