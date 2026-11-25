import farmbot.concurrency.FarmBotRuntime;
import farmbot.core.SmartFarmBot;
import farmbot.data.Reading;
import farmbot.data.SensorRepository;
import farmbot.data.SqliteSensorRepository;
import farmbot.model.SensorType;
import farmbot.module.MissionModule;

/**
 * Step 5 proof: start the live engine (sensor poller + db-writer + nav ticker) and
 * watch it for a few seconds. We should see:
 *   - the bot status change (RUNNING / SEARCHING / HALTED) as the nav thread ticks,
 *   - soil moisture drift below 30% and the sprayer switch itself ACTIVE,
 *   - the sensor_readings row count climb as the consumer batch-writes,
 *   - a clean shutdown that flushes the queue.
 *
 * Run from the project root:
 *   javac -cp out -d demo demo/Step5Check.java
 *   java --enable-native-access=ALL-UNNAMED -cp "out;demo;lib/sqlite-jdbc-3.46.1.3.jar" Step5Check
 */
public final class Step5Check {

    public static void main(String[] args) throws InterruptedException {
        String dbPath = "data/farmbot.db";
        System.out.println("=== Step 5: SensorPoller + DbWriter (producer/consumer) + nav ticker ===");

        SensorRepository repo = new SqliteSensorRepository(dbPath);
        repo.initSchema();
        int before = repo.count();
        System.out.println("rows in sensor_readings before run: " + before);
        System.out.println();

        SmartFarmBot bot = SmartFarmBot.demoRig();
        FarmBotRuntime runtime = new FarmBotRuntime(bot, repo, 1000, 1000); // poll 1s, tick 1s
        runtime.start();

        System.out.printf("%-4s %-10s %-18s %-14s %s%n", "t", "status", "soil moisture", "sprayer", "rows");
        for (int second = 1; second <= 8; second++) {
            Thread.sleep(1000);
            Reading soil = bot.latestSnapshot().get(SensorType.SOIL_MOISTURE);
            String soilText = soil == null ? "(none)" : String.format("%.1f%% %s", soil.getValue(), soil.getLabel());
            String sprayer = moduleState(bot, "SPRAY-01");
            System.out.printf("%-4d %-10s %-18s %-14s %d%n",
                    second, bot.getStatus(), soilText, sprayer, repo.count());
        }

        System.out.println();
        runtime.stop(); // stops producer, flushes consumer, cuts power, closes the db
        System.out.println();

        // Reopen to prove the rows were actually committed and nothing was lost.
        try (SensorRepository check = new SqliteSensorRepository(dbPath)) {
            int after = check.count();
            System.out.println("rows in sensor_readings after run:  " + after);
            System.out.println("readings added this run:            " + (after - before));
        }
        System.out.println("=== done ===");
    }

    private static String moduleState(SmartFarmBot bot, String moduleId) {
        for (MissionModule m : bot.getModules()) {
            if (m.getId().equals(moduleId)) {
                return m.getState().toString();
            }
        }
        return "(absent)";
    }
}
