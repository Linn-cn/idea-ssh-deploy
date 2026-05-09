package com.sshdeploy.deploy.remote;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoteShellCommandTest {
    @Test
    public void withRemoteWorkingDirectory_prependsCd() {
        assertEquals(
                "cd '/home/allin/project' && ./restart.sh",
                RemoteShellCommand.withRemoteWorkingDirectory("/home/allin/project", "./restart.sh"));
    }

    @Test
    public void withRemoteWorkingDirectory_escapesQuotesInPath() {
        assertEquals(
                "cd '/opt/it'\"'\"'s/app' && ls",
                RemoteShellCommand.withRemoteWorkingDirectory("/opt/it's/app", "ls"));
    }

    @Test
    public void withRemoteWorkingDirectory_normalizesBackslashes() {
        assertEquals(
                "cd '/opt/app' && true",
                RemoteShellCommand.withRemoteWorkingDirectory("\\opt\\app", "true"));
    }
}
