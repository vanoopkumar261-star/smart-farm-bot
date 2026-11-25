# Smart Farm Bot

A modular, line-following farm rover — **simulated** (no physical hardware). A base
platform (line-follower drive + base soil/obstacle/battery sensors) carries mission
modules (sprayer, seed analysis, 3D-scanner stub) that mount at runtime. A live engine
polls the sensors on background threads, persists readings to SQLite, and serves a
browser dashboard that is a thin layer over the exact same domain classes.

B.Tech OOP (Java) course project. Everything the UI shows is labelled **SIMULATED**.

## Requirements

- **Java 17+** (built with `--release 17`; runs on newer JDKs too).
- No Maven/Gradle. Only two jars, both in `lib/`:
  - `sqlite-jdbc-3.46.1.3.jar` — the JDBC driver
  - `junit-platform-console-standalone-1.11.3.jar` — for the tests

## Run

From the project root:

```
./run.sh      # Linux / macOS
run.bat       # Windows
```

Each **compiles the whole project and starts the web server** in one command. Then open
**http://localhost:8080/login.html**.

Demo logins (hashed with SHA-256 in memory, never stored in plain text):

| Username | Password | Role | Sees Hardware tab |
|---|---|---|---|
| `anoop` | `tech123` | Technician | yes |
| `farmer` | `farmer123` | Farmer | no |

The database file is created at `data/farmbot.db`; delete it for a clean start.

## Test

```
./test.sh     # Linux / macOS
test.bat      # Windows
```

Runs the JUnit 5 suite (25 tests): Motor validation, every navigation strategy, every
rule, the SeedLoader skipping a malformed row, and a repository insert/read-back on a
temporary database.

## What the demo shows

On start, `SeedLoader` imports `data/seed/*.csv` (the 09:00–09:19 story) into SQLite,
then the live engine continues from a seeded, deterministic simulation:

- soil moisture falls **below 30% → `DRY`** → the **irrigation advisory** fires and the
  **sprayer switches itself `ACTIVE`**;
- the obstacle sensor dips under 20 cm → the bot **HALTS**, logging an obstacle event;
- the line is briefly lost → the bot switches to **SEARCH** and recovers, logging a fault;
- the camera stays **INACTIVE** (a caught, non-critical failure — shown as a status, not a crash).

Field Analytics charts the SOIL-01 history read back from SQLite — live proof the JDBC
persistence layer is storing readings.

## Architecture (`src/farmbot/...`)

| Package | Responsibility |
|---|---|
| `model` | enums + value objects (`SensorType`, `BotStatus`, `Direction`, `ModuleState`, `ReadingStatus`, `StatusLabel`, `Thresholds`) |
| `exception` | custom exceptions (`SensorFailureException`, `LineLostException`, `ModuleNotReadyException`, `InvalidSpeedException`, `AuthenticationException`, `DataAccessException`) |
| `sensor` | `Component` → `Sensor` → Line / Obstacle / SoilMoisture / SoilTemperature / Battery / Camera; the `SignalSource` seam + `SimulatedSignal` |
| `module` | `MissionModule` + `BaseModule` → Sprayer / SeedAnalysis / Scan3D (stub) |
| `nav` | `NavigationStrategy` → FollowLine / Search / DetectPlant / LaneChange / Manual (stub) |
| `rules` | `Rule` → LowMoisture / ObstacleDensity / LowBattery; `RecommendationEngine` |
| `user` | `User` → `Farmer` / `Technician`; `AuthService` (in-memory, SHA-256) |
| `data` | `Reading` (immutable); `SensorRepository` (DAO) + `SqliteSensorRepository` (JDBC); `SeedLoader` (CSV import) |
| `concurrency` | `SensorPoller` (producer), `DbWriter` (consumer), `FarmBotRuntime` (threads + shutdown hook) |
| `core` | `SmartFarmBot` (composition root), `DriveTrain`, `Motor`, `Odometer` |
| `web` | `ApiServer` (JDK `HttpServer`, also the app entry point), `JsonUtil`, `SessionManager` |

`public/` holds the dashboard (served statically); `data/seed/` holds the seed CSVs.
`test/` holds the JUnit tests; `demo/` holds standalone step-check programs.

## Viva notes — the three polymorphism seams

- `Sensor.read()` — `SmartFarmBot` holds `List<Sensor>` and reads them all with no `instanceof`.
- `NavigationStrategy.decide()` — the bot holds one strategy and swaps it at runtime.
- `Rule.evaluate()` — `RecommendationEngine` runs a `List<Rule>` the same way.

`SmartFarmBot` is **composition** (it *has* a drive train, sensors and modules), not
inheritance. A new sensor, module, strategy or rule is **one new class** with no change
to the bot — the Open/Closed Principle.

The key point about the web layer: `farmbot.web` is a **presentation adapter, not a
second implementation** — every endpoint reads the same live `SmartFarmBot` and
`SensorRepository` the engine uses. The dashboard and the engine are provably one system.

## HTTP API (port 8080)

`GET /api/status · /api/sensors · /api/modules · /api/history?sensor=&limit= · /api/obstacles
· /api/faults · /api/recommendations` and `POST /api/login · /api/register · /api/strategy
· /api/logout`. Protected endpoints require the `X-Auth-Token` header returned by login.
