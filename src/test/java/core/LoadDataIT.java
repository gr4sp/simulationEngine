package core;

import core.Technical.Generator;
import core.Technical.NetworkAssets;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests against the local gr4spdb postgres database. The whole class
 * skips (does not fail) when the database is unreachable, so CI — which has no
 * postgres — stays green while local runs exercise the full LoadData path.
 *
 * Runs Gr4spSim.loadData() once (the exact sequence start() uses) and checks the
 * populated registers, which doubles as a shape-check on the data loaded by the
 * task-2.4 ETL refresh.
 */
@Tag("db")
public class LoadDataIT {

    private static Gr4spSim sim;

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

    @BeforeAll
    static void loadOnce() {
        Assumptions.assumeTrue(dbAvailable(),
                "postgres gr4spdb not reachable - skipping DB integration tests");
        sim = new Gr4spSim(42L);
        sim.loadData();
    }

    @Test
    void halfHourDemandRegisterIsPopulatedWithSaneValues() {
        Map<?, Double> demand = sim.getHalfhour_demand_register();
        assertFalse(demand.isEmpty(), "half-hour demand register must not be empty");
        int zeros = 0;
        double max = 0;
        for (Double v : demand.values()) {
            assertNotNull(v);
            assertTrue(Double.isFinite(v) && v >= 0, "demand value " + v);
            if (v == 0) zeros++;
            if (v > max) max = v;
        }
        assertTrue(max > 1000, "max demand " + max + " MW is not plausible for VIC");
        // One known missing AEMO interval (1999-04-12 22:00) is stored as 0; a jump
        // in this count would indicate a broken data refresh, not a lone gap.
        assertTrue(zeros <= 5, zeros + " zero-demand intervals (expected at most the known 1)");
    }

    @Test
    void monthlyConsumptionRegisterIsPopulated() {
        Map<?, Double> consumption = sim.getMonthly_consumption_register();
        assertFalse(consumption.isEmpty(), "monthly consumption register must not be empty");
        for (Double v : consumption.values()) {
            assertTrue(Double.isFinite(v) && v >= 0, "consumption value " + v);
        }
    }

    @Test
    void halfHourSolarExposureIsPopulated() {
        Map<?, Float> exposure = sim.getHalfhour_solar_exposure();
        assertFalse(exposure.isEmpty(), "half-hour solar exposure must not be empty");
        for (Float v : exposure.values()) {
            assertTrue(Float.isFinite(v) && v >= 0, "exposure value " + v);
        }
    }

    @Test
    void solarInstallationRegistersArePopulated() {
        assertFalse(sim.getSolar_number_installs().isEmpty(), "solar installs register");
        assertFalse(sim.getSolar_aggregated_kw().isEmpty(), "aggregated solar kW register");
    }

    @Test
    void inflationAndForecastRegistersArePopulated() {
        assertFalse(sim.getAnnual_inflation().isEmpty(), "annual inflation register");
        assertFalse(sim.getAnnual_forecast_consumption_register().isEmpty(),
                "forecast consumption register");
        int baseYear = sim.settings.getBaseYearConsumptionForecast();
        assertTrue(sim.getAnnual_forecast_consumption_register().containsKey(baseYear + 1),
                "forecast must cover the year after the base year (" + (baseYear + 1) + ")");
    }

    @Test
    void arenasAreLoaded() {
        assertFalse(sim.getArena_register().isEmpty(), "arena register must not be empty");
    }

    @Test
    void selectGenTechReturnsGeneratorsWithinCapacityRange() {
        ArrayList<Generator> gens = LoadData.selectGenTech(sim, "30", "100000");
        assertFalse(gens.isEmpty(), "no generators between 30 and 100000 MW nameplate");
        for (Generator g : gens) {
            assertNotNull(g.getName());
            assertNotNull(g.getFuelSourceDescriptor());
            assertTrue(g.getMaxCapacity() >= 30 && g.getMaxCapacity() <= 100000,
                    g.getName() + " capacity " + g.getMaxCapacity());
        }
    }

    @Test
    void selectNetworkReturnsAssetsWithSaneLosses() {
        for (String subname : new String[]{"distribution", "transmission"}) {
            ArrayList<NetworkAssets> nets = LoadData.selectNetwork(sim, subname);
            assertFalse(nets.isEmpty(), "no " + subname + " network assets");
            for (NetworkAssets n : nets) {
                assertTrue(n.getGridLosses() >= 0 && n.getGridLosses() < 1,
                        subname + " losses " + n.getGridLosses());
            }
        }
    }
}
