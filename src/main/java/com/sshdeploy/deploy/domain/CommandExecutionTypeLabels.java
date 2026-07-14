package com.sshdeploy.deploy.domain;

import com.sshdeploy.MyMessageBundle;

/**
 * Localized labels for {@link CommandExecutionType}.
 */
public final class CommandExecutionTypeLabels {
    private CommandExecutionTypeLabels() {
    }

    public static String label(CommandExecutionType type) {
        CommandExecutionType normalized = CommandExecutionType.normalize(type);
        return switch (normalized) {
            case GENERAL -> MyMessageBundle.message("command.type.general");
            case BEFORE -> MyMessageBundle.message("command.type.before");
            case AFTER -> MyMessageBundle.message("command.type.after");
            case TERMINAL -> MyMessageBundle.message("command.type.terminal");
        };
    }
}
