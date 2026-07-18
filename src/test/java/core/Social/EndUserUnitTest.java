package core.Social;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Household consumption lookup tables in EndUserUnit.computeConsumption:
 * gas vs all-electric profiles, household-size capping, month indexing.
 */
public class EndUserUnitTest {

    private static final double EPS = 1e-9;

    private static EndUserUnit household(int persons, boolean gas) {
        Date creation = new GregorianCalendar(2000, 0, 1).getTime();
        return new EndUserUnit(1, "TestHousehold", persons, 1, gas, false, 50000.0, creation);
    }

    @Test
    void singlePersonAllElectricMatchesTable() {
        EndUserUnit h = household(1, false);
        assertEquals(505.6666667, h.computeConsumption(0), EPS);   // January
        assertEquals(892.3333333, h.computeConsumption(6), EPS);   // July (winter peak)
        assertEquals(584.6666667, h.computeConsumption(11), EPS);  // December
    }

    @Test
    void singlePersonWithGasConsumesLessElectricity() {
        EndUserUnit h = household(1, true);
        assertEquals(296.3333333, h.computeConsumption(0), EPS);
        assertEquals(492.3333333, h.computeConsumption(6), EPS);
        // gas households consume less electricity than all-electric in every month
        EndUserUnit noGas = household(1, false);
        for (int month = 0; month < 12; month++) {
            assertTrue(h.computeConsumption(month) < noGas.computeConsumption(month),
                    "month " + month);
        }
    }

    @Test
    void threePersonHouseholdMatchesTable() {
        EndUserUnit h = household(3, false);
        assertEquals(773.6666667, h.computeConsumption(4), EPS);
        assertEquals(1117.0, h.computeConsumption(7), EPS);
    }

    @Test
    void householdsLargerThanFivePersonsUseFivePersonRow() {
        for (int month = 0; month < 12; month++) {
            assertEquals(household(5, false).computeConsumption(month),
                    household(9, false).computeConsumption(month), EPS);
            assertEquals(household(5, true).computeConsumption(month),
                    household(9, true).computeConsumption(month), EPS);
        }
    }

    @Test
    void consumptionGrowsWithHouseholdSize() {
        for (int persons = 1; persons < 5; persons++) {
            assertTrue(household(persons, false).computeConsumption(0)
                            <= household(persons + 1, false).computeConsumption(0),
                    persons + " -> " + (persons + 1) + " persons");
        }
    }
}
