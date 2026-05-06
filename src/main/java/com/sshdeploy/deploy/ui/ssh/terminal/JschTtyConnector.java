package com.sshdeploy.deploy.ui.ssh.terminal;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.sshdeploy.deploy.remote.RemoteCredentials;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.Session;
import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TtyConnector;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class JschTtyConnector implements TtyConnector {
    private static final Logger LOG = Logger.getInstance(JschTtyConnector.class);
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;

    private final ServerProfile target;
    private final RemoteCredentials targetCredentials;
    private final String workingDirectory;

    private Session targetSession;
    private ChannelShell shell;
    private InputStream inputStream;
    private InputStreamReader reader;
    private OutputStream outputStream;
    private final AtomicBoolean initiated = new AtomicBoolean(false);
    private final AtomicBoolean automationCancelled = new AtomicBoolean(false);

    private final Object commandLock = new Object();
    private String waitingMarker;
    private Integer waitingExitCode;
    private StringBuilder markerBuffer = new StringBuilder();

    private Dimension pendingTermSize;
    private Dimension pendingPixelSize;
    private String name = "SSH Deploy";
    private volatile String initErrorMessage;

    public JschTtyConnector(ServerProfile target,
                            RemoteCredentials targetCredentials,
                            @Nullable String workingDirectory) {
        this.target = target;
        this.targetCredentials = targetCredentials;
        this.workingDirectory = workingDirectory;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public boolean init(Questioner questioner) {
        if (initiated.get()) {
            return isConnected();
        }
        try {
            JSch jsch = new JSch();

            targetSession = createSession(jsch, target, targetCredentials, target.getHost(), target.getPort());
            targetSession.connect(CONNECT_TIMEOUT_MILLIS);

            shell = (ChannelShell) targetSession.openChannel("shell");
            shell.setPty(true);
            shell.connect(CONNECT_TIMEOUT_MILLIS);
            inputStream = shell.getInputStream();
            outputStream = shell.getOutputStream();
            reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            resizeImmediately();
            if (workingDirectory != null && !workingDirectory.isBlank()) {
                write(("cd " + escapePath(workingDirectory) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            initiated.set(true);
            return true;
        } catch (Exception ex) {
            LOG.warn("SSH terminal init failed", ex);
            initErrorMessage = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            initiated.set(true);
            close();
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (shell != null && shell.isConnected()) shell.disconnect();
        } catch (Exception ignored) {
        }
        try {
            if (targetSession != null && targetSession.isConnected()) targetSession.disconnect();
        } catch (Exception ignored) {
        }
        shell = null;
        targetSession = null;
    }

    public void awaitInitiated(long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!initiated.get()) {
            if (System.currentTimeMillis() - start > timeoutMillis) {
                throw new IllegalStateException("SSH terminal initialization timeout");
            }
            Thread.sleep(50L);
        }
        if (!isConnected()) {
            String msg = initErrorMessage == null ? "SSH terminal is not connected" : "SSH terminal connect failed: " + initErrorMessage;
            throw new IllegalStateException(msg);
        }
    }

    public void resetAutomationCancel() {
        automationCancelled.set(false);
    }

    public boolean isAutomationCancelled() {
        return automationCancelled.get();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        while (!initiated.get()) {
            try {
                Thread.sleep(30L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting terminal init", e);
            }
        }
        if (reader == null) {
            if (initErrorMessage != null) {
                throw new IOException("Terminal init failed: " + initErrorMessage);
            }
            return 0;
        }
        int n = reader.read(buf, offset, length);
        if (n > 0) {
            inspectForCommandMarker(buf, offset, n);
        }
        return n;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        for (byte b : bytes) {
            if (b == 3) { // Ctrl + C
                automationCancelled.set(true);
                break;
            }
        }
        if (outputStream != null) {
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    @Override
    public boolean isConnected() {
        return shell != null && shell.isConnected();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int waitFor() throws InterruptedException {
        while (!initiated.get() || isConnected()) {
            Thread.sleep(100L);
        }
        return 0;
    }

    @Override
    public boolean ready() throws IOException {
        return reader != null && reader.ready();
    }

    @Override
    public void resize(@NotNull Dimension termSize, @NotNull Dimension pixelSize) {
        pendingTermSize = termSize;
        pendingPixelSize = pixelSize;
        resizeImmediately();
    }

    private void resizeImmediately() {
        if (shell == null || pendingTermSize == null || pendingPixelSize == null) {
            return;
        }
        shell.setPtySize(
                pendingTermSize.width,
                pendingTermSize.height,
                pendingPixelSize.width,
                pendingPixelSize.height
        );
        pendingTermSize = null;
        pendingPixelSize = null;
    }

    private static Session createSession(JSch jsch,
                                         ServerProfile profile,
                                         @Nullable RemoteCredentials credentials,
                                         String host,
                                         int port) throws Exception {
        if (profile.getAuthType() == AuthType.PRIVATE_KEY) {
            if (credentials == null || credentials.getPrivateKeyPath() == null || credentials.getPrivateKeyPath().isBlank()) {
                throw new IllegalArgumentException("Private key path required");
            }
            if (credentials.getPrivateKeyPassphrase() == null || credentials.getPrivateKeyPassphrase().isBlank()) {
                jsch.addIdentity(credentials.getPrivateKeyPath());
            } else {
                jsch.addIdentity(credentials.getPrivateKeyPath(), credentials.getPrivateKeyPassphrase());
            }
        }

        Session session = jsch.getSession(profile.getUsername(), host, port);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        if (profile.getAuthType() == AuthType.PASSWORD) {
            if (credentials == null || credentials.getPassword() == null || credentials.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password required");
            }
            session.setPassword(credentials.getPassword());
        }
        return session;
    }

    public void runCommandSequential(String command) throws Exception {
        if (command == null || command.isBlank()) {
            return;
        }
        if (automationCancelled.get()) {
            throw new InterruptedException("Deployment cancelled by Ctrl+C");
        }
        String marker = "__SD_CMD_END_" + System.nanoTime() + "__";
        String wrapped = "{ " + command + "; }; printf '\\033]777;" + marker + ":%s\\007' \"$?\"\n";

        synchronized (commandLock) {
            waitingMarker = marker;
            waitingExitCode = null;
            markerBuffer.setLength(0);
        }
        write(wrapped.getBytes(StandardCharsets.UTF_8));

        long start = System.currentTimeMillis();
        while (true) {
            if (automationCancelled.get()) {
                throw new InterruptedException("Deployment cancelled by Ctrl+C");
            }
            Integer exit;
            synchronized (commandLock) {
                exit = waitingExitCode;
            }
            if (exit != null) {
                if (exit != 0) {
                    throw new IllegalStateException("Remote command failed, exit code: " + exit);
                }
                return;
            }
            if (System.currentTimeMillis() - start > 24 * 60 * 60 * 1000L) {
                throw new IllegalStateException("Remote command timeout");
            }
            Thread.sleep(80L);
        }
    }

    public void uploadFileSequential(File localFile, String remoteDir, Consumer<Integer> progress) throws Exception {
        if (automationCancelled.get()) {
            throw new InterruptedException("Deployment cancelled by Ctrl+C");
        }
        if (localFile == null || !localFile.exists()) {
            throw new IllegalStateException("Local upload source not found: " + (localFile == null ? "" : localFile.getAbsolutePath()));
        }
        if (targetSession == null || !targetSession.isConnected()) {
            throw new IllegalStateException("SSH session not connected");
        }

        ChannelSftp sftp = (ChannelSftp) targetSession.openChannel("sftp");
        sftp.connect(CONNECT_TIMEOUT_MILLIS);
        try {
            ensureRemoteDirectory(sftp, remoteDir);
            String remoteFile = remoteDir.endsWith("/") ? remoteDir + localFile.getName() : remoteDir + "/" + localFile.getName();
            int[] last = new int[]{-1};
            SftpProgressMonitor monitor = new SftpProgressMonitor() {
                private long transferred;
                private long total;

                @Override
                public void init(int op, String src, String dest, long max) {
                    transferred = 0;
                    total = max;
                    if (progress != null) progress.accept(0);
                }

                @Override
                public boolean count(long count) {
                    if (automationCancelled.get()) {
                        return false;
                    }
                    transferred += count;
                    long t = total <= 0 ? transferred : total;
                    int percent = (int) Math.min(100, (transferred * 100L) / t);
                    if (percent > last[0]) {
                        last[0] = percent;
                        if (progress != null) progress.accept(percent);
                    }
                    return true;
                }

                @Override
                public void end() {
                    if (progress != null && last[0] < 100) {
                        progress.accept(100);
                    }
                }
            };
            sftp.put(localFile.getAbsolutePath(), remoteFile, monitor, ChannelSftp.OVERWRITE);
            if (automationCancelled.get()) {
                throw new InterruptedException("Deployment cancelled by Ctrl+C");
            }
        } finally {
            sftp.disconnect();
        }
    }

    private void inspectForCommandMarker(char[] buf, int offset, int length) {
        synchronized (commandLock) {
            if (waitingMarker == null) {
                return;
            }
            markerBuffer.append(buf, offset, length);
            String content = markerBuffer.toString();
            String prefix = "\u001B]777;" + waitingMarker + ":";
            int idx = content.indexOf(prefix);
            if (idx >= 0) {
                int p = idx + prefix.length();
                StringBuilder digits = new StringBuilder();
                while (p < content.length() && Character.isDigit(content.charAt(p))) {
                    digits.append(content.charAt(p));
                    p++;
                }
                boolean hasBel = p < content.length() && content.charAt(p) == '\u0007';
                if (digits.length() > 0 && hasBel) {
                    waitingExitCode = Integer.parseInt(digits.toString());
                    waitingMarker = null;
                    markerBuffer.setLength(0);
                } else if (content.length() > 8192) {
                    markerBuffer = new StringBuilder(content.substring(Math.max(0, content.length() - 2048)));
                }
            } else if (content.length() > 8192) {
                markerBuffer = new StringBuilder(content.substring(Math.max(0, content.length() - 2048)));
            }
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

    private static String escapePath(String path) {
        StringBuilder escaped = new StringBuilder();
        for (char c : path.toCharArray()) {
            if (c == ' ' || c == ';' || c == '`' || c == '\\') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}

