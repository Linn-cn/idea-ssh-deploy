package com.sshdeploy.deploy.remote;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

/**
 * Shared SFTP channel setup for uploads. Non-interactive SFTP is sensitive to network RTT; JSch
 * pipelines a limited number of write requests by default—increasing the queue improves throughput
 * on typical deploy paths (LAN or VPN) at the cost of slightly higher memory during the transfer.
 */
public final class JschSftpChannels {
    /**
     * JSch default is modest; higher values reduce round-trip waits per megabyte on high-latency links.
     * See {@link ChannelSftp#setBulkRequests(int)}.
     */
    private static final int SFTP_BULK_WRITE_REQUESTS = 64;

    private JschSftpChannels() {
    }

    /**
     * Call after {@link ChannelSftp#connect(int)} and before {@link ChannelSftp#put} operations.
     */
    public static void applyFastUploadDefaults(ChannelSftp sftp) throws JSchException {
        if (sftp == null) {
            return;
        }
        sftp.setBulkRequests(SFTP_BULK_WRITE_REQUESTS);
    }
}
