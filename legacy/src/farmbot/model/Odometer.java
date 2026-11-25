package farmbot.model;

public class Odometer {
    private static final double TICK_DISTANCE_FACTOR = 0.01;
    private double totalDistance;

    public void update(Command command) {
        if (command.getDirection() != Direction.STOP) {
            totalDistance += command.getSpeed() * TICK_DISTANCE_FACTOR;
        }
    }

    public double getTotalDistance() {
        return totalDistance;
    }
}
