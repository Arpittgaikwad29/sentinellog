package com.sentinellog.websocket;

import com.sentinellog.model.Alert;
import com.sentinellog.model.LogEntry;
import com.sentinellog.service.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The heartbeat of SentinelLog.
 *
 * Two scheduled tasks run on independent cadences:
 *
 *  • logPipeline  (every 300ms) — generate a log → detect anomalies →
 *    fire alert if needed → broadcast log + optional alert to WebSocket clients.
 *
 *  • statsBroadcast (every 2s) — compute rolling stats and push to /topic/stats.
 *
 * The SimpMessagingTemplate is Spring's STOMP broadcast mechanism — it serialises
 * the payload to JSON and pushes it to every subscriber of the given topic.
 */
@Component
public class LogBroadcaster {

    private final LogGeneratorService   generator;
    private final AnomalyDetectorService detector;
    private final AlertEngineService    alertEngine;
    private final LogStore              store;
    private final StatsService          stats;
    private final SimpMessagingTemplate broker;

    public LogBroadcaster(LogGeneratorService generator,
                          AnomalyDetectorService detector,
                          AlertEngineService alertEngine,
                          LogStore store,
                          StatsService stats,
                          SimpMessagingTemplate broker) {
        this.generator   = generator;
        this.detector    = detector;
        this.alertEngine = alertEngine;
        this.store       = store;
        this.stats       = stats;
        this.broker      = broker;
    }

    // ── Log pipeline: generate → detect → store → broadcast ──

    @Scheduled(fixedRate = 300)   // ~3–4 logs/sec; tweak to taste
    public void logPipeline() {
        // 1. Generate
        LogEntry entry = generator.generate();

        // 2. Detect anomalies (mutates entry in place)
        detector.analyse(entry);

        // 3. Store
        store.addLog(entry);

        // 4. Broadcast log to all subscribers of /topic/logs
        broker.convertAndSend("/topic/logs", entry);

        // 5. Evaluate alert threshold
        Alert alert = alertEngine.evaluate(entry);
        if (alert != null) {
            store.addAlert(alert);
            // Broadcast alert separately so the UI can render it distinctly
            broker.convertAndSend("/topic/alerts", alert);
        }
    }

    // ── Stats broadcast: every 2 seconds ─────────────────────

    @Scheduled(fixedRate = 2000)
    public void statsBroadcast() {
        broker.convertAndSend("/topic/stats", stats.compute());
    }
}
