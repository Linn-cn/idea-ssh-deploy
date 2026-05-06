package com.sshdeploy.deploy.validation;

import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;

public final class DeployProfileValidator {
    private DeployProfileValidator() {
    }

    public static ValidationResult validateServer(ServerProfile server) {
        ValidationResult result = ValidationResult.ok();
        if (server == null) {
            result.addError("Server profile is required.");
            return result;
        }

        requireNotBlank(server.getName(), "Server name is required.", result);
        requireNotBlank(server.getHost(), "Server host is required.", result);
        requireNotBlank(server.getUsername(), "Server username is required.", result);
        requireNotBlank(server.getCredentialRef(), "Server credential reference is required.", result);

        if (server.getPort() <= 0 || server.getPort() > 65535) {
            result.addError("Server port must be between 1 and 65535.");
        }

        return result;
    }

    public static ValidationResult validateCommand(CommandTemplate commandTemplate) {
        ValidationResult result = ValidationResult.ok();
        if (commandTemplate == null) {
            result.addError("Command template is required.");
            return result;
        }

        requireNotBlank(commandTemplate.getName(), "Command name is required.", result);
        requireNotBlank(commandTemplate.getContent(), "Command content is required.", result);
        if (commandTemplate.getTimeoutSeconds() <= 0) {
            result.addError("Command timeout must be greater than zero.");
        }

        return result;
    }

    public static ValidationResult validateUpload(UploadConfig uploadConfig) {
        ValidationResult result = ValidationResult.ok();
        if (uploadConfig == null) {
            result.addError("Upload config is required.");
            return result;
        }

        requireNotBlank(uploadConfig.getLocalPath(), "Local path is required.", result);
        requireNotBlank(uploadConfig.getRemotePath(), "Remote path is required.", result);
        return result;
    }

    public static ValidationResult validateDeployProfile(DeployProfile deployProfile) {
        ValidationResult result = ValidationResult.ok();
        if (deployProfile == null) {
            result.addError("Deploy profile is required.");
            return result;
        }

        requireNotBlank(deployProfile.getName(), "Deploy profile name is required.", result);
        requireNotBlank(deployProfile.getServerRef(), "Deploy profile server reference is required.", result);
        requireNotBlank(deployProfile.getUploadConfigRef(), "Deploy profile upload config reference is required.", result);
        return result;
    }

    private static void requireNotBlank(String value, String error, ValidationResult result) {
        if (isBlank(value)) {
            result.addError(error);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
