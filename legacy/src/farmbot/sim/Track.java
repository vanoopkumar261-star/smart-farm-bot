package farmbot.sim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a virtual field from a text file - one "LEFT CENTER RIGHT" row per
 * tick - so an autorun demo can drive the line sensor without a Scanner.
 */
public class Track {
    private final List<int[]> rows = new ArrayList<>();

    public Track(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            rows.add(new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            });
        }
    }

    public int size() {
        return rows.size();
    }

    public int[] getRow(int index) {
        return rows.get(index % rows.size());
    }
}
