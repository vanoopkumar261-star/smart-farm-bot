package farmbot.model;

public class BotState {
    private final double soilMoisturePct;
    private final ObstacleLog obstacleLog;
    private final int lineLostCount;
    private final double distanceTravelled;

    public BotState(double soilMoisturePct, ObstacleLog obstacleLog, int lineLostCount, double distanceTravelled) {
        this.soilMoisturePct = soilMoisturePct;
        this.obstacleLog = obstacleLog;
        this.lineLostCount = lineLostCount;
        this.distanceTravelled = distanceTravelled;
    }

    public double getSoilMoisturePct() {
        return soilMoisturePct;
    }

    public ObstacleLog getObstacleLog() {
        return obstacleLog;
    }

    public int getLineLostCount() {
        return lineLostCount;
    }

    public double getDistanceTravelled() {
        return distanceTravelled;
    }
}
