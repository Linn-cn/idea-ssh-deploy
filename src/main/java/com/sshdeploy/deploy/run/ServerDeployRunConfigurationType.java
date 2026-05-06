package com.sshdeploy.deploy.run;

import com.sshdeploy.MyMessageBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.sshdeploy.PluginIcons;
import org.jetbrains.annotations.NotNull;

public final class ServerDeployRunConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "ServerDeployRunConfiguration";

    public ServerDeployRunConfigurationType() {
        super(ID, MyMessageBundle.message("runconfig.name"), MyMessageBundle.message("runconfig.description"), PluginIcons.SSH_DEPLOY);
        addFactory(new ServerDeployRunConfigurationFactory(this));
    }

    public static ServerDeployRunConfigurationType getInstance() {
        for (ConfigurationType type : ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList()) {
            if (type instanceof ServerDeployRunConfigurationType typed) {
                return typed;
            }
        }
        throw new IllegalStateException("ServerDeployRunConfigurationType is not registered");
    }

    private static final class ServerDeployRunConfigurationFactory extends ConfigurationFactory {
        protected ServerDeployRunConfigurationFactory(@NotNull ConfigurationType type) {
            super(type);
        }

        @Override
        public @NotNull String getId() {
            return ID + "Factory";
        }

        @Override
        public @NotNull ServerDeployRunConfiguration createTemplateConfiguration(@NotNull com.intellij.openapi.project.Project project) {
            return new ServerDeployRunConfiguration(project, this, MyMessageBundle.message("runconfig.name"));
        }
    }
}
