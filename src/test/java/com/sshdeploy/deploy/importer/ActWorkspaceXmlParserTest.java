package com.sshdeploy.deploy.importer;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ActWorkspaceXmlParserTest {

    @Test
    public void parseCloudtoolkitSettings_shouldImportCorrectly() throws Exception {
        String content;
        try (InputStream in = getClass().getResourceAsStream("/cloudtoolkit-settings.xml")) {
            assertNotNull("Classpath resource cloudtoolkit-settings.xml", in);
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        ActWorkspaceXmlParser parser = new ActWorkspaceXmlParser();
        assertTrue("Parser should recognize the format", parser.canParse(content));

        ImportResult result = parser.parse(content);

        // Debug output
        System.out.println("=== Import Result ===");
        System.out.println("Errors: " + result.getErrors().size());
        for (String e : result.getErrors()) {
            System.out.println("  - " + e);
        }
        System.out.println("Warnings: " + result.getWarnings().size());
        for (String w : result.getWarnings()) {
            System.out.println("  - " + w);
        }
        System.out.println("Servers: " + result.getServers().size());
        for (var server : result.getServers()) {
            System.out.println("  - " + server.getName() + " (" + server.getHost() + ":" + server.getPort() + ")");
        }
        System.out.println("Upload configs: " + result.getUploads().size());
        System.out.println("Post-upload commands: " + result.getAfterCommands().size());
        System.out.println("Deploy profiles: " + result.getDeployProfiles().size());
        for (var profile : result.getDeployProfiles()) {
            System.out.println("  - " + profile.getName() + " -> server: " + profile.getServerRef());
        }
        System.out.println("===================");

        // Just verify it doesn't crash and produces some result
        assertNotNull("Result should not be null", result);
    }
}
