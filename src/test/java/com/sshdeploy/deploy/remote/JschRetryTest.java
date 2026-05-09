package com.sshdeploy.deploy.remote;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JschRetryTest {
    @Test
    public void isTransient_recognizesChannelNotOpened() {
        assertTrue(JschRetry.isTransient(new Exception("channel is not opened.")));
    }

    @Test
    public void isTransient_chainsCauses() {
        assertTrue(JschRetry.isTransient(new RuntimeException("wrap", new Exception("Connection reset by peer"))));
    }

    @Test
    public void isTransient_rejectsUnrelated() {
        assertFalse(JschRetry.isTransient(new Exception("Permission denied (publickey).")));
    }
}
