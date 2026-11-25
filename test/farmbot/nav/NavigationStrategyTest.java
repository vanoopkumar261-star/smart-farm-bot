package farmbot.nav;

import farmbot.data.Reading;
import farmbot.exception.LineLostException;
import farmbot.model.Direction;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One test per navigation strategy's decision, as the spec requires. */
class NavigationStrategyTest {

    /** A snapshot carrying just a line reading; value is the packed L*4+C*2+R pattern. */
    private static Map<SensorType, Reading> linePattern(int pattern) {
        return snapshot(new Reading("LINE-01", SensorType.LINE, pattern, "bits", "", StatusLabel.ON_LINE, LocalDateTime.now()));
    }

    private static Map<SensorType, Reading> snapshot(Reading... readings) {
        Map<SensorType, Reading> map = new EnumMap<>(SensorType.class);
        for (Reading r : readings) {
            map.put(r.getSensorType(), r);
        }
        return map;
    }

    // ---- FollowLineStrategy ----

    @Test
    void followGoesForwardOnCentre() throws LineLostException {
        assertEquals(Direction.FORWARD, new FollowLineStrategy().decide(linePattern(0b010)));
    }

    @Test
    void followSteersLeftWhenLineUnderLeftEye() throws LineLostException {
        assertEquals(Direction.LEFT, new FollowLineStrategy().decide(linePattern(0b100)));
    }

    @Test
    void followSteersRightWhenLineUnderRightEye() throws LineLostException {
        assertEquals(Direction.RIGHT, new FollowLineStrategy().decide(linePattern(0b001)));
    }

    @Test
    void followThrowsWhenLineLost() {
        assertThrows(LineLostException.class, () -> new FollowLineStrategy().decide(linePattern(0b000)));
    }

    // ---- SearchStrategy ----

    @Test
    void searchSweepsWhileLostAndFinishesWhenLineSeen() {
        SearchStrategy search = new SearchStrategy();
        search.reset();
        assertFalse(search.isFinished(linePattern(0b000)));
        Direction first = search.decide(linePattern(0b000));
        assertTrue(first == Direction.SPIN_LEFT || first == Direction.SPIN_RIGHT);
        assertTrue(search.isFinished(linePattern(0b010)));
    }

    // ---- LaneChangeStrategy ----

    @Test
    void laneChangeRunsItsPlanThenFinishes() {
        LaneChangeStrategy lane = new LaneChangeStrategy(true);
        lane.reset();
        int steps = lane.getPlan().size();
        assertEquals(7, steps); // 2 turn-out + 3 cross + 2 turn-back
        for (int i = 0; i < steps; i++) {
            assertFalse(lane.isFinished(Map.of()));
            assertEquals(lane.getPlan().get(i), lane.decide(Map.of()));
        }
        assertTrue(lane.isFinished(Map.of()));
        assertEquals(Direction.STOP, lane.decide(Map.of()));
    }

    // ---- DetectPlantStrategy ----

    @Test
    void detectPlantStopsAtANearbyPlant() throws LineLostException {
        DetectPlantStrategy detect = new DetectPlantStrategy(2);
        detect.reset();
        Map<SensorType, Reading> plantAhead = snapshot(
                new Reading("LINE-01", SensorType.LINE, 0b010, "bits", "", StatusLabel.ON_LINE, LocalDateTime.now()),
                new Reading("OBS-01", SensorType.OBSTACLE, 30, "cm", "", StatusLabel.NEAR, LocalDateTime.now()));
        assertEquals(Direction.STOP, detect.decide(plantAhead));
        assertEquals(1, detect.getPlantsInspected());
    }

    @Test
    void detectPlantFollowsLineWhenClear() throws LineLostException {
        DetectPlantStrategy detect = new DetectPlantStrategy(2);
        detect.reset();
        assertEquals(Direction.FORWARD, detect.decide(linePattern(0b010)));
    }

    // ---- ManualStrategy ----

    @Test
    void manualRefusesToDrive() {
        assertThrows(UnsupportedOperationException.class, () -> new ManualStrategy().decide(Map.of()));
    }
}
