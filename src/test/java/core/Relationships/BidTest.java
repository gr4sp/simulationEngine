package core.Relationships;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for {@link Bid#compareTo(Bid)}, the merit-order comparator
 * that orders spot-market bids by ascending price ($/MWh). SpotMarketTest exercises
 * this indirectly through clearing; here we pin the comparator contract itself
 * (ordering, ties, negatives) so the sort key can't silently change. Actor/Asset
 * are irrelevant to the comparison, so null is passed for them (no DB needed).
 */
public class BidTest {

    private static Bid bid(double dollarMWh) {
        return new Bid(null, null, dollarMWh, 100.0);
    }

    @Test
    void cheaperBidSortsBeforeDearerBid() {
        assertTrue(bid(10.0).compareTo(bid(20.0)) < 0);
        assertTrue(bid(20.0).compareTo(bid(10.0)) > 0);
    }

    @Test
    void equalPriceBidsCompareEqual() {
        assertEquals(0, bid(42.0).compareTo(bid(42.0)));
    }

    @Test
    void negativeAndZeroPricesOrderCorrectly() {
        // Negative bids (e.g. must-run / renewable curtailment offers) clear first.
        assertTrue(bid(-5.0).compareTo(bid(0.0)) < 0);
        assertTrue(bid(0.0).compareTo(bid(5.0)) < 0);
        assertTrue(bid(-5.0).compareTo(bid(-1.0)) < 0);
    }

    @Test
    void collectionsSortProducesAscendingMeritOrder() {
        List<Bid> bids = new ArrayList<>();
        bids.add(bid(50.0));
        bids.add(bid(-10.0));
        bids.add(bid(50.0));
        bids.add(bid(15.0));
        bids.add(bid(0.0));

        Collections.sort(bids);

        double previous = Double.NEGATIVE_INFINITY;
        for (Bid b : bids) {
            assertTrue(b.dollarMWh >= previous,
                    "bids must be in non-decreasing price order after sort");
            previous = b.dollarMWh;
        }
        assertEquals(-10.0, bids.get(0).dollarMWh);
        assertEquals(50.0, bids.get(bids.size() - 1).dollarMWh);
    }

    @Test
    void comparatorConsistentWithNaturalContract() {
        // compareTo must be antisymmetric: sgn(a.compareTo(b)) == -sgn(b.compareTo(a)).
        Bid a = bid(12.5);
        Bid b = bid(30.0);
        assertEquals(-Integer.signum(b.compareTo(a)), Integer.signum(a.compareTo(b)));
    }
}
