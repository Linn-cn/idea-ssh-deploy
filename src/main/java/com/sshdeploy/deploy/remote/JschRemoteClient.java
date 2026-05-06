package com.sshdeploy.deploy.remote;

import com.sshdeploy.deploy.domain.AuthType;
import com.sshdeploy.deploy.domain.ServerProfile;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public final class JschRemoteClient implements RemoteClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int POLL_INTERVAL_MILLIS = 100;

    private Session targetSession;

    @Override
    public void connect(RemoteConnectRequest request) throws Exception {
        close();
        targetSession = createSession(request.getTarget(), request.getTargetCredentials());
        targetSession.connect(CONNECT_TIMEOUT_MILLIS);
    }

    @Override
    public void testConnection(RemoteConnectRequest request) throws Exception {
        connect(request);
    }

    @Override
    public void uploadFile(File localFile, String remotePath) throws Exception {
        uploadFile(localFile, remotePath, null);
    }

    @Override
    public void uploadFile(File localFile, String remotePath, UploadProgressListener listener) throws Exception {
        ChannelSftp sftp = openSftpChannel();
        try {
            ensureRemoteDirectory(sftp, parentPath(remotePath));
            SftpProgressMonitor monitor = null;
            if (listener != null) {
                monitor = new SftpProgressMonitor() {
                    private long transferred;
                    private long total;

                    @Override
                    public void init(int op, String src, String dest, long max) {
                        this.transferred = 0;
                        this.total = max;
                        listener.onProgress(0, max);
                    }

                    @Override
                    public boolean count(long count) {
                        transferred += count;
                        listener.onProgress(transferred, total);
                        return true;
                    }

                    @Override
                    public void end() {
                        listener.onProgress(total, total);
                    }
                };
            }
            sftp.put(localFile.getAbsolutePath(), remotePath, monitor, ChannelSftp.OVERWRITE);
        } finally {
            sftp.disconnect();
        }
    }

    @Override
    public void uploadDirectory(File localDirectory, String remoteDirectory, List<String> filters) throws Exception {
        ChannelSftp sftp = openSftpChannel();
        try {
            ensureRemoteDirectory(sftp, remoteDirectory);
            List<PathMatcher> matchers = buildMatchers(filters);
            uploadDirectoryRecursive(sftp, localDirectory, remoteDirectory, localDirectory.toPath(), matchers);
        } finally {
            sftp.disconnect();
        }
    }

    @Override
    public RemoteCommandResult execute(String command, int timeoutSeconds) throws Exception {
        return executeStreaming(command, timeoutSeconds, null, null);
    }

    @Override
    public RemoteCommandResult executeStreaming(String command,
                                                int timeoutSeconds,
                                                Consumer<String> stdOutConsumer,
                                                Consumer<String> stdErrConsumer) throws Exception {
        ChannelExec channel = (ChannelExec) targetSession.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);

        InputStream outStream = channel.getInputStream();
        InputStream errStream = channel.getErrStream();
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];

        channel.connect(CONNECT_TIMEOUT_MILLIS);
        long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;
        try {
            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("Remote command timeout: " + command);
                }

                pumpStream(outStream, outBuffer, tmp, stdOutConsumer);
                pumpStream(errStream, errBuffer, tmp, stdErrConsumer);

                if (channel.isClosed()) {
                    pumpStream(outStream, outBuffer, tmp, stdOutConsumer);
                    pumpStream(errStream, errBuffer, tmp, stdErrConsumer);
                    break;
                }
                Thread.sleep(POLL_INTERVAL_MILLIS);
            }
            int exitStatus = channel.getExitStatus();
            return new RemoteCommandResult(
                    exitStatus,
                    outBuffer.toString(StandardCharsets.UTF_8),
                    errBuffer.toString(StandardCharsets.UTF_8)
            );
        } finally {
            channel.disconnect();
        }
    }

    @Override
    public void close() {
        if (targetSession != null && targetSession.isConnected()) {
            targetSession.disconnect();
        }
    }

    private Session createSession(ServerProfile profile, RemoteCredentials credentials) throws Exception {
        return createSession(profile, credentials, profile.getHost(), profile.getPort());
    }

    private Session createSession(ServerProfile profile,
                                  RemoteCredentials credentials,
                                  String host,
                                  int port) throws Exception {
        if (profile == null) {
            throw new IllegalArgumentException("Server profile is required.");
        }

        JSch jsch = new JSch();
        if (profile.getAuthType() == AuthType.PRIVATE_KEY) {
            if (isBlank(credentials.getPrivateKeyPath())) {
                throw new IllegalArgumentException("Private key path is required for private key auth.");
            }
            if (isBlank(credentials.getPrivateKeyPassphrase())) {
                jsch.addIdentity(credentials.getPrivateKeyPath());
            } else {
                jsch.addIdentity(credentials.getPrivateKeyPath(), credentials.getPrivateKeyPassphrase());
            }
        }

        Session session = jsch.getSession(profile.getUsername(), host, port);
        if (profile.getAuthType() == AuthType.PASSWORD) {
            if (isBlank(credentials.getPassword())) {
                throw new IllegalArgumentException("Password is required for password auth.");
            }
            session.setPassword(credentials.getPassword());
        }

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setServerAliveInterval(5_000);
        return session;
    }

    private ChannelSftp openSftpChannel() throws Exception {
        Channel channel = targetSession.openChannel("sftp");
        channel.connect(CONNECT_TIMEOUT_MILLIS);
        return (ChannelSftp) channel;
    }

    private void uploadDirectoryRecursive(ChannelSftp sftp,
                                          File localDirectory,
                                          String remoteDirectory,
                                          Path rootPath,
                                          List<PathMatcher> matchers) throws Exception {
        File[] children = localDirectory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String remoteChildPath = remoteDirectory + "/" + child.getName();
            if (child.isDirectory()) {
                ensureRemoteDirectory(sftp, remoteChildPath);
                uploadDirectoryRecursive(sftp, child, remoteChildPath, rootPath, matchers);
            } else {
                Path relativePath = rootPath.relativize(child.toPath());
                if (shouldUpload(relativePath, matchers)) {
                    sftp.put(child.getAbsolutePath(), remoteChildPath);
                }
            }
        }
    }

    private boolean shouldUpload(Path relativePath, List<PathMatcher> matchers) {
        if (matchers.isEmpty()) {
            return true;
        }

        Path normalized = Path.of(relativePath.toString().replace("\\", "/"));
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(normalized)) {
                return true;
            }
        }
        return false;
    }

    private List<PathMatcher> buildMatchers(List<String> filters) {
        List<PathMatcher> matchers = new ArrayList<>();
        if (filters == null) {
            return matchers;
        }
        for (String filter : filters) {
            if (filter != null && !filter.isBlank()) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + filter.trim()));
            }
        }
        return matchers;
    }

    private void ensureRemoteDirectory(ChannelSftp sftp, String remotePath) throws SftpException {
        if (isBlank(remotePath)) {
            return;
        }

        String normalized = remotePath.replace("\\", "/");
        String[] segments = normalized.split("/");
        StringBuilder path = new StringBuilder();
        if (normalized.startsWith("/")) {
            path.append("/");
        }

        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }

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

    private String parentPath(String path) {
        int idx = path.lastIndexOf('/');
        if (idx <= 0) {
            return "";
        }
        return path.substring(0, idx);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void pumpStream(InputStream stream,
                                   ByteArrayOutputStream aggregate,
                                   byte[] tmp,
                                   Consumer<String> consumer) throws Exception {
        if (stream == null) {
            return;
        }
        while (stream.available() > 0) {
            int read = stream.read(tmp, 0, tmp.length);
            if (read <= 0) {
                break;
            }
            aggregate.write(tmp, 0, read);
            if (consumer != null) {
                consumer.accept(new String(tmp, 0, read, StandardCharsets.UTF_8));
            }
        }
    }
}
