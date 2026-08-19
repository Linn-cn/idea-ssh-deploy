package com.sshdeploy.deploy.remote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Live Linux host metrics collected over SSH for the status dialog.
 */
public final class ServerStatusSnapshot {
    private boolean online;
    private String hostName = "";
    private String ipAddress = "";
    private String sshUsername = "";
    private int sshPort;
    private String osName = "";
    private String architecture = "";
    private String kernelVersion = "";
    private String cpuModel = "";
    private int cpuCores;
    private BigDecimal cpuUsage = BigDecimal.ZERO;
    private BigDecimal loadAverage1 = BigDecimal.ZERO;
    private BigDecimal loadAverage5 = BigDecimal.ZERO;
    private BigDecimal loadAverage15 = BigDecimal.ZERO;
    private long memoryTotalBytes;
    private long memoryAvailableBytes;
    private BigDecimal memoryUsage = BigDecimal.ZERO;
    private long swapTotalBytes;
    private long swapFreeBytes;
    private BigDecimal swapUsage = BigDecimal.ZERO;
    private long uptimeSeconds;
    private List<DiskPartition> disks = new ArrayList<>();

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    public BigDecimal getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(BigDecimal cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public BigDecimal getLoadAverage1() {
        return loadAverage1;
    }

    public void setLoadAverage1(BigDecimal loadAverage1) {
        this.loadAverage1 = loadAverage1;
    }

    public BigDecimal getLoadAverage5() {
        return loadAverage5;
    }

    public void setLoadAverage5(BigDecimal loadAverage5) {
        this.loadAverage5 = loadAverage5;
    }

    public BigDecimal getLoadAverage15() {
        return loadAverage15;
    }

    public void setLoadAverage15(BigDecimal loadAverage15) {
        this.loadAverage15 = loadAverage15;
    }

    public long getMemoryTotalBytes() {
        return memoryTotalBytes;
    }

    public void setMemoryTotalBytes(long memoryTotalBytes) {
        this.memoryTotalBytes = memoryTotalBytes;
    }

    public long getMemoryAvailableBytes() {
        return memoryAvailableBytes;
    }

    public void setMemoryAvailableBytes(long memoryAvailableBytes) {
        this.memoryAvailableBytes = memoryAvailableBytes;
    }

    public BigDecimal getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(BigDecimal memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public long getSwapTotalBytes() {
        return swapTotalBytes;
    }

    public void setSwapTotalBytes(long swapTotalBytes) {
        this.swapTotalBytes = swapTotalBytes;
    }

    public long getSwapFreeBytes() {
        return swapFreeBytes;
    }

    public void setSwapFreeBytes(long swapFreeBytes) {
        this.swapFreeBytes = swapFreeBytes;
    }

    public BigDecimal getSwapUsage() {
        return swapUsage;
    }

    public void setSwapUsage(BigDecimal swapUsage) {
        this.swapUsage = swapUsage;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public List<DiskPartition> getDisks() {
        return disks;
    }

    public void setDisks(List<DiskPartition> disks) {
        this.disks = disks == null ? new ArrayList<>() : disks;
    }
}
