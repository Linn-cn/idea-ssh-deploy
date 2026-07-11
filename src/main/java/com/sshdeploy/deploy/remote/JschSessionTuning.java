package com.sshdeploy.deploy.remote;

import com.jcraft.jsch.Session;

import java.util.Properties;

/**
 * Shared SSH transport preferences for throughput (especially LAN uploads).
 */
public final class JschSessionTuning {
    /**
     * Prefer AEAD / lighter ciphers first. Server and client negotiate the first mutual match.
     */
    private static final String PREFERRED_CIPHERS =
            "aes128-gcm@openssh.com,"
                    + "chacha20-poly1305@openssh.com,"
                    + "aes256-gcm@openssh.com,"
                    + "aes128-ctr,aes192-ctr,aes256-ctr";

    private JschSessionTuning() {
    }

    public static void apply(Properties config) {
        if (config == null) {
            return;
        }
        config.put("StrictHostKeyChecking", "no");
        config.put("cipher.s2c", PREFERRED_CIPHERS);
        config.put("cipher.c2s", PREFERRED_CIPHERS);
    }

    public static void apply(Session session) {
        if (session == null) {
            return;
        }
        Properties config = new Properties();
        apply(config);
        session.setConfig(config);
    }
}
