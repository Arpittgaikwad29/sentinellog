package com.sentinellog.controller;

import com.sentinellog.model.Alert;
import com.sentinellog.model.DashboardStats;
import com.sentinellog.model.LogEntry;
import com.sentinellog.service.LogStore;
import com.sentinellog.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints used by the dashboard on initial page load and
 * for any client that prefers polling over WebSocket.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LogController {

    private final LogStore     store;
    private final StatsService statsService;

    public LogController(LogStore store, StatsService statsService) {
        this.store        = store;
        this.statsService = statsService;
    }

    /**
     * GET /api/logs?limit=100
     * Returns recent log entries (newest first).
     */
    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(store.getRecentLogs(Math.min(limit, 500)));
    }

    /**
     * GET /api/logs/anomalies
     * Returns only anomalous entries from the current buffer.
     */
    @GetMapping("/logs/anomalies")
    public ResponseEntity<List<LogEntry>> getAnomalies() {
        return ResponseEntity.ok(store.getAnomalies());
    }

    /**
     * GET /api/alerts?limit=50
     * Returns recent alerts.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAlerts(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(store.getRecentAlerts(Math.min(limit, 200)));
    }

    /**
     * GET /api/stats
     * Returns a fresh stats snapshot (same payload as WebSocket /topic/stats).
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(statsService.compute());
    }

    /**
     * GET /api/health
     * Simple health probe for CI / uptime monitors.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", "1.0.0",
                "logs_buffered", String.valueOf(store.getRecentLogs(1).size())));
    }
}
