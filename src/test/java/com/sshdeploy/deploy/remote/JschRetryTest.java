package com.sshdeploy.deploy.remote;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JschRetryTest {
    @Test
    public void isTransient_recognizesChannelNotOpened() {
        assertTrue(JschRetry.isTransient(new Exception("channel is not opened.")));
        assertTrue(JschRetry.isTransient(new Exception("channel is not open")));
    }

    @Test
    public void isTransient_recognizesChannelClosedVariants() {
        assertTrue(JschRetry.isTransient(new Exception("channel is closed.")));
        assertTrue(JschRetry.isTransient(new Exception("Channel closed")));
        assertTrue(JschRetry.isTransient(new Exception("channel close")));
        assertTrue(JschRetry.isTransient(new Exception("session is down")));
        assertTrue(JschRetry.isTransient(new Exception("session is closed")));
    }

    @Test
    public void isTransient_recognizesUnsetExitStatusMessage() {
        assertTrue(JschRetry.isTransient(
                new IllegalStateException("Remote channel closed before exit status was received (exit=-1)")));
    }

    @Test
    public void isUnsetExitStatus() {
        assertTrue(JschRetry.isUnsetExitStatus(-1));
        assertFalse(JschRetry.isUnsetExitStatus(0));
        assertFalse(JschRetry.isUnsetExitStatus(1));
        assertFalse(JschRetry.isUnsetExitStatus(255));
    }

    @Test
    public void isTransient_chainsCauses() {
        assertTrue(JschRetry.isTransient(new RuntimeException("wrap", new Exception("Connection reset by peer"))));
    }

    @Test
    public void isTransient_rejectsUnrelated() {
        assertFalse(JschRetry.isTransient(new Exception("Permission denied (publickey).")));
        assertFalse(JschRetry.isTransient(new Exception("Command failed, exit code 1")));
    }
}
