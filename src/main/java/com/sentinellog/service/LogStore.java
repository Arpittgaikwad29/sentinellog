package com.sentinellog.service;

import com.sentinellog.model.Alert;
import com.sentinellog.model.LogEntry;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory store for log entries and alerts.
 *
 * Uses a ConcurrentLinkedDeque as a bounded ring buffer (capped at MAX_SIZE).
 * All reads return immutable snapshots — safe to pass across threads.
 */
@Service
public class LogStore {

    private static final int MAX_LOGS   = 1_000;
    private static final int MAX_ALERTS = 200;

    private final ConcurrentLinkedDeque<LogEntry> logs   = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Alert>    alerts = new ConcurrentLinkedDeque<>();

    private final AtomicLong totalLogsIngested    = new AtomicLong(0);
    private final AtomicLong totalAnomaliesFound  = new AtomicLong(0);

    // ──────────────────────────────────────────────────────────
    // Log operations
    // ──────────────────────────────────────────────────────────

    public void addLog(LogEntry entry) {
        logs.addFirst(entry);
        totalLogsIngested.incrementAndGet();
        if (entry.isAnomaly()) totalAnomaliesFound.incrementAndGet();

        // Trim to ring-buffer size
        while (logs.size() > MAX_LOGS) logs.pollLast();
    }

    /** Returns the most recent N logs (newest first). */
    public List<LogEntry> getRecentLogs(int limit) {
        return logs.stream().limit(limit).collect(Collectors.toList());
    }

    /** Returns all anomalous logs from the current buffer. */
    public List<LogEntry> getAnomalies() {
        return logs.stream().filter(LogEntry::isAnomaly).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────
    // Alert operations
    // ──────────────────────────────────────────────────────────

    public void addAlert(Alert alert) {
        alerts.addFirst(alert);
        while (alerts.size() > MAX_ALERTS) alerts.pollLast();
    }

    public List<Alert> getRecentAlerts(int limit) {
        return alerts.stream().limit(limit).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────
    // Counters
    // ──────────────────────────────────────────────────────────

    public long getTotalLogsIngested()   { return totalLogsIngested.get(); }
    public long getTotalAnomaliesFound() { return totalAnomaliesFound.get(); }

    /** Snapshot of all current log entries for stats computation. */
    public List<LogEntry> snapshot() {
        return new ArrayList<>(logs);
    }
}
