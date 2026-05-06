package com.sshdeploy.deploy.security;

import java.util.Objects;
import java.util.UUID;

public final class CredentialRefManager {
    private static final String PREFIX = "server";

    public String createServerCredentialRef(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("serverId must not be blank.");
        }
        return PREFIX + ":" + serverId + ":" + UUID.randomUUID();
    }

    public String createServerCredentialRef(String serverId, String suffix) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("serverId must not be blank.");
        }
        if (suffix == null || suffix.isBlank()) {
            throw new IllegalArgumentException("suffix must not be blank.");
        }
        return PREFIX + ":" + serverId + ":" + suffix.trim();
    }

    public boolean isServerCredentialRef(String credentialRef) {
        return credentialRef != null && credentialRef.startsWith(PREFIX + ":");
    }

    public String serverIdFromRef(String credentialRef) {
        if (!isServerCredentialRef(credentialRef)) {
            return "";
        }
        String[] parts = credentialRef.split(":", 3);
        return parts.length >= 2 ? Objects.toString(parts[1], "") : "";
    }
}
