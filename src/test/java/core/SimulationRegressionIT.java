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
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    /** "rows/max-year" of solar_installation_monthly in backupDB/DB-2021-8-21.sql. */
    private static final String SHIPPED_DB_VINTAGE = "153/2019";

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

    /**
     * The baseline is only meaningful against the database snapshot the repository
     * ships. A gr4spdb refreshed by scripts/data/*.py carries later data - rooftop
     * solar installs to 2025 rather than 2019, demand to 2026 rather than mid-2021 -
     * which legitimately produces a different run. Without this check that shows up
     * as an unreadable several-hundred-column diff instead of the real explanation.
     */
    private static String databaseVintage() {
        String sql = "SELECT count(*) || '/' || max(year) FROM solar_installation_monthly";
        try (Connection c = DriverManager.getConnection(Gr4spSim.url);
             java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "unknown";
        } catch (Exception e) {
            return "unreadable (" + e.getMessage() + ")";
        }
    }

    @Test
    void shortSeededRunMatchesGoldenBaseline() throws Exception {
        Assumptions.assumeTrue(dbAvailable(),
                "postgres gr4spdb not reachable - skipping simulation regression test");

        String vintage = databaseVintage();
        Assumptions.assumeTrue(SHIPPED_DB_VINTAGE.equals(vintage),
                "the database behind " + Gr4spSim.url + " is not the shipped snapshot"
                + " (solar_installation_monthly rows/max-year is " + vintage
                + ", expected " + SHIPPED_DB_VINTAGE + ").\n"
                + "The regression baseline corresponds to backupDB/DB-2021-8-21.sql. A gr4spdb"
                + " refreshed with scripts/data/*.py holds later data and cannot reproduce it,"
                + " so this check is skipped rather than reported as a failure.\n"
                + "To run it, restore the snapshot into another database and point the suite at it:\n"
                + "  createdb gr4spdb_ref && pg_restore -d gr4spdb_ref backupDB/DB-2021-8-21.sql\n"
                + "  ./gradlew test -Dgr4sp.db.url=\"jdbc:postgresql://localhost:5432/gr4spdb_ref?user=postgres\"");

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

        // When the column counts differ the per-cell diff below is useless - every
        // row just reports the same count mismatch. The useful information is which
        // columns appeared or vanished, since each is one generator in gen_register.
        String[] actualHeader = actual.get(0).split(",", -1);
        if (header.length != actualHeader.length) {
            List<String> onlyExpected = new ArrayList<>(Arrays.asList(header));
            onlyExpected.removeAll(Arrays.asList(actualHeader));
            List<String> onlyActual = new ArrayList<>(Arrays.asList(actualHeader));
            onlyActual.removeAll(Arrays.asList(header));
            // Full names are far too long to read in a CI annotation, and most of the
            // difference is usually one repeated kind of unit. Summarise by id and by
            // fuel/year, then show a couple of whole names from each side as samples.
            fail("header column count differs: baseline " + header.length
                    + " vs produced " + actualHeader.length
                    + "\n  missing from the produced run (" + onlyExpected.size() + "): ids "
                    + summariseIds(onlyExpected) + " | kinds " + summariseKinds(onlyExpected)
                    + "\n  present only in the produced run (" + onlyActual.size() + "): ids "
                    + summariseIds(onlyActual) + " | kinds " + summariseKinds(onlyActual)
                    + "\n  sample baseline: " + sample(onlyExpected)
                    + "\n  sample produced: " + sample(onlyActual));
        }
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

    /** Leading ids only, e.g. "323-342, 347, 528" - column names are far too long to list. */
    private static String summariseIds(List<String> names) {
        List<Integer> ids = new ArrayList<>();
        for (String n : names) {
            String head = n.split(" - ", 2)[0].trim();
            try {
                ids.add(Integer.parseInt(head));
            } catch (NumberFormatException ignored) {
                // not an id-prefixed generator column; skipped from the id summary
            }
        }
        Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); ) {
            int j = i;
            while (j + 1 < ids.size() && ids.get(j + 1) == ids.get(j) + 1) j++;
            if (sb.length() > 0) sb.append(", ");
            sb.append(ids.get(i));
            if (j > i) sb.append('-').append(ids.get(j));
            i = j + 1;
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }

    /** Counts by "fuel startYear-endYear", which is what usually differs in bulk. */
    private static String summariseKinds(List<String> names) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String n : names) {
            String[] p = n.split(" - ");
            String key = p.length >= 7 ? p[1] + " " + p[5] + "-" + p[6] : "(unparsed)";
            counts.merge(key, 1, Integer::sum);
        }
        return counts.isEmpty() ? "(none)" : counts.toString();
    }

    private static String sample(List<String> names) {
        return names.isEmpty() ? "(none)"
                : names.subList(0, Math.min(2, names.size())).toString();
    }

    private static Double tryParse(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
