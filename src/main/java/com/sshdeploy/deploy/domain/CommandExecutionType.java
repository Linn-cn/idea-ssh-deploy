package com.sshdeploy.deploy.domain;

import java.util.Locale;

public enum CommandExecutionType {
    GENERAL,
    BEFORE,
    AFTER,
    TERMINAL;

    /**
     * Maps persisted / backup strings to a type; blank or unknown values become {@link #AFTER}.
     */
    public static CommandExecutionType fromPersisted(String raw) {
        if (raw == null || raw.isBlank()) {
            return AFTER;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AFTER;
        }
    }

    public static CommandExecutionType normalize(CommandExecutionType type) {
        return type == null ? AFTER : type;
    }

    /** Slot dropdowns show {@link #GENERAL} plus the slot-specific type. */
    public boolean matchesSlot(CommandExecutionType slot) {
        CommandExecutionType self = normalize(this);
        CommandExecutionType target = normalize(slot);
        return self == GENERAL || self == target;
    }
}
