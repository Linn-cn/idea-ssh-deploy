package com.sshdeploy.deploy.storage;

import com.sshdeploy.deploy.domain.ServerProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployPluginStateServiceTest {
    @Test
    public void upsertAndDeleteServer_shouldWork() {
        DeployPluginStateService service = new DeployPluginStateService();
        ServerProfile profile = new ServerProfile();
        profile.setId("s-1");
        profile.setName("dev-host");
        profile.setHost("10.0.0.1");
        profile.setUsername("root");
        profile.setCredentialRef("cred-1");

        service.upsertServer(profile);
        assertEquals(1, service.getServers().size());
        assertTrue(service.findServerById("s-1").isPresent());

        profile.setHost("10.0.0.2");
        service.upsertServer(profile);
        assertEquals(1, service.getServers().size());
        assertEquals("10.0.0.2", service.findServerById("s-1").get().getHost());

        assertTrue(service.deleteServerById("s-1"));
        assertFalse(service.findServerById("s-1").isPresent());
    }

    @Test
    public void searchServers_shouldMatchByNameAndHost() {
        DeployPluginStateService service = new DeployPluginStateService();

        ServerProfile p1 = new ServerProfile();
        p1.setId("s-1");
        p1.setName("alpha");
        p1.setHost("192.168.1.10");
        p1.setUsername("root");
        p1.setCredentialRef("cred-a");

        ServerProfile p2 = new ServerProfile();
        p2.setId("s-2");
        p2.setName("beta");
        p2.setHost("10.0.0.5");
        p2.setUsername("deploy");
        p2.setCredentialRef("cred-b");

        service.upsertServer(p1);
        service.upsertServer(p2);

        assertEquals(1, service.searchServers("alpha").size());
        assertEquals(1, service.searchServers("10.0.0").size());
        assertEquals(2, service.searchServers("").size());
    }
}
