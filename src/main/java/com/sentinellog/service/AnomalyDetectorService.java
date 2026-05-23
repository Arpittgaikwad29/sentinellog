package com.sentinellog.service;

import com.sentinellog.model.LogEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects anomalies in log entries using two complementary techniques:
 *
 *  1. Z-score on response time — flags entries whose response time
 *     deviates more than ZSCORE_THRESHOLD standard deviations from
 *     the per-service rolling mean.
 *
 *  2. Error rate spike — flags when ERROR/FATAL log rate in a 10-entry
 *     sliding window exceeds ERROR_RATE_THRESHOLD.
 *
 * Both detectors operate on a per-service basis so a noisy service
 * doesn't pollute baselines for others.
 */
@Service
public class AnomalyDetectorService {

    // ── Tuning knobs ─────────────────────────────────────────
    private static final int    WINDOW_SIZE          = 50;   // rolling window per service
    private static final double ZSCORE_THRESHOLD     = 2.5;  // σ threshold for response time
    private static final double ERROR_RATE_THRESHOLD = 0.40; // 40% errors in recent window
    private static final int    MIN_SAMPLES          = 10;   // need at least N samples before scoring
    // ─────────────────────────────────────────────────────────

    /** Per-service sliding window of response times */
    private final Map<String, Deque<Double>> responseWindows = new ConcurrentHashMap<>();

    /** Per-service sliding window of log levels (for error-rate check) */
    private final Map<String, Deque<LogEntry.LogLevel>> levelWindows = new ConcurrentHashMap<>();

    /**
     * Analyses the given entry and sets anomaly fields in place.
     * Returns the same entry (mutated) for fluent chaining.
     */
    public LogEntry analyse(LogEntry entry) {
        String svc = entry.getService();

        // Update sliding windows
        Deque<Double> rtWindow = responseWindows
                .computeIfAbsent(svc, k -> new ArrayDeque<>(WINDOW_SIZE + 1));
        Deque<LogEntry.LogLevel> lvlWindow = levelWindows
                .computeIfAbsent(svc, k -> new ArrayDeque<>(WINDOW_SIZE + 1));

        // ── 1. Z-score check ──────────────────────────────────
        if (rtWindow.size() >= MIN_SAMPLES) {
            double[] values = rtWindow.stream().mapToDouble(Double::doubleValue).toArray();
            double mean  = mean(values);
            double stdev = stdev(values, mean);

            if (stdev > 0) {
                double z = (entry.getResponseTimeMs() - mean) / stdev;
                entry.setZScore(Math.round(z * 100.0) / 100.0);

                if (Math.abs(z) > ZSCORE_THRESHOLD) {
                    entry.setAnomaly(true);
                    entry.setAnomalyReason(String.format(
                            "Response time %.0fms is %.1fσ from mean (%.0fms)",
                            (double) entry.getResponseTimeMs(), z, mean));
                }
            }
        }

        // ── 2. Error rate spike check ─────────────────────────
        if (!entry.isAnomaly() && lvlWindow.size() >= MIN_SAMPLES) {
            long errors = lvlWindow.stream()
                    .filter(l -> l == LogEntry.LogLevel.ERROR || l == LogEntry.LogLevel.FATAL)
                    .count();
            double rate = (double) errors / lvlWindow.size();

            if (rate >= ERROR_RATE_THRESHOLD) {
                entry.setAnomaly(true);
                entry.setAnomalyReason(String.format(
                        "Error rate spike: %.0f%% of recent logs are errors", rate * 100));
            }
        }

        // ── 3. HTTP 5xx on non-error log level ────────────────
        if (!entry.isAnomaly() && entry.getStatusCode() >= 500) {
            entry.setAnomaly(true);
            entry.setAnomalyReason("HTTP " + entry.getStatusCode() + " server error");
        }

        // Maintain windows (trim oldest)
        rtWindow.addLast((double) entry.getResponseTimeMs());
        if (rtWindow.size() > WINDOW_SIZE) rtWindow.pollFirst();

        lvlWindow.addLast(entry.getLevel());
        if (lvlWindow.size() > WINDOW_SIZE) lvlWindow.pollFirst();

        return entry;
    }

    // ── Maths helpers ─────────────────────────────────────────

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double stdev(double[] values, double mean) {
        double variance = 0;
        for (double v : values) variance += (v - mean) * (v - mean);
        return Math.sqrt(variance / values.length);
    }
}
