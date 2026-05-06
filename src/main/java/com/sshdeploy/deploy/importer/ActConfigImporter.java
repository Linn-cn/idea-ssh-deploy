package com.sshdeploy.deploy.importer;

import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;
import com.sshdeploy.deploy.security.CredentialRefManager;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ActConfigImporter {

    private final DeployPluginStateService stateService;
    private final CredentialStore credentialStore;
    private final List<ActConfigParser> parsers;

    public ActConfigImporter(DeployPluginStateService stateService,
                             CredentialStore credentialStore,
                             List<ActConfigParser> parsers) {
        this.stateService = stateService;
        this.credentialStore = credentialStore;
        this.parsers = parsers;
    }

    public ImportResult importFromFile(File configFile) {
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            return importFromString(content);
        } catch (Exception e) {
            ImportResult result = new ImportResult();
            result.addError("Failed to read file: " + e.getMessage());
            return result;
        }
    }

    public ImportResult importFromString(String content) {
        for (ActConfigParser parser : parsers) {
            if (parser.canParse(content)) {
                ImportResult result = parser.parse(content);
                if (result != null && !result.hasErrors()) {
                    persistImport(result);
                }
                return result;
            }
        }

        ImportResult result = new ImportResult();
        result.addError("No parser found for the provided configuration format.");
        return result;
    }

    private void persistImport(ImportResult result) {
        Map<String, String> serverIdMapping = persistServers(result.getServers(), result);
        Map<String, String> commandIdMapping = persistCommands(result);
        Map<String, String> uploadIdMapping = persistUploads(result.getUploads());
        persistDeployProfiles(result.getDeployProfiles(), serverIdMapping, uploadIdMapping, commandIdMapping);
    }

    private Map<String, String> persistServers(List<ServerProfile> servers, ImportResult importResult) {
        CredentialRefManager refManager = new CredentialRefManager();
        Map<String, String> idMap = new HashMap<>();
        for (ServerProfile server : servers) {
            Optional<ServerProfile> existing = findExistingServerByEndpoint(server);
            if (existing.isPresent()) {
                idMap.put(server.getId(), existing.get().getId());
                continue;
            }
            String credRef = refManager.createServerCredentialRef(server.getId());
            server.setCredentialRef(credRef);
            String secret = importResult.getServerImportSecret(server.getId());
            if (secret != null && !secret.isBlank()) {
                credentialStore.savePassword(credRef, server.getUsername(), secret);
            }
            String oldId = server.getId();
            stateService.upsertServer(server);
            idMap.put(oldId, server.getId());
        }
        return idMap;
    }

    private Optional<ServerProfile> findExistingServerByEndpoint(ServerProfile candidate) {
        String h = normalizeHost(candidate.getHost());
        int p = candidate.getPort();
        for (ServerProfile existing : stateService.getServers()) {
            if (existing.getPort() == p && normalizeHost(existing.getHost()).equals(h)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    private static String normalizeHost(String host) {
        return host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, String> persistCommands(ImportResult result) {
        Map<String, String> idMap = new HashMap<>();
        for (CommandTemplate cmd : result.getBeforeCommands()) {
            persistOneCommand(cmd, idMap);
        }
        for (CommandTemplate cmd : result.getAfterCommands()) {
            persistOneCommand(cmd, idMap);
        }
        for (Map.Entry<String, String> e : result.getCommandIdAliases().entrySet()) {
            String resolved = idMap.get(e.getValue());
            if (resolved != null) {
                idMap.put(e.getKey(), resolved);
            }
        }
        return idMap;
    }

    private void persistOneCommand(CommandTemplate cmd, Map<String, String> idMap) {
        String oldId = cmd.getId();
        Optional<CommandTemplate> existingByContent = findExistingCommandByNormalizedContent(cmd.getContent());
        if (existingByContent.isPresent()) {
            idMap.put(oldId, existingByContent.get().getId());
            return;
        }
        stateService.upsertCommand(cmd);
        idMap.put(oldId, cmd.getId());
    }

    private Optional<CommandTemplate> findExistingCommandByNormalizedContent(String content) {
        String norm = normalizeCommandContent(content);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        for (CommandTemplate existing : stateService.getCommands()) {
            if (normalizeCommandContent(existing.getContent()).equals(norm)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    private static String normalizeCommandContent(String text) {
        return text == null ? "" : text.trim();
    }

    private Map<String, String> persistUploads(List<UploadConfig> uploads) {
        java.util.Map<String, String> idMap = new java.util.HashMap<>();
        for (UploadConfig upload : uploads) {
            String oldId = upload.getId();
            stateService.upsertUpload(upload);
            idMap.put(oldId, upload.getId());
        }
        return idMap;
    }

    private void persistDeployProfiles(List<DeployProfile> deployProfiles,
                                       Map<String, String> serverIdMapping,
                                       Map<String, String> uploadIdMapping,
                                       Map<String, String> commandIdMapping) {
        for (DeployProfile profile : deployProfiles) {
            String newServerId = serverIdMapping.getOrDefault(profile.getServerRef(), profile.getServerRef());
            String newUploadId = uploadIdMapping.getOrDefault(profile.getUploadConfigRef(), profile.getUploadConfigRef());

            profile.setServerRef(newServerId);
            profile.setUploadConfigRef(newUploadId);

            List<String> newBeforeRefs = new java.util.ArrayList<>();
            for (String oldRef : profile.getBeforeCommandRefs()) {
                newBeforeRefs.add(commandIdMapping.getOrDefault(oldRef, oldRef));
            }
            profile.setBeforeCommandRefs(newBeforeRefs);

            List<String> newAfterRefs = new java.util.ArrayList<>();
            for (String oldRef : profile.getAfterCommandRefs()) {
                newAfterRefs.add(commandIdMapping.getOrDefault(oldRef, oldRef));
            }
            profile.setAfterCommandRefs(newAfterRefs);

            stateService.upsertDeployProfile(profile);
        }
    }
}
