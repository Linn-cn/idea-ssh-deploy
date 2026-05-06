package com.sshdeploy.deploy.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "ServerDeployPluginSettings", storages = @Storage("server-deploy-settings.xml"))
public final class PluginSettingsService implements PersistentStateComponent<PluginSettingsService.StateBean> {
    private StateBean state = new StateBean();
    public enum LanguageMode {
        FOLLOW_IDE,
        ZH_CN,
        EN_US
    }

    public static PluginSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettingsService.class);
    }

    @Override
    public @Nullable StateBean getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateBean state) {
        this.state = state;
    }

    public int getDefaultCommandTimeoutSeconds() {
        return state.defaultCommandTimeoutSeconds;
    }

    public void setDefaultCommandTimeoutSeconds(int seconds) {
        state.defaultCommandTimeoutSeconds = Math.max(1, seconds);
    }

    public String getDefaultEncoding() {
        return state.defaultEncoding;
    }

    public void setDefaultEncoding(String encoding) {
        state.defaultEncoding = (encoding == null || encoding.isBlank()) ? "UTF-8" : encoding.trim();
    }

    public LanguageMode getLanguageMode() {
        try {
            return LanguageMode.valueOf(state.languageMode);
        } catch (Exception ignore) {
            return LanguageMode.FOLLOW_IDE;
        }
    }

    public void setLanguageMode(LanguageMode mode) {
        state.languageMode = (mode == null ? LanguageMode.FOLLOW_IDE : mode).name();
    }

    public static final class StateBean {
        public int defaultCommandTimeoutSeconds = 60;
        public String defaultEncoding = "UTF-8";
        public String languageMode = LanguageMode.FOLLOW_IDE.name();
    }
}
