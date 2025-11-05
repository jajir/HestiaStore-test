package org.hestiastore.index.benchmark.plainload;

public class SystemState {

    private long totalDirectorySize = 0;

    private long fileCount = 0;

    private long usedMemoryBytes = 0;

    private long cpuBefore;

    private long cpuAfter;

    private long startTime;

    private long endTime;

    private double cpuUsage;

    @Override
    public String toString() {
        return "TestFileSystemState [totalSize=" + totalDirectorySize
                + ", fileCount=" + fileCount + ", usedMemoryBytes="
                + usedMemoryBytes + ", cpuBefore=" + cpuBefore + ", cpuAfter="
                + cpuAfter + ", startTime=" + startTime + ", endTime=" + endTime
                + ", cpuUsage=" + cpuUsage + " ]";
    }

    public long getFileCount() {
        return fileCount;
    }

    public void setFileCount(long fileCount) {
        this.fileCount = fileCount;
    }

    public void setTotalDirectorySize(long totalSize) {
        this.totalDirectorySize = totalSize;
    }

    public long getTotalDirectorySize() {
        return totalDirectorySize;
    }

    public void setUsedMemoryBytes(long usedMemoryBytes) {
        this.usedMemoryBytes = usedMemoryBytes;
    }

    public long getUsedMemoryBytes() {
        return usedMemoryBytes;
    }

    public long getCpuBefore() {
        return cpuBefore;
    }

    public void setCpuBefore(long cpuBefore) {
        this.cpuBefore = cpuBefore;
    }

    public long getCpuAfter() {
        return cpuAfter;
    }

    public void setCpuAfter(long cpuAfter) {
        this.cpuAfter = cpuAfter;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
}
