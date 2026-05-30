package com.sshdeploy.deploy.pipeline;

import com.sshdeploy.deploy.domain.CommandTemplate;
import com.sshdeploy.deploy.domain.DeployProfile;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.domain.UploadConfig;
import com.sshdeploy.deploy.remote.RemoteClient;
import com.sshdeploy.deploy.remote.RemoteClientFactory;
import com.sshdeploy.deploy.remote.RemoteCommandResult;
import com.sshdeploy.deploy.remote.RemoteConnectRequest;
import com.sshdeploy.deploy.security.CredentialStore;
import com.sshdeploy.deploy.storage.DeployPluginStateService;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployOrchestratorTest {
    @Test
    public void execute_withTestConnectionOnly_shouldCallTestConnectionAndEmitLogs() {
        DeployPluginStateService stateService = new DeployPluginStateService();
        setupFixture(stateService);

        FakeRemoteClient fakeRemoteClient = new FakeRemoteClient();
        DeployOrchestrator orchestrator = new DeployOrchestrator(
                stateService,
                new FixedCredentialStore("pwd"),
                new CredentialResolver(),
                () -> fakeRemoteClient
        );

        List<DeployLogEvent> events = new ArrayList<>();
        DeployExecutionResult result = orchestrator.execute(
                "profile-1",
                ".",
                DeployExecutionOptions.builder().testConnectionOnly(true).build(),
                events::add
        );

        assertTrue(result.isSuccess());
        assertEquals(0, fakeRemoteClient.connectCount);
        assertEquals(1, fakeRemoteClient.testConnectionCount);
        assertFalse(events.isEmpty());
    }

    @Test
    public void execute_withDryRun_shouldSkipRemoteOpsAfterConnect() {
        DeployPluginStateService stateService = new DeployPluginStateService();
        setupFixture(stateService);

        FakeRemoteClient fakeRemoteClient = new FakeRemoteClient();
        DeployOrchestrator orchestrator = new DeployOrchestrator(
                stateService,
                new FixedCredentialStore("pwd"),
                new CredentialResolver(),
                new FixedFactory(fakeRemoteClient)
        );

        DeployExecutionResult result = orchestrator.execute(
                "profile-1",
                ".",
                DeployExecutionOptions.builder().dryRun(true).build()
        );

        assertTrue(result.isSuccess());
        assertEquals(1, fakeRemoteClient.connectCount);
        assertEquals(0, fakeRemoteClient.uploadFileCount);
        assertEquals(0, fakeRemoteClient.uploadDirectoryCount);
        assertEquals(0, fakeRemoteClient.executeCount);
    }

    private void setupFixture(DeployPluginStateService stateService) {
        ServerProfile server = new ServerProfile();
        server.setId("server-1");
        server.setName("dev");
        server.setHost("127.0.0.1");
        server.setPort(22);
        server.setUsername("root");
        server.setCredentialRef("cred-1");
        stateService.upsertServer(server);

        UploadConfig upload = new UploadConfig();
        upload.setId("upload-1");
        upload.setDirectory(false);
        upload.setLocalPath("build.gradle.kts");
        upload.setRemotePath("/tmp");
        stateService.upsertUpload(upload);

        CommandTemplate command = new CommandTemplate();
        command.setId("cmd-1");
        command.setName("echo");
        command.setContent("echo ok");
        stateService.upsertCommand(command);

        DeployProfile profile = new DeployProfile();
        profile.setId("profile-1");
        profile.setName("p1");
        profile.setServerRef("server-1");
        profile.setUploadConfigRef("upload-1");
        profile.getAfterCommandRefs().add("cmd-1");
        stateService.upsertDeployProfile(profile);
    }

    private static final class FixedFactory implements RemoteClientFactory {
        private final RemoteClient client;

        private FixedFactory(RemoteClient client) {
            this.client = client;
        }

        @Override
        public RemoteClient create() {
            return client;
        }
    }

    private static final class FixedCredentialStore implements CredentialStore {
        private final String value;

        private FixedCredentialStore(String value) {
            this.value = value;
        }

        @Override
        public void savePassword(String credentialKey, String username, String password) {
        }

        @Override
        public String readPassword(String credentialKey) {
            return value;
        }

        @Override
        public void delete(String credentialKey) {
        }
    }

    private static final class FakeRemoteClient implements RemoteClient {
        private int connectCount;
        private int testConnectionCount;
        private int uploadFileCount;
        private int uploadDirectoryCount;
        private int executeCount;

        @Override
        public void connect(RemoteConnectRequest request) {
            connectCount++;
        }

        @Override
        public void testConnection(RemoteConnectRequest request) {
            testConnectionCount++;
        }

        @Override
        public void uploadFile(java.io.File localFile, String remotePath) {
            uploadFileCount++;
        }

        @Override
        public void uploadDirectory(java.io.File localDirectory, String remoteDirectory, List<String> filters) {
            uploadDirectoryCount++;
        }

        @Override
        public List<com.sshdeploy.deploy.remote.RemoteDirectoryEntry> listDirectory(String remotePath) {
            return List.of();
        }

        @Override
        public String resolveHomeDirectory() {
            return "/home/user";
        }

        @Override
        public RemoteCommandResult execute(String command, int timeoutSeconds) {
            executeCount++;
            return new RemoteCommandResult(0, "ok", "");
        }

        @Override
        public void close() {
        }
    }
}
