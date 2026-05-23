package com.sentinellog.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a single log entry emitted by a service.
 * Anomaly flag and z-score are set by the AnomalyDetectorService.
 */
@Data
@Builder
public class LogEntry {

    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private LogLevel level;

    private String service;

    private String message;

    private long responseTimeMs;

    private int statusCode;

    /** True when this entry is flagged as anomalous by the detector */
    private boolean anomaly;

    /** Z-score of the response time relative to recent baseline; 0 if not computed */
    private double zScore;

    /** Human-readable reason the anomaly was flagged */
    private String anomalyReason;

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR, FATAL
    }
}
