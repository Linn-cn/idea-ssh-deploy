package com.sshdeploy.deploy.remote;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UploadProgressStepsTest {
    @Test
    public void reportsEveryTwoPercentAndAlwaysHundred() {
        int[] last = new int[]{-1};
        assertTrue(UploadProgressSteps.shouldReport(last, 0));
        assertEquals(0, last[0]);
        assertFalse(UploadProgressSteps.shouldReport(last, 1));
        assertTrue(UploadProgressSteps.shouldReport(last, 2));
        assertEquals(2, last[0]);
        assertFalse(UploadProgressSteps.shouldReport(last, 3));
        assertTrue(UploadProgressSteps.shouldReport(last, 4));
        assertTrue(UploadProgressSteps.shouldReport(last, 100));
        assertEquals(100, last[0]);
        assertFalse(UploadProgressSteps.shouldReport(last, 100));
    }
}
