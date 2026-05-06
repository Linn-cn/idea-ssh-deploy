package com.sshdeploy.deploy.importer;

import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImportResult {
    private final List<ServerProfile> servers = new ArrayList<>();
    private final List<UploadConfig> uploads = new ArrayList<>();
    private final List<CommandTemplate> beforeCommands = new ArrayList<>();
    private final List<CommandTemplate> afterCommands = new ArrayList<>();
    private final List<DeployProfile> deployProfiles = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    /** server id -> secret for password safe */
    private final Map<String, String> serverImportSecrets = new LinkedHashMap<>();
    /** duplicate ACT command id -> canonical ACT command id (same normalized command content) */
    private final Map<String, String> commandIdAliases = new LinkedHashMap<>();

    public List<ServerProfile> getServers() { return servers; }
    public List<UploadConfig> getUploads() { return uploads; }
    public List<CommandTemplate> getBeforeCommands() { return beforeCommands; }
    public List<CommandTemplate> getAfterCommands() { return afterCommands; }
    public List<DeployProfile> getDeployProfiles() { return deployProfiles; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }

    public void addServer(ServerProfile server) { servers.add(server); }
    public void addUploadConfig(UploadConfig upload) { uploads.add(upload); }
    public void addBeforeCommand(CommandTemplate cmd) { beforeCommands.add(cmd); }
    public void addAfterCommand(CommandTemplate cmd) { afterCommands.add(cmd); }
    public void addDeployProfile(DeployProfile profile) { deployProfiles.add(profile); }
    public void addWarning(String msg) { warnings.add(msg); }
    public void addError(String msg) { errors.add(msg); }

    /**
     * For password auth: plain password. For private key: {@code keyPathOrContent + "::" + passphrase} (see {@code CredentialResolver}).
     */
    public void putServerImportSecret(String serverId, String secret) {
        if (serverId != null && !serverId.isBlank()) {
            serverImportSecrets.put(serverId, secret == null ? "" : secret);
        }
    }

    public String getServerImportSecret(String serverId) {
        return serverImportSecrets.get(serverId);
    }

    public void addCommandIdAlias(String duplicateActCommandId, String canonicalActCommandId) {
        if (duplicateActCommandId != null && !duplicateActCommandId.isBlank()
                && canonicalActCommandId != null && !canonicalActCommandId.isBlank()) {
            commandIdAliases.put(duplicateActCommandId.trim(), canonicalActCommandId.trim());
        }
    }

    public Map<String, String> getCommandIdAliases() {
        return commandIdAliases;
    }
}
