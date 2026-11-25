# Smart Farm Bot

Java OOP line-following farm rover — a simulated console demo (the graded
artifact) plus a browser dashboard that's a thin, wired layer over the exact
same domain classes. See `PSEUDOCODE.md` for full class-level design and viva
notes.

## Build

From the project root (`E:\My projects\Line_bot follower`), PowerShell:

```
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -d out (Get-ChildItem -Recurse -Path src -Filter *.java).FullName
```

Or Git Bash:

```
mkdir -p out
javac -d out $(find src -name "*.java")
```

## Run the console demo (the graded OOP artifact)

```
java -cp out farmbot.main.Main
```

Runs, in order: polymorphic sensor demo (4 sensors incl. ESP32-CAM inactive)
→ exception demo (`InvalidSpeedException`, `MotorStallException`) → user
registry demo (Farmer + Technician, plus both `AuthenticationException` and
duplicate-username paths) → a 12-tick autonomous run over
`tracks/track1.txt` (follows the line, loses it, switches to `SEARCH`,
reacquires and switches back) → a live strategy-swap demo (Lane Change,
Detect Plant, Manual — the last one logs a fault instead of crashing) →
recommendation engine output → the original interactive Scanner loop
(type `LEFT CENTER RIGHT`, e.g. `0 1 0`; `9 9 9` to stop).

## Run the web dashboard

**Must be launched from the project root** — `public/` and `tracks/` are
resolved as relative paths:

```
java -cp out farmbot.web.WebServer
```

Then open `http://localhost:8080/login.html`. Register a Farmer and a
Technician account, log in as each, and confirm the dashboard differs (the
Technician's Hardware page appears in the nav; a Farmer's does not). All 4
real strategy buttons plus Manual (which returns a clean 4xx error instead
of crashing) are live; "Step Tick" advances the simulated line-follow run
one step and refreshes every panel.

The page works fully offline (no CDN calls) — Tailwind CSS is precompiled
to `public/css/tailwind.build.css`, and Inter/JetBrains Mono/Material
Symbols are self-hosted under `public/fonts/`.

## Exception paths verified end-to-end

| Exception | Where | How to trigger |
|---|---|---|
| `InvalidSpeedException` | `Motor.setSpeed()` | Console demo does this automatically (speed 150) |
| `MotorStallException` | `Motor.actuate()` | Console demo forces `stalled=true` |
| `LineLostException` | `FollowLineStrategy.decide()` | Autorun hits the line-lost rows in `track1.txt`; strategy swaps to `SearchStrategy` |
| `SensorFailureException` (non-critical) | `Esp32CamSensor.read()` | Always — the camera is simulated/inactive |
| `AuthenticationException` | `UserRegistry.authenticate()` | Wrong password, console demo and `POST /api/login` |
| `IllegalArgumentException` | `UserRegistry.register()` | Duplicate username, console demo and `POST /api/register` |
| `UnsupportedOperationException` | `ManualStrategy.decide()` / `SprayPump.actuate()` | Console demo's strategy-swap section; web: `POST /api/strategy {"strategy":"MANUAL"}` returns a clean 400, not a 500 |

## Project layout

```
src/farmbot/
  model/        enums + value objects (Direction, BotStatus, SensorReading, Command, Fault, ...)
  exceptions/   5 custom checked/unchecked exceptions
  hardware/     Component -> Sensor/Actuator trees (LineSensor, ObstacleSensor, SoilMoistureSensor,
                Esp32CamSensor, Motor, SprayPump, DriveTrain)
  sim/          SensorDataSource seam: ManualSource, SimulatedSource, SerialSource (stub), Track
  navigation/   Strategy pattern: FollowLine, Search, LaneChange, DetectPlant, Manual
  control/      Controller (tick() loop), Robot, LineFollowerBot
  users/        User -> Farmer/Technician, UserRegistry
  rules/        Rule -> LowMoisture/ObstacleDensity/PathLoss, RecommendationEngine
  web/          WebServer (JDK HttpServer, no dependencies), Json, SessionManager, route handlers
  main/         Main - the console demo entry point
public/         login/register/dashboard/hardware/coming-soon pages, css/js/fonts/assets
tracks/         track1.txt - the autorun demo's virtual field
```

## Viva notes

See `PSEUDOCODE.md`'s closing section for the three polymorphism seams
(`Sensor.read()`, `NavigationStrategy.decide()`, `Rule.evaluate()`) and why
`Robot` is composition, not inheritance, over `Controller`/sensors/
`DriveTrain`. The web layer adds one more point worth making: it calls the
same `Controller`/`Sensor`/`UserRegistry` classes the console demo uses —
`farmbot.web` is a presentation layer, not a second implementation.
