package com.sshdeploy.deploy.storage;

import com.sshdeploy.deploy.domain.BuiltinFileMatchRules;
import com.sshdeploy.deploy.domain.CommandExecutionType;
import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.FileMatchRule;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service(Service.Level.APP)
@State(name = "ServerDeployState", storages = @Storage("server-deploy.xml"))
public final class DeployPluginStateService implements PersistentStateComponent<DeployPluginStateService.State> {
    private State state = new State();

    public static DeployPluginStateService getInstance() {
        return ApplicationManager.getApplication().getService(DeployPluginStateService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        if (this.state.fileMatchRules == null) {
            this.state.fileMatchRules = new ArrayList<>();
        }
        if (this.state.commands == null) {
            this.state.commands = new ArrayList<>();
        }
        for (CommandTemplate command : this.state.commands) {
            if (command != null) {
                command.setExecutionType(CommandExecutionType.normalize(command.getExecutionType()));
            }
        }
    }

    public List<ServerProfile> getServers() {
        return state.servers;
    }

    public Optional<ServerProfile> findServerById(String id) {
        return findById(state.servers, ServerProfile::getId, id);
    }

    public void upsertServer(@NotNull ServerProfile serverProfile) {
        upsert(state.servers, ServerProfile::getId, ServerProfile::setId, serverProfile);
    }

    public boolean deleteServerById(String id) {
        return deleteById(state.servers, ServerProfile::getId, id);
    }

    public List<ServerProfile> searchServers(String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isEmpty()) {
            return new ArrayList<>(state.servers);
        }

        return state.servers.stream()
                .filter(server -> contains(server.getName(), normalized)
                        || contains(server.getHost(), normalized)
                        || contains(server.getUsername(), normalized))
                .sorted(Comparator.comparing(ServerProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<CommandTemplate> getCommands() {
        return state.commands;
    }

    public Optional<CommandTemplate> findCommandById(String id) {
        return findById(state.commands, CommandTemplate::getId, id);
    }

    public void upsertCommand(@NotNull CommandTemplate commandTemplate) {
        upsert(state.commands, CommandTemplate::getId, CommandTemplate::setId, commandTemplate);
    }

    public boolean deleteCommandById(String id) {
        return deleteById(state.commands, CommandTemplate::getId, id);
    }

    public List<FileMatchRule> getUserFileMatchRules() {
        return state.fileMatchRules;
    }

    /**
     * Built-in rules first (fixed order), then user rules sorted by name (case-insensitive).
     */
    public List<FileMatchRule> getAllFileMatchRulesForDisplay() {
        List<FileMatchRule> result = new ArrayList<>(BuiltinFileMatchRules.builtinRules());
        List<FileMatchRule> users = new ArrayList<>(state.fileMatchRules);
        users.sort(Comparator.comparing(FileMatchRule::getName, String.CASE_INSENSITIVE_ORDER));
        result.addAll(users);
        return result;
    }

    public Optional<FileMatchRule> findUserFileMatchRuleById(String id) {
        return findById(state.fileMatchRules, FileMatchRule::getId, id);
    }

    public void upsertUserFileMatchRule(@NotNull FileMatchRule rule) {
        if (BuiltinFileMatchRules.isBuiltinId(rule.getId())) {
            throw new IllegalArgumentException("Cannot persist built-in rule id.");
        }
        upsert(state.fileMatchRules, FileMatchRule::getId, FileMatchRule::setId, rule);
    }

    public boolean deleteUserFileMatchRuleById(String id) {
        if (BuiltinFileMatchRules.isBuiltinId(id)) {
            return false;
        }
        return deleteById(state.fileMatchRules, FileMatchRule::getId, id);
    }

    public List<UploadConfig> getUploads() {
        return state.uploads;
    }

    public Optional<UploadConfig> findUploadById(String id) {
        return findById(state.uploads, UploadConfig::getId, id);
    }

    public void upsertUpload(@NotNull UploadConfig uploadConfig) {
        upsert(state.uploads, UploadConfig::getId, UploadConfig::setId, uploadConfig);
    }

    public boolean deleteUploadById(String id) {
        return deleteById(state.uploads, UploadConfig::getId, id);
    }

    public List<DeployProfile> getDeployProfiles() {
        return state.deployProfiles;
    }

    public Optional<DeployProfile> findDeployProfileById(String id) {
        return findById(state.deployProfiles, DeployProfile::getId, id);
    }

    public void upsertDeployProfile(@NotNull DeployProfile deployProfile) {
        upsert(state.deployProfiles, DeployProfile::getId, DeployProfile::setId, deployProfile);
    }

    public boolean deleteDeployProfileById(String id) {
        return deleteById(state.deployProfiles, DeployProfile::getId, id);
    }

    public void clearAll() {
        state = new State();
    }

    static <T> Optional<T> findById(List<T> source, Function<T, String> idGetter, String id) {
        if (isBlank(id)) {
            return Optional.empty();
        }
        return source.stream().filter(item -> Objects.equals(idGetter.apply(item), id)).findFirst();
    }

    static <T> void upsert(List<T> source,
                           Function<T, String> idGetter,
                           BiConsumer<T, String> idSetter,
                           T value) {
        String id = idGetter.apply(value);
        if (isBlank(id)) {
            throw new IllegalArgumentException("Entity id must not be blank.");
        }

        for (int i = 0; i < source.size(); i++) {
            if (Objects.equals(idGetter.apply(source.get(i)), id)) {
                source.set(i, value);
                return;
            }
        }

        idSetter.accept(value, id);
        source.add(value);
    }

    static <T> boolean deleteById(List<T> source, Function<T, String> idGetter, String id) {
        return source.removeIf(item -> Objects.equals(idGetter.apply(item), id));
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class State {
        public List<ServerProfile> servers = new ArrayList<>();
        public List<CommandTemplate> commands = new ArrayList<>();
        public List<FileMatchRule> fileMatchRules = new ArrayList<>();
        public List<UploadConfig> uploads = new ArrayList<>();
        public List<DeployProfile> deployProfiles = new ArrayList<>();
    }
}
