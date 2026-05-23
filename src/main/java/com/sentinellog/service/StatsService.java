package com.sentinellog.service;

import com.sentinellog.model.DashboardStats;
import com.sentinellog.model.LogEntry;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes aggregated statistics from the current log buffer snapshot.
 * Called on each stats broadcast tick.
 */
@Service
public class StatsService {

    private final LogStore logStore;

    // Used for logs-per-second rolling rate
    private long previousTotal = 0;
    private long previousTime  = System.currentTimeMillis();

    public StatsService(LogStore logStore) {
        this.logStore = logStore;
    }

    public DashboardStats compute() {
        List<LogEntry> snapshot = logStore.snapshot();

        long totalLogs    = logStore.getTotalLogsIngested();
        long anomalyCount = logStore.getTotalAnomaliesFound();
        double anomalyRate = totalLogs > 0 ? (double) anomalyCount / totalLogs : 0.0;

        // Response time stats
        DoubleSummaryStatistics rtStats = snapshot.stream()
                .mapToDouble(LogEntry::getResponseTimeMs)
                .summaryStatistics();

        double p99 = computePercentile(snapshot, 99);

        // Level breakdown
        Map<String, Long> levelCounts = snapshot.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getLevel().name(),
                        Collectors.counting()));

        // Anomalies per service
        Map<String, Long> anomaliesByService = snapshot.stream()
                .filter(LogEntry::isAnomaly)
                .collect(Collectors.groupingBy(LogEntry::getService, Collectors.counting()));

        // Active services
        long activeServices = snapshot.stream()
                .map(LogEntry::getService)
                .distinct()
                .count();

        // Logs per second (rolling)
        long now = System.currentTimeMillis();
        long elapsed = Math.max(now - previousTime, 1);
        long delta = totalLogs - previousTotal;
        double logsPerSecond = delta * 1000.0 / elapsed;
        previousTotal = totalLogs;
        previousTime  = now;

        return DashboardStats.builder()
                .totalLogs(totalLogs)
                .anomalyCount(anomalyCount)
                .anomalyRate(Math.round(anomalyRate * 10000.0) / 100.0)
                .avgResponseTimeMs(rtStats.getCount() > 0 ? Math.round(rtStats.getAverage()) : 0)
                .p99ResponseTimeMs(p99)
                .levelCounts(levelCounts)
                .anomaliesByService(anomaliesByService)
                .logsPerSecond(Math.round(logsPerSecond * 10.0) / 10.0)
                .activeServices((int) activeServices)
                .build();
    }

    private double computePercentile(List<LogEntry> entries, int percentile) {
        if (entries.isEmpty()) return 0;
        long[] sorted = entries.stream()
                .mapToLong(LogEntry::getResponseTimeMs)
                .sorted()
                .toArray();
        int idx = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }
}
