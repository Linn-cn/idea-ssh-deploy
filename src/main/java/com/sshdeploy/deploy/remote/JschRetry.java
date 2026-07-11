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

    /**
     * JSch leaves exit status at {@code -1} when the channel closes before an exit-status packet arrives.
     * That is almost always a transport glitch, not a real shell exit code (0–255).
     */
    public static boolean isUnsetExitStatus(int exitStatus) {
        return exitStatus < 0;
    }

    public static boolean isTransient(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && isTransientMessage(msg)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    public static boolean isTransientMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase();
        if (m.contains("channel is not opened") || m.contains("channel is not open")) {
            return true;
        }
        if (m.contains("channel is closed") || m.contains("channel closed") || m.contains("channel close")) {
            return true;
        }
        if (m.contains("session is down") || m.contains("session is closed") || m.contains("session closed")) {
            return true;
        }
        if (m.contains("connection reset")) {
            return true;
        }
        if (m.contains("broken pipe")) {
            return true;
        }
        if (m.contains("socket closed") || m.contains("socket is closed")) {
            return true;
        }
        if (m.contains("connection is closed") || m.contains("connection closed")) {
            return true;
        }
        if (m.contains("foreign host")) {
            return true;
        }
        if (m.contains("end of io stream") || m.contains("pipe closed")) {
            return true;
        }
        if (m.contains("timeout") || m.contains("timed out")) {
            return true;
        }
        return false;
    }
}
