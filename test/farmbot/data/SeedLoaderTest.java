package farmbot.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The SeedLoader must skip a malformed row, not crash the whole import. */
class SeedLoaderTest {

    @Test
    void skipsMalformedRowsButImportsGoodOnes(@TempDir Path tmp) throws Exception {
        Path seedDir = tmp.resolve("seed");
        Files.createDirectories(seedDir);
        Files.writeString(seedDir.resolve("soil.csv"),
                "sensor_id,sensor_type,value,unit,raw,status,recorded_at\n"
              + "SOIL-01,SOIL_MOISTURE,41.2,%,,NORMAL,2026-11-23 09:00:00\n"
              + "SOIL-01,SOIL_MOISTURE,NOTANUMBER,%,,NORMAL,2026-11-23 09:01:00\n"  // bad value
              + "SOIL-01,SOIL_MOISTURE,39.6,%,,BOGUS_WORD,2026-11-23 09:02:00\n"    // bad status
              + "SOIL-01,SOIL_MOISTURE,38.9,%,,DRY,2026-11-23 09:03:00\n");

        Path db = tmp.resolve("seedtest.db");
        try (SensorRepository repo = new SqliteSensorRepository(db.toString())) {
            repo.initSchema();
            int imported = new SeedLoader(repo, seedDir).importAll();
            assertEquals(2, imported, "two good rows imported");
            assertEquals(2, repo.count(), "two rows persisted; the two bad ones skipped");
        }
    }

    @Test
    void parseRowRejectsWrongColumnCount() {
        SeedLoader loader = new SeedLoader(null, Path.of("."));
        assertThrows(IllegalArgumentException.class, () -> loader.parseRow("too,few,columns"));
    }
}
