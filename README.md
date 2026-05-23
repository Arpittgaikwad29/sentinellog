<div align="center">

<img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
<img src="https://img.shields.io/badge/WebSocket-STOMP-purple?style=for-the-badge&logo=socket.io&logoColor=white"/>
<img src="https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Tests-Passing-3fb950?style=for-the-badge&logo=github-actions&logoColor=white"/>

<br/><br/>

```
███████╗███████╗███╗   ██╗████████╗██╗███╗   ██╗███████╗██╗      ██████╗  ██████╗
██╔════╝██╔════╝████╗  ██║╚══██╔══╝██║████╗  ██║██╔════╝██║     ██╔═══██╗██╔════╝
███████╗█████╗  ██╔██╗ ██║   ██║   ██║██╔██╗ ██║█████╗  ██║     ██║   ██║██║  ███╗
╚════██║██╔══╝  ██║╚██╗██║   ██║   ██║██║╚██╗██║██╔══╝  ██║     ██║   ██║██║   ██║
███████║███████╗██║ ╚████║   ██║   ██║██║ ╚████║███████╗███████╗╚██████╔╝╚██████╔╝
╚══════╝╚══════╝╚═╝  ╚═══╝   ╚═╝   ╚═╝╚═╝  ╚═══╝╚══════╝╚══════╝ ╚═════╝  ╚═════╝
```

### Real-time log anomaly detection dashboard with WebSocket streaming

*Logs stream in. Anomalies light up red. Alerts fire. All in real time.*

[Quick Start](#-quick-start) · [Architecture](#-architecture) · [How Detection Works](#-anomaly-detection) · [API Reference](#-rest-api-reference) · [Configuration](#-configuration) · [Contributing](#-contributing)

</div>

---

## What Is SentinelLog?

SentinelLog is a production-quality Java application that:

1. **Generates** realistic structured log events from 8 simulated microservices (api-gateway, auth-service, payment-service, etc.)
2. **Detects** anomalies in real time using Z-score statistics + error-rate spike analysis
3. **Streams** every log and alert instantly to connected browsers via WebSocket / STOMP
4. **Visualises** the live feed on a dark terminal-style dashboard — no page refresh, no polling

It runs with a single command and requires nothing but Java 21. No Docker, no Kafka, no Redis, no npm.

---

## Why This Project Stands Out

| Skill | How it's shown |
|---|---|
| **WebSocket / STOMP** | Full duplex streaming — rarer than REST in most Java portfolios |
| **Concurrency** | `ConcurrentLinkedDeque` ring buffer, `AtomicLong` counters, per-service thread-safe state |
| **Statistical ML** | Z-score sliding window adapts per service — no hardcoded thresholds |
| **Clean architecture** | Model → Service → WebSocket → REST — each layer has one job |
| **Java 21** | Records, switch expressions, text blocks, virtual threads ready |
| **Zero-dependency frontend** | Vanilla JS + Canvas sparkline — no React, no build step, no npm |
| **Production instincts** | Alert cooldown, ring buffer cap, P99 percentile, per-service isolation |

---

## Dashboard Preview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ ⚡ SentinelLog    Total: 12.4K   Anomalies: 847   Rate: 6.8%   3.3 logs/sec  ●Live│
├──────────────┬──────────────────────────────────────────┬──────────────────────┤
│ Services     │ [ALL] [ANOMALY] [ERROR] [WARN]  filter…  │ Performance          │
│              ├──────────────────────────────────────────┤ Avg RT    P99 RT     │
│ ● api-gate.. │ TIME     LEVEL   SERVICE        MESSAGE  │  142ms     891ms     │
│ ⚠ payment.. │ 14:32:01 INFO    api-gateway    Request  │                      │
│ ● auth-svc  │ 14:32:01 ERROR   payment-svc    Connect  │ Services  Anomalies  │
│ ⚠ search-s  │ 14:32:00 WARN ⚠  auth-service   Retry 2  │    8        847      │
│              │ 14:32:00 FATAL ⚠ payment-svc   Deadlock │                      │
│ Recent Alerts│ 14:31:59 INFO    inventory-svc  Cache hit│ Log Throughput       │
│              │ 14:31:59 ERROR ⚠ search-svc    Timeout  │ ▁▂▃▄▂▅▃▄▅▆▄▃▅▄▂▃▄▅  │
│ 🔴 Critical  │ 14:31:58 INFO    api-gateway    Auth ok  │                      │
│ payment-svc  │ 14:31:58 WARN    notification   Queue 78%│ Level Distribution   │
│ 3 anomalies  │ 14:31:57 DEBUG   analytics-svc  Fetch    │ INFO  ████████ 8,921 │
│              │                                    ...   │ WARN  ███      1,203 │
│ 🟠 High      │                                          │ ERROR ██         847 │
│ auth-service │                                          │ FATAL ▌           23 │
└──────────────┴──────────────────────────────────────────┴──────────────────────┘
```

Anomalous rows flash red on arrival. Alerts appear in the sidebar with severity colour-coding. Stats refresh every 2 seconds without any page interaction.

---

## Quick Start

**Prerequisites:** Java 21+ and that's it.

```bash
# 1. Clone
git clone https://github.com/yourusername/sentinellog.git
cd sentinellog

# 2. Run (Maven wrapper included — no local Maven needed)
./mvnw spring-boot:run

# 3. Open the dashboard
open http://localhost:8080
```

Logs start streaming immediately. You will see your first anomaly within ~10 seconds as the chaos injector kicks in.

**Other commands:**

```bash
# Run the test suite
./mvnw test

# Build a self-contained fat JAR
./mvnw package -DskipTests
java -jar target/sentinellog-1.0.0.jar

# Run with a different port
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      Spring Boot 3 / Java 21                      │
│                                                                    │
│   ┌─────────────────────┐      ┌──────────────────────────────┐   │
│   │  LogGeneratorService │─────►│   AnomalyDetectorService     │   │
│   │                      │      │                              │   │
│   │  8 simulated svcs    │      │  • Z-score sliding window    │   │
│   │  Chaos injection     │      │  • Error-rate spike rule     │   │
│   │  every ~15 entries   │      │  • HTTP 5xx unconditional    │   │
│   └─────────────────────┘      └──────────────┬───────────────┘   │
│            │ LogEntry                          │ LogEntry (flagged) │
│            ▼                                   ▼                   │
│   ┌──────────────────────────────────────────────────────────┐    │
│   │                        LogStore                           │    │
│   │        ConcurrentLinkedDeque  (ring buffer, cap 1000)     │    │
│   └───────────────────────────────┬──────────────────────────┘    │
│                                   │                                │
│                     ┌─────────────▼──────────────┐                │
│                     │      AlertEngineService      │                │
│                     │   burst threshold (>=3)      │                │
│                     │   cooldown (20 entries)      │                │
│                     └─────────────┬───────────────┘                │
│                                   │                                │
│   ┌───────────────────────────────▼─────────────────────────────┐ │
│   │                      LogBroadcaster                          │ │
│   │              @Scheduled — runs every 300ms                   │ │
│   │                                                              │ │
│   │  generate → detect → store → broadcast log                   │ │
│   │  evaluate alert → store → broadcast alert (if triggered)     │ │
│   │  broadcast stats every 2 seconds                             │ │
│   └───────┬──────────────────────────────────────────────────────┘ │
│           │ SimpMessagingTemplate (STOMP broker)                    │
│           │                                                         │
│  /topic/logs   ──► JSON LogEntry  (~3/sec)                         │
│  /topic/alerts ──► JSON Alert     (on threshold breach)            │
│  /topic/stats  ──► JSON Stats     (every 2 seconds)                │
│                                                                     │
│  REST: /api/logs  /api/logs/anomalies  /api/alerts  /api/stats     │
│        /api/health                                                  │
└─────────────────────────────────────────────────────────────────────┘
                           |
               SockJS + STOMP (WebSocket with fallback)
                           |
          ┌────────────────▼──────────────────────┐
          │          Browser Dashboard             │
          │   (Vanilla JS · no build step)         │
          │                                        │
          │  Live Log Feed  │  Alerts  │  Stats    │
          │  filter/search  │ severity │  P99 RT   │
          │  red flash on   │ sidebar  │  sparkline │
          │  anomaly rows   │ cooldown │  level bars│
          └───────────────────────────────────────┘
```

### Data flow — one tick (300ms)

```
LogGeneratorService.generate()
        │  picks a service, simulates latency + level
        │  injects chaos every ~15 entries (3-8x slower)
        ▼
AnomalyDetectorService.analyse(entry)
        │  updates per-service Deque<Double> (response times)
        │  updates per-service Deque<LogLevel> (levels)
        │  computes z = (rt - mean) / stdev
        │  flags entry if |z| > 2.5, error rate > 40%, or HTTP >= 500
        ▼
LogStore.addLog(entry)
        │  addFirst() to ConcurrentLinkedDeque
        │  pollLast() if size > 1000
        │  increments AtomicLong counters
        ▼
SimpMessagingTemplate → /topic/logs
        ▼
AlertEngineService.evaluate(entry)
        │  if counter >= 3 AND cooldown == 0: fire Alert
        │  reset counter, start 20-entry cooldown
        ▼
SimpMessagingTemplate → /topic/alerts  (if fired)
```

---

## Anomaly Detection

SentinelLog uses three complementary detection rules, applied independently per service. Each service maintains its own sliding window so a slow service does not pollute baselines for fast ones.

### Rule 1 — Z-score on response time

```
z = (responseTimeMs - windowMean) / windowStdDev
```

| Parameter | Value | Meaning |
|---|---|---|
| Window size | 50 entries | Rolling sample per service |
| Min samples | 10 | Warm-up before scoring (avoids cold-start false positives) |
| Threshold | `|z| > 2.5` | Flags ~1.2% of entries under a normal distribution |

The computed z-score is stored on the `LogEntry` object and visible in the UI tooltip.

**Why Z-score instead of a fixed threshold?**
A fixed "flag anything > 500ms" rule fails in practice. The `analytics-svc` baseline is 200ms; the `notification-svc` baseline is 30ms. A 400ms response is totally normal for analytics but a severe anomaly for notifications. Z-score adapts automatically — same technique used in production APM tools like Datadog and New Relic.

### Rule 2 — Error-rate spike

```
errorRate = count(ERROR or FATAL in window) / windowSize
```

Fires when `errorRate >= 40%` of the recent window, even if individual response times look normal (e.g. fast-failing services).

**Why?** A service returning immediate 500 errors looks fast by latency but is clearly broken. This catches silent fast-fail scenarios that Z-score misses.

### Rule 3 — HTTP 5xx status (unconditional)

Any entry with `statusCode >= 500` is flagged immediately, regardless of response time or error rate. A 500 is always anomalous — no threshold needed.

### Alert engine

```
anomalyCounter[service]++

if anomalyCounter[service] >= 3 AND cooldown[service] == 0:
    fire Alert
    anomalyCounter[service] = 0
    cooldown[service] = 20           ← no new alert for 20 entries

else if cooldown[service] > 0:
    cooldown[service]--
```

Alert severity mapping:

| Log level that triggered | Alert severity |
|---|---|
| `FATAL` | `CRITICAL` |
| `ERROR` | `HIGH` |
| `WARN` | `MEDIUM` |
| `INFO` / `DEBUG` | `LOW` |

---

## Project Structure

```
sentinellog/
│
├── pom.xml                                    Maven dependencies + Spring Boot parent
├── mvnw                                       Maven wrapper (no local Maven needed)
├── .gitignore
├── README.md
│
├── .github/
│   ├── CONTRIBUTING.md                        Contribution guidelines
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
│
├── docs/
│   └── ARCHITECTURE.md                        Deep-dive design decisions
│
└── src/
    ├── main/
    │   ├── java/com/sentinellog/
    │   │   │
    │   │   ├── SentinelLogApplication.java    Entry point + @EnableScheduling
    │   │   │
    │   │   ├── config/
    │   │   │   └── WebSocketConfig.java       STOMP broker + /ws endpoint + SockJS
    │   │   │
    │   │   ├── model/                         Pure data — zero business logic
    │   │   │   ├── LogEntry.java              id, timestamp, level, service, message,
    │   │   │   │                              responseTimeMs, statusCode, anomaly,
    │   │   │   │                              zScore, anomalyReason
    │   │   │   ├── Alert.java                 id, timestamp, severity, service,
    │   │   │   │                              title, description, triggerCount
    │   │   │   └── DashboardStats.java        totalLogs, anomalyCount, avgRT,
    │   │   │                                  p99RT, levelCounts, logsPerSecond
    │   │   │
    │   │   ├── service/                       All business logic lives here
    │   │   │   ├── LogGeneratorService.java   8 service profiles, chaos injection,
    │   │   │   │                              realistic messages + latency patterns
    │   │   │   ├── AnomalyDetectorService.java Z-score + error-rate + HTTP rules,
    │   │   │   │                              per-service ConcurrentHashMap windows
    │   │   │   ├── AlertEngineService.java    burst counter + cooldown per service
    │   │   │   ├── LogStore.java              ConcurrentLinkedDeque ring buffer,
    │   │   │   │                              AtomicLong counters, snapshot()
    │   │   │   └── StatsService.java          P99, level breakdown, logs/sec rate
    │   │   │
    │   │   ├── websocket/
    │   │   │   └── LogBroadcaster.java        @Scheduled pipeline — the core loop
    │   │   │
    │   │   └── controller/
    │   │       └── LogController.java         5 REST endpoints + CORS
    │   │
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    │           └── index.html                 Full dashboard — 600 lines, zero deps
    │
    └── test/java/com/sentinellog/
        ├── AnomalyDetectorServiceTest.java    6 unit tests, no Spring context
        └── LogControllerIntegrationTest.java  5 MockMvc integration tests
```

---

## REST API Reference

Base URL: `http://localhost:8080`

All responses are `application/json`. No authentication required.

---

### GET /api/logs

Returns recent log entries from the ring buffer, newest first.

**Query parameters:**

| Parameter | Type | Default | Max | Description |
|---|---|---|---|---|
| `limit` | int | 100 | 500 | Number of entries to return |

**Example:**
```bash
curl http://localhost:8080/api/logs?limit=2
```

**Response:**
```json
[
  {
    "id": "f3a2b1c0-1234-5678-abcd-ef0123456789",
    "timestamp": "2024-03-15T14:32:01.234Z",
    "level": "ERROR",
    "service": "payment-service",
    "message": "Connection timeout after 4821ms",
    "responseTimeMs": 4821,
    "statusCode": 503,
    "anomaly": true,
    "zScore": 3.74,
    "anomalyReason": "Response time 4821ms is 3.7σ from mean (612ms)"
  },
  {
    "id": "a1b2c3d4-abcd-ef01-2345-678901234567",
    "timestamp": "2024-03-15T14:32:00.891Z",
    "level": "INFO",
    "service": "api-gateway",
    "message": "Request processed successfully",
    "responseTimeMs": 47,
    "statusCode": 200,
    "anomaly": false,
    "zScore": 0.0,
    "anomalyReason": null
  }
]
```

---

### GET /api/logs/anomalies

Returns only anomalous entries from the current buffer.

```bash
curl http://localhost:8080/api/logs/anomalies
```

**Response:**
```json
[
  {
    "id": "...",
    "timestamp": "2024-03-15T14:31:58.003Z",
    "level": "FATAL",
    "service": "payment-service",
    "message": "Database deadlock detected on transaction 7823",
    "responseTimeMs": 6102,
    "statusCode": 500,
    "anomaly": true,
    "zScore": 5.12,
    "anomalyReason": "Response time 6102ms is 5.1σ from mean (580ms)"
  }
]
```

---

### GET /api/alerts

Returns recent alerts, newest first.

**Query parameters:**

| Parameter | Type | Default | Max |
|---|---|---|---|
| `limit` | int | 50 | 200 |

```bash
curl http://localhost:8080/api/alerts?limit=3
```

**Response:**
```json
[
  {
    "id": "c0ffee00-dead-beef-cafe-123456789abc",
    "timestamp": "2024-03-15T14:31:55.120Z",
    "severity": "CRITICAL",
    "service": "payment-service",
    "title": "🔴 Critical anomaly burst in payment-service",
    "description": "3 anomalies detected in recent window. Trigger: Response time 6102ms is 5.1σ from mean",
    "triggerCount": 3
  },
  {
    "id": "deadbeef-cafe-1234-5678-abcdef012345",
    "timestamp": "2024-03-15T14:30:12.440Z",
    "severity": "HIGH",
    "service": "auth-service",
    "title": "🟠 High error rate in auth-service",
    "description": "3 anomalies detected in recent window. Trigger: HTTP 503 server error",
    "triggerCount": 3
  }
]
```

---

### GET /api/stats

Returns a fresh aggregated stats snapshot. Same shape as the `/topic/stats` WebSocket payload.

```bash
curl http://localhost:8080/api/stats
```

**Response:**
```json
{
  "totalLogs": 12483,
  "anomalyCount": 847,
  "anomalyRate": 6.78,
  "avgResponseTimeMs": 142,
  "p99ResponseTimeMs": 891,
  "levelCounts": {
    "INFO":  8921,
    "WARN":  1203,
    "ERROR":  847,
    "FATAL":   23,
    "DEBUG":  489
  },
  "anomaliesByService": {
    "payment-service": 312,
    "auth-service":    198,
    "search-service":  147,
    "api-gateway":      90,
    "user-service":     60,
    "analytics-svc":    40
  },
  "logsPerSecond": 3.3,
  "activeServices": 8
}
```

---

### GET /api/health

Lightweight health probe for CI pipelines and uptime monitors.

```bash
curl http://localhost:8080/api/health
```

**Response:**
```json
{
  "status": "UP",
  "version": "1.0.0",
  "logs_buffered": "1"
}
```

Always returns HTTP `200 OK` while the app is running.

---

## WebSocket Reference

SentinelLog uses **STOMP over SockJS** — the standard Spring WebSocket stack. SockJS automatically selects the best transport (WebSocket → long-polling) based on what the client supports.

### Connection endpoint

```
WebSocket: ws://localhost:8080/ws
SockJS:    http://localhost:8080/ws
```

### Topics

| Topic | Payload | Frequency | Description |
|---|---|---|---|
| `/topic/logs` | `LogEntry` | ~3/sec | Every log entry including anomalies |
| `/topic/alerts` | `Alert` | On burst | Fires when >= 3 anomalies hit same service |
| `/topic/stats` | `DashboardStats` | Every 2s | Rolling aggregated metrics |

### Connecting — JavaScript (SockJS + STOMP)

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);
client.debug = null; // silence STOMP debug logs

client.connect({}, () => {
  console.log('Connected to SentinelLog');

  // Live log stream
  client.subscribe('/topic/logs', msg => {
    const log = JSON.parse(msg.body);
    if (log.anomaly) {
      console.warn(`ANOMALY [${log.service}] z=${log.zScore} — ${log.anomalyReason}`);
    }
  });

  // Alerts
  client.subscribe('/topic/alerts', msg => {
    const alert = JSON.parse(msg.body);
    console.error(`${alert.severity}: ${alert.title}`);
  });

  // Rolling stats
  client.subscribe('/topic/stats', msg => {
    const stats = JSON.parse(msg.body);
    document.title = `Anomaly rate: ${stats.anomalyRate}%`;
  });
});
```

### Connecting — Python

```python
import json
import websocket

def on_message(ws, message):
    if '"anomaly":true' in message:
        print("ANOMALY detected:", message[:200])

ws = websocket.WebSocketApp(
    "ws://localhost:8080/ws/websocket",
    on_message=on_message
)
ws.run_forever()
```

---

## Configuration

### application.properties

```properties
# Server port
server.port=8080

# WebSocket message size limits
spring.websocket.max-text-message-size=65536
spring.websocket.max-binary-message-size=65536

# Logging
logging.level.com.sentinellog=INFO
logging.level.org.springframework.web.socket=WARN
```

### Anomaly sensitivity

Edit constants at the top of `AnomalyDetectorService.java`:

```java
private static final int    WINDOW_SIZE          = 50;   // rolling window per service
private static final int    MIN_SAMPLES          = 10;   // warm-up before scoring
private static final double ZSCORE_THRESHOLD     = 2.5;  // lower = more sensitive
private static final double ERROR_RATE_THRESHOLD = 0.40; // lower = more sensitive
```

### Log generation rate

Edit `LogBroadcaster.java`:

```java
@Scheduled(fixedRate = 300)   // ms between entries
// 300ms = ~3.3 logs/sec  (default)
// 100ms = ~10 logs/sec
//  50ms = ~20 logs/sec   (stress test)
```

### Alert thresholds

Edit `AlertEngineService.java`:

```java
private static final int ANOMALY_BURST_THRESHOLD = 3;   // anomalies before alert fires
private static final int COOLDOWN_ENTRIES        = 20;  // entries before re-alerting
```

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | **Java 21** | Records, switch expressions, text blocks; virtual thread ready |
| Framework | **Spring Boot 3.2** | Auto-config, embedded Tomcat, `@Scheduled` |
| WebSocket | **Spring WebSocket + STOMP + SockJS** | Topic-based pub/sub, browser fallback |
| Build | **Maven 3.9** | Wrapper included — no local install needed |
| Frontend | **Vanilla JS + Canvas** | Zero build step, clone and run |
| Testing | **JUnit 5 + MockMvc** | Unit + integration coverage |
| Boilerplate | **Lombok** | `@Data`, `@Builder` — keeps models readable |

**Why no database?** The ring buffer is intentional. SentinelLog is a streaming system, not a warehouse. Adding PostgreSQL, InfluxDB, or Elasticsearch is a natural next step and is listed in the ideas below.

**Why no Kafka/Redis?** Same reasoning — the project demonstrates streaming and concurrency without infrastructure complexity. Anyone can clone and run in 30 seconds.

---

## Tests

```bash
./mvnw test
```

**Unit tests** (`AnomalyDetectorServiceTest.java`) — no Spring context, instant:

```
PASS  Normal entries below threshold are not flagged
PASS  Latency spike beyond 2.5σ is flagged as anomaly
PASS  HTTP 500 status code is always flagged
PASS  HTTP 503 status code is always flagged
PASS  Different services have independent baselines
PASS  Z-score is computed and stored on entries
```

**Integration tests** (`LogControllerIntegrationTest.java`) — full Spring context via MockMvc:

```
PASS  GET /api/health returns UP status
PASS  GET /api/logs returns a JSON array
PASS  GET /api/logs/anomalies returns a JSON array
PASS  GET /api/alerts returns a JSON array
PASS  GET /api/stats returns expected fields
```

---

## Interview Talking Points

**"Why WebSocket instead of polling or SSE?"**
STOMP over WebSocket gives topic-based pub/sub. `/topic/logs`, `/topic/alerts`, and `/topic/stats` are independent channels on one connection. SSE is one-way only. Polling at 3 entries/sec would require a new HTTP request every 300ms, wasting bandwidth and adding latency. WebSocket holds one connection and the server pushes when it has data.

**"Why Z-score instead of a fixed threshold?"**
Fixed thresholds do not generalise. The `analytics-svc` baseline is 200ms; `notification-svc` is 30ms. A 400ms response is normal for analytics and anomalous for notifications. Z-score computes relative deviation from each service's own rolling baseline — it adapts automatically without per-service configuration. Same technique used in Datadog, New Relic, and other production APM tools.

**"How does the ring buffer prevent memory leaks?"**
`ConcurrentLinkedDeque` gives O(1) `addFirst()` and `pollLast()`. After every insert we check `size() > 1000` and call `pollLast()`. The deque never exceeds 1,001 elements. Using ArrayList would require copying on trim — LinkedDeque is pointer manipulation only. `ConcurrentLinkedDeque` (vs `ArrayDeque`) is safe for concurrent producers without external synchronisation.

**"How does the alert cooldown prevent storms?"**
Each service has an `AtomicInteger` burst counter and a cooldown counter. When the counter reaches 3, an alert fires, counter resets, and cooldown is set to 20. On every subsequent entry for that service, cooldown decrements. No new alert fires until cooldown reaches 0. A flapping service cannot flood the alert channel — a real operational concern in any on-call system.

**"What is the concurrency model?"**
One `@Scheduled` thread runs `logPipeline()` every 300ms. `ConcurrentLinkedDeque` handles concurrent reads from the REST layer while the scheduler writes. `AtomicLong` counters in `LogStore` avoid synchronisation overhead. `ConcurrentHashMap` in `AnomalyDetectorService` isolates per-service state without a global lock. The STOMP broker fans out to subscribers on its own thread pool.

**"How would you scale this?"**
Replace in-memory `LogStore` with Redis Streams or Kafka. Replace `@Scheduled` generator with a real `POST /api/ingest` endpoint. Deploy multiple instances behind a load balancer with a shared STOMP broker (RabbitMQ or Redis pub/sub). The service layer is stateless enough that horizontal scaling is mostly infrastructure, not code.

---

## Ideas for Extension

- [ ] Persist logs to PostgreSQL or InfluxDB for historical queries
- [ ] Add `POST /api/ingest` to accept logs from real services
- [ ] Docker + docker-compose setup for one-command infra
- [ ] Configurable anomaly rules via `application.properties` (no recompile)
- [ ] Export anomaly report as CSV download
- [ ] Grafana-compatible `/metrics` endpoint via Micrometer
- [ ] Email / Slack / PagerDuty alert integration
- [ ] Multi-tenant support with per-user dashboards

---

## Contributing

See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for how to report bugs, suggest features, and submit pull requests.

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit with a clear message: `git commit -m "feat: add X"`
4. Push and open a pull request

---

## License

MIT License — free to use, modify, and distribute.

```
Copyright (c) 2024 SentinelLog Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is furnished
to do so.
```

---

<div align="center">

Built with Java 21 + Spring Boot · WebSocket streaming · Statistical anomaly detection

*Star the repo if this helped you*

</div>
