package com.sshdeploy.deploy.remote;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface RemoteClient extends AutoCloseable {
    @FunctionalInterface
    interface UploadProgressListener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    void connect(RemoteConnectRequest request) throws Exception;

    default void testConnection(RemoteConnectRequest request) throws Exception {
        connect(request);
    }

    void uploadFile(File localFile, String remotePath) throws Exception;

    default void uploadFile(File localFile, String remotePath, UploadProgressListener listener) throws Exception {
        uploadFile(localFile, remotePath);
        if (listener != null) {
            listener.onProgress(localFile.length(), localFile.length());
        }
    }

    void uploadDirectory(File localDirectory, String remoteDirectory, List<String> filters) throws Exception;

    /**
     * Lists immediate child directories under {@code remotePath} (SFTP {@code ls}).
     */
    List<RemoteDirectoryEntry> listDirectory(String remotePath) throws Exception;

    /**
     * Returns the remote user's home directory (SFTP home).
     */
    String resolveHomeDirectory() throws Exception;

    RemoteCommandResult execute(String command, int timeoutSeconds) throws Exception;

    default RemoteCommandResult executeStreaming(String command,
                                                 int timeoutSeconds,
                                                 Consumer<String> stdOutConsumer,
                                                 Consumer<String> stdErrConsumer) throws Exception {
        RemoteCommandResult result = execute(command, timeoutSeconds);
        if (stdOutConsumer != null && result.getStdOut() != null && !result.getStdOut().isBlank()) {
            stdOutConsumer.accept(result.getStdOut());
        }
        if (stdErrConsumer != null && result.getStdErr() != null && !result.getStdErr().isBlank()) {
            stdErrConsumer.accept(result.getStdErr());
        }
        return result;
    }

    @Override
    void close();
}
