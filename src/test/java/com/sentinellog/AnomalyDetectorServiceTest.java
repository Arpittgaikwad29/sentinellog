package com.sentinellog;

import com.sentinellog.model.LogEntry;
import com.sentinellog.service.AnomalyDetectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnomalyDetectorService.
 *
 * Tests both the Z-score path (latency spike) and HTTP 5xx path.
 * No Spring context needed — service is instantiated directly.
 */
class AnomalyDetectorServiceTest {

    private AnomalyDetectorService detector;

    @BeforeEach
    void setUp() {
        detector = new AnomalyDetectorService();
    }

    @Test
    @DisplayName("Normal entries below threshold are not flagged")
    void normalEntriesNotFlagged() {
        for (int i = 0; i < 20; i++) {
            LogEntry e = buildEntry("api-gateway", 100, 200, LogEntry.LogLevel.INFO);
            detector.analyse(e);
            assertThat(e.isAnomaly()).isFalse();
        }
    }

    @Test
    @DisplayName("Latency spike beyond 2.5σ is flagged as anomaly")
    void latencySpikeDetected() {
        // Warm up baseline with consistent low latency
        for (int i = 0; i < 15; i++) {
            LogEntry e = buildEntry("api-gateway", 100, 200, LogEntry.LogLevel.INFO);
            detector.analyse(e);
        }

        // Inject a huge spike
        LogEntry spike = buildEntry("api-gateway", 5000, 200, LogEntry.LogLevel.WARN);
        detector.analyse(spike);

        assertThat(spike.isAnomaly()).isTrue();
        assertThat(spike.getAnomalyReason()).contains("σ from mean");
    }

    @Test
    @DisplayName("HTTP 500 status code is always flagged")
    void http500AlwaysFlagged() {
        LogEntry e = buildEntry("payment-service", 80, 500, LogEntry.LogLevel.ERROR);
        detector.analyse(e);

        assertThat(e.isAnomaly()).isTrue();
        assertThat(e.getAnomalyReason()).contains("500");
    }

    @Test
    @DisplayName("HTTP 503 status code is always flagged")
    void http503AlwaysFlagged() {
        LogEntry e = buildEntry("payment-service", 80, 503, LogEntry.LogLevel.ERROR);
        detector.analyse(e);
        assertThat(e.isAnomaly()).isTrue();
    }

    @Test
    @DisplayName("Different services have independent baselines")
    void servicesAreIsolated() {
        // Warm up service-a with normal latency
        for (int i = 0; i < 15; i++) {
            detector.analyse(buildEntry("service-a", 100, 200, LogEntry.LogLevel.INFO));
        }

        // service-b has NO baseline yet — a spike should not be flagged by z-score
        // (needs MIN_SAMPLES first), but 500 HTTP codes still fire
        LogEntry b = buildEntry("service-b", 5000, 200, LogEntry.LogLevel.INFO);
        detector.analyse(b);
        assertThat(b.isAnomaly()).isFalse(); // no baseline yet → z-score not computed
    }

    @Test
    @DisplayName("Z-score is computed and stored on entries")
    void zScorePopulated() {
        for (int i = 0; i < 15; i++) {
            detector.analyse(buildEntry("auth-service", 100, 200, LogEntry.LogLevel.INFO));
        }
        LogEntry spike = buildEntry("auth-service", 9000, 200, LogEntry.LogLevel.WARN);
        detector.analyse(spike);

        assertThat(spike.getZScore()).isGreaterThan(2.0);
    }

    // ── Helper ──────────────────────────────────────────────
    private LogEntry buildEntry(String service, long responseTimeMs,
                                int statusCode, LogEntry.LogLevel level) {
        return LogEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .service(service)
                .level(level)
                .message("test message")
                .responseTimeMs(responseTimeMs)
                .statusCode(statusCode)
                .anomaly(false)
                .zScore(0.0)
                .build();
    }
}
