# Smart Farm Bot — Technical Architecture

Generated from the actual source tree (`src/farmbot/`, `test/`, `public/`). Every
claim below cites a real file; line numbers are current as of this writing. Anything
that differs from earlier step summaries is called out under **⚠ Discrepancies**.

---

## 0. Key numbers

| Metric | Value | Source |
|---|---|---|
| Packages under `src/farmbot/` | **11** | `concurrency, core, data, exception, model, module, nav, rules, sensor, user, web` |
| Production classes | **64** | `find src -name '*.java'` |
| Production LOC | **4230** | `wc -l` over `src/**/*.java` |
| Test classes | **5** | `find test -name '*.java'` |
| Tests (methods) | **25** | JUnit run, all passing |
| Test LOC | **346** | `wc -l` over `test/**/*.java` |
| DB tables created | **4** (`sensor_readings`, `obstacle_events`, `fault_log`, `modules`) | `SqliteSensorRepository.initSchema()` |
| DB tables actually written | **3** (`sensor_readings`, `fault_log`, `obstacle_events`) | only these have `INSERT INTO` |
| HTTP endpoints | **11** | `ApiServer.handleApi` |
| Long-lived threads / pools | **4** (sensor-poller, db-writer, nav-ticker, web pool of 8) | + transient `ForkJoinPool` during polls, + 2 shutdown-hook threads |
| Seed CSV files | **8** | `data/seed/*.csv` |
| `instanceof` in the 3 domain seams | **0** | all 4 real `instanceof` are in `JsonUtil` (serialization) |

---

## 1. Package map

| Package | Classes | Responsibility |
|---|---|---|
| `model` | `SensorType, BotStatus, Direction, ModuleState, ReadingStatus, StatusLabel, Thresholds` | Enums + the one constants holder; no behaviour beyond lookups |
| `exception` | `FarmBotException, SensorFailureException, LineLostException, ModuleNotReadyException, InvalidSpeedException, AuthenticationException, DataAccessException` | Custom exception hierarchy (4 checked under `FarmBotException`, 2 unchecked) |
| `sensor` | `Component, Sensor, LineSensor, ObstacleSensor, SoilMoistureSensor, SoilTemperatureSensor, BatterySensor, CameraSensor, SignalSource, SimulatedSignal` | Hardware abstraction; `read()` template method; the simulated-signal seam |
| `module` | `MissionModule, BaseModule, SprayerModule, SeedAnalysisModule, Scan3DModule` | Runtime-mounted mission modules behind one interface |
| `nav` | `NavigationStrategy, FollowLineStrategy, SearchStrategy, DetectPlantStrategy, LaneChangeStrategy, ManualStrategy` | Strategy pattern for driving behaviour |
| `rules` | `Rule, LowMoistureRule, ObstacleDensityRule, LowBatteryRule, RecommendationEngine, RuleContext, Recommendation, Severity` | Advisory rules evaluated over a read-only context |
| `user` | `User, Farmer, Technician, Role, AuthService` | Accounts + in-memory SHA-256 authentication |
| `data` | `Reading, SensorRepository, SqliteSensorRepository, SeedLoader, FaultRecord, ObstacleEvent` | Immutable model + DAO + JDBC impl + CSV importer |
| `concurrency` | `SensorPoller, DbWriter, FarmBotRuntime` | Producer, consumer, and the thread/lifecycle owner |
| `core` | `SmartFarmBot, DriveTrain, Motor, Odometer` | Composition root + drivetrain/odometry |
| `web` | `ApiServer, JsonUtil, SessionManager` | JDK `HttpServer` presentation layer + hand-written JSON + session tokens |

---

## 2. The three polymorphism seams (the 2-mark centrepiece)

Each seam: an abstraction, concrete subclasses, one shared method, and the **exact
loop that processes them with no `instanceof`**.

### Seam A — Sensors
- **Abstraction:** `abstract class Sensor extends Component` (`src/farmbot/sensor/Sensor.java`); `read()` is a **template method** (lines 40–47) calling abstract `classify(double)` + `formatRaw(double)`.
- **Concrete subclasses:** `LineSensor, ObstacleSensor, SoilMoistureSensor, SoilTemperatureSensor, BatterySensor, CameraSensor` (the last overrides `read()` for exception translation).
- **Shared method:** `Reading read()`.
- **Processed in one loop, no `instanceof`:** `src/farmbot/core/SmartFarmBot.java:111`
  ```java
  List<Reading> sensorReadings = sensors.parallelStream()
          .map(this::safeRead)        // safeRead -> sensor.read()
          .flatMap(Optional::stream)
          .collect(Collectors.toList());
  ```

### Seam B — Mission modules
- **Abstraction:** `interface MissionModule` (`src/farmbot/module/MissionModule.java`) + `abstract class BaseModule` (shared lifecycle).
- **Concrete subclasses:** `SprayerModule, SeedAnalysisModule, Scan3DModule` (stub).
- **Shared methods:** `update(Map<SensorType,Reading>)` and `Reading read()`.
- **Processed in one loop, no `instanceof`:** `src/farmbot/core/SmartFarmBot.java:123`
  ```java
  for (MissionModule module : modules) {
      module.update(this.latest);
      if (module.getState().isReady()) {
          try { all.add(module.read()); }
          catch (ModuleNotReadyException notReady) { /* stub stays INACTIVE */ }
      }
  }
  ```

### Seam C — Navigation strategies
- **Abstraction:** `interface NavigationStrategy` (`src/farmbot/nav/NavigationStrategy.java`); `decide(...)`, `reset()`, `isFinished(...)`.
- **Concrete subclasses:** `FollowLineStrategy, SearchStrategy, DetectPlantStrategy, LaneChangeStrategy, ManualStrategy` (stub).
- **Shared method:** `Direction decide(Map<SensorType,Reading>)`.
- **Processed polymorphically (one held reference, swapped at runtime):** `src/farmbot/core/SmartFarmBot.java:191` inside `tick(double)`:
  ```java
  NavigationStrategy current = strategy;      // volatile field
  direction = current.decide(snap);           // line 191
  ```

> **Bonus seam — Rules:** `RecommendationEngine.evaluate()` (`src/farmbot/rules/RecommendationEngine.java:27`) runs a `List<Rule>` with `rule.evaluate(context)` in a loop — same pattern.

**⚠ Discrepancy:** earlier summaries said "0 `instanceof` in `src/`". Precise truth:
the **three domain seams contain zero `instanceof`**, but `JsonUtil` has **4**
(`src/farmbot/web/JsonUtil.java:34,36,38,40`) — a generic JSON serializer dispatching
on runtime value type. That is legitimate serialization, not domain type-switching.
`SmartFarmBot.java:46` also contains the word in a comment.

---

## 3. Exception hierarchy

Base: `public class FarmBotException extends Exception` (`exception/FarmBotException.java:13`) — **checked**.

| Exception | extends | Checked? | Thrown at | Caught at |
|---|---|---|---|---|
| `SensorFailureException` | `FarmBotException` | checked | `sensor/Sensor.java:42`, `sensor/CameraSensor.java:33` (translation) | `core/SmartFarmBot.java:140` (`safeRead`) |
| `LineLostException` | `FarmBotException` | checked | `nav/FollowLineStrategy.java:41` | `core/SmartFarmBot.java:193` (`tick`) |
| `ModuleNotReadyException` | `FarmBotException` | checked | `module/BaseModule.java:73`, `module/Scan3DModule.java:33,38`, `module/SeedAnalysisModule.java:52` | `core/SmartFarmBot.java:128`, `web/ApiServer.java:256` |
| `AuthenticationException` | `FarmBotException` | checked | `user/AuthService.java:68` | `web/ApiServer.java:171` → HTTP 401 |
| `InvalidSpeedException` | `IllegalArgumentException` | **unchecked** | `core/Motor.java:29`, `core/DriveTrain.java:39` | not caught in `src` (programming error; asserted in `MotorTest`) |
| `DataAccessException` | `RuntimeException` | **unchecked** | `data/SqliteSensorRepository.java` (12 sites, wraps every `SQLException`) | `concurrency/DbWriter.java` (`catch RuntimeException`), `web/ApiServer.java` (`catch RuntimeException` → 500) |

### Control-flow path: `LineLostException → SearchStrategy`
`FollowLineStrategy.decide()` throws when all IR eyes read 0 → caught in
`SmartFarmBot.tick()` (`:193`) → constructs a new `SearchStrategy`, `status = SEARCHING`,
keeps driving. On the status transition, `FarmBotRuntime.recordTransition()` writes a
`LineLostException` row to `fault_log`.

### Critical-failure path (⚠ differs from spec wording)
`Sensor.read()` throws `SensorFailureException(critical=true)` for a critical sensor →
caught in `SmartFarmBot.safeRead()` (`:140`) → **`status = FAULT` + `driveTrain.cutPower()`**
+ one-time `System.err` log. The spec said "writes the fault to `fault_log`"; the actual
code does **not** write `fault_log` from `safeRead`. `fault_log` rows come from
`FarmBotRuntime`: two standing faults at `start()` (`CAM-01` ConnectException, `SCAN3D-01`
ModuleNotReadyException) and one per `→SEARCHING` transition. The camera's non-critical
failure is caught in `safeRead` and surfaced as `INACTIVE`; its fault_log row is the
standing one recorded by `FarmBotRuntime`.

---

## 4. Concurrency model

| Thread / pool | Owner | Role |
|---|---|---|
| `sensor-poller` (1 daemon, `ScheduledExecutorService`) | `SensorPoller` (`concurrency/SensorPoller.java:36`) | **Producer** — every ~1 s calls `bot.pollSensorsAndModules()`, `queue.offer(reading)` for each |
| `db-writer` (1 dedicated `Thread`) | `DbWriter` (`concurrency/DbWriter.java:31`) | **Consumer** — `queue.poll(200ms)` + `drainTo(batch, 64)` → `repository.insertBatch` |
| `nav-ticker` (1 daemon, `ScheduledExecutorService`) | `FarmBotRuntime` (`concurrency/FarmBotRuntime.java:51`) | Control loop — every ~1 s calls `bot.tick(seconds)` + `recordTransition()` |
| HTTP pool (`newFixedThreadPool(8)`) | `ApiServer` (`web/ApiServer.java:84`) | Serves requests concurrently |
| `ForkJoinPool.commonPool` (transient) | `sensors.parallelStream()` in `SmartFarmBot.pollSensorsAndModules` | Reads sensors in parallel each poll |

- **Hand-off:** a single `LinkedBlockingQueue<Reading>` owned by `FarmBotRuntime`
  (`FarmBotRuntime.java:43`); the poller is the only writer, the `DbWriter` the only reader.
- **Shared state thread-safety:** `SmartFarmBot` uses `volatile BotStatus status`, a
  `volatile Map<SensorType,Reading> latest` (rebuilt then published atomically),
  `CopyOnWriteArrayList` for sensors/modules, `ConcurrentHashMap.newKeySet()` for logged
  faults; `Odometer` uses `AtomicLong`; `SqliteSensorRepository` methods are `synchronized`.
- **Shutdown (lossless):** `FarmBotRuntime.stop()` is guarded by an `AtomicBoolean` and
  ordered: `navTicker.shutdown()` → `poller.stop()` (stop producing) → `writer.stop()`
  (sets `running=false`, the run loop keeps draining `while (running || !queue.isEmpty())`,
  then `flushRemaining()`) → `bot.shutDown()` (cut power) → `repository.close()`. Registered
  as a JVM shutdown hook in `FarmBotRuntime.start()`.

---

## 5. JDBC / persistence layer

- **DAO interface:** `data/SensorRepository.java` (`extends AutoCloseable`). Methods:
  `initSchema, insert, insertBatch, findBySensor, latestPerSensor, count, recordFault,
  recentFaults, recordObstacleEvent, obstacleEvents, close`.
- **JDBC impl:** `data/SqliteSensorRepository.java`, one file DB `data/farmbot.db`.

### Tables (`initSchema`, lines 68–103)
| Table | Columns | Written by |
|---|---|---|
| `sensor_readings` | `id PK AUTOINCREMENT, sensor_id, sensor_type, value REAL, unit, raw, status, recorded_at` + index `idx_readings_sensor(sensor_id, id)` | `insert`, `insertBatch` |
| `fault_log` | `id PK, fault_time, source, exception, message` | `recordFault` |
| `obstacle_events` | `id PK, event_time, distance_cm REAL, outcome` | `recordObstacleEvent` |
| `modules` | `module_id PK, name, state, mounted_at` | **nothing — ⚠ created but never written** |

### JDBC discipline
- **`PreparedStatement` everywhere** (no string concatenation of values).
- **Batch + transaction + rollback:** `insertBatch` (`:135`) sets `autoCommit=false`,
  `addBatch` per row, `executeBatch`, `commit`; `catch (SQLException) → connection.rollback()`
  then rethrow; `finally → autoCommit=true`.
- **Try-with-resources** on every `Statement`/`PreparedStatement`/`ResultSet`.
- **Single held `Connection`, all public methods `synchronized`** — SQLite is one-writer;
  synchronising means the `db-writer` and web threads never touch the connection at once.
- **Every `SQLException` wrapped** in `DataAccessException` so JDBC types never leak past the DAO.

### SeedLoader (`data/SeedLoader.java`)
`importIfEmpty()` is **idempotent**: `if (repository.count() > 0) return 0;` else `importAll()`.
`importAll` reads `data/seed/*.csv` with a `BufferedReader`; `parseRow` validates 7 columns,
numeric value, known `SensorType`/`StatusLabel`, parseable timestamp; a bad row is logged and
skipped (never aborts the file). Rows are batch-inserted via the DAO.

---

## 6. Web / API layer

JDK `com.sun.net.httpserver.HttpServer` only; JSON built/parsed by hand in `JsonUtil`.
Two contexts: `/` → static files from `public/` (path-traversal guarded); `/api/` → routes.

| Method | Path | Auth | Request body | Response JSON shape |
|---|---|---|---|---|
| POST | `/api/login` | no | `{username,password}` | `{token,username,role,canAccessHardware}` or `{error}` (401) |
| POST | `/api/register` | no | `{username,password,role}` | `{username,role}` (201) or `{error}` (400) |
| POST | `/api/logout` | yes | — | `{ok:true}` |
| GET | `/api/status` | yes | — | `{status,statusDescription,strategy,speed,distanceMeters,battery,simulated}` |
| GET | `/api/sensors` | yes | — | `{sensors:[{id,name,type,critical,connected,value,unit,raw,status,level,recordedAt}],simulated}` |
| GET | `/api/modules` | yes | — | `{modules:[{id,name,description,state,mountedAt,value,unit,status}],simulated}` |
| GET | `/api/history?sensor=&limit=` | yes | — | `{sensor,history:[{value,unit,status,level,recordedAt}]}` |
| GET | `/api/obstacles` | yes | — | `{detected,undetected,total,events:[{eventTime,distanceCm,outcome}]}` |
| GET | `/api/faults` | yes | — | `{faults:[{faultTime,source,exception,message}]}` |
| GET | `/api/recommendations` | yes | — | `{recommendations:[{ruleId,severity,title,message,action}]}` |
| POST | `/api/strategy` | yes | `{strategy}` | `{strategy}` or `{error}` (400; `MANUAL` → clean 400) |

- **Auth:** `AuthService.hash()` = SHA-256 hex (`user/AuthService.java`); `SessionManager.open()`
  issues a 32-byte hex token; the browser returns it as the `X-Auth-Token` header; every
  `/api/*` except login/register requires `sessions.lookup(token)` (else 401).
- **Role gating:** login returns `canAccessHardware` (`Technician` → true via method override);
  `nav.js` hides the Hardware tab for a Farmer and `hardware.js` redirects a Farmer who deep-links.
- **Presentation adapter, no business logic:** every handler reads the live `SmartFarmBot` or
  `SensorRepository` and renders JSON — the dashboard and the console engine are the same objects.

---

## 7. End-to-end data flow (one soil reading)

| Hop | Class · method | File |
|---|---|---|
| 1. Simulate value | `SimulatedSignal.next()` | `sensor/SimulatedSignal.java` |
| 2. Build `Reading` | `SoilMoistureSensor` (via `Sensor.read()` template) | `sensor/Sensor.java:45` |
| 3. Poll (parallel) | `SmartFarmBot.pollSensorsAndModules()` | `core/SmartFarmBot.java:111` |
| 4. Enqueue | `SensorPoller.pollOnce()` → `queue.offer(reading)` | `concurrency/SensorPoller.java` |
| 5. Hand-off | `LinkedBlockingQueue<Reading>` | `concurrency/FarmBotRuntime.java:43` |
| 6. Consume + batch | `DbWriter.run()` → `drainTo` → `insertBatch` | `concurrency/DbWriter.java` |
| 7. Persist | `SqliteSensorRepository.insertBatch()` (PreparedStatement, txn) | `data/SqliteSensorRepository.java:135` |
| 8. Store | SQLite file `data/farmbot.db`, table `sensor_readings` | — |
| 9. Serve | `ApiServer.handleHistory()` → `repository.findBySensor()` | `web/ApiServer.java` |
| 10. Render | `fetch('/api/history')` → SVG polyline | `public/js/field-analytics.js`, `public/js/dashboard.js` |

(Live current values take a shorter path: `SmartFarmBot.latestSnapshot()` → `/api/sensors`
→ `dashboard.js`, without round-tripping SQLite.)

---

## 8. OOP concept → location map (viva cheat-sheet)

| Concept | Best example |
|---|---|
| Encapsulation | `Motor.setSpeed()` validates → `InvalidSpeedException`; all fields private (`core/Motor.java`) |
| Inheritance | `Component → Sensor → SoilMoistureSensor` (`sensor/`) |
| Method overriding | `CameraSensor.read()` overrides `Sensor.read()` for exception translation (`sensor/CameraSensor.java:30`) |
| Polymorphism | `sensors.parallelStream().map(this::safeRead)` (`core/SmartFarmBot.java:111`) |
| Abstract class | `Sensor` (template method `read()`), `BaseModule`, `Rule` |
| Interface | `MissionModule`, `NavigationStrategy`, `SignalSource` (`@FunctionalInterface`), `SensorRepository` |
| Enum | `StatusLabel` (word→level mapping), `SensorType`, `Direction`, `BotStatus`, `Role` |
| Generics | `Map<SensorType,Reading>`, `BlockingQueue<Reading>`, `Optional<Recommendation>` |
| Collections | `CopyOnWriteArrayList`, `EnumMap`, `LinkedBlockingQueue`, `ConcurrentHashMap` |
| Exception handling | `FarmBotException` hierarchy; `LineLostException` control flow (`core/SmartFarmBot.java:193`) |
| File I/O | `SeedLoader.importFile()` with `BufferedReader` + skip-bad-row (`data/SeedLoader.java`) |
| Multithreading | `SensorPoller` (producer) + `DbWriter` (consumer) + `BlockingQueue` + `volatile`/atomics |
| JDBC | `SqliteSensorRepository.insertBatch()` — PreparedStatement, batch, transaction, rollback |

---

## 9. Rubric evidence map (10 marks = 5 × 2)

| Criterion (2 marks) | Evidence |
|---|---|
| OOP architecture & encapsulation | `SmartFarmBot` composition root; private fields + validating setters (`Motor`, `DriveTrain`); `Reading` immutable value object |
| Inheritance, polymorphism, reusability | 3 seams (§2); `DetectPlantStrategy` reuses `FollowLineStrategy` by composition; `BaseModule`/`Sensor` share code |
| Exception handling, collections, file I/O | 6 custom exceptions (§3); `EnumMap`/`Deque`/`ConcurrentHashMap`; `SeedLoader` CSV import — test `SeedLoaderTest` |
| Multithreading + JDBC | `SensorPoller`/`DbWriter`/`FarmBotRuntime` (§4); `SqliteSensorRepository` (§5) — test `SqliteSensorRepositoryTest` |
| Functionality, testing, code quality, viva | 25 JUnit tests pass; compiles clean `--release 17 -Xlint:all -Werror`; live demo story (§11) |

Test files: `test/farmbot/core/MotorTest.java`, `test/farmbot/nav/NavigationStrategyTest.java`,
`test/farmbot/rules/RulesTest.java`, `test/farmbot/data/SeedLoaderTest.java`,
`test/farmbot/data/SqliteSensorRepositoryTest.java`.

---

## 10. Demo story beats

The live run is **deterministic** — `SmartFarmBot.demoRig()` (`core/SmartFarmBot.java`) gives
every `SimulatedSignal` a fixed-seed `Random` and a short scripted `replay(...)` sequence, so
the beats land on the same ticks every run.

| Beat | Mechanism |
|---|---|
| Line lost → SEARCH → recovered | `LINE-01` replays `[2,2,2,0,0,4,2,2]`; `000` → `LineLostException` → `tick` swaps to `SearchStrategy`; eyes re-acquire → resume |
| Obstacle HALT | `OBS-01` replays `[180,170,15,15,160,175]`; `<20 cm` → `StatusLabel.HALT` → `tick` sets `HALTED` before any strategy; logs an `obstacle_events` row |
| Soil < 30% → DRY | `SOIL-01` starts 31%, drift −0.4%/poll, clamp min 22 → crosses 30 then settles in the DRY band |
| Irrigation advisory | `LowMoistureRule.evaluate()` returns a `WARNING` recommendation; shown via `/api/recommendations` |
| Sprayer ACTIVE | `SprayerModule.update()` sees moisture `< MOISTURE_LOW` → `state = ACTIVE`; tank drains 0.02%/poll (~83 min to empty, so it stays ACTIVE through a viva) |
| Camera INACTIVE | `CameraSensor.read()` always throws non-critical `SensorFailureException` → shown as `INACTIVE`, not a crash |

---

## 11. Design decisions & trade-offs

| Decision | One-line justification |
|---|---|
| Single held `Connection`, `synchronized` DAO methods | SQLite is single-writer; serialising at the DAO is simpler and safer than a connection pool for this scale |
| Custom checked parent `FarmBotException` | One catch point for all expected domain conditions; unchecked faults (`InvalidSpeedException`, `DataAccessException`) stay separate as programming/infrastructure errors |
| No frameworks (JDK `HttpServer`, hand-written JSON) | Spec hard constraint; keeps the dependency surface to 2 jars and nothing to explain away in the viva |
| In-memory users (`AuthService`), no `users` table | Auth is demo-only; readings/faults/events persist. JDBC marks are covered by `sensor_readings` — a `users` table would be dead weight |
| DAO interface (`SensorRepository`) | The poller/web layers depend on an abstraction, not JDBC — swappable, and testable with a temp DB |
| Web = presentation adapter | Endpoints read the same live objects as the engine; the dashboard provably isn't a second/faked implementation |
| Deterministic `demoRig` (seeded RNG + scripted replay) | The demo story lands on cue every run instead of gambling on a random walk crossing 30% |

---

## ⚠ Discrepancies vs earlier step summaries

1. **`instanceof`** — earlier note said "0 in `src/`". Real count: **4** in `JsonUtil`
   (serialization type-dispatch) + 1 in a `SmartFarmBot` comment. The **three domain
   seams have zero** — the claim holds for the OOP centrepiece, not literally for the repo.
2. **`modules` table** — created in `initSchema` but **never written** (no `INSERT INTO modules`).
   Module state is served live from `SmartFarmBot.getModules()`, not persisted. Same "unused
   table" situation the `users` table had before it was dropped; consider dropping `modules` too.
3. **Critical `SensorFailureException` → `fault_log`** — spec wording said it writes the fault
   to `fault_log`. Actual code: `SmartFarmBot.safeRead` sets `FAULT` + cuts power + logs to
   `stderr`; `fault_log` rows are written by `FarmBotRuntime` (2 standing faults + line-loss).
4. **Table count** — 4 created / 3 written (earlier the schema had 5 incl. `users`; `users`
   was dropped in the Step-8 prep).
