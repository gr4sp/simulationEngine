package core.settings;

import com.esotericsoftware.yamlbeans.YamlReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every settings YAML must parse into Settings — this catches config rot such as
 * the unresolved merge-conflict markers once baked into BAUVIC26.yaml. The
 * *future*.yaml files are partial overlays (applied after the base year), so the
 * full-schema assertions only apply to the base files.
 */
public class SettingsYamlTest {

    /**
     * Scenario configs written against a pre-2021 Settings schema (old flat arena
     * block, priceMinMWh instead of basePriceMWh, etc.). They cannot be loaded by
     * the current code and need a deliberate migration or retirement decision —
     * the old price fields have different semantics, so a mechanical rename would
     * silently change scenario economics. Remove a file from this set once it has
     * been migrated; this test then enforces the current schema for it.
     */
    private static final Set<String> KNOWN_LEGACY_SCHEMA = new HashSet<>(Arrays.asList(
            "BAURegional.yaml", "BAUVIC26.yaml", "JTVIC.yaml", "RTVIC.yaml"));

    @Test
    void everySettingsYamlParsesIntoSettings() {
        File dir = new File("simulationSettings");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yaml"));
        assertNotNull(files, "simulationSettings directory must exist");
        assertTrue(files.length > 0, "no settings YAML files found");

        List<String> problems = new ArrayList<>();
        for (File f : files) {
            Settings s;
            try {
                s = new YamlReader(new FileReader(f)).read(Settings.class);
            } catch (Exception e) {
                if (!KNOWN_LEGACY_SCHEMA.contains(f.getName())) {
                    problems.add(f.getName() + ": " + e.getMessage());
                }
                continue;
            }
            if (s == null) {
                problems.add(f.getName() + ": parsed to null");
                continue;
            }
            boolean isFutureOverlay = f.getName().toLowerCase().contains("future");
            if (!isFutureOverlay) {
                if (s.getStartDate() == null) problems.add(f.getName() + ": startDate missing");
                if (s.getEndDate() == null) problems.add(f.getName() + ": endDate missing");
                if (s.generators == null || s.generators.isEmpty())
                    problems.add(f.getName() + ": generators map empty");
                else if (!s.generators.containsKey("Default"))
                    problems.add(f.getName() + ": Default generator fallback missing");
            }
        }
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
    }
}
