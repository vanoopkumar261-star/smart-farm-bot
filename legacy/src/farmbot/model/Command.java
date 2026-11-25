package farmbot.model;

public class Command {
    private final Direction direction;
    private final int speed;

    public Command(Direction direction, int speed) {
        this.direction = direction;
        this.speed = speed;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getSpeed() {
        return speed;
    }
}
