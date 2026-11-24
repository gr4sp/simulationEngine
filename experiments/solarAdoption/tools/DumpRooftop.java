import core.Gr4spSim;
import java.io.PrintWriter;
import java.util.*;
/** Prints, for every half-hour of the irradiance register, the rooftop plane-of-array irradiance (kW/m2) the model
 *  uses (Gr4spSim.getRooftopExposure) and the rooftop efficiency: output per kW = exposure x efficiency. */
public class DumpRooftop {
    public static void main(String[] a) throws Exception {
        Gr4spSim sim = new Gr4spSim(42);
        sim.setEndSimDate(new GregorianCalendar(1999, 0, 1).getTime());
        sim.settings.folderOutput = a[0];
        sim.start();
        double eff = sim.settings.getSolarEfficiency("Solar", "Solar - Rooftop");
        TreeSet<Date> ts = new TreeSet<>(sim.getHalfhour_solar_exposure().keySet());
        try (PrintWriter w = new PrintWriter(a[0] + "/rooftop_exposure.csv")) {
            w.println("time,poa_kw_m2,efficiency");
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Date t : ts) w.println(f.format(t) + "," + sim.getRooftopExposure(t) + "," + eff);
        }
        System.out.println("DONE");
        System.exit(0);
    }
}
