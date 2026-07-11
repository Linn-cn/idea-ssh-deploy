package com.sshdeploy.deploy.ui.common;

import com.google.gson.JsonSyntaxException;
import com.sshdeploy.MyMessageBundle;
import com.sshdeploy.deploy.backup.ConfigSection;
import com.sshdeploy.deploy.backup.PluginConfigBackupService;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.security.PasswordSafeCredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Shared file-chooser + import/export wiring for tool-window section config I/O.
 */
public final class ConfigSectionIoSupport {
    private ConfigSectionIoSupport() {
    }

    public static void exportSection(@NotNull Component parent,
                                     @NotNull ConfigSection section,
                                     @NotNull DeployPluginStateService stateService,
                                     @Nullable CredentialStore credentialStore) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(MyMessageBundle.message(exportTitleKey(section)));
        chooser.setSelectedFile(new File(defaultFileName(section)));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file.getName().indexOf('.') < 0) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            CredentialStore store = credentialStore != null ? credentialStore : new PasswordSafeCredentialStore();
            String json = PluginConfigBackupService.exportSectionToJson(section, stateService, store);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Messages.showInfoMessage(parent,
                    MyMessageBundle.message("config.section.export.success", file.getAbsolutePath()),
                    MyMessageBundle.message(exportTitleKey(section)));
        } catch (IOException ex) {
            Messages.showErrorDialog(parent,
                    MyMessageBundle.message("settings.backup.error.writeFailed", ex.getMessage()),
                    MyMessageBundle.message(exportTitleKey(section)));
        }
    }

    public static void importSection(@NotNull Component parent,
                                     @NotNull ConfigSection section,
                                     @NotNull DeployPluginStateService stateService,
                                     @Nullable CredentialStore credentialStore,
                                     @Nullable Runnable afterSuccess) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(MyMessageBundle.message(importTitleKey(section)));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            CredentialStore store = credentialStore != null ? credentialStore : new PasswordSafeCredentialStore();
            PluginConfigBackupService.ImportResult result =
                    PluginConfigBackupService.importSectionFromJson(section, json, stateService, store);
            ApplicationManager.getApplication().saveSettings();
            Messages.showInfoMessage(parent, formatImportSuccess(section, result),
                    MyMessageBundle.message(importTitleKey(section)));
            if (afterSuccess != null) {
                afterSuccess.run();
            }
        } catch (JsonSyntaxException ex) {
            Messages.showErrorDialog(parent,
                    MyMessageBundle.message("settings.backup.error.invalidJson", ex.getMessage()),
                    MyMessageBundle.message(importTitleKey(section)));
        } catch (IOException ex) {
            Messages.showErrorDialog(parent,
                    MyMessageBundle.message("settings.backup.error.readFailed", ex.getMessage()),
                    MyMessageBundle.message(importTitleKey(section)));
        }
    }

    private static @NotNull String formatImportSuccess(@NotNull ConfigSection section,
                                                       @NotNull PluginConfigBackupService.ImportResult result) {
        return switch (section) {
            case SERVERS -> MyMessageBundle.message(
                    "config.section.import.success.servers", result.serversAdded(), result.serversSkipped());
            case COMMANDS -> MyMessageBundle.message(
                    "config.section.import.success.commands", result.commandsAdded(), result.commandsSkipped());
            case FILE_MATCH_RULES -> MyMessageBundle.message(
                    "config.section.import.success.rules", result.rulesAdded(), result.rulesSkipped());
        };
    }

    private static @NotNull String defaultFileName(@NotNull ConfigSection section) {
        return switch (section) {
            case SERVERS -> "ssh-deploy-servers.json";
            case COMMANDS -> "ssh-deploy-commands.json";
            case FILE_MATCH_RULES -> "ssh-deploy-file-match-rules.json";
        };
    }

    private static @NotNull String exportTitleKey(@NotNull ConfigSection section) {
        return switch (section) {
            case SERVERS -> "server.manager.export.title";
            case COMMANDS -> "command.manager.export.title";
            case FILE_MATCH_RULES -> "fileMatch.manager.export.title";
        };
    }

    private static @NotNull String importTitleKey(@NotNull ConfigSection section) {
        return switch (section) {
            case SERVERS -> "server.manager.import.title";
            case COMMANDS -> "command.manager.import.title";
            case FILE_MATCH_RULES -> "fileMatch.manager.import.title";
        };
    }
}
