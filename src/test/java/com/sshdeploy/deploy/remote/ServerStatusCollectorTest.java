package com.sshdeploy.deploy.remote;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class ServerStatusCollectorTest {
    @Test
    public void parse_shouldReadHostLoadAndPhysicalDisks() {
        String output = """
                @@HOST@@
                monitor-01
                @@OS@@
                Ubuntu 24.04
                @@KERNEL@@
                6.8
                @@ARCH@@
                x86_64
                @@CPU_MODEL@@
                Intel(R) Xeon(R)
                @@CORES@@
                4
                @@CPU1@@
                100 10 30 800 20 0 0 0
                @@CPU2@@
                130 10 50 880 30 0 0 0
                @@LOAD@@
                1.23 0.80 0.55 1/120 1000
                @@MEM@@
                MemTotal: 8388608
                MemAvailable: 4194304
                SwapTotal: 2097152
                SwapFree: 1048576
                @@UPTIME@@
                3600
                @@DISK@@
                Filesystem 1-blocks Used Available Capacity Mounted on
                tmpfs 6697324544 4067328 6693257216 1% /run
                /dev/vda1 52720218112 3855476736 48864741376 8% /
                /dev/loop0 58195968 58195968 0 100% /snap/core18/2999
                /dev/vda2 499122176 107583897 391538279 22% /boot
                @@INODE@@
                Filesystem Inodes IUsed IFree IUse% Mounted on
                tmpfs 1635089 728 1634361 1% /run
                /dev/vda1 3276800 120000 3156800 4% /
                /dev/loop0 0 0 0 - /snap/core18/2999
                /dev/vda2 65536 320 65216 1% /boot
                """;

        ServerStatusSnapshot result = ServerStatusCollector.parse(output);

        assertEquals("x86_64", result.getArchitecture());
        assertEquals("Intel(R) Xeon(R)", result.getCpuModel());
        assertEquals(new BigDecimal("1.23"), result.getLoadAverage1());
        assertEquals(3600L, result.getUptimeSeconds());
        assertEquals("/", result.getDisks().get(0).getMountPath());
        assertEquals(new BigDecimal("7.31"), result.getDisks().get(0).getUsageRate());
        assertEquals(3276800L, result.getDisks().get(0).getInodeTotal());
        assertEquals(120000L, result.getDisks().get(0).getInodeUsed());
        assertEquals(3156800L, result.getDisks().get(0).getInodeAvailable());
        assertEquals(new BigDecimal("3.66"), result.getDisks().get(0).getInodeUsageRate());
        assertEquals(2, result.getDisks().size());
    }
}
