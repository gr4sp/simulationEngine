package core.Technical;

import com.esotericsoftware.yamlbeans.YamlReader;
import core.settings.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure computational methods of Generator: solar yield, emission-factor curve,
 * LCOE-based bid price and capacity factor. Uses VIC.yaml settings; the test
 * fuel/tech resolves to the "Default" generator settings.
 */
public class GeneratorTest {

    private static final double EPS = 1e-9;

    private static Settings settings;

    @BeforeAll
    static void loadSettings() throws Exception {
        String root = Paths.get(".").toAbsolutePath().normalize().toString();
        YamlReader reader = new YamlReader(new FileReader(root + "/simulationSettings/VIC.yaml"));
        settings = reader.read(Settings.class);
        settings.computeSolarEfficiency();
    }

    private static Date date(int year, int month, int day) {
        return new GregorianCalendar(year, month - 1, day).getTime();
    }

    private static Generator makeGenerator(int startYear) {
        return new Generator(1, "VIC1", "Existing Plant", "TestGen", "TestOwner",
                "UnitTestTech", "UnitTestFuel", 500.0, "S",
                date(startYear, 1, 1), date(2100, 1, 1), null,
                "TESTDUID", 1, 0.0, null, "In Service", settings);
    }

    @Test
    void solarGenerationIsCapacityTimesExposureTimesEfficiency() {
        assertEquals(3.0, Generator.getSolarGeneration(2.0f, 3.0f, 0.5), EPS);
        assertEquals(0.0, Generator.getSolarGeneration(0.0f, 3.0f, 0.5), EPS);
    }

    @Test
    void emissionsFactorFollowsExponentialAgeCurve() {
        Generator g = makeGenerator(2000);
        int age = 10;
        double expected = g.minEF + g.linRateEF * Math.exp(g.expRateEF * age);
        assertEquals(expected, g.getEmissionsFactor(2000 + age), EPS);
    }

    @Test
    void emissionsFactorStopsGrowingAfterLifecycle() {
        Generator g = makeGenerator(2000);
        // default lifecycle is 30 years: anything older is capped at the 30-year level
        assertEquals(g.getEmissionsFactor(2030), g.getEmissionsFactor(2045), EPS);
    }

    @Test
    void lcoePriceUsesMaxCapacityFactorWhenNeverBid() {
        Generator g = makeGenerator(2000);
        assertEquals(g.basePriceMWh / g.maxCapacityFactor, g.priceMWhLCOE(), EPS);
    }

    @Test
    void lcoePriceUsesHistoricCapacityFactorOnceBidding() {
        Generator g = makeGenerator(2000);
        g.setBidsInSpot(4, 0.0);
        g.setHistoricCapacityFactor(0.5);
        assertEquals(g.basePriceMWh / 0.5, g.priceMWhLCOE(), EPS);
    }

    @Test
    void lcoePriceFallsBackToMaxCapacityFactorWhenHistoricExceedsIt() {
        Generator g = makeGenerator(2000);
        g.setBidsInSpot(4, 0.0);
        g.setHistoricCapacityFactor(g.maxCapacityFactor + 0.05);
        assertEquals(g.basePriceMWh / g.maxCapacityFactor, g.priceMWhLCOE(), EPS);
    }

    @Test
    void lcoePriceIsCappedAtMarketPriceCap() {
        Generator g = makeGenerator(2000);
        g.basePriceMWh = 1e7;
        assertEquals(g.marketPriceCap, g.priceMWhLCOE(), EPS);
    }

    @Test
    void capacityFactorUsesSummerValueForDecJanFeb() {
        Generator g = makeGenerator(2000);
        // Australian summer = Dec (12), Jan (1), Feb (2) -> summer CF; other months
        // -> annual CF. (The summer CFs in VIC.yaml are currently equalised to their
        // annual values, so the returned number matches either way, but the corrected
        // branch now fires for the summer months.)
        for (int month : new int[]{12, 1, 2}) {
            assertEquals(g.maxCapacityFactorSummer, g.getCapacityFactor(month), EPS, "summer month " + month);
        }
        for (int month : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11}) {
            assertEquals(g.maxCapacityFactor, g.getCapacityFactor(month), EPS, "non-summer month " + month);
        }
    }
}
