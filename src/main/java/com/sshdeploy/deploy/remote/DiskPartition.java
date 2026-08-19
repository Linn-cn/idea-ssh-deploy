package com.sshdeploy.deploy.remote;

import java.math.BigDecimal;

/**
 * One physical disk mount collected from a remote Linux host.
 */
public final class DiskPartition {
    private String fileSystem = "";
    private String mountPath = "";
    private long totalBytes;
    private long usedBytes;
    private long availableBytes;
    private BigDecimal usageRate = BigDecimal.ZERO;
    private String totalText = "";
    private String usedText = "";
    private String availableText = "";
    private long inodeTotal;
    private long inodeUsed;
    private long inodeAvailable;
    private BigDecimal inodeUsageRate = BigDecimal.ZERO;

    public String getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(String fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public void setAvailableBytes(long availableBytes) {
        this.availableBytes = availableBytes;
    }

    public BigDecimal getUsageRate() {
        return usageRate;
    }

    public void setUsageRate(BigDecimal usageRate) {
        this.usageRate = usageRate;
    }

    public String getTotalText() {
        return totalText;
    }

    public void setTotalText(String totalText) {
        this.totalText = totalText;
    }

    public String getUsedText() {
        return usedText;
    }

    public void setUsedText(String usedText) {
        this.usedText = usedText;
    }

    public String getAvailableText() {
        return availableText;
    }

    public void setAvailableText(String availableText) {
        this.availableText = availableText;
    }

    public long getInodeTotal() {
        return inodeTotal;
    }

    public void setInodeTotal(long inodeTotal) {
        this.inodeTotal = inodeTotal;
    }

    public long getInodeUsed() {
        return inodeUsed;
    }

    public void setInodeUsed(long inodeUsed) {
        this.inodeUsed = inodeUsed;
    }

    public long getInodeAvailable() {
        return inodeAvailable;
    }

    public void setInodeAvailable(long inodeAvailable) {
        this.inodeAvailable = inodeAvailable;
    }

    public BigDecimal getInodeUsageRate() {
        return inodeUsageRate;
    }

    public void setInodeUsageRate(BigDecimal inodeUsageRate) {
        this.inodeUsageRate = inodeUsageRate;
    }
}
