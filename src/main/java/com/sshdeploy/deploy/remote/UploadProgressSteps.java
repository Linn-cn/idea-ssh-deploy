package com.sshdeploy.deploy.remote;

/**
 * Throttles upload progress UI updates to every {@link #PERCENT_STEP} percent (and always 100%).
 */
public final class UploadProgressSteps {
    public static final int PERCENT_STEP = 2;

    private UploadProgressSteps() {
    }

    /**
     * @param lastReportedPercent holder; use {@code -1} before the first update
     * @param percent             current progress 0–100
     * @return {@code true} if this update should be shown; updates {@code lastReportedPercent[0]} when true
     */
    public static boolean shouldReport(int[] lastReportedPercent, int percent) {
        int p = Math.min(100, Math.max(0, percent));
        int last = lastReportedPercent[0];
        if (last >= p) {
            return false;
        }
        if (p < 100 && last >= 0 && p < last + PERCENT_STEP) {
            return false;
        }
        lastReportedPercent[0] = p;
        return true;
    }
}
