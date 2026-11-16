package core.Social;

import core.Gr4spSim;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Solar adoption, step 3 (experiments/solarAdoption/DESIGN.md, sections 2-3): household rooftop PV adoption as a
 * decision, behind the switch rooftopAdoption: endogenous. Only built in that mode, so recorded runs are untouched.
 *
 * Cohorts (decision D1): household type (6) x dwelling (4) x main heating fuel (2) x tenure (3) x equivalised income
 * quintile (5) = 720, each a share of the model's households in the year (householdCohortsFile). A cohort holds a count
 * of adopters, not individual households.
 *
 * Each month, the non-adopters of cohort c install at the hazard
 *     h_c(t) = e_c [a + b A_c(t)] f(PB_c(t)),   f(PB) = 1 / (1 + exp(k (PB - PB0)))
 * A_c: share of the cohort that has adopted (imitation); e_c: eligibility and barriers (renters, flats, quintile 1,
 * multiplied); PB_c: simple payback in years of a system of the month's mean size (installation register) =
 * net cost / first-year saving, in current dollars:
 *   net cost = system price x size - federal rebate (with its means test) - certificates (deeming x zone rating x
 *              (multiplier on the first kW, Solar Credits) x price) - Victorian Solar Homes rebate (owner-occupiers
 *              under its income limit); values from rooftopEconomicsFile, each holding from its date to the next row
 *   saving   = output x self-consumed share x the household tariff + output x (1 - share) x the feed-in tariff the
 *              system gets at installation ("retail": the tariff); output = size x the model's rooftop yield (mean annual
 *              plane-of-array irradiation of the record x the rooftop efficiency), so it matches the rooftop generators.
 * The household tariff is the model's (EndUserUnit.currentTariff, real January-2019 c/kWh, last month's), turned into
 * current dollars with the CPI factor.
 *
 * Output to SimPolicies: monthly installations and kW, which replace the recorded register (solar_aggregated_kw) as
 * the source of the rooftop generators. The register itself is left as recorded, so the N21 add-back of the historic
 * demand keeps the recorded fleet while Arena subtracts the simulated one. Systems of non-household owners (small
 * businesses) are not modelled: a share of the recorded register's capacity and installations is added as is
 * (rooftopNonResidentialShareFile by year, else the constant rooftopNonResidentialShare).
 */
public class RooftopAdoption implements java.io.Serializable {

    static final String[] TENURES = {"renting", "mortgage", "outright"};

    //cohort attributes, index c
    final List<String> type = new ArrayList<>(), dwelling = new ArrayList<>(), fuel = new ArrayList<>(), tenure = new ArrayList<>();
    final List<Integer> quintile = new ArrayList<>();
    final Map<String, Integer> index = new HashMap<>();
    //year -> share, household income bounds (AUD/yr) per cohort
    final TreeMap<Integer, double[]> share = new TreeMap<>(), incomeLo = new TreeMap<>(), incomeHi = new TreeMap<>();
    //economics register: item -> date -> value (String: fit_export can be "retail")
    final Map<String, TreeMap<Date, String>> econ = new HashMap<>();
    //self-consumed share by household type (rooftopSelfConsumptionFile): type -> date -> share
    final Map<String, TreeMap<Date, Double>> selfConsumption = new HashMap<>();
    //non-household share of the register by year: {kW, installations} (rooftopNonResidentialShareFile; empty: constant)
    final TreeMap<Integer, double[]> nonResidential = new TreeMap<>();

    //state
    double[] adopters, adoptersKw, lastPayback, households;
    double yieldIrradiation = -1;   //mean annual plane-of-array irradiation (kWh/m2), times efficiency = kWh/kW/yr
    float lastTariffNominal = 0f;
    //household installations not yet passed to SimPolicies: released once they add up to one system, so months of
    //near-zero hazard do not create rooftop generators of almost no capacity
    double pendingInstalls = 0, pendingKw = 0;
    final TreeMap<Date, Double> simInstalls = new TreeMap<>(), simKw = new TreeMap<>();
    //yearly report: year -> per cohort {households, adopters, adopters' kW, payback, installs in the year}
    final TreeMap<Integer, double[][]> report = new TreeMap<>();
    double[] installsThisYear;

    public RooftopAdoption(Gr4spSim data) throws IOException {
        Path folder = Paths.get("").toAbsolutePath().normalize().resolve("simulationSettings");
        readCohorts(folder.resolve(data.settings.getHouseholdCohortsFile()));
        readEconomics(folder.resolve(data.settings.getRooftopEconomicsFile()));
        readSelfConsumption(folder.resolve(data.settings.getRooftopSelfConsumptionFile()));
        if (data.settings.getRooftopNonResidentialShareFile() != null)
            for (String[] r : csv(folder.resolve(data.settings.getRooftopNonResidentialShareFile())))
                nonResidential.put(Integer.parseInt(r[0]), new double[]{Double.parseDouble(r[1]), Double.parseDouble(r[2])});
        int n = type.size();
        adopters = new double[n];
        adoptersKw = new double[n];
        lastPayback = new double[n];
        households = new double[n];
        installsThisYear = new double[n];
    }

    private static List<String[]> csv(Path p) throws IOException {
        List<String[]> rows = new ArrayList<>();
        boolean header = true;
        for (String line : Files.readAllLines(p)) {
            if (line.startsWith("#") || line.isBlank()) continue;
            if (header) { header = false; continue; }
            rows.add(splitCsv(line));
        }
        return rows;
    }

    //comma split that keeps quoted fields together
    static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean q = false;
        for (char ch : line.toCharArray()) {
            if (ch == '"') q = !q;
            else if (ch == ',' && !q) { out.add(b.toString()); b.setLength(0); }
            else b.append(ch);
        }
        out.add(b.toString());
        return out.toArray(new String[0]);
    }

    private static Date date(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException("bad date " + s);
        }
    }

    //year,type,dwelling,fuel,tenure,quintile,share,income_lo,income_hi
    private void readCohorts(Path p) throws IOException {
        List<String[]> rows = csv(p);
        for (String[] r : rows) {
            String key = r[1] + "|" + r[2] + "|" + r[3] + "|" + r[4] + "|" + r[5];
            if (!index.containsKey(key)) {
                index.put(key, type.size());
                type.add(r[1]); dwelling.add(r[2]); fuel.add(r[3]); tenure.add(r[4]); quintile.add(Integer.parseInt(r[5]));
            }
        }
        for (String[] r : rows) {
            int y = Integer.parseInt(r[0]);
            int c = index.get(r[1] + "|" + r[2] + "|" + r[3] + "|" + r[4] + "|" + r[5]);
            share.computeIfAbsent(y, k -> new double[type.size()])[c] = Double.parseDouble(r[6]);
            incomeLo.computeIfAbsent(y, k -> new double[type.size()])[c] = Double.parseDouble(r[7]);
            incomeHi.computeIfAbsent(y, k -> new double[type.size()])[c] = Double.parseDouble(r[8]);
        }
    }

    //item,date_from,value,...
    private void readEconomics(Path p) throws IOException {
        for (String[] r : csv(p)) econ.computeIfAbsent(r[0], k -> new TreeMap<>()).put(date(r[1]), r[2].trim());
    }

    //type,date_from,share,...
    private void readSelfConsumption(Path p) throws IOException {
        for (String[] r : csv(p))
            selfConsumption.computeIfAbsent(r[0], k -> new TreeMap<>()).put(date(r[1]), Double.parseDouble(r[2]));
    }

    //value of an economics item in force at t (null before its first row)
    String econAt(String item, Date t) {
        TreeMap<Date, String> m = econ.get(item);
        if (m == null) return null;
        Map.Entry<Date, String> e = m.floorEntry(t);
        return e == null ? null : e.getValue();
    }

    double econNum(String item, Date t, double otherwise) {
        String v = econAt(item, t);
        if (v == null) return otherwise;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return otherwise;
        }
    }

    double selfConsumedShare(String householdType, Date t) {
        TreeMap<Date, Double> m = selfConsumption.containsKey(householdType) ? selfConsumption.get(householdType) : selfConsumption.get("ALL");
        if (m == null) throw new IllegalStateException("no self-consumption share for " + householdType + " (nor ALL)");
        Map.Entry<Date, Double> e = m.floorEntry(t);
        return e == null ? m.firstEntry().getValue() : e.getValue();
    }

    private static <V> V atYear(TreeMap<Integer, V> m, int y) {
        Map.Entry<Integer, V> e = m.floorEntry(y);
        return e == null ? m.firstEntry().getValue() : e.getValue();
    }

    //Mean annual irradiation on the rooftop plane (kWh/m2) over the complete years of the irradiance register
    //(at least 95 % of the half-hours), as LoadData.rooftopForecastFromModelYield sums it
    private double meanAnnualIrradiation(Gr4spSim data) {
        TreeMap<Integer, double[]> byYear = new TreeMap<>();
        Calendar c = Calendar.getInstance();
        for (Date t : data.getHalfhour_solar_exposure().keySet()) {
            c.setTime(t);
            double[] s = byYear.computeIfAbsent(c.get(Calendar.YEAR), k -> new double[2]);
            s[0] += data.getRooftopExposure(t) * 0.5;
            s[1] += 1;
        }
        double sum = 0;
        int n = 0;
        for (double[] s : byYear.values())
            if (s[1] >= 0.95 * 17520) { sum += s[0]; n++; }
        if (n == 0) throw new IllegalStateException("rooftopAdoption: no complete year of irradiance");
        data.LOGGER.info("rooftopAdoption: mean annual rooftop irradiation " + sum / n + " kWh/m2 over " + n + " years");
        return sum / n;
    }

    //Mean system size (kW) of the month: the register's, else the nearest earlier month's, else its first
    private static double systemKw(Gr4spSim data, Date t) {
        HashMap<Date, Float> m = data.getSolar_system_capacity_kw();
        Float v = m.get(t);
        if (v != null && v > 0) return v;
        Date best = null, first = null;
        for (Map.Entry<Date, Float> e : m.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (first == null || e.getKey().before(first)) first = e.getKey();
            if (!e.getKey().after(t) && (best == null || e.getKey().after(best))) best = e.getKey();
        }
        if (best == null) best = first;
        return best == null ? data.settings.getForecastSolarInstallCapacity() : m.get(best);
    }

    //Household tariff of last month in current c/kWh (EndUserUnit reports real January-2019 c/kWh once the spot
    //market has run 12 months); the last positive value is kept when the tariff is not set
    private float tariffNominal(Gr4spSim data, Date t) {
        if (data.getConsumptionActors().isEmpty()) return lastTariffNominal;
        float real = data.getConsumptionActors().get(0).getCurrentTariff();
        if (real <= 0f) return lastTariffNominal;
        Calendar c = Calendar.getInstance();
        c.setTime(t);
        c.set(Calendar.MONTH, 0);
        Float conv = data.getCpi_conversion().get(c.getTime());
        Calendar s = Calendar.getInstance();
        s.setTime(data.getStartSpotMarketDate());
        s.add(Calendar.MONTH, 11);
        //before then the tariff is the recorded historic one (current dollars)
        float nominal = (t.before(s.getTime()) || conv == null || conv <= 0f) ? real : real / conv;
        lastTariffNominal = nominal;
        return nominal;
    }

    //Simple payback (years) for cohort c at t; +infinity when the saving is not positive
    double payback(Gr4spSim data, int c, Date t, double sizeKw, double yieldKwhPerKw, double tariff, int year) {
        double price = econNum("system_price", t, 0) * 1000.0 * sizeKw;
        //federal rebate: rate per W up to the cap; from July 2007 on the first kW only; means test on household income
        double rebate = 0;
        double rate = econNum("rebate_rate", t, 0);
        if (rate > 0) {
            double kwRebated = t.before(date("2007-07-01")) ? sizeKw : Math.min(sizeKw, 1.0);
            rebate = Math.min(rate * 1000.0 * kwRebated, econNum("rebate_cap", t, Double.MAX_VALUE));
            double limit = econNum("rebate_income_limit", t, 0);
            if (limit > 0) rebate *= eligibleShare(c, year, limit);
        }
        //certificates: deeming years x zone rating (MWh/kW/yr) x (multiplier on the first kW, Solar Credits) x price
        double cert = 0;
        if (econAt("certificate_multiplier", t) != null) {
            double mult = econNum("certificate_multiplier", t, 1);
            double multKw = Math.min(sizeKw, econNum("certificate_multiplied_kw", t, sizeKw));
            double kwWeighted = mult * multKw + (sizeKw - multKw);
            cert = Math.floor(econNum("certificate_deeming_years", t, 0) * econNum("certificate_zone_rating", t, 0) * kwWeighted)
                    * econNum("certificate_price", t, 0);
        }
        //Victorian Solar Homes rebate: owner-occupiers under the income limit
        double vic = 0;
        double vicRate = econNum("vic_rebate_rate", t, 0);
        if (vicRate > 0 && !tenure.get(c).equals("renting")) {
            vic = Math.min(vicRate * price, econNum("vic_rebate_cap", t, Double.MAX_VALUE));
            double limit = econNum("vic_rebate_income_limit", t, 0);
            if (limit > 0) vic *= eligibleShare(c, year, limit);
        }
        double netCost = Math.max(0.0, price - rebate - cert - vic);
        //first-year saving (AUD): self-consumed output at the tariff, exports at the feed-in tariff of the date
        double output = sizeKw * yieldKwhPerKw;
        double sc = selfConsumedShare(type.get(c), t);
        String fit = econAt("fit_export", t);
        double fitCents = (fit == null || fit.equalsIgnoreCase("retail")) ? tariff : Double.parseDouble(fit);
        double saving = (output * sc * tariff + output * (1 - sc) * fitCents) / 100.0;
        return saving > 0 ? netCost / saving : Double.POSITIVE_INFINITY;
    }

    //Share of cohort c's households with household income below `limit` (current AUD/yr): uniform within the bounds
    double eligibleShare(int c, int year, double limit) {
        double lo = atYear(incomeLo, year)[c], hi = atYear(incomeHi, year)[c];
        if (hi <= lo) return limit > lo ? 1 : 0;
        return Math.max(0, Math.min(1, (limit - lo) / (hi - lo)));
    }

    double eligibility(Gr4spSim data, int c) {
        double e = 1.0;
        if (tenure.get(c).equals("renting")) e *= data.settings.getAdoptionRenterFactor();
        if (dwelling.get(c).contains("FLAT")) e *= data.settings.getAdoptionFlatFactor();
        if (quintile.get(c) == 1) e *= data.settings.getAdoptionLowIncomeFactor();
        return e;
    }

    /**
     * One month of adoption at t (first day of the month). Called by SimPolicies before it creates the month's rooftop
     * generators from installsAt / kwAt.
     */
    public void step(Gr4spSim data, Date t) {
        if (yieldIrradiation < 0) yieldIrradiation = meanAnnualIrradiation(data);
        Calendar cal = Calendar.getInstance();
        cal.setTime(t);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        Integer hh = data.getMonthly_domestic_consumers_register().get(t);
        if (hh == null) {
            //last month's count (the register is complete for the run; this is a guard)
            double s = 0;
            for (double h : households) s += h;
            hh = (int) Math.round(s);
        }
        double[] sh = atYear(share, year);
        double sizeKw = systemKw(data, t);
        double yieldKwhPerKw = yieldIrradiation * data.settings.getSolarEfficiency("Solar", "Solar - Rooftop");
        float tariff = tariffNominal(data, t);
        double a = data.settings.getAdoptionInnovation(), b = data.settings.getAdoptionImitation();
        double k = data.settings.getAdoptionPaybackSlope(), pb0 = data.settings.getAdoptionPaybackMidpoint();
        double installs = 0;
        for (int c = 0; c < sh.length; c++) {
            households[c] = sh[c] * hh;
            if (tariff <= 0f || households[c] <= 0) { lastPayback[c] = Double.POSITIVE_INFINITY; continue; }
            double nonAdopters = Math.max(0.0, households[c] - adopters[c]);
            double adopted = Math.min(1.0, adopters[c] / households[c]);
            double pb = payback(data, c, t, sizeKw, yieldKwhPerKw, tariff, year);
            lastPayback[c] = pb;
            double f = Double.isInfinite(pb) ? 0.0 : 1.0 / (1.0 + Math.exp(k * (pb - pb0)));
            double h = Math.min(1.0, eligibility(data, c) * (a + b * adopted) * f);
            double n = h * nonAdopters;
            adopters[c] += n;
            adoptersKw[c] += n * sizeKw;
            installsThisYear[c] += n;
            installs += n;
        }
        //systems of non-household owners: the recorded register's share
        Integer recordedKw = data.getSolar_aggregated_kw().get(t);
        Integer recordedN = data.getSolar_number_installs().get(t);
        double nonResKw = data.settings.getRooftopNonResidentialShare(), nonResN = nonResKw;
        if (!nonResidential.isEmpty()) {
            double[] v = atYear(nonResidential, year);
            nonResKw = v[0];
            nonResN = v[1];
        }
        pendingInstalls += installs;
        pendingKw += installs * sizeKw;
        double householdN = 0, householdKw = 0;
        if (pendingInstalls >= 1.0) {
            householdN = pendingInstalls;
            householdKw = pendingKw;
            pendingInstalls = 0;
            pendingKw = 0;
        }
        double kw = householdKw + (recordedKw == null ? 0 : nonResKw * recordedKw);
        double n = householdN + (recordedN == null ? 0 : nonResN * recordedN);
        if (kw > 0) {
            simInstalls.put(t, n);
            simKw.put(t, kw);
        }
        if (month == 11) {
            double[][] r = new double[sh.length][];
            for (int c = 0; c < sh.length; c++)
                r[c] = new double[]{households[c], adopters[c], adoptersKw[c], lastPayback[c], installsThisYear[c]};
            report.put(year, r);
            installsThisYear = new double[sh.length];
        }
    }

    public Double installsAt(Date t) { return simInstalls.get(t); }

    public Double kwAt(Date t) { return simKw.get(t); }

    /** Adopters and kW by cohort at the end of each year (and the month's mean payback), one row per cohort and year. */
    public void write(String file) {
        try (Writer w = Files.newBufferedWriter(Paths.get(file))) {
            w.write("year,type,dwelling,fuel,tenure,quintile,households,adopters,adopters_kw,installs_in_year,payback_years_december\n");
            for (Map.Entry<Integer, double[][]> e : report.entrySet()) {
                double[][] r = e.getValue();
                for (int c = 0; c < r.length; c++)
                    w.write(e.getKey() + ",\"" + type.get(c) + "\"," + dwelling.get(c) + "," + fuel.get(c) + "," + tenure.get(c) + ","
                            + quintile.get(c) + "," + String.format(Locale.ROOT, "%.2f,%.2f,%.2f,%.2f,%.3f", r[c][0], r[c][1], r[c][2], r[c][4],
                            Double.isInfinite(r[c][3]) ? -1.0 : r[c][3]) + "\n");
            }
        } catch (IOException ex) {
            System.err.println("RooftopAdoption.write: " + ex);
        }
    }
}
