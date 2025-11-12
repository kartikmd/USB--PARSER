package com.myorg.usbparser.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerfLogger {
    private static final Logger PERF = LoggerFactory.getLogger("performance");

    private PerfLogger() {}

    public static void frame(String label, long tookMs, double cpuPct, String mem) {
        // Match the Face-Recognition style
        PERF.info("{} - Frame processed in {} ms (FPS: {}), CPU: {}, Memory: {}",
                label, tookMs, tookMs > 0 ? String.format("%.2f", 1000.0 / tookMs) : "0.00",
                cpuPct + "%", mem);
    }

    public static void info(String msg) {
        PERF.info(msg);
    }
}
