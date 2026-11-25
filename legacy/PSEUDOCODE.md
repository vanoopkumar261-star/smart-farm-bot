# Smart Farm Bot — Full Pseudocode

Every class in the architecture (§5 of CLAUDE.md), in pseudocode. This is the
design reference before any Java is written — fields, constructors, and method
bodies at pseudocode level. Package root: `farmbot`.

---

## 1. Enums

```
ENUM Direction
    FORWARD, BACKWARD, LEFT, RIGHT, STOP

ENUM BotStatus
    IDLE, RUNNING, SEARCHING, HALTED, FAULT

ENUM SensorType
    LINE, OBSTACLE, SOIL_MOISTURE, CAMERA

ENUM Permission
    START_STOP, VIEW_FIELD_STATUS, CALIBRATE_SENSORS, VIEW_RAW_LOGS
```

---

## 2. Custom Exceptions

```
CLASS SensorFailureException EXTENDS Exception
    FIELD sensorId : String
    FIELD critical : boolean
    CONSTRUCTOR(sensorId, message, critical)
        CALL super(message)
        SET this.sensorId = sensorId
        SET this.critical = critical
    METHOD isCritical() : boolean
        RETURN critical

CLASS LineLostException EXTENDS Exception
    FIELD lastKnownX, lastKnownY : int
    CONSTRUCTOR(message, lastKnownX, lastKnownY)
        CALL super(message)

CLASS MotorStallException EXTENDS Exception
    FIELD motorId : String
    CONSTRUCTOR(motorId, message)
        CALL super(message)

CLASS InvalidSpeedException EXTENDS Exception
    CONSTRUCTOR(message)
        CALL super(message)

CLASS AuthenticationException EXTENDS Exception
    CONSTRUCTOR(message)
        CALL super(message)
```

---

## 3. Tree A — Hardware Abstraction

```
ABSTRACT CLASS Component
    FIELD id : String
    FIELD name : String
    FIELD status : String   // "OK", "INACTIVE", "FAULT"

    CONSTRUCTOR(id, name)
        SET this.id = id
        SET this.name = name
        SET this.status = "OK"

    METHOD initialize() : void
        SET status = "OK"

    METHOD shutdown() : void
        SET status = "INACTIVE"

    METHOD getStatus() : String
        RETURN status

    ABSTRACT METHOD getDescription() : String
```

```
ABSTRACT CLASS Sensor EXTENDS Component
    FIELD type : SensorType
    FIELD history : Queue<SensorReading>   // ring buffer, capacity 100

    CONSTRUCTOR(id, name, type)
        CALL super(id, name)
        SET this.type = type
        SET history = new Queue()

    ABSTRACT METHOD read() : SensorReading
        // each subclass implements differently — this is the polymorphism seam

    METHOD isActive() : boolean
        RETURN status == "OK"

    METHOD logReading(reading : SensorReading) : void
        IF history.size() >= 100 THEN history.removeOldest()
        history.add(reading)

    METHOD getHistory() : List<SensorReading>
        RETURN history.asList()
```

```
CLASS LineSensor EXTENDS Sensor
    FIELD source : SensorDataSource

    CONSTRUCTOR(id, source)
        CALL super(id, "Line Sensor", SensorType.LINE)
        SET this.source = source

    METHOD read() : SensorReading
        TRY
            value = source.poll(id)          // e.g. 0 = on line, 1 = off line
            reading = new SensorReading(id, value, now())
            CALL logReading(reading)
            RETURN reading
        CATCH IOException e
            THROW new SensorFailureException(id, "line sensor read failed", false)

    METHOD getDescription() : String
        RETURN "Detects the guide line under the chassis"
```

```
CLASS ObstacleSensor EXTENDS Sensor
    FIELD source : SensorDataSource
    FIELD thresholdCm : double

    CONSTRUCTOR(id, source, thresholdCm)
        CALL super(id, "Obstacle Sensor", SensorType.OBSTACLE)

    METHOD read() : SensorReading
        distance = source.poll(id)
        reading = new SensorReading(id, distance, now())
        CALL logReading(reading)
        IF distance < thresholdCm THEN
            reading.flagObstacle = true
        RETURN reading

    METHOD getDescription() : String
        RETURN "Detects obstacles within " + thresholdCm + "cm"
```

```
CLASS SoilMoistureSensor EXTENDS Sensor
    FIELD source : SensorDataSource

    CONSTRUCTOR(id, source)
        CALL super(id, "Soil Moisture Sensor", SensorType.SOIL_MOISTURE)

    METHOD read() : SensorReading
        moisturePct = source.poll(id)
        reading = new SensorReading(id, moisturePct, now())
        CALL logReading(reading)
        RETURN reading

    METHOD getDescription() : String
        RETURN "Reads soil moisture percentage"
```

```
CLASS CameraSensor EXTENDS Sensor
    FIELD connected : boolean

    CONSTRUCTOR(id)
        CALL super(id, "ESP32-CAM", SensorType.CAMERA)
        SET connected = false   // no hardware — always false in simulation

    METHOD read() : SensorReading
        TRY
            IF NOT connected THEN
                THROW new ConnectException("camera not connected")
            frame = pollFrame()
            RETURN new SensorReading(id, frame, now())
        CATCH ConnectException e
            SET status = "INACTIVE"
            THROW new SensorFailureException(id, "camera inactive", false)

    METHOD getDescription() : String
        RETURN "Live field video feed (inactive — no hardware attached)"
```

```
ABSTRACT CLASS Actuator EXTENDS Component
    ABSTRACT METHOD actuate(command : Command) : void
```

```
CLASS Motor EXTENDS Actuator
    FIELD speed : int   // 0-100
    FIELD direction : Direction
    FIELD stalled : boolean

    CONSTRUCTOR(id, name)
        CALL super(id, name)
        SET speed = 0
        SET direction = Direction.STOP

    METHOD setSpeed(newSpeed : int) : void
        IF newSpeed < 0 OR newSpeed > 100 THEN
            THROW new InvalidSpeedException("speed must be 0-100, got " + newSpeed)
        SET speed = newSpeed

    METHOD actuate(command : Command) : void
        IF stalled THEN
            THROW new MotorStallException(id, "motor stalled, cannot actuate")
        CALL setSpeed(command.speed)
        SET direction = command.direction

    METHOD stop() : void
        SET speed = 0
        SET direction = Direction.STOP

    METHOD getDescription() : String
        RETURN "Drives one wheel of the chassis"
```

```
CLASS SprayPump EXTENDS Actuator          // stub — future work
    METHOD actuate(command : Command) : void
        THROW new UnsupportedOperationException("spray system not yet implemented")

    METHOD getDescription() : String
        RETURN "Future work: chemical/water spray actuator"
```

---

## 4. Tree B — Users

```
ABSTRACT CLASS User
    FIELD username : String
    FIELD passwordHash : String

    CONSTRUCTOR(username, passwordHash)
        SET this.username = username
        SET this.passwordHash = passwordHash

    METHOD login(inputPassword : String) : boolean
        hashed = hash(inputPassword)
        IF hashed != passwordHash THEN
            THROW new AuthenticationException("invalid credentials")
        RETURN true

    ABSTRACT METHOD getPermissions() : Set<Permission>
    ABSTRACT METHOD getDashboardView() : String
```

```
CLASS Farmer EXTENDS User
    METHOD getPermissions() : Set<Permission>
        RETURN { START_STOP, VIEW_FIELD_STATUS }

    METHOD getDashboardView() : String
        RETURN "FarmerDashboard"   // buttons + recommendations, no raw logs
```

```
CLASS Technician EXTENDS User
    METHOD getPermissions() : Set<Permission>
        RETURN { START_STOP, VIEW_FIELD_STATUS, CALIBRATE_SENSORS, VIEW_RAW_LOGS }

    METHOD getDashboardView() : String
        RETURN "TechnicianDashboard"   // adds Hardware tab + logs
```

```
CLASS UserRegistry                       // supports registration requirement
    FIELD users : Map<String, User>

    METHOD register(username, password, role : String) : User
        IF users.containsKey(username) THEN
            THROW new IllegalArgumentException("username taken")
        hashed = hash(password)
        newUser = (role == "TECHNICIAN") ? new Technician(username, hashed)
                                          : new Farmer(username, hashed)
        users.put(username, newUser)
        RETURN newUser

    METHOD authenticate(username, password) : User
        user = users.get(username)
        IF user == null THEN
            THROW new AuthenticationException("no such user")
        CALL user.login(password)          // may throw AuthenticationException
        RETURN user
```

---

## 5. Tree C — Recommendation Rules

```
ABSTRACT CLASS Rule
    ABSTRACT METHOD evaluate(state : BotState) : Recommendation   // may return null
```

```
CLASS LowMoistureRule EXTENDS Rule
    FIELD thresholdPct : double

    METHOD evaluate(state : BotState) : Recommendation
        IF state.soilMoisturePct < thresholdPct THEN
            RETURN new Recommendation("Soil moisture low — consider irrigation")
        RETURN null
```

```
CLASS ObstacleDensityRule EXTENDS Rule
    FIELD windowSize : int

    METHOD evaluate(state : BotState) : Recommendation
        recentObstacles = state.obstacleLog.countInLastNMoves(windowSize)
        IF recentObstacles > 3 THEN
            RETURN new Recommendation("High obstacle density on current row — inspect field")
        RETURN null
```

```
CLASS PathLossRule EXTENDS Rule
    METHOD evaluate(state : BotState) : Recommendation
        IF state.lineLostCount > 2 THEN
            RETURN new Recommendation("Line repeatedly lost — check path markings")
        RETURN null
```

```
CLASS RecommendationEngine
    FIELD rules : List<Rule>

    CONSTRUCTOR()
        SET rules = [ new LowMoistureRule(30.0),
                      new ObstacleDensityRule(5),
                      new PathLossRule() ]

    METHOD runAll(state : BotState) : List<Recommendation>
        results = new List()
        FOR EACH rule IN rules
            rec = rule.evaluate(state)       // polymorphic dispatch
            IF rec != null THEN results.add(rec)
        RETURN results
```

---

## 6. Navigation — Strategy Pattern

```
INTERFACE NavigationStrategy
    METHOD decide(readings : List<SensorReading>) : Command
```

```
CLASS FollowLineStrategy IMPLEMENTS NavigationStrategy
    METHOD decide(readings) : Command
        lineReading = findByType(readings, SensorType.LINE)
        IF lineReading.value == OFF_LINE THEN
            THROW new LineLostException("line not detected", lastX, lastY)
        IF lineReading.value == CENTER THEN
            RETURN new Command(Direction.FORWARD, 60)
        ELSE IF lineReading.value == LEFT_OF_LINE THEN
            RETURN new Command(Direction.RIGHT, 40)   // correct back toward line
        ELSE
            RETURN new Command(Direction.LEFT, 40)
```

```
CLASS DetectPlantStrategy IMPLEMENTS NavigationStrategy
    METHOD decide(readings) : Command
        cameraReading = findByType(readings, SensorType.CAMERA)
        IF cameraReading.detectsPlant() THEN
            RETURN new Command(Direction.STOP, 0)     // pause at plant (simulated)
        RETURN new Command(Direction.FORWARD, 50)
```

```
CLASS LaneChangeStrategy IMPLEMENTS NavigationStrategy
    FIELD stage : int   // internal small state machine: turn -> cross -> turn -> align

    METHOD decide(readings) : Command
        SWITCH stage
            CASE 0: stage++; RETURN new Command(Direction.RIGHT, 50)
            CASE 1: stage++; RETURN new Command(Direction.FORWARD, 50)
            CASE 2: stage++; RETURN new Command(Direction.LEFT, 50)
            CASE 3: stage = 0; RETURN new Command(Direction.FORWARD, 50)
```

```
CLASS SearchStrategy IMPLEMENTS NavigationStrategy
    FIELD ticksSearching : int
    FIELD maxTicks : int = 20

    METHOD decide(readings) : Command
        SET ticksSearching++
        IF ticksSearching > maxTicks THEN
            THROW new LineLostException("search timeout", lastX, lastY)
        RETURN new Command(Direction.LEFT, 20)   // sweep to reacquire line
```

```
CLASS ManualStrategy IMPLEMENTS NavigationStrategy      // stub, exception-handling tick
    METHOD decide(readings) : Command
        THROW new UnsupportedOperationException("manual control not yet implemented")
```

---

## 7. Hardware Seam

```
INTERFACE SensorDataSource
    METHOD poll(sensorId : String) : double
```

```
CLASS SimulatedSource IMPLEMENTS SensorDataSource
    FIELD track : Track
    FIELD botX, botY : int

    METHOD poll(sensorId) : double
        cell = track.getCellAt(botX, botY)
        SWITCH sensorId's type
            CASE LINE: RETURN (cell == '#') ? CENTER : OFF_LINE
            CASE OBSTACLE: RETURN track.distanceToNearestObstacle(botX, botY)
            CASE SOIL_MOISTURE: RETURN track.moistureAt(botX, botY)
```

```
CLASS SerialSource IMPLEMENTS SensorDataSource     // stub for semester-4 hardware link
    METHOD poll(sensorId) : double
        THROW new UnsupportedOperationException("hardware link not yet implemented")
```

---

## 8. Supporting / Composition Classes

```
CLASS Track
    FIELD grid : char[][]      // '#' = path, '.' = soil

    CONSTRUCTOR(filePath : String)
        TRY
            grid = loadFromFile(filePath)
        CATCH FileNotFoundException e
            THROW e   // propagate — caller decides fallback (e.g. default 10x10 grid)

    METHOD getCellAt(x, y) : char
        RETURN grid[y][x]
```

```
CLASS SensorReading
    FIELD sensorId : String
    FIELD value : double
    FIELD timestamp : long
    FIELD flagObstacle : boolean
```

```
CLASS Command
    FIELD direction : Direction
    FIELD speed : int
```

```
CLASS Fault
    FIELD sensorId : String
    FIELD message : String
    FIELD timestamp : long
    FIELD critical : boolean
```

```
CLASS ObstacleEvent
    FIELD timestamp : long
    FIELD wasDetected : boolean   // true = sensor saw it, false = undetected collision

CLASS ObstacleLog
    FIELD events : List<ObstacleEvent>

    METHOD record(detected : boolean) : void
        events.add(new ObstacleEvent(now(), detected))

    METHOD countDetected() : int
        RETURN events.count(e -> e.wasDetected)

    METHOD countCollisions() : int
        RETURN events.count(e -> NOT e.wasDetected)

    METHOD countInLastNMoves(n) : int
        RETURN events.tail(n).size()
```

```
CLASS Odometer
    FIELD totalDistance : double

    METHOD update(command : Command) : void
        IF command.direction != Direction.STOP THEN
            SET totalDistance += (command.speed * TICK_DISTANCE_FACTOR)

    METHOD getTotalDistance() : double
        RETURN totalDistance
```

```
CLASS DriveTrain
    FIELD leftMotor, rightMotor : Motor

    METHOD execute(command : Command) : void
        CALL leftMotor.actuate(command)    // may throw MotorStallException
        CALL rightMotor.actuate(command)

    METHOD stop() : void
        CALL leftMotor.stop()
        CALL rightMotor.stop()

    METHOD cutPower() : void
        CALL stop()
```

```
CLASS Controller
    FIELD strategy : NavigationStrategy
    FIELD sensors : List<Sensor>
    FIELD driveTrain : DriveTrain
    FIELD odometer : Odometer
    FIELD faults : List<Fault>

    METHOD setStrategy(newStrategy : NavigationStrategy) : void
        SET strategy = newStrategy

    METHOD pollAllSensors() : List<SensorReading>
        readings = new List()
        FOR EACH sensor IN sensors
            TRY
                readings.add(sensor.read())      // polymorphic
            CATCH SensorFailureException e
                faults.add(new Fault(e.sensorId, e.getMessage(), now(), e.isCritical()))
        RETURN readings

    METHOD tick() : void
        // see main loop in section 9 — Controller owns the try/catch/finally body
```

```
CLASS Robot                          // composition root
    FIELD id : String
    FIELD controller : Controller
    FIELD sensors : List<Sensor>
    FIELD driveTrain : DriveTrain
    FIELD status : BotStatus

    CONSTRUCTOR(id, sensors, driveTrain)
        SET this.sensors = sensors
        SET this.driveTrain = driveTrain
        SET controller = new Controller(sensors, driveTrain)
        SET status = BotStatus.IDLE

CLASS LineFollowerBot EXTENDS Robot   // concrete bot used in this project
    METHOD start() : void
        SET status = BotStatus.RUNNING
        CALL controller.setStrategy(new FollowLineStrategy())
```

```
CLASS BotState             // snapshot passed to RecommendationEngine
    FIELD soilMoisturePct : double
    FIELD obstacleLog : ObstacleLog
    FIELD lineLostCount : int
    FIELD distanceTravelled : double

CLASS Recommendation
    FIELD message : String
    FIELD timestamp : long
```

---

## 9. Main Navigation Loop (ties everything together)

```
START
    initialise sensors, driveTrain, controller
    controller.setStrategy(new FollowLineStrategy())
    status = BotStatus.RUNNING

    WHILE status == BotStatus.RUNNING
        TRY
            readings = controller.pollAllSensors()        // polymorphic Sensor.read()
            command  = controller.strategy.decide(readings) // polymorphic dispatch
            driveTrain.execute(command)
            odometer.update(command)
            logReadings(readings)

        CATCH LineLostException
            controller.setStrategy(new SearchStrategy())   // runtime strategy swap
            IF searchExceededTimeout THEN status = BotStatus.HALTED

        CATCH SensorFailureException e
            faults.add(e)                                  // surfaces in UI fault panel
            IF e.isCritical() THEN status = BotStatus.HALTED

        CATCH MotorStallException
            driveTrain.stop()
            status = BotStatus.HALTED

        FINALLY
            refreshDashboard()

    END WHILE

    driveTrain.cutPower()
STOP
```

---

## Notes for the viva

- **Polymorphism, seam 1:** `Controller.pollAllSensors()` calls `sensor.read()` on a
  `List<Sensor>` without knowing which concrete subclass it is holding.
- **Polymorphism, seam 2:** `Controller.strategy.decide(readings)` — same call
  site, different behaviour per `NavigationStrategy` implementation, swapped at
  runtime (`FollowLineStrategy` → `SearchStrategy` on `LineLostException`).
- **Polymorphism, seam 3:** `RecommendationEngine.runAll()` calls `rule.evaluate()`
  over `List<Rule>`.
- **Composition, not inheritance, for `Robot`:** it *has-a* `Controller`, *has-a*
  `List<Sensor>`, *has-a* `DriveTrain` — this directly answers the brief's
  confusion between data flow and class relationships (§3.3 of CLAUDE.md).
- **Exception handling tick:** `Motor.setSpeed()` validates and throws
  `InvalidSpeedException`; the tick loop's try/catch/finally halts gracefully
  instead of crashing; `ManualStrategy` and `SprayPump` throw
  `UnsupportedOperationException` as deliberate stubs.
