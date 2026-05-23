package com.sentinellog.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * An alert triggered when anomaly thresholds are breached.
 * Sent over WebSocket as a high-priority message.
 */
@Data
@Builder
public class Alert {

    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private AlertSeverity severity;

    private String service;

    private String title;

    private String description;

    /** Number of anomalies that triggered this alert */
    private int triggerCount;

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
