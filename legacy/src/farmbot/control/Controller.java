package farmbot.control;

import farmbot.exceptions.InvalidSpeedException;
import farmbot.exceptions.LineLostException;
import farmbot.exceptions.MotorStallException;
import farmbot.exceptions.SensorFailureException;
import farmbot.hardware.DriveTrain;
import farmbot.hardware.Sensor;
import farmbot.model.BotStatus;
import farmbot.model.Command;
import farmbot.model.Fault;
import farmbot.model.Odometer;
import farmbot.model.ObstacleLog;
import farmbot.model.SensorReading;
import farmbot.model.SensorType;
import farmbot.navigation.FollowLineStrategy;
import farmbot.navigation.NavigationStrategy;
import farmbot.navigation.SearchStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {
    private final List<Sensor> sensors;
    private final DriveTrain driveTrain;
    private NavigationStrategy strategy;
    private final List<Fault> faults = new ArrayList<>();
    private final Odometer odometer = new Odometer();
    private final ObstacleLog obstacleLog = new ObstacleLog();
    private BotStatus status = BotStatus.IDLE;
    private int lineLostCount = 0;

    public Controller(List<Sensor> sensors, DriveTrain driveTrain) {
        this.sensors = sensors;
        this.driveTrain = driveTrain;
        this.strategy = new FollowLineStrategy();
    }

    /** One control-loop cycle: poll -> decide -> act, PSEUDOCODE.md §9. */
    public void tick() {
        List<SensorReading> readings = pollAllSensors();
        if (status == BotStatus.HALTED) {
            driveTrain.stop();
            return;
        }

        recoverStrategyIfLineFound(readings);

        try {
            Command command = strategy.decide(readings);
            driveTrain.execute(command);
            odometer.update(command);
            status = (strategy instanceof SearchStrategy) ? BotStatus.SEARCHING : BotStatus.RUNNING;
        } catch (LineLostException e) {
            lineLostCount++;
            faults.add(new Fault("line-1", e.getMessage(), false));
            if (strategy instanceof SearchStrategy) {
                status = BotStatus.HALTED;
                driveTrain.stop();
            } else {
                strategy = new SearchStrategy();
                status = BotStatus.SEARCHING;
            }
        } catch (MotorStallException | InvalidSpeedException e) {
            driveTrain.stop();
            status = BotStatus.HALTED;
        } catch (UnsupportedOperationException e) {
            // e.g. ManualStrategy / SprayPump stubs - not implemented yet,
            // but that's not a hardware fault, so the bot keeps running.
            faults.add(new Fault("strategy", e.getMessage(), false));
        } finally {
            // hook point for a future dashboard refresh (web layer, Day 5)
        }
    }

    private List<SensorReading> pollAllSensors() {
        List<SensorReading> readings = new ArrayList<>();
        for (Sensor sensor : sensors) {
            try {
                SensorReading reading = sensor.read();
                readings.add(reading);
                if (reading.getSensorType() == SensorType.OBSTACLE && reading.isFlagObstacle()) {
                    obstacleLog.record(true);
                }
            } catch (SensorFailureException e) {
                faults.add(new Fault(e.getSensorId(), e.getMessage(), e.isCritical()));
                if (e.isCritical()) {
                    status = BotStatus.HALTED;
                }
            }
        }
        return readings;
    }

    private void recoverStrategyIfLineFound(List<SensorReading> readings) {
        if (!(strategy instanceof SearchStrategy)) {
            return;
        }
        for (SensorReading r : readings) {
            if (r.getSensorType() == SensorType.LINE) {
                int center = ((int) r.getValue() / 10) % 10;
                if (center == 1) {
                    strategy = new FollowLineStrategy();
                }
            }
        }
    }

    public void setStrategy(NavigationStrategy strategy) {
        this.strategy = strategy;
    }

    public NavigationStrategy getStrategy() {
        return strategy;
    }

    public BotStatus getStatus() {
        return status;
    }

    public void setStatus(BotStatus status) {
        this.status = status;
    }

    public List<Fault> getFaults() {
        return Collections.unmodifiableList(faults);
    }

    public Odometer getOdometer() {
        return odometer;
    }

    public ObstacleLog getObstacleLog() {
        return obstacleLog;
    }

    public int getLineLostCount() {
        return lineLostCount;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public DriveTrain getDriveTrain() {
        return driveTrain;
    }
}
