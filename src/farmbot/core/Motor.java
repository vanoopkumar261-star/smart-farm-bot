package farmbot.core;

import farmbot.exception.InvalidSpeedException;
import farmbot.sensor.Component;

/**
 * One DC drive motor. Speed is a duty cycle, 0-100%.
 *
 * Encapsulation: the speed can only change through setSpeed(), and setSpeed()
 * rejects anything outside 0-100 with InvalidSpeedException. No other class can
 * put the motor into an impossible state.
 *
 * Fields are volatile: the control thread writes them and the web thread reads them.
 */
public class Motor extends Component {

    public static final int MIN_SPEED = 0;
    public static final int MAX_SPEED = 100;

    private volatile int speed;
    private volatile boolean reversed;

    public Motor(String id, String name) {
        super(id, name);
    }

    public void setSpeed(int speed) {
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new InvalidSpeedException(speed);
        }
        this.speed = speed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    /** Emergency / safe stop: always succeeds, never throws. */
    public void cutPower() {
        this.speed = 0;
        this.reversed = false;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isReversed() {
        return reversed;
    }

    public boolean isRunning() {
        return speed > 0;
    }

    @Override
    public String getCategory() {
        return "Actuator";
    }
}
