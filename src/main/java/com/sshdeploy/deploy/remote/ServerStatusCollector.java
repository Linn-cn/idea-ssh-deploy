package com.sshdeploy.deploy.remote;

import com.sshdeploy.deploy.domain.ServerProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects Linux host metrics over SSH using the same probes as the Silas server monitor.
 */
public final class ServerStatusCollector {
    static final String COMMAND = """
            export LC_ALL=C
            printf '@@HOST@@\\n'; hostname
            printf '@@OS@@\\n'; if [ -r /etc/os-release ]; then . /etc/os-release; printf '%s\\n' "${PRETTY_NAME:-${NAME:-}}"; else uname -s; fi
            printf '@@KERNEL@@\\n'; uname -r
            printf '@@ARCH@@\\n'; uname -m
            printf '@@CPU_MODEL@@\\n'; cpu_model=$(awk -F: '/^[Mm]odel name|^Hardware|^Processor|^Model/ {sub(/^ +/, "", $2); if ($2 != "") { print $2; exit }}' /proc/cpuinfo); [ -z "$cpu_model" ] && cpu_model=$(lscpu 2>/dev/null | awk -F: '/Model name|Model:/ {sub(/^ +/, "", $2); if ($2 != "") { print $2; exit }}'); printf '%s\\n' "$cpu_model"
            printf '@@CORES@@\\n'; getconf _NPROCESSORS_ONLN
            printf '@@CPU1@@\\n'; awk '/^cpu / {print $2, $3, $4, $5, $6, $7, $8, $9; exit}' /proc/stat
            sleep 3
            printf '@@CPU2@@\\n'; awk '/^cpu / {print $2, $3, $4, $5, $6, $7, $8, $9; exit}' /proc/stat
            printf '@@LOAD@@\\n'; cat /proc/loadavg
            printf '@@MEM@@\\n'; awk '/^MemTotal:|^MemAvailable:|^SwapTotal:|^SwapFree:/ {print $1, $2}' /proc/meminfo
            printf '@@UPTIME@@\\n'; awk '{print int($1)}' /proc/uptime
            printf '@@DISK@@\\n'; df -P -B1 -x tmpfs -x devtmpfs -x squashfs -x overlay -x efivarfs
            printf '@@INODE@@\\n'; df -P -i -x tmpfs -x devtmpfs -x squashfs -x overlay -x efivarfs
            """;

    /**
     * Connects to {@code profile} and returns parsed live metrics.
     *
     * @param profile the SSH server to query
     * @param credentials password or key material for {@code profile}
     * @return collected snapshot
     */
    public ServerStatusSnapshot collect(ServerProfile profile, RemoteCredentials credentials) throws Exception {
        try (RemoteClient client = new DefaultRemoteClientFactory().create()) {
            client.connect(new RemoteConnectRequest(profile, credentials));
            RemoteCommandResult result = client.execute(COMMAND, 25);
            if (result.getExitCode() != 0) {
                String detail = result.getStdErr() == null || result.getStdErr().isBlank()
                        ? result.getStdOut()
                        : result.getStdErr();
                throw new IllegalStateException("Status collect command failed: " + detail);
            }
            ServerStatusSnapshot snapshot = parse(result.getStdOut());
            snapshot.setOnline(true);
            snapshot.setIpAddress(profile.getHost());
            snapshot.setSshUsername(profile.getUsername());
            snapshot.setSshPort(profile.getPort());
            return snapshot;
        }
    }

    static ServerStatusSnapshot parse(String output) {
        Map<String, List<String>> values = sections(output);
        ServerStatusSnapshot result = new ServerStatusSnapshot();
        result.setHostName(optional(values, "HOST"));
        result.setOsName(optional(values, "OS"));
        result.setKernelVersion(optional(values, "KERNEL"));
        result.setArchitecture(optional(values, "ARCH"));
        result.setCpuModel(optional(values, "CPU_MODEL"));
        result.setCpuCores(Integer.parseInt(first(values, "CORES")));
        result.setCpuUsage(cpuUsage(first(values, "CPU1"), first(values, "CPU2")));
        fillLoad(result, first(values, "LOAD"));
        fillMemory(result, values.get("MEM"));
        result.setUptimeSeconds(Long.parseLong(first(values, "UPTIME")));
        result.setDisks(disks(values.get("DISK"), values.get("INODE")));
        return result;
    }

    private static Map<String, List<String>> sections(String output) {
        Map<String, List<String>> values = new HashMap<>();
        String key = null;
        for (String line : output.replace("\r", "").split("\n")) {
            if (line.startsWith("@@") && line.endsWith("@@")) {
                key = line.substring(2, line.length() - 2);
                values.put(key, new ArrayList<>());
            } else if (key != null && !line.isBlank()) {
                values.get(key).add(line.trim());
            }
        }
        return values;
    }

    private static String first(Map<String, List<String>> values, String key) {
        if (!values.containsKey(key) || values.get(key).isEmpty()) {
            throw new IllegalStateException("Server did not return " + key);
        }
        return values.get(key).get(0);
    }

    private static String optional(Map<String, List<String>> values, String key) {
        if (!values.containsKey(key) || values.get(key).isEmpty()) {
            return "";
        }
        return nullable(values.get(key).get(0));
    }

    private static String nullable(String value) {
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return value.trim();
    }

    private static BigDecimal cpuUsage(String before, String after) {
        long[] first = Arrays.stream(before.split("\\s+")).mapToLong(Long::parseLong).toArray();
        long[] second = Arrays.stream(after.split("\\s+")).mapToLong(Long::parseLong).toArray();
        long total = Arrays.stream(second).sum() - Arrays.stream(first).sum();
        long idle = second[3] + second[4] - first[3] - first[4];
        return total <= 0 ? BigDecimal.ZERO.setScale(2) : percent(total - idle, total);
    }

    private static void fillLoad(ServerStatusSnapshot result, String load) {
        String[] values = load.split("\\s+");
        if (values.length < 3) {
            throw new IllegalStateException("Incomplete load average");
        }
        result.setLoadAverage1(decimal(values[0]));
        result.setLoadAverage5(decimal(values[1]));
        result.setLoadAverage15(decimal(values[2]));
    }

    private static void fillMemory(ServerStatusSnapshot result, List<String> lines) {
        Map<String, Long> memory = new HashMap<>();
        if (lines != null) {
            for (String line : lines) {
                String[] values = line.split("\\s+");
                if (values.length == 2) {
                    memory.put(values[0], Long.parseLong(values[1]) * 1024);
                }
            }
        }
        long total = required(memory, "MemTotal:");
        long available = required(memory, "MemAvailable:");
        long swapTotal = required(memory, "SwapTotal:");
        long swapFree = required(memory, "SwapFree:");
        result.setMemoryTotalBytes(total);
        result.setMemoryAvailableBytes(available);
        result.setMemoryUsage(percent(total - available, total));
        result.setSwapTotalBytes(swapTotal);
        result.setSwapFreeBytes(swapFree);
        result.setSwapUsage(swapTotal == 0 ? BigDecimal.ZERO.setScale(2) : percent(swapTotal - swapFree, swapTotal));
    }

    private static long required(Map<String, Long> values, String key) {
        Long value = values.get(key);
        if (value == null) {
            throw new IllegalStateException("Server did not return " + key);
        }
        return value;
    }

    private static List<DiskPartition> disks(List<String> lines, List<String> inodeLines) {
        if (lines == null || lines.size() < 2) {
            throw new IllegalStateException("Server did not return disk metrics");
        }
        Map<String, long[]> inodes = inodeMap(inodeLines);
        List<DiskPartition> disks = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] parts = line.split("\\s+");
            if (parts.length < 6 || !isPhysicalDisk(parts[0], parts[parts.length - 1])) {
                continue;
            }
            long total = Long.parseLong(parts[1]);
            long used = Long.parseLong(parts[2]);
            long available = Long.parseLong(parts[3]);
            DiskPartition disk = new DiskPartition();
            disk.setFileSystem(parts[0]);
            disk.setMountPath(parts[parts.length - 1]);
            disk.setTotalBytes(total);
            disk.setUsedBytes(used);
            disk.setAvailableBytes(available);
            disk.setUsageRate(percent(used, total));
            fillInodes(disk, inodes.get(diskKey(parts[0], parts[parts.length - 1])));
            disk.setTotalText(size(total));
            disk.setUsedText(size(used));
            disk.setAvailableText(size(available));
            disks.add(disk);
        }
        return disks;
    }

    private static Map<String, long[]> inodeMap(List<String> lines) {
        Map<String, long[]> result = new HashMap<>();
        if (lines == null || lines.size() < 2) {
            return result;
        }
        for (String line : lines.subList(1, lines.size())) {
            String[] parts = line.split("\\s+");
            if (parts.length < 6 || !isPhysicalDisk(parts[0], parts[parts.length - 1])) {
                continue;
            }
            result.put(diskKey(parts[0], parts[parts.length - 1]), new long[]{
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2]),
                    Long.parseLong(parts[3])
            });
        }
        return result;
    }

    private static void fillInodes(DiskPartition disk, long[] inodes) {
        if (inodes == null || inodes.length < 3) {
            return;
        }
        disk.setInodeTotal(inodes[0]);
        disk.setInodeUsed(inodes[1]);
        disk.setInodeAvailable(inodes[2]);
        disk.setInodeUsageRate(percent(inodes[1], inodes[0]));
    }

    private static String diskKey(String fileSystem, String mountPath) {
        return fileSystem + "@" + mountPath;
    }

    private static boolean isPhysicalDisk(String fileSystem, String mountPath) {
        if (fileSystem.startsWith("/dev/loop") || mountPath.startsWith("/snap/")) {
            return false;
        }
        return fileSystem.startsWith("/dev/");
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal percent(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /**
     * Formats a byte count as a short capacity string such as {@code 1.5GB}.
     *
     * @param bytes size in bytes
     * @return human-readable capacity
     */
    public static String size(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        BigDecimal value = BigDecimal.valueOf(bytes);
        int index = 0;
        while (value.compareTo(BigDecimal.valueOf(1024)) >= 0 && index < units.length - 1) {
            value = value.divide(BigDecimal.valueOf(1024), 4, RoundingMode.HALF_UP);
            index++;
        }
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + units[index];
    }
}
