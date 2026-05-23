package com.sentinellog.service;

import com.sentinellog.model.LogEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic-looking log entries from simulated microservices.
 *
 * Every ~15 entries a "chaos event" is injected — a burst of slow responses
 * or errors from one service — so anomalies appear naturally without being
 * purely random.
 */
@Service
public class LogGeneratorService {

    // ── Simulated microservices ───────────────────────────────
    private static final List<ServiceProfile> SERVICES = List.of(
            new ServiceProfile("api-gateway",      50,  30,  200,  0.02, 0.01),
            new ServiceProfile("auth-service",     80,  20,  150,  0.03, 0.02),
            new ServiceProfile("user-service",     60,  25,  180,  0.04, 0.01),
            new ServiceProfile("payment-service",  90,  40,  250,  0.05, 0.03),
            new ServiceProfile("inventory-svc",    40,  15,  120,  0.02, 0.01),
            new ServiceProfile("notification-svc", 30,  10,   90,  0.01, 0.01),
            new ServiceProfile("search-service",   70,  30,  300,  0.03, 0.02),
            new ServiceProfile("analytics-svc",   200,  80,  600,  0.02, 0.01)
    );

    private static final List<String> MESSAGES_INFO = List.of(
            "Request processed successfully",
            "Cache hit for key user:session:%d",
            "User %d authenticated via JWT",
            "Order %d created, dispatched to queue",
            "Health check passed",
            "Database connection pool: %d/%d active",
            "Fetched %d records in %dms",
            "Rate limit: %d/%d requests consumed"
    );

    private static final List<String> MESSAGES_WARN = List.of(
            "Retry attempt %d of 3 for downstream call",
            "Response time degradation detected: %dms",
            "Cache miss — falling back to database",
            "Circuit breaker in HALF_OPEN state",
            "Queue depth at %d%% capacity",
            "Memory usage at %d%%"
    );

    private static final List<String> MESSAGES_ERROR = List.of(
            "Connection timeout after %dms",
            "NullPointerException in OrderService.process()",
            "Database deadlock detected on transaction %d",
            "Authentication token expired or invalid",
            "Service %s unavailable — max retries exceeded",
            "Unhandled exception: %s"
    );

    private static final List<String> EXCEPTION_NAMES = List.of(
            "SocketTimeoutException", "OptimisticLockException",
            "IllegalStateException", "DataIntegrityViolationException"
    );

    private static final List<String> HTTP_PATHS = List.of(
            "/api/v1/users/%d", "/api/v1/orders/%d", "/api/v1/products/%d",
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/search",
            "/health", "/metrics", "/api/v1/cart/%d", "/api/v1/payments/%d"
    );

    private final Random rng = new Random();
    private int entryCount = 0;

    // Track per-service chaos state
    private String chaosService = null;
    private int    chaosRemaining = 0;

    /**
     * Generates and returns a single log entry. Called on a schedule.
     */
    public LogEntry generate() {
        entryCount++;
        Random r = ThreadLocalRandom.current();

        // ── Chaos injection (every ~15 normal entries) ────────
        if (chaosRemaining <= 0 && r.nextInt(15) == 0) {
            int idx = r.nextInt(SERVICES.size());
            chaosService = SERVICES.get(idx).name();
            chaosRemaining = 5 + r.nextInt(8);   // 5–12 anomalous entries
        }

        ServiceProfile profile = pickService(r);
        boolean inChaos = chaosRemaining > 0 && profile.name().equals(chaosService);
        if (inChaos) chaosRemaining--;

        // Determine log level
        LogEntry.LogLevel level = pickLevel(profile, inChaos, r);

        // Determine response time
        long responseTime = pickResponseTime(profile, inChaos, r);

        // Determine HTTP status
        int statusCode = pickStatusCode(level, r);

        // Build message
        String message = buildMessage(level, profile, responseTime, r);

        return LogEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .level(level)
                .service(profile.name())
                .message(message)
                .responseTimeMs(responseTime)
                .statusCode(statusCode)
                .anomaly(false)
                .zScore(0.0)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────

    private ServiceProfile pickService(Random r) {
        return SERVICES.get(r.nextInt(SERVICES.size()));
    }

    private LogEntry.LogLevel pickLevel(ServiceProfile p, boolean chaos, Random r) {
        double roll = r.nextDouble();
        if (chaos) {
            if (roll < 0.35) return LogEntry.LogLevel.ERROR;
            if (roll < 0.55) return LogEntry.LogLevel.WARN;
            if (roll < 0.60) return LogEntry.LogLevel.FATAL;
        } else {
            if (roll < p.errorRate())  return LogEntry.LogLevel.ERROR;
            if (roll < p.errorRate() + p.warnRate()) return LogEntry.LogLevel.WARN;
            if (roll < p.errorRate() + p.warnRate() + 0.001) return LogEntry.LogLevel.FATAL;
            if (roll < p.errorRate() + p.warnRate() + 0.05)  return LogEntry.LogLevel.DEBUG;
        }
        return LogEntry.LogLevel.INFO;
    }

    private long pickResponseTime(ServiceProfile p, boolean chaos, Random r) {
        if (chaos) {
            // Chaos: response time 3–8× normal
            double multiplier = 3.0 + r.nextDouble() * 5.0;
            return (long)(p.baseResponseMs() * multiplier + r.nextGaussian() * p.jitterMs());
        }
        long rt = (long)(p.baseResponseMs() + r.nextGaussian() * p.jitterMs());
        return Math.max(1, rt);
    }

    private int pickStatusCode(LogEntry.LogLevel level, Random r) {
        return switch (level) {
            case ERROR -> r.nextBoolean() ? 500 : 503;
            case FATAL -> 500;
            case WARN  -> r.nextInt(4) == 0 ? 429 : 200;
            default    -> 200;
        };
    }

    private String buildMessage(LogEntry.LogLevel level, ServiceProfile profile, long rt, Random r) {
        int id1 = 1000 + r.nextInt(9000);
        int id2 = 10   + r.nextInt(90);

        return switch (level) {
            case INFO, DEBUG -> {
                String tpl = MESSAGES_INFO.get(r.nextInt(MESSAGES_INFO.size()));
                yield String.format(tpl.replace("%d", "%1$d"), id1, id2);
            }
            case WARN -> {
                String tpl = MESSAGES_WARN.get(r.nextInt(MESSAGES_WARN.size()));
                yield String.format(tpl.replace("%d", "%1$d"), rt, id2);
            }
            case ERROR, FATAL -> {
                String tpl = MESSAGES_ERROR.get(r.nextInt(MESSAGES_ERROR.size()));
                if (tpl.contains("%s")) {
                    String sub = tpl.contains("Service") ?
                            profile.name() :
                            EXCEPTION_NAMES.get(r.nextInt(EXCEPTION_NAMES.size()));
                    yield String.format(tpl, sub);
                }
                yield String.format(tpl.replace("%d", "%1$d"), rt, id2);
            }
        };
    }

    /**
     * Immutable profile for a simulated service.
     *
     * @param name           service identifier
     * @param baseResponseMs typical response latency
     * @param jitterMs       standard deviation of latency (normal distribution)
     * @param maxNormalMs    highest non-anomalous response time
     * @param errorRate      fraction of entries that are ERROR
     * @param warnRate       fraction of entries that are WARN
     */
    private record ServiceProfile(
            String name,
            long   baseResponseMs,
            long   jitterMs,
            long   maxNormalMs,
            double errorRate,
            double warnRate
    ) {}
}
