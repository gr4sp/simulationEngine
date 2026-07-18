package core.Technical;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Network-loss computations of Spm over hand-built asset trees (no DB, no scheduler).
 */
public class SpmTest {

    private static final double EPS = 1e-9;

    private static Date date(int year) {
        return new GregorianCalendar(year, 0, 1).getTime();
    }

    private static NetworkAssets net(int id, double gridLosses, int startYear, int endYear) {
        return new NetworkAssets(id, "Distribution", "sub", "grid", "net" + id, "owner",
                null, gridLosses, "HV", date(startYear), date(endYear));
    }

    private static Spm spm(ArrayList<Spm> contained, ArrayList<NetworkAssets> nets) {
        return new Spm(1, contained, new ArrayList<Generator>(), nets,
                new ArrayList<Storage>(), new ArrayList<ConnectionPoint>());
    }

    @Test
    void lossesAreAveragedAcrossActiveNetworks() {
        ArrayList<NetworkAssets> nets = new ArrayList<>();
        nets.add(net(1, 0.10, 1990, 2100));
        nets.add(net(2, 0.20, 1990, 2100));
        Spm s = spm(new ArrayList<Spm>(), nets);

        assertEquals(0.15, s.computeNetworksLosses(date(2020)), EPS);
    }

    @Test
    void retiredNetworksContributeNothingButStillCountInTheAverage() {
        ArrayList<NetworkAssets> nets = new ArrayList<>();
        nets.add(net(1, 0.10, 1990, 2100));
        nets.add(net(2, 0.20, 1990, 2100));
        nets.add(net(3, 0.30, 1990, 2000)); // retired well before 2020
        Spm s = spm(new ArrayList<Spm>(), nets);

        // Pins CURRENT behaviour: the divisor is network_assets.size(), which includes
        // inactive assets, so a retired network dilutes the average rather than being
        // excluded from it: (0.10 + 0.20) / 3.
        assertEquals(0.10, s.computeNetworksLosses(date(2020)), EPS);
    }

    @Test
    void recursiveLossesSumAveragesAcrossSpmLevels() {
        ArrayList<NetworkAssets> childNets = new ArrayList<>();
        childNets.add(net(1, 0.30, 1990, 2100));
        Spm child = spm(new ArrayList<Spm>(), childNets);

        ArrayList<NetworkAssets> parentNets = new ArrayList<>();
        parentNets.add(net(2, 0.10, 1990, 2100));
        parentNets.add(net(3, 0.20, 1990, 2100));
        ArrayList<Spm> contained = new ArrayList<>();
        contained.add(child);
        Spm parent = spm(contained, parentNets);

        // parent level average (0.15) + child level average (0.30)
        assertEquals(0.45, parent.computeRecursiveNetworksLosses(parent), EPS);
    }

    @Test
    void spmWithoutNetworksHasZeroLosses() {
        Spm s = spm(new ArrayList<Spm>(), new ArrayList<NetworkAssets>());
        assertEquals(0.0, s.computeNetworksLosses(date(2020)), EPS);
        assertEquals(0.0, s.computeRecursiveNetworksLosses(s), EPS);
    }
}
