import farmbot.data.Reading;
import farmbot.data.SeedLoader;
import farmbot.data.SensorRepository;
import farmbot.data.SqliteSensorRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Step 4 proof: open the SQLite database, create the schema, import the seed CSVs,
 * and read the rows back - showing the JDBC round-trip and the SeedLoader working.
 *
 * Run from the project root:
 *   javac -cp out -d demo demo/Step4Check.java
 *   java  -cp "out;demo;lib/sqlite-jdbc-3.46.1.3.jar" Step4Check
 */
public final class Step4Check {

    public static void main(String[] args) {
        String dbPath = "data/farmbot.db";

        System.out.println("=== Step 4: Repository + SeedLoader ===");
        System.out.println("database: " + dbPath);
        System.out.println();

        try (SensorRepository repo = new SqliteSensorRepository(dbPath)) {
            repo.initSchema();
            System.out.println("[schema] all tables created (CREATE TABLE IF NOT EXISTS)");
            System.out.println();

            SeedLoader loader = new SeedLoader(repo, Path.of("data", "seed"));
            loader.importIfEmpty();
            System.out.println();

            System.out.println("total readings in sensor_readings: " + repo.count());
            System.out.println();

            // The story: soil moisture crosses below 30% at 09:16.
            System.out.println("--- history: SOIL-01 (read back from SQLite, oldest first) ---");
            List<Reading> moisture = repo.findBySensor("SOIL-01", 60);
            for (Reading r : moisture) {
                String flag = r.getValue() < 30.0 ? "   <-- DRY (below 30%)" : "";
                System.out.printf("  %s  %5.1f%s  %-8s%s%n",
                        r.getRecordedAtText(), r.getValue(), r.getUnit(), r.getLabel(), flag);
            }
            System.out.println();

            System.out.println("--- latest reading per sensor ---");
            Map<String, Reading> latest = repo.latestPerSensor();
            for (Reading r : latest.values()) {
                System.out.printf("  %-8s %-16s %8.2f %-6s %s%n",
                        r.getSensorId(), r.getSensorType(), r.getValue(), r.getUnit(), r.getLabel());
            }
        }

        System.out.println();
        System.out.println("=== done ===");
    }
}
