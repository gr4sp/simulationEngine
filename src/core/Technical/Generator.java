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

    private int id;//asset id
    private String region; //the list of generation assets includes all the regions of the NEM, therefore this has to be filtered out using VIC1 (or VIC) for  Victoria
    private String assetType; // options are: "Existing Plant", "Lighting System", "Non- Existing Plant", and "Project"
    private String name;//name of power plant or "Site Name" according to the GenerationInformation database from the NEM
    private String ownerName;//name of the owner
    private String techTypeDescriptor; //combustion steam-subcritical, combustion Open Cycle Gas turbines (OCGT) phtovoltaic flat panet, hydro-gravity, hydro-run of river, solar PV, wind onshore/offshore,
    private String fuelSourceDescriptor; //fuel source descriptor: according to AEMO it can be: brown coal, black coal, gas, Diesel, waste coal mine gas, landfill methane, etc.
    private String ownership;//public, private, PPP, Cooperative?
    private double maxCapacity; //I assume the maximum capacity as being the nameplate capacity in MW
    private double availableCapacity; //Capacity available once the capacity factor is taken into account. It can change every month for all techs, but for solar it changes every 30min.
    private double storageCapacity; // in MWh
    private String dispatchTypeDescriptor; // scheduled (S), Semi-scheduled (SS), non-scheduled (NS)

    private String duid;
    private int numUnits; // in NEM description
    private String fuelBucketSummary; // type of fuel

    private int lifecycle; //life cycle in years

    //Surplus capacity after supplying households with solar. The number of
    //households equals the no_units
    public double solarSurplusCapacity;

    //LCOE Price Parameters
    public double basePriceMWh;
    public double marketPriceCap;

    //Price Evolution
    private double historicCapacityFactor;
    private double historicGeneratedMWh;
    private double monthlyGeneratedMWh;
    private double historicRevenue;
    private double potentialRevenue;
    private int bidsInSpot;

    //Capacity Factor
    public double minCapacityFactor;
    public double maxCapacityFactor;
    public double maxCapacityFactorSummer;

    //NonScheduled evolution
    private int numTimesUsedOffSpot;


    //Emission Factor
    public double minEF;
    public double linRateEF;
    public double expRateEF;

    //Solar Efficiency Generator
    public double solarEfficiency;


    private Double2D location;//coordinates where the generation is located

    //Start and end of operations
    private Date start;
    private int startYear;
    private Date end;

    //Visualization Parameters
    public double diameter;

    //TODO: Ownership and Management definition
    ArrayList<ActorAssetRelationship> assetRelationships;

    //Market Participation
    Boolean inPrimaryMarket;
    Boolean inSecondaryMarket;

    /**
     * Create here other constructors that call the main constructor to include fix values to the parameters and make the class more flexible:
     * public Generator (); this *include here the parameters values.
     **/


    public Generator(int genId, String region, String assetType,
                     String genName, String owner, String techType, String fuelType,
                     Double gencap, String dispachType, Date start, Date expectedEnd, Date end,
                     String duid, int no_units, double storageCapacityMwh, String fuelBucketSummary, String unit_status, Settings settings) {
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

        //default lifecycle of 30 years
        this.lifecycle = 30;

        historicCapacityFactor = getCapacityFactor(1);
        historicGeneratedMWh = 0.0;
        monthlyGeneratedMWh = 0.0;
        historicRevenue = 0.0;
        potentialRevenue = 0.0;
        bidsInSpot = 0;
        numTimesUsedOffSpot = 0;

        //Load Settings specified through YAML settings file
        basePriceMWh = settings.getBasePriceMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        marketPriceCap = settings.getMarketPriceCap(this.fuelSourceDescriptor, this.techTypeDescriptor);

        minCapacityFactor = settings.getMinCapacityFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        maxCapacityFactor = settings.getMaxCapacityFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        maxCapacityFactorSummer = settings.getMaxCapacityFactorSummer(this.fuelSourceDescriptor, this.techTypeDescriptor);

        minEF = settings.getMinEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);
        linRateEF = settings.getLinRateEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);
        expRateEF = settings.getExpRateEF(this.fuelSourceDescriptor, this.techTypeDescriptor, this.startYear);

        if( settings.isMarketPaticipant( dispatchTypeDescriptor,"primary", maxCapacity ) )
            inPrimaryMarket = true;
        else
            inPrimaryMarket = false;

        if( settings.isMarketPaticipant( dispatchTypeDescriptor,"secondary", maxCapacity ) )
            inSecondaryMarket = true;
        else
            inSecondaryMarket = false;


        solarEfficiency = settings.getSolarEfficiency(this.fuelSourceDescriptor, this.techTypeDescriptor);

        //Price if a generator sells always at full capacity
        //double targetPrice =  priceMinMWh();
        //double targetGen = maxCapacity * 8760;

        //Max capacity (MW)* hours per year (H) * TargetPrice ($/MWH) => $
        // double targetReveneueYear = targetGen * targetPrice;

       /* System.out.println("Gen " + this.fuelSourceDescriptor);
        System.out.println("Target Price $/MWh: "+ targetPrice);
        System.out.println("Target Generation MWh: "+ targetGen);
        System.out.println("Target Historic Revenue per year $: "+ targetReveneueYear);*/
    }

    // get solar generation using monthly or half hour solar exposure
    static public double getSolarGeneration(float solarExposure, float capacitySolar, double solarEfficiency) {

        double generation = 0.0;

        generation = capacitySolar * solarExposure * solarEfficiency;

        //in this case, the energy yield is given in energy output per area, or energy generated in KWh//m2 per month
        return generation;
    }



    //Once the generator has been running for longer than its lifecycle (30 years by default), the Emission Factor will stop growing,
    //It will remain at the 30 year level
    //Brown Coal generators prior 1964 (year Hazelwood started operating) had less thermal efficiency
    public double getEmissionsFactor(int currentYear) {

        int age = currentYear - startYear;
        if (age > this.lifecycle) age = this.lifecycle;

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

        // Update Capacity based on historic amount sold, and reset every year
//        if(rounds%12 > 0)
//            historicCapacityFactor = historicGeneratedMW / (maxCapacity * (double)rounds);
//        else
//            historicCapacityFactor = getCapacityFactor(1);

        // Update Capacity based on historic amount sold
        if (bidsInSpot > 0) {
            //            historicCapacityFactor = historicRevenue / (maxCapacity * priceMinMWh * (bidsInSpot / 2.0));
            historicCapacityFactor = historicRevenue / potentialRevenue;
            historicCapacityFactor = (double) Math.round(historicCapacityFactor * 100000d) / 100000d;

            if (historicCapacityFactor > 1.0) {
                System.out.printf("Capacity Factor greater than 1: " + historicCapacityFactor);
            }

        }

    }

    //If historic is below minCapacityFactor, then use min CapacityFactor to ge prices.
    // MinCapacityFactors makes the assumption that the capacity
    //factor didn't fall  below that threshold by selling through OTCs instead of SPOT
    public double priceMWhLCOE() {

        double result;
        double historicCF = historicCapacityFactor;

        //Make sure bid is not below base price/minCF
        if (historicCF < minCapacityFactor) {
            historicCF = minCapacityFactor;
        }

        //If the generator has never bid and never produced even off market (OTC), then use the LCOE assuming it starts with maxCapacity
        //If historicCF is above maxCF because it participated in a bid with high pay off, then make sure price above base price/maxCF
        //otherwise just use the historicCF
        if (bidsInSpot == 0 || historicCF > maxCapacityFactor) {
            result = basePriceMWh() / maxCapacityFactor;
        } else {
            result = basePriceMWh() / historicCF;
        }

        if (name.equalsIgnoreCase("Challicum Hills")){
            //System.out.println(result);
            result = result;
        }
        //double result =  priceMinMWh() +  ( (priceMaxMWh()-priceMinMWh()) * (Math.exp( - priceRateParameterMWh() * historicCF)) );

        //make sure price does not go above maximum allowed price
        if(result > marketPriceCap) result = marketPriceCap;

        return result;
    }

    public double getSolarSurplusCapacity() { return solarSurplusCapacity; }

    //$/MWH
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

    //returns MW
    public void computeAvailableSolarCapacity(SimState state, Date today, Date currentTime, int currentMonth, double consumption) {
        Gr4spSim data = (Gr4spSim) state;

        //Solar doesn't use capacity factor, but instead uses the available solar exposure by month
        if (currentTime != null) {
            double dollarMWh = 0;

            //get Generation in KWh
            float solarExposure = (float) 0.0;
            if (data.getHalfhour_solar_exposure().containsKey(currentTime)) {
                //get Generation in KWh
                solarExposure = data.getHalfhour_solar_exposure().get(currentTime);
            }
//            else {
//                System.out.println("\t\t\t" + currentTime + " For some reason this key is lost, use last available value ");
//            }


            //generation in KWh given sun
            double halfHourGeneration = getSolarGeneration(solarExposure, (float) this.getMaxCapacity() * (float) 1000.0, this.solarEfficiency);

            //Solar capacity is the same as half hour generation in this case. MWh each half hour will be capacity in MW
            availableCapacity = halfHourGeneration / (float) 1000.0;

            //Solar suplus as a function of generation and number of units consuming - (30days * 48 halfhour/day = 1440 approx equals half hours in month)
            solarSurplusCapacity = availableCapacity - (numUnits * consumption / 1440 );
            if(solarSurplusCapacity < 0 ) solarSurplusCapacity = 0;

        }
    }
    public void computeAvailableCapacity(SimState state, Date today, Date currentTime, int currentMonth) {
            double capacityFactor = getCapacityFactor(currentMonth);
            availableCapacity = this.getMaxCapacity() * capacityFactor;
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

    //When a Bid is added, keep track of the potential historic revenue;
    public void setBidsInSpot(int bidsInSpot, double availableCapacity) {
        this.bidsInSpot = bidsInSpot;
        //potentialRevenue += (availableCapacity * (priceMinMWh/maxCapacityFactor) ) / 2.0;
        potentialRevenue += (availableCapacity * priceMWhLCOE()) / 2.0;

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

    public void setHistoricRevenue(double historicRevenue) {
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


    public void setFuelSourceDescriptor(String fuelSourceDescriptor) { this.fuelSourceDescriptor = fuelSourceDescriptor; }

    public String getOwnerName(){ return ownerName; }


    public String getTechTypeDescriptor() {
        return techTypeDescriptor;
    }


    public void setTechTypeDescriptor(String techTypeDescriptor) {
        this.techTypeDescriptor = techTypeDescriptor;
    }


    public double getHistoricCapacityFactor() {
        return historicCapacityFactor;
    }

    public void setHistoricCapacityFactor(double h){ historicCapacityFactor = h;}

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

    public Boolean getInPrimaryMarket() { return inPrimaryMarket; }

    public Boolean getInSecondaryMarket() { return inSecondaryMarket; }

    public double getAvailableCapacity() { return availableCapacity; }
}