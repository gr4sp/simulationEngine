package core;

import com.esotericsoftware.yamlbeans.YamlReader;
import core.settings.Settings;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class Gr4spSimTest {

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new Gr4spSim(42L));
    }

    @Test
    void vicYamlParsesSuccessfully() throws Exception {
        String root = Paths.get(".").toAbsolutePath().normalize().toString();
        YamlReader reader = new YamlReader(new FileReader(root + "/simulationSettings/VIC.yaml"));
        Settings s = reader.read(Settings.class);
        assertNotNull(s, "Settings object must not be null");
    }

    @Test
    void vicYamlHasExpectedFields() throws Exception {
        String root = Paths.get(".").toAbsolutePath().normalize().toString();
        YamlReader reader = new YamlReader(new FileReader(root + "/simulationSettings/VIC.yaml"));
        Settings s = reader.read(Settings.class);
        assertNotNull(s.getStartDate(), "startDate must be set");
        assertNotNull(s.getEndDate(), "endDate must be set");
        assertNotNull(s.getAreaCode(), "areaCode must be set");
        assertFalse(s.generators == null || s.generators.isEmpty(), "generators map must be populated");
    }
}
