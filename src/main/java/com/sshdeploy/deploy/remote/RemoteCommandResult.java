package com.sshdeploy.deploy.remote;

public final class RemoteCommandResult {
    private final int exitCode;
    private final String stdOut;
    private final String stdErr;

    public RemoteCommandResult(int exitCode, String stdOut, String stdErr) {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }
}
