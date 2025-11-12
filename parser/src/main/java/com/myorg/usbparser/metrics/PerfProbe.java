package com.myorg.usbparser.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class PerfProbe {
    private static final Logger PERF = LoggerFactory.getLogger("performance");
    private static final OperatingSystemMXBean OS =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private final long t0 = System.nanoTime();
    private long last = t0;
    private final String label;

    public PerfProbe(String label) { this.label = label; }

    public void mark(String stepName, long unitsProcessed) {
        long now = System.nanoTime();
        double ms = (now - last) / 1_000_000.0;
        double sec = ms / 1000.0;
        last = now;

        double fps = (sec > 0) ? (unitsProcessed / sec) : 0.0;
        double cpuPct = OS.getProcessCpuLoad();
        if (cpuPct >= 0) cpuPct *= 100.0; else cpuPct = 0.0;

        Runtime rt = Runtime.getRuntime();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        double usedMB = usedBytes / (1024.0 * 1024.0);

        PERF.info("{} - {} in {} ms (FPS: {}), CPU: {}%, Memory: {} MB",
                label,
                stepName,
                String.format("%.2f", ms),
                String.format("%.2f", fps),
                String.format("%.1f", cpuPct),
                String.format("%.2f", usedMB)
        );
    }

    public void done(String stepName) {
        long now = System.nanoTime();
        double totalMs = (now - t0) / 1_000_000.0;
        PERF.info("{} - {} total {} ms",
                label, stepName, String.format("%.2f", totalMs));
    }
}
