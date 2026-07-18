package core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seeded end-to-end regression: runs a short simulation (1998 to END_YEAR, fixed
 * seed) exactly the way runFromPythonEMA() drives it, and compares the resulting
 * year-summary CSV cell-by-cell against a committed golden baseline. Any change
 * to simulation logic that shifts results makes this fail loudly — the safety
 * net for Phase 3.
 *
 * Skips when the local gr4spdb postgres is unreachable (CI has no DB).
 *
 * To regenerate the baseline after an INTENDED behaviour change:
 *   ./gradlew test --tests core.SimulationRegressionIT -Dgr4sp.updateBaseline=true
 * (requires build.gradle to pass the property through, see test { systemProperty })
 * then commit the updated baseline file.
 */
@Tag("sim")
public class SimulationRegressionIT {

    private static final long SEED = 42L;
    // Past the 2019 forecast base year so the run exercises the forecast-era model
    // paths (the code Phase 3 will change), not just historic-data replay.
    private static final int END_YEAR = 2030;
    private static final File BASELINE =
            new File("src/test/resources/regression/VICSimDataYearSummary_seed_42_to_2030.csv");
    private static final double REL_TOLERANCE = 1e-6;

    private static boolean dbAvailable() {
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection c = DriverManager.getConnection(Gr4spSim.url)) {
                return c.isValid(3);
            }
        } catch (Exception e) {
            return false;
        }
    }

    @TempDir
    Path tempOut;

    @Test
    void shortSeededRunMatchesGoldenBaseline() throws Exception {
        Assumptions.assumeTrue(dbAvailable(),
                "postgres gr4spdb not reachable - skipping simulation regression test");

        Gr4spSim sim = new Gr4spSim(SEED);
        sim.setEndSimDate(new GregorianCalendar(END_YEAR, 0, 1).getTime());
        sim.settings.folderOutput = tempOut.toString();
        sim.settingsAfterBaseYear.folderOutput = tempOut.toString();

        // Drive the run the way runFromPythonEMA() does (minus the deprecated
        // SecurityManager): SaveData ends the schedule and writes the CSVs when
        // the simulated date reaches endSimDate.
        sim.start();
        while (sim.schedule.step(sim)) {
            // step until SaveData calls finish() at endSimDate
        }
        sim.finish();

        File produced = tempOut.resolve("csv").resolve(sim.yamlFileName)
                .resolve("VICSimDataYearSummary_seed_" + SEED + ".csv").toFile();
        assertTrue(produced.isFile(), "year summary CSV not written: " + produced);

        if (System.getProperty("gr4sp.updateBaseline") != null) {
            BASELINE.getParentFile().mkdirs();
            Files.copy(produced.toPath(), BASELINE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            fail("Baseline regenerated at " + BASELINE + " - inspect, commit, and re-run without -Dgr4sp.updateBaseline");
        }

        assertTrue(BASELINE.isFile(), "committed baseline missing: " + BASELINE
                + " (generate with -Dgr4sp.updateBaseline=true)");

        List<String> expected = Files.readAllLines(BASELINE.toPath());
        List<String> actual = Files.readAllLines(produced.toPath());
        assertEquals(expected.size(), actual.size(), "row count differs");

        List<String> mismatches = new ArrayList<>();
        String[] header = expected.get(0).split(",", -1);
        for (int row = 0; row < expected.size() && mismatches.size() < 20; row++) {
            String[] e = expected.get(row).split(",", -1);
            String[] a = actual.get(row).split(",", -1);
            if (e.length != a.length) {
                mismatches.add("row " + row + ": column count " + e.length + " vs " + a.length);
                continue;
            }
            for (int col = 0; col < e.length && mismatches.size() < 20; col++) {
                if (e[col].equals(a[col])) continue;
                Double de = tryParse(e[col]);
                Double da = tryParse(a[col]);
                if (de != null && da != null) {
                    double scale = Math.max(Math.abs(de), Math.abs(da));
                    if (Math.abs(de - da) <= REL_TOLERANCE * Math.max(scale, 1e-12)) continue;
                }
                String colName = col < header.length ? header[col] : ("col" + col);
                mismatches.add("row " + row + ", " + colName + ": expected " + e[col] + " got " + a[col]);
            }
        }
        assertTrue(mismatches.isEmpty(),
                () -> "simulation output diverged from golden baseline:\n" + String.join("\n", mismatches));
    }

    private static Double tryParse(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
