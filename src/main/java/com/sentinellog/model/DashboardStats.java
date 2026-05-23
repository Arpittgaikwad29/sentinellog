package com.sentinellog.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Aggregated stats snapshot sent to connected clients on each tick.
 */
@Data
@Builder
public class DashboardStats {

    private long totalLogs;
    private long anomalyCount;
    private double anomalyRate;
    private double avgResponseTimeMs;
    private double p99ResponseTimeMs;

    /** Count of logs per level (INFO, WARN, ERROR, etc.) */
    private Map<String, Long> levelCounts;

    /** Count of anomalies per service */
    private Map<String, Long> anomaliesByService;

    /** Rolling log rate: logs per second over last 10 seconds */
    private double logsPerSecond;

    /** Active services detected */
    private int activeServices;
}
