package com.sshdeploy.deploy.remote;

/**
 * Detects transient JSch failures worth retrying (channel races, brief server/network glitches).
 */
public final class JschRetry {
    /** Total attempts including the first try (e.g. 3 means initial + 2 retries). */
    public static final int MAX_ATTEMPTS = 3;
    /** Pause between attempts when a transient failure occurs. */
    public static final long DELAY_MS = 1000L;

    private JschRetry() {
    }

    public static boolean isTransient(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("channel is not opened")) {
                    return true;
                }
                if (m.contains("connection reset")) {
                    return true;
                }
                if (m.contains("broken pipe")) {
                    return true;
                }
                if (m.contains("socket closed")) {
                    return true;
                }
                if (m.contains("timeout") || m.contains("timed out")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }
}
