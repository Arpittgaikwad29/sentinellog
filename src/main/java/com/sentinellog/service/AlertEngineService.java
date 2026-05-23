package com.sentinellog.service;

import com.sentinellog.model.Alert;
import com.sentinellog.model.LogEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watches anomaly counts per service and fires alerts when thresholds are crossed.
 *
 * Alert cooldown prevents alert storms: once an alert fires for a service,
 * no new alert will fire for that service for COOLDOWN_ENTRIES entries.
 */
@Service
public class AlertEngineService {

    private static final int ANOMALY_BURST_THRESHOLD = 3;  // N anomalies → alert
    private static final int COOLDOWN_ENTRIES        = 20; // entries before re-alerting

    /** Rolling anomaly counter per service (resets on alert) */
    private final Map<String, AtomicInteger> anomalyCounts = new ConcurrentHashMap<>();

    /** Remaining cooldown entries per service */
    private final Map<String, AtomicInteger> cooldowns = new ConcurrentHashMap<>();

    /**
     * Evaluates a log entry and returns an Alert if one should be fired,
     * or null if no alert is needed.
     */
    public Alert evaluate(LogEntry entry) {
        if (!entry.isAnomaly()) return null;

        String svc = entry.getService();

        // Check cooldown
        AtomicInteger cooldown = cooldowns.computeIfAbsent(svc, k -> new AtomicInteger(0));
        if (cooldown.get() > 0) {
            cooldown.decrementAndGet();
            return null;
        }

        // Increment anomaly burst counter
        AtomicInteger counter = anomalyCounts.computeIfAbsent(svc, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count >= ANOMALY_BURST_THRESHOLD) {
            // Fire alert, reset counter, start cooldown
            counter.set(0);
            cooldown.set(COOLDOWN_ENTRIES);
            return buildAlert(svc, entry, count);
        }

        return null;
    }

    private Alert buildAlert(String service, LogEntry trigger, int count) {
        Alert.AlertSeverity severity = switch (trigger.getLevel()) {
            case FATAL -> Alert.AlertSeverity.CRITICAL;
            case ERROR -> Alert.AlertSeverity.HIGH;
            case WARN  -> Alert.AlertSeverity.MEDIUM;
            default    -> Alert.AlertSeverity.LOW;
        };

        String title = switch (severity) {
            case CRITICAL -> "🔴 Critical anomaly burst in " + service;
            case HIGH     -> "🟠 High error rate in " + service;
            case MEDIUM   -> "🟡 Degraded performance in " + service;
            case LOW      -> "🔵 Minor anomaly cluster in " + service;
        };

        String description = String.format(
                "%d anomalies detected in recent window. Trigger: %s",
                count, trigger.getAnomalyReason());

        return Alert.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .severity(severity)
                .service(service)
                .title(title)
                .description(description)
                .triggerCount(count)
                .build();
    }
}
