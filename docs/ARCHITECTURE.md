# SentinelLog — Architecture Deep Dive

This document explains the design decisions behind each major component. The README covers the what; this covers the why.

---

## Why Spring Boot + STOMP, not Node.js + Socket.io?

The Java ecosystem has two things the Node.js equivalent lacks in this context:

1. **Real concurrency** — `@Scheduled` runs on a managed thread pool. Java threads (and in Java 21, virtual threads) handle concurrent WebSocket subscribers without the single-threaded event loop constraint.
2. **Type safety across the stack** — `LogEntry`, `Alert`, and `DashboardStats` are strongly typed. Jackson serialises them to JSON automatically. There is no chance of sending a field with the wrong type to a subscriber.

STOMP specifically was chosen over raw WebSocket because it adds topic-based pub/sub at the protocol level. The broker handles fan-out — `SimpMessagingTemplate.convertAndSend("/topic/logs", entry)` pushes to every subscriber in one line, regardless of how many are connected.

---

## Ring Buffer Design

`LogStore` uses `ConcurrentLinkedDeque` as a bounded ring buffer:

```java
logs.addFirst(entry);           // O(1) — prepend newest
while (logs.size() > MAX_LOGS) // trim
    logs.pollLast();            // O(1) — remove oldest
```

**Why not `ArrayDeque`?** `ArrayDeque` is not thread-safe. The REST layer reads the deque concurrently while the `@Scheduled` thread writes. `ConcurrentLinkedDeque` handles this without external synchronisation.

**Why not `ArrayList`?** `ArrayList.remove(0)` is O(n) — it shifts every element left. At 1,000 entries and 3 inserts/sec, this would cause measurable GC pressure over time.

**Why a deque, not a fixed-size array?** A circular array would need a write pointer and index arithmetic. `ConcurrentLinkedDeque` gives the same O(1) behaviour with simpler code and built-in thread safety.

**Cap at 1,000** — enough for a meaningful display window (~5 minutes of data at 3/sec) without memory growth.

---

## Per-Service Anomaly Isolation

`AnomalyDetectorService` holds:

```java
Map<String, Deque<Double>>         responseWindows  // per-service RT history
Map<String, Deque<LogEntry.LogLevel>> levelWindows  // per-service level history
```

Both are `ConcurrentHashMap` so new services can be added at runtime without locking the whole map.

**Why per-service?** A global window would let one noisy service (e.g. `analytics-svc` with its 200ms baseline) absorb anomalies from a fast service (e.g. `notification-svc` at 30ms). A 150ms response from `notification-svc` would look normal against a global mean dominated by `analytics-svc`.

Per-service windows mean each service is judged against its own history. The `analytics-svc` needs to be 2.5σ above its own 200ms baseline — not above a polluted global mean.

---

## Chaos Injection

`LogGeneratorService` tracks `chaosService` and `chaosRemaining`:

```java
if (chaosRemaining <= 0 && r.nextInt(15) == 0) {
    chaosService   = pickRandomService();
    chaosRemaining = 5 + r.nextInt(8);  // 5–12 anomalous entries
}
```

During a chaos window, the selected service generates 3–8× normal latency and elevated error rates. This produces natural-looking anomaly bursts rather than uniformly random noise — which is more realistic (real incidents tend to cluster) and makes the dashboard more interesting to demo.

---

## Alert Cooldown

Without cooldown:

```
anomaly → alert
anomaly → alert  (500ms later)
anomaly → alert  (1 second later)
anomaly → alert  ...
```

A service degrading for 30 seconds at 3 anomalies/sec would produce ~90 alerts. In a real on-call system, this pages the engineer 90 times for one incident — alert fatigue, the classic cause of engineers ignoring alerts.

With cooldown (set to 20 entries):

```
anomaly count = 1
anomaly count = 2
anomaly count = 3 → ALERT FIRES, cooldown = 20
anomaly ...      → cooldown = 19, 18, 17 ... suppressed
anomaly ...      → cooldown = 0, ready to re-evaluate
```

The service has ~6 seconds (20 entries × 300ms) to recover before a new alert is considered. If it recovers, no second alert. If it does not, a second alert fires with fresh context.

---

## Stats Computation

`StatsService.compute()` takes a snapshot of the `LogStore` deque (a `new ArrayList<>(deque)` copy — O(n) but only called every 2 seconds) and computes:

- Mean response time: single-pass sum / count
- P99: sort the response times array, index into 99th percentile position
- Level counts: `Collectors.groupingBy(level, counting())`
- Logs/sec: `(totalNow - totalPrev) / elapsedMs * 1000`

The snapshot copy ensures stats are computed on a consistent view — no concurrent modifications mid-computation.

---

## Frontend Architecture

`index.html` is intentionally a single file with no build step. Reasons:

1. **Demonstrability** — any interviewer can open it, view source, and understand it immediately. No webpack config, no `node_modules`, no transpilation.
2. **Portability** — the JAR serves it as a static resource. There is nothing to deploy separately.
3. **Clarity** — the DOM manipulation is explicit and readable. React adds abstraction that obscures the WebSocket → DOM update pipeline.

The feed is re-rendered as an HTML string on every log arrival:

```javascript
feed.innerHTML = visible.slice(0, 300).map(renderRow).join('');
```

This is faster than per-element DOM updates for a frequently changing list and avoids virtual DOM overhead. The 300-row display cap prevents scroll performance degradation in long-running sessions.

---

## What Is Not Here (and Why)

| Feature | Why omitted | Natural extension |
|---|---|---|
| Database | Streaming focus — history not the goal | Add JPA + PostgreSQL |
| Authentication | Demo app — no multi-user concern | Spring Security + JWT |
| Real log ingestion | Generator keeps it self-contained | Add `POST /api/ingest` |
| Docker | Adds friction for a Java-first demo | Add `Dockerfile` + compose |
| Metrics endpoint | Not needed for the core demo | Add Micrometer + `/actuator/prometheus` |
| Alert notifications | Dashboard is the notification | Add WebhookService |
