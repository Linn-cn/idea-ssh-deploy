package com.sshdeploy.deploy.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.BuiltinFileMatchRules;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.FileMatchRule;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.security.CredentialRefManager;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PluginConfigBackupService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final CredentialRefManager CRED_REF_MANAGER = new CredentialRefManager();

    private PluginConfigBackupService() {
    }

    public record ImportResult(
            int serversAdded,
            int serversSkipped,
            int commandsAdded,
            int commandsSkipped,
            int rulesAdded,
            int rulesSkipped
    ) {
    }

    public static @NotNull String exportToJson(@NotNull DeployPluginStateService stateService,
                                               @NotNull CredentialStore credentialStore) {
        PluginConfigSnapshot snap = newEmptySnapshot();
        fillServers(snap, stateService, credentialStore);
        fillCommands(snap, stateService);
        fillFileMatchRules(snap, stateService);
        return GSON.toJson(snap);
    }

    /**
     * Exports only the given section; other lists in the snapshot are empty.
     */
    public static @NotNull String exportSectionToJson(@NotNull ConfigSection section,
                                                      @NotNull DeployPluginStateService stateService,
                                                      @NotNull CredentialStore credentialStore) {
        PluginConfigSnapshot snap = newEmptySnapshot();
        switch (section) {
            case SERVERS -> fillServers(snap, stateService, credentialStore);
            case COMMANDS -> fillCommands(snap, stateService);
            case FILE_MATCH_RULES -> fillFileMatchRules(snap, stateService);
        }
        return GSON.toJson(snap);
    }

    public static @NotNull ImportResult importFromJson(@NotNull String json,
                                                       @NotNull DeployPluginStateService stateService,
                                                       @NotNull CredentialStore credentialStore) throws JsonSyntaxException {
        PluginConfigSnapshot snap = parseSnapshot(json);
        int sa = 0, ss = 0, ca = 0, cs = 0, ra = 0, rs = 0;
        int[] serverCounts = mergeServers(snap.servers, stateService, credentialStore);
        sa = serverCounts[0];
        ss = serverCounts[1];
        int[] commandCounts = mergeCommands(snap.commands, stateService);
        ca = commandCounts[0];
        cs = commandCounts[1];
        int[] ruleCounts = mergeFileMatchRules(snap.fileMatchRules, stateService);
        ra = ruleCounts[0];
        rs = ruleCounts[1];
        return new ImportResult(sa, ss, ca, cs, ra, rs);
    }

    /**
     * Imports only the given section from JSON. Other sections in a full backup are ignored.
     */
    public static @NotNull ImportResult importSectionFromJson(@NotNull ConfigSection section,
                                                              @NotNull String json,
                                                              @NotNull DeployPluginStateService stateService,
                                                              @NotNull CredentialStore credentialStore) throws JsonSyntaxException {
        PluginConfigSnapshot snap = parseSnapshot(json);
        return switch (section) {
            case SERVERS -> {
                int[] c = mergeServers(snap.servers, stateService, credentialStore);
                yield new ImportResult(c[0], c[1], 0, 0, 0, 0);
            }
            case COMMANDS -> {
                int[] c = mergeCommands(snap.commands, stateService);
                yield new ImportResult(0, 0, c[0], c[1], 0, 0);
            }
            case FILE_MATCH_RULES -> {
                int[] c = mergeFileMatchRules(snap.fileMatchRules, stateService);
                yield new ImportResult(0, 0, 0, 0, c[0], c[1]);
            }
        };
    }

    private static @NotNull PluginConfigSnapshot newEmptySnapshot() {
        PluginConfigSnapshot snap = new PluginConfigSnapshot();
        snap.exportedAt = Instant.now().toString();
        return snap;
    }

    private static @NotNull PluginConfigSnapshot parseSnapshot(@NotNull String json) throws JsonSyntaxException {
        PluginConfigSnapshot snap = GSON.fromJson(json, PluginConfigSnapshot.class);
        if (snap == null) {
            throw new JsonSyntaxException("empty document");
        }
        if (snap.formatVersion < 1) {
            throw new JsonSyntaxException("unsupported formatVersion");
        }
        if (snap.servers == null) {
            snap.servers = new ArrayList<>();
        }
        if (snap.commands == null) {
            snap.commands = new ArrayList<>();
        }
        if (snap.fileMatchRules == null) {
            snap.fileMatchRules = new ArrayList<>();
        }
        return snap;
    }

    private static void fillServers(@NotNull PluginConfigSnapshot snap,
                                    @NotNull DeployPluginStateService stateService,
                                    @NotNull CredentialStore credentialStore) {
        for (ServerProfile s : stateService.getServers()) {
            PluginConfigSnapshot.ServerBackupRow row = new PluginConfigSnapshot.ServerBackupRow();
            row.name = nullToEmpty(s.getName());
            row.host = nullToEmpty(s.getHost());
            row.port = s.getPort();
            row.username = nullToEmpty(s.getUsername());
            row.authType = s.getAuthType() == null ? AuthType.PASSWORD.name() : s.getAuthType().name();
            row.tags = s.getTags() == null ? new ArrayList<>() : new ArrayList<>(s.getTags());
            row.description = nullToEmpty(s.getDescription());
            if (s.getCredentialRef() != null && !s.getCredentialRef().isBlank()) {
                String secret = credentialStore.readPassword(s.getCredentialRef());
                row.credentialSecret = secret == null ? "" : secret;
            } else {
                row.credentialSecret = "";
            }
            snap.servers.add(row);
        }
    }

    private static void fillCommands(@NotNull PluginConfigSnapshot snap,
                                     @NotNull DeployPluginStateService stateService) {
        for (CommandTemplate c : stateService.getCommands()) {
            PluginConfigSnapshot.CommandBackupRow row = new PluginConfigSnapshot.CommandBackupRow();
            row.name = nullToEmpty(c.getName());
            row.content = nullToEmpty(c.getContent());
            row.timeoutSeconds = c.getTimeoutSeconds();
            row.failFast = c.isFailFast();
            row.executionType = c.getExecutionType() == null ? CommandExecutionType.AFTER.name() : c.getExecutionType().name();
            snap.commands.add(row);
        }
    }

    private static void fillFileMatchRules(@NotNull PluginConfigSnapshot snap,
                                           @NotNull DeployPluginStateService stateService) {
        for (FileMatchRule r : stateService.getUserFileMatchRules()) {
            if (BuiltinFileMatchRules.isBuiltinId(r.getId())) {
                continue;
            }
            PluginConfigSnapshot.FileRuleBackupRow row = new PluginConfigSnapshot.FileRuleBackupRow();
            row.name = nullToEmpty(r.getName());
            row.pattern = nullToEmpty(r.getPattern());
            snap.fileMatchRules.add(row);
        }
    }

    /** @return {@code [added, skipped]} */
    private static int @NotNull [] mergeServers(@NotNull List<PluginConfigSnapshot.ServerBackupRow> rows,
                                                @NotNull DeployPluginStateService stateService,
                                                @NotNull CredentialStore credentialStore) {
        int added = 0;
        int skipped = 0;
        for (PluginConfigSnapshot.ServerBackupRow row : rows) {
            if (row == null) {
                continue;
            }
            if (isBlank(row.name) || isBlank(row.host)) {
                skipped++;
                continue;
            }
            if (serverDuplicate(stateService, row.name, row.host, row.port, row.username)) {
                skipped++;
                continue;
            }
            ServerProfile profile = new ServerProfile();
            profile.setId(UUID.randomUUID().toString());
            profile.setName(row.name.trim());
            profile.setHost(row.host.trim());
            profile.setPort(row.port > 0 && row.port <= 65535 ? row.port : 22);
            profile.setUsername(row.username == null ? "" : row.username.trim());
            profile.setDescription(row.description == null ? "" : row.description.trim());
            profile.setTags(row.tags == null ? new ArrayList<>() : new ArrayList<>(row.tags));
            AuthType auth = parseAuthType(row.authType);
            profile.setAuthType(auth);
            profile.setCredentialRef(CRED_REF_MANAGER.createServerCredentialRef(profile.getId()));
            stateService.upsertServer(profile);
            String secret = row.credentialSecret == null ? "" : row.credentialSecret;
            if (!secret.isBlank()) {
                credentialStore.savePassword(profile.getCredentialRef(), profile.getUsername(), secret);
            }
            added++;
        }
        return new int[]{added, skipped};
    }

    /** @return {@code [added, skipped]} */
    private static int @NotNull [] mergeCommands(@NotNull List<PluginConfigSnapshot.CommandBackupRow> rows,
                                                 @NotNull DeployPluginStateService stateService) {
        int added = 0;
        int skipped = 0;
        for (PluginConfigSnapshot.CommandBackupRow row : rows) {
            if (row == null) {
                continue;
            }
            String name = row.name == null ? "" : row.name.trim();
            String content = row.content == null ? "" : row.content.trim();
            if (name.isBlank() || content.isBlank()) {
                skipped++;
                continue;
            }
            if (commandNameExists(stateService, name)) {
                skipped++;
                continue;
            }
            CommandTemplate cmd = new CommandTemplate();
            cmd.setId(UUID.randomUUID().toString());
            cmd.setName(name);
            cmd.setContent(content);
            cmd.setTimeoutSeconds(row.timeoutSeconds > 0 ? row.timeoutSeconds : 60);
            cmd.setFailFast(row.failFast);
            cmd.setExecutionType(parseExecutionType(row.executionType));
            stateService.upsertCommand(cmd);
            added++;
        }
        return new int[]{added, skipped};
    }

    /** @return {@code [added, skipped]} */
    private static int @NotNull [] mergeFileMatchRules(@NotNull List<PluginConfigSnapshot.FileRuleBackupRow> rows,
                                                       @NotNull DeployPluginStateService stateService) {
        int added = 0;
        int skipped = 0;
        for (PluginConfigSnapshot.FileRuleBackupRow row : rows) {
            if (row == null) {
                continue;
            }
            String name = row.name == null ? "" : row.name.trim();
            String pattern = row.pattern == null ? "" : row.pattern.trim();
            if (name.isBlank() || pattern.isBlank()) {
                skipped++;
                continue;
            }
            if (fileRuleNameExists(stateService, name)) {
                skipped++;
                continue;
            }
            FileMatchRule rule = new FileMatchRule();
            rule.setId(UUID.randomUUID().toString());
            rule.setName(name);
            rule.setPattern(pattern);
            stateService.upsertUserFileMatchRule(rule);
            added++;
        }
        return new int[]{added, skipped};
    }

    private static boolean serverDuplicate(DeployPluginStateService state,
                                           String name,
                                           String host,
                                           int port,
                                           String username) {
        String h = host.trim().toLowerCase(Locale.ROOT);
        String u = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        for (ServerProfile existing : state.getServers()) {
            if (existing.getName() != null && existing.getName().trim().equalsIgnoreCase(name.trim())) {
                return true;
            }
            String eh = existing.getHost() == null ? "" : existing.getHost().trim().toLowerCase(Locale.ROOT);
            String eu = existing.getUsername() == null ? "" : existing.getUsername().trim().toLowerCase(Locale.ROOT);
            if (eh.equals(h) && existing.getPort() == port && eu.equals(u)) {
                return true;
            }
        }
        return false;
    }

    private static boolean commandNameExists(DeployPluginStateService state, String name) {
        for (CommandTemplate c : state.getCommands()) {
            if (c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fileRuleNameExists(DeployPluginStateService state, String name) {
        for (FileMatchRule r : state.getUserFileMatchRules()) {
            if (r.getName() != null && r.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static AuthType parseAuthType(String raw) {
        if (raw == null || raw.isBlank()) {
            return AuthType.PASSWORD;
        }
        try {
            return AuthType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AuthType.PASSWORD;
        }
    }

    private static CommandExecutionType parseExecutionType(String raw) {
        return CommandExecutionType.fromPersisted(raw);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
