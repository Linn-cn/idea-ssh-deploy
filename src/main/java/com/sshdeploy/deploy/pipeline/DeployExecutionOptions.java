package com.sshdeploy.deploy.pipeline;

public final class DeployExecutionOptions {
    private final boolean dryRun;
    private final boolean testConnectionOnly;
    private final boolean skipBuild;
    private final boolean skipUpload;
    private final boolean skipBeforeCommands;
    private final boolean skipAfterCommands;
    private final boolean skipTerminalCommand;

    private DeployExecutionOptions(Builder builder) {
        this.dryRun = builder.dryRun;
        this.testConnectionOnly = builder.testConnectionOnly;
        this.skipBuild = builder.skipBuild;
        this.skipUpload = builder.skipUpload;
        this.skipBeforeCommands = builder.skipBeforeCommands;
        this.skipAfterCommands = builder.skipAfterCommands;
        this.skipTerminalCommand = builder.skipTerminalCommand;
    }

    public static DeployExecutionOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isTestConnectionOnly() {
        return testConnectionOnly;
    }

    public boolean isSkipBuild() {
        return skipBuild;
    }

    public boolean isSkipUpload() {
        return skipUpload;
    }

    public boolean isSkipBeforeCommands() {
        return skipBeforeCommands;
    }

    public boolean isSkipAfterCommands() {
        return skipAfterCommands;
    }

    public boolean isSkipTerminalCommand() {
        return skipTerminalCommand;
    }

    public static final class Builder {
        private boolean dryRun;
        private boolean testConnectionOnly;
        private boolean skipBuild;
        private boolean skipUpload;
        private boolean skipBeforeCommands;
        private boolean skipAfterCommands;
        private boolean skipTerminalCommand;

        public Builder dryRun(boolean value) {
            this.dryRun = value;
            return this;
        }

        public Builder testConnectionOnly(boolean value) {
            this.testConnectionOnly = value;
            return this;
        }

        public Builder skipBuild(boolean value) {
            this.skipBuild = value;
            return this;
        }

        public Builder skipUpload(boolean value) {
            this.skipUpload = value;
            return this;
        }

        public Builder skipBeforeCommands(boolean value) {
            this.skipBeforeCommands = value;
            return this;
        }

        public Builder skipAfterCommands(boolean value) {
            this.skipAfterCommands = value;
            return this;
        }

        public Builder skipTerminalCommand(boolean value) {
            this.skipTerminalCommand = value;
            return this;
        }

        public DeployExecutionOptions build() {
            return new DeployExecutionOptions(this);
        }
    }
}
