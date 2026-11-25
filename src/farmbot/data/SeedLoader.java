package farmbot.data;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads the historical seed data in {@code data/seed/*.csv} and imports it into the
 * database on first start, so the dashboard has a believable backlog to chart and the
 * demo can replay the 09:00-09:19 story.
 *
 * Robustness is the point here (file I/O marks): the loader uses a BufferedReader,
 * and a single malformed row is logged and skipped rather than aborting the whole
 * import. One bad line in one file never loses the other 159 good readings.
 */
public final class SeedLoader {

    /** The seven columns every seed file shares, in order. */
    private static final int EXPECTED_COLUMNS = 7;

    private final SensorRepository repository;
    private final Path seedDirectory;

    public SeedLoader(SensorRepository repository, Path seedDirectory) {
        this.repository = repository;
        this.seedDirectory = seedDirectory;
    }

    /**
     * Imports every CSV, but only if the database has no readings yet. This makes
     * start-up idempotent: running the program a second time does not duplicate the
     * seed rows.
     *
     * @return the number of readings imported (0 if the database was already seeded)
     */
    public int importIfEmpty() {
        int existing = repository.count();
        if (existing > 0) {
            System.out.println("[seed] database already holds " + existing + " readings - skipping import");
            return 0;
        }
        return importAll();
    }

    /**
     * Imports every {@code *.csv} file in the seed directory, regardless of what is
     * already stored. Each file is parsed into Reading objects and batch-inserted.
     *
     * @return the total number of readings imported
     */
    public int importAll() {
        if (!Files.isDirectory(seedDirectory)) {
            System.err.println("[seed] no seed directory at " + seedDirectory.toAbsolutePath() + " - nothing to import");
            return 0;
        }

        List<Path> csvFiles = new ArrayList<>();
        try (Stream<Path> entries = Files.list(seedDirectory)) {
            entries.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                   .sorted()
                   .forEach(csvFiles::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list seed directory " + seedDirectory, e);
        }

        int total = 0;
        for (Path csv : csvFiles) {
            total += importFile(csv);
        }
        System.out.println("[seed] imported " + total + " readings from " + csvFiles.size() + " file(s)");
        return total;
    }

    /** Parses and imports one CSV file. Returns the number of rows imported from it. */
    private int importFile(Path csv) {
        List<Reading> readings = new ArrayList<>();
        int lineNo = 0;
        int skipped = 0;

        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 || line.isBlank()) {
                    continue; // header row, or a blank separator line
                }
                try {
                    readings.add(parseRow(line));
                } catch (RuntimeException badRow) {
                    skipped++;
                    System.err.println("[seed] " + csv.getFileName() + ":" + lineNo
                            + " skipped malformed row (" + badRow.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("[seed] could not read " + csv.getFileName() + " (" + e.getMessage() + ") - skipping file");
            return 0;
        }

        repository.insertBatch(readings);
        System.out.println("[seed]   " + csv.getFileName() + ": " + readings.size()
                + " imported" + (skipped > 0 ? ", " + skipped + " skipped" : ""));
        return readings.size();
    }

    /**
     * Turns one CSV line into a Reading. Any problem - wrong column count, a value
     * that is not a number, an unknown sensor type or status word - throws, and the
     * caller records the line as skipped.
     *
     * Columns: sensor_id, sensor_type, value, unit, raw, status, recorded_at
     */
    Reading parseRow(String line) {
        String[] parts = line.split(",", -1); // -1 keeps trailing empty fields (unit/raw may be blank)
        if (parts.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException("expected " + EXPECTED_COLUMNS + " columns, found " + parts.length);
        }

        String sensorId = parts[0].trim();
        if (sensorId.isEmpty()) {
            throw new IllegalArgumentException("sensor_id is blank");
        }

        SensorType sensorType;
        try {
            sensorType = SensorType.valueOf(parts[1].trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown sensor_type '" + parts[1].trim() + "'");
        }

        double value;
        try {
            value = Double.parseDouble(parts[2].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("value '" + parts[2].trim() + "' is not a number");
        }

        String unit = parts[3].trim();
        String raw = parts[4].trim();
        StatusLabel label = StatusLabel.fromText(parts[5]); // throws on an unknown word

        LocalDateTime recordedAt;
        try {
            recordedAt = LocalDateTime.parse(parts[6].trim(), Reading.TIME_FORMAT);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("recorded_at '" + parts[6].trim() + "' is not a valid timestamp");
        }

        return new Reading(sensorId, sensorType, value, unit, raw, label, recordedAt);
    }
}
