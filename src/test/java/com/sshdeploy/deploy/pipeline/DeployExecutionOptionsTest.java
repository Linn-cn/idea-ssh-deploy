package com.sshdeploy.deploy.pipeline;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployExecutionOptionsTest {
    @Test
    public void defaults_shouldDisableAllSkips() {
        DeployExecutionOptions options = DeployExecutionOptions.defaults();
        assertFalse(options.isDryRun());
        assertFalse(options.isTestConnectionOnly());
        assertFalse(options.isSkipBuild());
        assertFalse(options.isSkipUpload());
    }

    @Test
    public void builder_shouldApplyFlags() {
        DeployExecutionOptions options = DeployExecutionOptions.builder()
                .dryRun(true)
                .testConnectionOnly(true)
                .skipBuild(true)
                .skipUpload(true)
                .skipBeforeCommands(true)
                .skipAfterCommands(true)
                .skipTerminalCommand(true)
                .build();

        assertTrue(options.isDryRun());
        assertTrue(options.isTestConnectionOnly());
        assertTrue(options.isSkipBuild());
        assertTrue(options.isSkipUpload());
        assertTrue(options.isSkipBeforeCommands());
        assertTrue(options.isSkipAfterCommands());
        assertTrue(options.isSkipTerminalCommand());
    }
}
