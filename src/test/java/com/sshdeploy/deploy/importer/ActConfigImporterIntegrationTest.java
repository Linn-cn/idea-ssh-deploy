package com.sshdeploy.deploy.importer;

import com.sshdeploy.deploy.domain.*;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.*;

public class ActConfigImporterIntegrationTest {

    @Test
    public void importFromFile_shouldPersistAndVerify() throws Exception {
        String content = new String(Files.readAllBytes(
                Paths.get("src/test/resources/cloudtoolkit-settings.xml")));

        DeployPluginStateService stateService = new DeployPluginStateService();
        CredentialStore credentialStore = new FixedCredentialStore();

        ActWorkspaceXmlParser parser = new ActWorkspaceXmlParser();
        ImportResult parseResult = parser.parse(content);

        assertFalse("Parse should not have errors", parseResult.hasErrors());
        assertTrue("Should have servers", parseResult.getServers().size() > 0);

        ActConfigImporter importer = new ActConfigImporter(stateService, credentialStore, Collections.singletonList(parser));
        ImportResult importResult = importer.importFromString(content);

        assertFalse("Import should not have errors", importResult.hasErrors());
        assertTrue("Should have servers after import", importResult.getServers().size() > 0);
        assertTrue("Should import ACT commands", importResult.getAfterCommands().size() > 0);

        // Verify persistence (full cloudtoolkit-settings.xml may have empty runConfiguration → no deploy profiles)
        assertEquals("Servers should be persisted", importResult.getServers().size(), stateService.getServers().size());
        assertEquals("Deploy profiles should be persisted", importResult.getDeployProfiles().size(), stateService.getDeployProfiles().size());
        assertFalse("Commands should be persisted", stateService.getCommands().isEmpty());

        for (DeployProfile profile : stateService.getDeployProfiles()) {
            assertNotNull("Profile name", profile.getName());
            assertTrue("Profile serverRef should be set", profile.getServerRef() != null && !profile.getServerRef().isBlank());
            assertTrue("Profile uploadConfigRef should be set", profile.getUploadConfigRef() != null && !profile.getUploadConfigRef().isBlank());
            assertTrue("Server should exist: " + profile.getServerRef(),
                    stateService.findServerById(profile.getServerRef()).isPresent());
            assertTrue("Upload should exist: " + profile.getUploadConfigRef(),
                    stateService.findUploadById(profile.getUploadConfigRef()).isPresent());
        }
    }

    private static final class FixedCredentialStore implements CredentialStore {
        @Override
        public void savePassword(String credentialKey, String username, String password) {
        }

        @Override
        public String readPassword(String credentialKey) {
            return null;
        }

        @Override
        public void delete(String credentialKey) {
        }
    }
}
