package core.Technical;

import core.Relationships.ActorAssetRelationship;
import core.Gr4spSim;
import core.settings.Settings;
import sim.engine.SimState;
import sim.util.Double2D;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/*** Definition of the class generator ****/

public class Generator implements java.io.Serializable, Asset {
    private static final long serialVersionUID = 1;

    public static boolean carbonTax = false;
    public static double carbonTaxValue = 23.00;

    public boolean reducedCapacity = false;
    public double reducedCapacityValue;

    public static boolean subExists = false;
    public static boolean coalSubExists = false;
    public static double renewableSubVal = 0.9;

    public boolean isSub = false;

    private int id;// asset id
    private String region; // the list of generation assets includes all the regions of the NEM, therefore
                           // this has to be filtered out using VIC1 (or VIC) for Victoria
    private String assetType; // options are: "Existing Plant", "Lighting System", "Non- Existing Plant", and
                              // "Project"
    private String name;// name of power plant or "Site Name" according to the GenerationInformation
                        // database from the NEM
    private String ownerName;// name of the owner
    private String techTypeDescriptor; // combustion steam-subcritical, combustion Open Cycle Gas turbines (OCGT)
                                       // phtovoltaic flat panet, hydro-gravity, hydro-run of river, solar PV, wind
                                       // onshore/offshore,
    private String fuelSourceDescriptor; // fuel source descriptor: according to AEMO it can be: brown coal, black coal,
                                         // gas, Diesel, waste coal mine gas, landfill methane, etc.
    private String ownership;// public, private, PPP, Cooperative?
    private double maxCapacity; // I assume the maximum capacity as being the nameplate capacity in MW
    private double availableCapacity; // Capacity available once the capacity factor is taken into account. It can
                                      // change every month for all techs, but for solar it changes every 30min.
    private double storageCapacity; // in MWh
    private String dispatchTypeDescriptor; // scheduled (S), Semi-scheduled (SS), non-scheduled (NS)

    private String duid;
    private int numUnits; // in NEM description
    private String fuelBucketSummary; // type of fuel

    private int lifecycle; // life cycle in years

    // Surplus capacity after supplying households with solar. The number of
    // households equals the no_units
    public double solarSurplusCapacity;

    // LCOE Price Parameters
    public double basePriceMWh;
    public double marketPriceCap;

    // Price Evolution
    private double historicCapacityFactor;
    private double historicGeneratedMWh;
    private double monthlyGeneratedMWh;
    private double historicRevenue;
    private double potentialRevenue;
    private int bidsInSpot;

    // Capacity Factor
    public double minCapacityFactor;
    public double maxCapacityFactor;
    public double maxCapacityFactorSummer;

    // NonScheduled evolution
    private int numTimesUsedOffSpot;

    // Emission Factor
    public double minEF;
    public double linRateEF;
    public double expRateEF;

    // Solar Efficiency Generator
    public double solarEfficiency;

    private Double2D location;// coordinates where the generation is located

    // Start and end of operations
    private Date start;
    private int startYear;
    private Date end;

    // Visualization Parameters
    public double diameter;

    // TODO: Ownership and Management definition
    ArrayList<ActorAssetRelationship> assetRelationships;

    // Market Participation
    Boolean inPrimaryMarket;
    Boolean inSecondaryMarket;

    /**
     * Create here other constructors that call the main constructor to include fix
     * values to the parameters and make the class more flexible:
     * public Generator (); this *include here the parameters values.
     **/

    public Generator(int genId, String region, String assetType,
            String genName, String owner, String techType, String fuelType,
            Double gencap, String dispachType, Date start, Date expectedEnd, Date end,
            String duid, int no_units, double storageCapacityMwh, String fuelBucketSummary, String unit_status,
            Settings settings) {
        this.id = genId;
        this.region = region;
        this.assetType = assetType;
        this.name = genName;
        this.ownerName = owner;
        this.maxCapacity = gencap;
        this.techTypeDescriptor = techType.trim();
        this.fuelSourceDescriptor = fuelType.trim();
        this.dispatchTypeDescriptor = dispachType;
        this.assetRelationships = new ArrayList<>();
        this.diameter = 50.0;
        this.start = start;

        Calendar c = Calendar.getInstance();
        c.setTime(this.start);
        this.startYear = c.get(Calendar.YEAR);

        this.end = end;

        if (this.end == null)
            this.end = expectedEnd;

        this.duid = duid;
        this.numUnits = no_units;
        this.storageCapacity = storageCapacityMwh;
        this.fuelBucketSummary = null;
        if (fuelBucketSummary != null)
            this.fuelBucketSummary = fuelBucketSummary.trim();

        this.location = null;

        // default lifecycle of 30 years
        this.lifecycle = 30;

        historicCapacityFactor = getCapacityFactor(1);
        historicGeneratedMWh = 0.0;
        monthlyGeneratedMWh = 0.0;
        historicRevenue = 0.0;
        potentialRevenue = 0.0;
        bidsInSpot = 0;
        numTimesUsedOffSpot = 0;

        // Load Settings specified through YAML settings file
        basePriceMWh = settings.getBasePriceMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        marketPriceCap = settings.getMarketPriceCap(this.fuelSourceDescriptor, this.techTypeDescriptor);

        minCapacityFactor = settings.getMinCapacityFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        maxCapacityFactor = settings.getMaxCapacityFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        maxCapacityFactorSummer = settings.getMaxCapacityFactorSummer(this.fuelSourceDescriptor,
                this.techTypeDescriptor);

        minEF = settings.getMinEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);
        linRateEF = settings.getLinRateEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);
        expRateEF = settings.getExpRateEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);

        if (settings.isMarketPaticipant(dispatchTypeDescriptor, "primary", maxCapacity))
            inPrimaryMarket = true;
        else
            inPrimaryMarket = false;

        if (settings.isMarketPaticipant(dispatchTypeDescriptor, "secondary", maxCapacity))
            inSecondaryMarket = true;
        else
            inSecondaryMarket = false;

        solarEfficiency = settings.getSolarEfficiency(this.fuelSourceDescriptor, this.techTypeDescriptor);

        if (Generator.subExists
                && (this.fuelSourceDescriptor.contains("Solar") || this.fuelSourceDescriptor.contains("Wind"))) {
            this.isSub = true;
        }

        // if (Generator.coalSubExists && (this.fuelSourceDescriptor.contains("Coal"))){
        // this.isSub = true;
        // }
        // System.out.println(this.fuelSourceDescriptor);
        // System.out.println(this.techTypeDescriptor);

    }

    // get solar generation using monthly or half hour solar exposure
    static public double getSolarGeneration(float solarExposure, float capacitySolar, double solarEfficiency) {

        double generation = 0.0;

        generation = capacitySolar * solarExposure * solarEfficiency;

        // in this case, the energy yield is given in energy output per area, or energy
        // generated in KWh//m2 per month
        return generation;
    }

    // Once the generator has been running for longer than its lifecycle (30 years
    // by default), the Emission Factor will stop growing,
    // It will remain at the 30 year level
    // Brown Coal generators prior 1964 (year Hazelwood started operating) had less
    // thermal efficiency
    @Override
    public double getEmissionsFactor(int currentYear) {

        int age = currentYear - startYear;
        if (age > this.lifecycle)
            age = this.lifecycle;

        double emissionsFactor = minEF + (linRateEF * (Math.exp(expRateEF * (age))));
        return emissionsFactor;

    }

    public int monthsInSpot(SimState state) {
        Gr4spSim data = (Gr4spSim) state;

        Date today = data.getCurrentSimDate();
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        int currentMonth = c.get(Calendar.MONTH) + 1;
        int currentYear = c.get(Calendar.YEAR) + 1;
        if (this.start.after(data.getStartSpotMarketDate())) {
            c.setTime(this.start);
        } else {
            c.setTime(data.getStartSpotMarketDate());
        }
        int startMonth = c.get(Calendar.MONTH) + 1;
        int startYear = c.get(Calendar.YEAR) + 1;

        int months = (currentYear - startYear) * 12 + currentMonth - startMonth;

        return months;
    }

    public void updateHistoricCapacityFactor(SimState state) {

        // Update Capacity based on historic amount sold
        if (bidsInSpot > 0) {
            // historicCapacityFactor = historicRevenue / (maxCapacity * priceMinMWh *
            // (bidsInSpot / 2.0));
            historicCapacityFactor = historicRevenue / potentialRevenue;
            historicCapacityFactor = (double) Math.round(historicCapacityFactor * 100000d) / 100000d;

            if (historicCapacityFactor > 1.0) {
                System.out.println("Capacity Factor greater than 1: " + historicCapacityFactor);
            }

        }

    }

    public double priceMWhLCOE(int year) {

        // Adjust for Java Date
        year = year % 1900;

        double historicCF = Math.max(historicCapacityFactor, minCapacityFactor);

        double result;

        if (this.isSub) {
            result = (bidsInSpot == 0 || historicCF > maxCapacityFactor)
                    ? (Generator.renewableSubVal * basePriceMWh) / maxCapacityFactor
                    : (Generator.renewableSubVal * basePriceMWh) / historicCF;
        } else {
            result = (bidsInSpot == 0 || historicCF > maxCapacityFactor) ? basePriceMWh / maxCapacityFactor
                    : basePriceMWh / historicCF;
        }
        if (Generator.carbonTax && (year + 1900 > 2011)) {
            double tax = (getEmissionsFactor(year) * Generator.carbonTaxValue);

            result += tax;
        }

        return Math.min(result, marketPriceCap);

    }

    public double getSolarSurplusCapacity() {
        return solarSurplusCapacity;
    }

    // $/MWH
    public double basePriceMWh() {
        return basePriceMWh;
    }

    public double marketPriceCap() {
        return marketPriceCap;
    }

    public double getCapacityFactor(int currentMonth) {

        if (currentMonth > 11 && currentMonth < 3)
            return maxCapacityFactorSummer;
        else
            return maxCapacityFactor;
    }

    // returns MW
    public void computeAvailableSolarCapacity(SimState state, Date today, Date currentTime, int currentMonth,
            double consumption) {
        Gr4spSim data = (Gr4spSim) state;

        // Solar doesn't use capacity factor, but instead uses the available solar
        // exposure by month
        if (currentTime != null) {

            availableCapacity = getSolarGeneration(
                    data.getDaily_solar_exposure().getOrDefault(currentTime, (float) 0.0),
                    (float) this.maxCapacity * (float) 1000.0, this.solarEfficiency) / (float) 1000.0;

        }
    }

    public void computeAvailableCapacity(SimState state, Date today, Date currentTime, int currentMonth) {
        double capacityFactor = getCapacityFactor(currentMonth);
        availableCapacity = this.getMaxCapacity() * capacityFactor;

        // Reduced capacity action
        if (this.reducedCapacity) {
            availableCapacity *= reducedCapacityValue;
        }
    }

    @Override
    public double diameter() {
        return diameter;
    }

    @Override
    public String toString() {
        return "Generator [id=" + id + ", name=" + name + ", fuelSourceDescriptor="
                + fuelSourceDescriptor + ", ownership=" + ownership + ", techTypeDescriptor=" + techTypeDescriptor
                + ", maxCapacity=" + maxCapacity
                + ", lifecycle=" + lifecycle + ", " +
                ", location=" + location
                + "]";
    }

    @Override
    public void addAssetRelationship(ActorAssetRelationship newAssetRel) {
        this.assetRelationships.add(newAssetRel);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getfuelSourceDescriptor() {
        return fuelSourceDescriptor;
    }

    public void setfuelSourceDescriptor(String fuelSourceDescriptor) {
        this.fuelSourceDescriptor = fuelSourceDescriptor;
    }

    public int getBidsInSpot() {
        return bidsInSpot;
    }

    // When a Bid is added, keep track of the potential historic revenue;
    public void setBidsInSpot(int bidsInSpot, double availableCapacity, int year) {
        this.bidsInSpot = bidsInSpot;
        // potentialRevenue += (availableCapacity * (priceMinMWh/maxCapacityFactor) ) /
        // 2.0;
        potentialRevenue += (availableCapacity * priceMWhLCOE(year)) / 2.0;

        // if (Generator.carbonTax && (year + 1900 > 2011)){
        // potentialRevenue -= (getEmissionsFactor(year) * availableCapacity *
        // Generator.carbonTaxValue) / 2.0;
        // }
        // if (potentialRevenue < 0) System.out.println((year+1900) + " TOO MUCH " +
        // Generator.carbonTaxValue);

    }

    public int getNumTimesUsedOffSpot() {
        return numTimesUsedOffSpot;
    }

    public void setNumTimesUsedOffSpot(int numTimesUsedOffSpot) {
        this.numTimesUsedOffSpot = numTimesUsedOffSpot;
    }

    public double getMonthlyGeneratedMWh() {
        return monthlyGeneratedMWh;
    }

    public void setMonthlyGeneratedMWh(double monthlyGeneratedMWh) {
        this.monthlyGeneratedMWh = monthlyGeneratedMWh;
    }

    public double getHistoricGeneratedMWh() {
        return historicGeneratedMWh;
    }

    public void setHistoricGeneratedMWh(double historicGeneratedMWh) {
        this.historicGeneratedMWh = historicGeneratedMWh;
    }

    public double getHistoricRevenue() {
        return historicRevenue;
    }

    public void setHistoricRevenue(double historicRevenue, int year) {
        this.historicRevenue = historicRevenue;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(int lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Double2D getLocation() {
        return location;
    }

    public void setLocation(Double2D location) {
        this.location = location;
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) {
        this.ownership = ownership;
    }

    public String getFuelSourceDescriptor() {
        return fuelSourceDescriptor;
    }

    public void setFuelSourceDescriptor(String fuelSourceDescriptor) {
        this.fuelSourceDescriptor = fuelSourceDescriptor;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTechTypeDescriptor() {
        return techTypeDescriptor;
    }

    public void setTechTypeDescriptor(String techTypeDescriptor) {
        this.techTypeDescriptor = techTypeDescriptor;
    }

    public double getHistoricCapacityFactor() {
        return historicCapacityFactor;
    }

    public void setHistoricCapacityFactor(double h) {
        historicCapacityFactor = h;
    }

    public String getDispatchTypeDescriptor() {
        return dispatchTypeDescriptor;
    }

    public void setBasePriceMWh(double basePriceMWh) {
        this.basePriceMWh = basePriceMWh;
    }

    public void setMarketPriceCap(double marketPriceCap) {
        this.marketPriceCap = marketPriceCap;
    }

    public double getBasePriceMWh() {
        return basePriceMWh;
    }

    public double getMarketPriceCap() {
        return marketPriceCap;
    }

    public Boolean getInPrimaryMarket() {
        return inPrimaryMarket;
    }

    public Boolean getInSecondaryMarket() {
        return inSecondaryMarket;
    }

    public void setInPrimaryMarket(boolean v) {
        inPrimaryMarket = v;
    }

    public void setInSecondaryMarket(boolean v) {
        inSecondaryMarket = v;
    }

    public double getAvailableCapacity() {
        return availableCapacity;
    }
}