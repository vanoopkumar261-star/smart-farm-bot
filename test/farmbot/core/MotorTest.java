package farmbot.core;

import farmbot.exception.InvalidSpeedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Motor.setSpeed validation: the encapsulation guard the spec asks for. */
class MotorTest {

    @Test
    void acceptsSpeedWithinRange() {
        Motor motor = new Motor("MOT-L", "Left Drive Motor");
        motor.setSpeed(0);
        assertEquals(0, motor.getSpeed());
        motor.setSpeed(100);
        assertEquals(100, motor.getSpeed());
        motor.setSpeed(55);
        assertEquals(55, motor.getSpeed());
        assertTrue(motor.isRunning());
    }

    @Test
    void rejectsSpeedBelowZero() {
        Motor motor = new Motor("MOT-L", "Left Drive Motor");
        assertThrows(InvalidSpeedException.class, () -> motor.setSpeed(-1));
    }

    @Test
    void rejectsSpeedAbove100() {
        Motor motor = new Motor("MOT-R", "Right Drive Motor");
        assertThrows(InvalidSpeedException.class, () -> motor.setSpeed(101));
    }

    @Test
    void cutPowerAlwaysStops() {
        Motor motor = new Motor("MOT-L", "Left Drive Motor");
        motor.setSpeed(80);
        motor.cutPower();
        assertEquals(0, motor.getSpeed());
        assertFalse(motor.isRunning());
    }
}
