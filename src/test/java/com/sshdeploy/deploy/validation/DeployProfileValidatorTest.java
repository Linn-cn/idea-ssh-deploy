package com.sshdeploy.deploy.validation;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployProfileValidatorTest {
    @Test
    public void validateServer_shouldFail_whenRequiredFieldsMissing() {
        ServerProfile serverProfile = new ServerProfile();
        serverProfile.setName("");
        serverProfile.setHost("");
        serverProfile.setUsername("");
        serverProfile.setCredentialRef("");
        serverProfile.setPort(0);
        serverProfile.setAuthType(AuthType.PASSWORD);

        ValidationResult result = DeployProfileValidator.validateServer(serverProfile);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 5);
    }

    @Test
    public void validateDeployProfile_shouldPass_whenCoreReferencesPresent() {
        DeployProfile deployProfile = new DeployProfile();
        deployProfile.setName("dev-deploy");
        deployProfile.setServerRef("server-1");
        deployProfile.setUploadConfigRef("upload-1");

        ValidationResult result = DeployProfileValidator.validateDeployProfile(deployProfile);

        assertTrue(result.isValid());
    }
}
