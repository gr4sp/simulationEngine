package core.Relationships;

import core.Gr4spSim;
import core.Technical.Generator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Merit-order clearing logic of the spot market. Uses real Generator bids built
 * from VIC.yaml settings ("Default" generator settings apply to the test fuel/tech),
 * and a bare Gr4spSim (constructor only, no DB) where settings are needed.
 */
public class SpotMarketTest {

    private static final double EPS = 1e-9;

    private static Gr4spSim sim;

    @BeforeAll
    static void setUp() {
        sim = new Gr4spSim(42L);
    }

    private static Date date(int year, int month, int day) {
        return new GregorianCalendar(year, month - 1, day).getTime();
    }

    private static Generator makeGenerator(String name) {
        return new Generator(1, "VIC1", "Existing Plant", name, "TestOwner",
                "UnitTestTech", "UnitTestFuel", 1000.0, "S",
                date(1990, 1, 1), date(2100, 1, 1), null,
                "TESTDUID", 1, 0.0, null, "In Service", sim.settings);
    }

    private static Bid bid(double dollarMWh, double capacity) {
        return new Bid(null, makeGenerator("gen@" + dollarMWh), dollarMWh, capacity);
    }

    @Test
    void bidsSortByPriceAscending() {
        ArrayList<Bid> bids = new ArrayList<>();
        bids.add(bid(30, 1));
        bids.add(bid(10, 1));
        bids.add(bid(20, 1));
        Collections.sort(bids);
        assertEquals(10, bids.get(0).dollarMWh, EPS);
        assertEquals(20, bids.get(1).dollarMWh, EPS);
        assertEquals(30, bids.get(2).dollarMWh, EPS);
    }

    @Test
    void marginalBidSetsPriceAndIsProrated() {
        SpotMarket market = new SpotMarket("test");
        Bid cheap = bid(10, 100);
        Bid marginal = bid(20, 100);
        Bid expensive = bid(30, 100);
        market.addBidder(cheap);
        market.addBidder(marginal);
        market.addBidder(expensive);

        market.computeMarketPrice(150.0, sim, date(2005, 6, 1), 2005);

        assertEquals(20, market.getMarketPrice(), EPS);
        assertEquals(0, market.getUnmetDemand(), EPS);
        assertEquals(2, market.getSuccessfulBids().size());
        // Cheapest bid dispatched fully, marginal bid prorated to the residual 50 MW
        assertEquals(100, cheap.capacity, EPS);
        assertEquals(50, marginal.capacity, EPS);
        // The expensive bid is never dispatched
        assertFalse(market.getSuccessfulBids().contains(expensive));
        // Successful generators are credited with capacity/2 MWh (30-min market)
        assertEquals(50, ((Generator) cheap.asset).getMonthlyGeneratedMWh(), EPS);
        assertEquals(25, ((Generator) marginal.asset).getMonthlyGeneratedMWh(), EPS);
        assertEquals(50 * 10, ((Generator) cheap.asset).getHistoricRevenue(), EPS);
    }

    @Test
    void samePriceTierSharesResidualProRata() {
        SpotMarket market = new SpotMarket("test");
        Bid cheap = bid(10, 100);
        Bid tierA = bid(20, 60);
        Bid tierB = bid(20, 60);
        market.addBidder(cheap);
        market.addBidder(tierA);
        market.addBidder(tierB);

        market.computeMarketPrice(160.0, sim, date(2005, 6, 1), 2005);

        assertEquals(20, market.getMarketPrice(), EPS);
        assertEquals(0, market.getUnmetDemand(), EPS);
        assertEquals(3, market.getSuccessfulBids().size());
        // Residual 60 MW split pro-rata across the 120 MW offered at the marginal price
        assertEquals(30, tierA.capacity, EPS);
        assertEquals(30, tierB.capacity, EPS);
    }

    @Test
    void exactCapacityMatchIsDispatchedFullyWithoutProration() {
        SpotMarket market = new SpotMarket("test");
        Bid only = bid(15, 100);
        market.addBidder(only);

        market.computeMarketPrice(100.0, sim, date(2005, 6, 1), 2005);

        assertEquals(15, market.getMarketPrice(), EPS);
        assertEquals(0, market.getUnmetDemand(), EPS);
        assertEquals(100, only.capacity, EPS);
    }

    @Test
    void supplyShortfallAppliesImportMarkupAndRecordsUnmetDemand() {
        SpotMarket market = new SpotMarket("test");
        Bid only = bid(50, 100);
        market.addBidder(only);

        // 2005 is before the forecast base year, so the historic import price factor applies
        int year = 2005;
        assertTrue(year < sim.settings.getBaseYearConsumptionForecast());
        market.computeMarketPrice(500.0, sim, date(year, 6, 1), year);

        double expectedPrice = 50 * (1.0 + sim.settings.getImportPriceFactor());
        assertEquals(expectedPrice, market.getMarketPrice(), EPS);
        assertEquals(400, market.getUnmetDemand(), EPS);
    }

    @Test
    void emissionIntensityIsCapacityWeightedAverage() {
        SpotMarket market = new SpotMarket("test");
        Bid a = bid(10, 100);
        Bid b = bid(20, 300);
        market.getSuccessfulBids().add(a);
        market.getSuccessfulBids().add(b);

        int year = 2020;
        market.computeGenEmissionIntensity(year);

        double efA = ((Generator) a.asset).getEmissionsFactor(year);
        double efB = ((Generator) b.asset).getEmissionsFactor(year);
        double expected = (100 * efA + 300 * efB) / 400.0;
        assertEquals(expected, market.getEmissionsIntensity(), EPS);
    }
}
