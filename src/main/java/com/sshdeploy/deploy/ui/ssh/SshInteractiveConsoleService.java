package com.sshdeploy.deploy.ui.ssh;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.Disposable;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
public final class SshInteractiveConsoleService implements Disposable {
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int CMD_POLL_MILLIS = 100;

    private volatile Session session;

    public void disconnect() {
        try {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void dispose() {
        disconnect();
    }

    public void deployAndOpenShell(ServerProfile target,
                                     RemoteCredentials targetCredentials,
                                     File uploadSource,
                                     String remoteDir,
                                     List<String> preCommands,
                                     List<String> postCommands,
                                     Consumer<Integer> uploadProgressCallback) throws Exception {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetCredentials, "targetCredentials");
        Objects.requireNonNull(uploadSource, "uploadSource");
        Objects.requireNonNull(remoteDir, "remoteDir");

        disconnect();

        JSch jsch = new JSch();

        session = createSession(jsch, target, targetCredentials, target.getHost(), target.getPort());
        session.connect(CONNECT_TIMEOUT_MILLIS);

        // 1) 预置：remote 目录存在性检查
        execAndFailOnError("test -d " + shellQuote(remoteDir), null);

        // 2) pre commands
        if (preCommands != null) {
            for (String cmd : preCommands) {
                if (cmd == null || cmd.isBlank()) continue;
                execAndFailOnError(cmd, null);
            }
        }

        // 3) upload
        uploadFileViaSftp(uploadSource, remoteDir, uploadProgressCallback);

        // 4) post commands
        if (postCommands != null) {
            for (String cmd : postCommands) {
                if (cmd == null || cmd.isBlank()) continue;
                execAndFailOnError(cmd, null);
            }
        }
    }

    private Session createSession(JSch jsch,
                                   ServerProfile profile,
                                   @Nullable RemoteCredentials credentials,
                                   String host,
                                   int port) throws JSchException {
        if (profile.getAuthType() == AuthType.PRIVATE_KEY) {
            if (credentials == null || isBlank(credentials.getPrivateKeyPath())) {
                throw new IllegalArgumentException("Private key path is required for private key auth.");
            }
            if (isBlank(credentials.getPrivateKeyPassphrase())) {
                jsch.addIdentity(credentials.getPrivateKeyPath());
            } else {
                jsch.addIdentity(credentials.getPrivateKeyPath(), credentials.getPrivateKeyPassphrase());
            }
        }

        Session s = jsch.getSession(profile.getUsername(), host, port);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        s.setConfig(config);
        s.setServerAliveInterval(5_000);

        if (profile.getAuthType() == AuthType.PASSWORD) {
            if (credentials == null || isBlank(credentials.getPassword())) {
                throw new IllegalArgumentException("Password is required for password auth.");
            }
            s.setPassword(credentials.getPassword());
        }
        return s;
    }

    private void execAndFailOnError(String command, @Nullable Consumer<String> outputConsumer) throws Exception {
        String cmd = command.trim();
        if (cmd.isEmpty()) return;

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channel.setCommand(cmd);
        channel.setInputStream(null);
        channel.setOutputStream(out);
        channel.setErrStream(err);

        channel.connect(CONNECT_TIMEOUT_MILLIS);

        while (!channel.isClosed()) {
            Thread.sleep(CMD_POLL_MILLIS);
        }

        int exit = channel.getExitStatus();
        String stdout = out.toString(StandardCharsets.UTF_8);
        String stderr = err.toString(StandardCharsets.UTF_8);
        if (!stdout.isBlank()) {
            if (outputConsumer != null) {
                outputConsumer.accept(stdout.trim());
            }
        }
        if (!stderr.isBlank()) {
            if (outputConsumer != null) {
                outputConsumer.accept(stderr.trim());
            }
        }

        channel.disconnect();

        if (exit != 0) {
            throw new IllegalStateException("Remote command failed: " + exit + ", cmd=" + cmd);
        }
    }

    private void uploadFileViaSftp(File localFile, String remoteDir, Consumer<Integer> uploadProgressCallback) throws Exception {
        if (!localFile.exists()) {
            throw new IllegalStateException("Local upload source not found: " + localFile.getAbsolutePath());
        }
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect(CONNECT_TIMEOUT_MILLIS);
        try {
            ensureRemoteDirectory(sftp, remoteDir);

            String remoteFile = remoteDir.endsWith("/") ? remoteDir + localFile.getName() : remoteDir + "/" + localFile.getName();

            int[] last = new int[]{-1};
            // JSch's SftpProgressMonitor doesn't guarantee stable 'total' semantics.
            // We still aim for 1% granularity by logging when percent increases.
            SftpProgressMonitor monitor2 = new SftpProgressMonitor() {
                private long transferred;
                private long total;

                @Override
                public void init(int op, String src, String dest, long max) {
                    transferred = 0;
                    total = max;
                    if (uploadProgressCallback != null) {
                        uploadProgressCallback.accept(0);
                    }
                }

                @Override
                public boolean count(long count) {
                    transferred += count;
                    long t = total <= 0 ? transferred : total;
                    int percent = (int) Math.min(100, (transferred * 100L) / t);
                    if (percent > last[0]) {
                        last[0] = percent;
                        if (uploadProgressCallback != null) {
                            uploadProgressCallback.accept(percent);
                        }
                    }
                    return true;
                }

                @Override
                public void end() {
                    if (uploadProgressCallback != null) {
                        uploadProgressCallback.accept(100);
                    }
                }
            };

            sftp.put(localFile.getAbsolutePath(), remoteFile, monitor2, ChannelSftp.OVERWRITE);
        } finally {
            sftp.disconnect();
        }
    }

    private static void ensureRemoteDirectory(ChannelSftp sftp, String remotePath) throws SftpException {
        if (remotePath == null || remotePath.isBlank()) return;
        String normalized = remotePath.replace("\\", "/");
        String[] segments = normalized.split("/");
        StringBuilder path = new StringBuilder();
        if (normalized.startsWith("/")) {
            path.append("/");
        }
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) continue;
            if (path.length() > 1 && path.charAt(path.length() - 1) != '/') {
                path.append('/');
            }
            path.append(segment);
            try {
                sftp.stat(path.toString());
            } catch (SftpException ex) {
                sftp.mkdir(path.toString());
            }
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

