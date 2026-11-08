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
    //Offer at short-run marginal cost instead of the LCOE rule when non-negative
    public double srmcMWh;
    //Storage offers storageOfferFactor x referencePriceMWh (previous month's mean spot price) when non-negative
    public double storageOfferFactor;
    private double referencePriceMWh;
    //Power offered each half-hour as a share of nameplate when non-negative; otherwise nameplate x capacity factor
    public double availabilityFactor;
    //At most cfCapacity x 24 h of energy a day (hydro)
    public boolean dailyEnergyLimit;
    //Nameplate x this month's capacity factor, MW
    private double cfCapacity;
    private double energyTodayMWh;
    private long energyDay = Long.MIN_VALUE;
    //Daily buy-low, sell-high storage (roundTripEfficiency > 0): state of charge and today's plan by half-hour
    public double roundTripEfficiency;
    private double stateOfChargeMWh;
    private long planDay = Long.MIN_VALUE;
    private final boolean[] chargeSlot = new boolean[48];
    private final boolean[] dischargeSlot = new boolean[48];
    //Short-run cost offer, price bands, peak-only energy and breakdowns (see GeneratorSettings)
    public double heatRateGJperMWh;
    public double variableOMMWh;
    public String fuelPriceFile;
    public double fuelPriceGJ;
    public double mustRunShare;
    public double mustRunPriceMWh;
    public boolean peakOnly;
    public double forcedOutageRate;
    public double outageHours;
    private double carbonPriceTonne;
    private int priceYear;
    private long outageSlot = Long.MIN_VALUE;
    private long outUntilSlot = Long.MIN_VALUE;
    //outagesPerUnit: one outage clock per unit of a multi-unit row (null when the row breaks down as one block)
    private long[] unitOutUntilSlot;
    //This month's fuel price ($/GJ) and the factor that turns real cost settings into this year's dollars
    private double currentFuelPriceGJ;
    private double costFactor = 1.0;

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
        srmcMWh = settings.getSrmcMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        storageOfferFactor = settings.getStorageOfferFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        availabilityFactor = settings.getAvailabilityFactor(this.fuelSourceDescriptor, this.techTypeDescriptor);
        dailyEnergyLimit = settings.getDailyEnergyLimit(this.fuelSourceDescriptor, this.techTypeDescriptor);
        roundTripEfficiency = settings.getRoundTripEfficiency(this.fuelSourceDescriptor, this.techTypeDescriptor);
        heatRateGJperMWh = settings.getHeatRateGJperMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        variableOMMWh = settings.getVariableOMMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        fuelPriceFile = settings.getFuelPriceFile(this.fuelSourceDescriptor, this.techTypeDescriptor);
        fuelPriceGJ = settings.getFuelPriceGJ(this.fuelSourceDescriptor, this.techTypeDescriptor);
        mustRunShare = settings.getMustRunShare(this.fuelSourceDescriptor, this.techTypeDescriptor);
        mustRunPriceMWh = settings.getMustRunPriceMWh(this.fuelSourceDescriptor, this.techTypeDescriptor);
        peakOnly = settings.getPeakOnly(this.fuelSourceDescriptor, this.techTypeDescriptor);
        forcedOutageRate = settings.getForcedOutageRate(this.fuelSourceDescriptor, this.techTypeDescriptor);
        outageHours = settings.getOutageHours(this.fuelSourceDescriptor, this.techTypeDescriptor);
        if (settings.getOutagesPerUnit() && numUnits > 1) {
            unitOutUntilSlot = new long[numUnits];
            java.util.Arrays.fill(unitOutUntilSlot, Long.MIN_VALUE);
        }
        priceYear = this.startYear;
        if (roundTripEfficiency > 0) {
            if (storageCapacity <= 0)
                storageCapacity = maxCapacity * settings.getStorageHours(this.fuelSourceDescriptor, this.techTypeDescriptor);
            stateOfChargeMWh = storageCapacity / 2.0;
        }

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

        // Update Capacity based on historic amount sold
        if (bidsInSpot > 0) {
            //historicCapacityFactor = historicRevenue / (maxCapacity * priceMinMWh * (bidsInSpot / 2.0));
            historicCapacityFactor = historicRevenue / potentialRevenue;
            historicCapacityFactor = (double) Math.round(historicCapacityFactor * 100000d) / 100000d;

            //Only the LCOE offer uses this factor; other offer rules can push it above 1 without effect
            if (historicCapacityFactor > 1.0 && usesLcoeOffer()) {
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

        //make sure price does not go above maximum allowed price
        if(result > marketPriceCap) result = marketPriceCap;

        return result;
    }

    //Price offered in the spot market. Generators with a short-run marginal cost set (variable renewables)
    //offer at it; storage with a storageOfferFactor offers at a markup on the previous month's mean price
    //(LCOE rule until there is one); the rest use the LCOE rule. Off-market prices still use priceMWhLCOE()
    public double offerPriceMWh() {
        if (isArbitrageStorage()) return 0.0;
        if (srmcMWh >= 0) return srmcMWh;
        if (heatRateGJperMWh > 0)
            return heatRateGJperMWh * currentFuelPriceGJ + variableOMMWh * costFactor + getEmissionsFactor(priceYear) * carbonPriceTonne;
        if (storageOfferFactor >= 0 && referencePriceMWh > 0) return storageOfferFactor * referencePriceMWh;
        return priceMWhLCOE();
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

        // Australian summer = Dec (12), Jan (1), Feb (2). currentMonth is 1-12
        // (Calendar.MONTH + 1). The former condition (> 11 && < 3) could never be
        // true, so maxCapacityFactorSummer never applied.
        if (currentMonth == 12 || currentMonth < 3)
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
//            solarSurplusCapacity = availableCapacity - (numUnits * consumption / 1440 );
//            if(solarSurplusCapacity < 0 ) solarSurplusCapacity = 0;

        }
    }
    public void computeAvailableCapacity(SimState state, Date today, Date currentTime, int currentMonth) {
            double capacityFactor = getCapacityFactor(currentMonth);
            cfCapacity = this.getMaxCapacity() * capacityFactor;
            availableCapacity = availabilityFactor >= 0 ? this.getMaxCapacity() * availabilityFactor : cfCapacity;
    }

    //MW offered in this half-hour: the available capacity, or what is left of today's energy if energy-limited
    public double offerCapacity(Date currentTime) {
        if (forcedOutageRate > 0 && slotIndex(currentTime) < outUntilSlot) return 0.0;
        double inService = unitsInServiceShare(currentTime);
        if (inService <= 0.0) return 0.0;
        if (isArbitrageStorage())
            return dischargeSlot[slotOf(currentTime)] ? Math.min(maxCapacity * inService, stateOfChargeMWh * 2.0) : 0.0;
        if (!dailyEnergyLimit) return availableCapacity * inService;
        long t = currentTime.getTime();
        long day = Math.floorDiv(t + java.util.TimeZone.getDefault().getOffset(t), 86400000L);
        if (day != energyDay) {
            energyDay = day;
            energyTodayMWh = 0.0;
        }
        if (peakOnly && !dischargeSlot[slotOf(currentTime)]) return 0.0;
        double remainingMWh = cfCapacity * 24.0 - energyTodayMWh;
        return Math.max(0.0, Math.min(availableCapacity * inService, remainingMWh * 2.0));
    }

    //Share of the row's units not on outage (1 unless outagesPerUnit applies to this row)
    private double unitsInServiceShare(Date currentTime) {
        if (unitOutUntilSlot == null || forcedOutageRate <= 0) return 1.0;
        long slot = slotIndex(currentTime);
        int out = 0;
        for (long u : unitOutUntilSlot) if (slot < u) out++;
        return 1.0 - (double) out / unitOutUntilSlot.length;
    }

    public boolean isArbitrageStorage() {
        return roundTripEfficiency > 0;
    }

    //True when the spot offer comes from the LCOE rule (no short-run cost, fuel cost or storage rule set)
    public boolean usesLcoeOffer() {
        return !isArbitrageStorage() && srmcMWh < 0 && heatRateGJperMWh <= 0 && !(storageOfferFactor >= 0 && referencePriceMWh > 0);
    }

    public boolean plansDay() {
        return isArbitrageStorage() || (peakOnly && dailyEnergyLimit);
    }

    private static long dayOf(Date time) {
        long t = time.getTime();
        return Math.floorDiv(t + java.util.TimeZone.getDefault().getOffset(t), 86400000L);
    }

    private static long slotIndex(Date time) {
        long t = time.getTime();
        return Math.floorDiv(t + java.util.TimeZone.getDefault().getOffset(t), 1800000L);
    }

    //Each half-hour: a unit in service breaks down with the probability that gives forcedOutageRate in the long run,
    //and stays out for an exponentially distributed time (mean outageHours, at least one half-hour)
    public void updateOutage(Date currentTime, ec.util.MersenneTwisterFast random) {
        if (forcedOutageRate <= 0) return;
        long slot = slotIndex(currentTime);
        if (slot == outageSlot) return;
        outageSlot = slot;
        double meanSlots = outageHours * 2.0;
        double p = forcedOutageRate / ((1.0 - forcedOutageRate) * meanSlots);
        if (unitOutUntilSlot != null) {
            for (int u = 0; u < unitOutUntilSlot.length; u++)
                if (slot >= unitOutUntilSlot[u] && random.nextDouble() < p)
                    unitOutUntilSlot[u] = slot + Math.max(1L, Math.round(-Math.log(1.0 - random.nextDouble()) * meanSlots));
            return;
        }
        if (slot < outUntilSlot) return;
        if (random.nextDouble() < p)
            outUntilSlot = slot + Math.max(1L, Math.round(-Math.log(1.0 - random.nextDouble()) * meanSlots));
    }

    public boolean isOut(Date currentTime) {
        if (unitOutUntilSlot != null) return forcedOutageRate > 0 && unitsInServiceShare(currentTime) <= 0.0;
        return forcedOutageRate > 0 && slotIndex(currentTime) < outUntilSlot;
    }

    public void setFuelAndCarbon(double fuelPriceGJ, double carbonPriceTonne, int year, double costFactor) {
        this.currentFuelPriceGJ = fuelPriceGJ;
        this.carbonPriceTonne = carbonPriceTonne;
        this.priceYear = year;
        this.costFactor = costFactor;
    }

    private static int slotOf(Date time) {
        long t = time.getTime();
        return (int) (Math.floorMod(t + java.util.TimeZone.getDefault().getOffset(t), 86400000L) / 1800000L);
    }

    //Plan the day once, from its half-hourly demand: discharge in the highest-demand half-hours that a full store
    //can supply, charge in the lowest-demand ones that refill it after losses. Missing half-hours count as the day's mean
    public void planStorageDay(Date currentTime, java.util.Map<Date, Double> demandRegister) {
        long t = currentTime.getTime();
        long day = Math.floorDiv(t + java.util.TimeZone.getDefault().getOffset(t), 86400000L);
        if (day == planDay) return;
        planDay = day;
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        c.add(Calendar.MINUTE, -30 * slotOf(currentTime));
        double[] demand = new double[48];
        boolean[] known = new boolean[48];
        double sum = 0;
        int n = 0;
        for (int i = 0; i < 48; i++) {
            Double d = demandRegister.get(c.getTime());
            if (d != null) { demand[i] = d; known[i] = true; sum += d; n++; }
            c.add(Calendar.MINUTE, 30);
        }
        for (int i = 0; i < 48; i++) if (!known[i]) demand[i] = n > 0 ? sum / n : 0.0;
        Integer[] order = new Integer[48];
        for (int i = 0; i < 48; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(demand[a], demand[b]));
        double energy = isArbitrageStorage() ? storageCapacity : cfCapacity * 24.0;
        double power = isArbitrageStorage() ? maxCapacity : availableCapacity;
        int discharging = power > 0 ? (int) Math.ceil(energy / (power * 0.5)) : 0;
        int charging = isArbitrageStorage() ? (int) Math.ceil(storageCapacity / (maxCapacity * 0.5 * roundTripEfficiency)) : 0;
        java.util.Arrays.fill(chargeSlot, false);
        java.util.Arrays.fill(dischargeSlot, false);
        int maxDischarging = isArbitrageStorage() ? 24 : 48;
        for (int k = 0; k < Math.min(discharging, maxDischarging); k++) dischargeSlot[order[47 - k]] = true;
        for (int k = 0; k < Math.min(charging, 24); k++) chargeSlot[order[k]] = true;
    }

    //MW drawn from the grid to charge in this half-hour (losses applied on the way in); 0 outside charge slots
    public double chargeThisHalfHour(Date currentTime) {
        if (!chargeSlot[slotOf(currentTime)]) return 0.0;
        double mw = Math.max(0.0, Math.min(maxCapacity, (storageCapacity - stateOfChargeMWh) / roundTripEfficiency * 2.0));
        stateOfChargeMWh += mw / 2.0 * roundTripEfficiency;
        return mw;
    }

    public double getStateOfChargeMWh() {
        return stateOfChargeMWh;
    }

    public void addEnergyToday(double mwh) {
        if (isArbitrageStorage()) stateOfChargeMWh = Math.max(0.0, stateOfChargeMWh - mwh);
        energyTodayMWh += mwh;
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
        //With an availability factor the price feedback still measures use against nameplate x capacity factor
        double potentialCapacity = availabilityFactor >= 0 ? Math.min(availableCapacity, cfCapacity) : availableCapacity;
        potentialRevenue += (potentialCapacity * offerPriceMWh()) / 2.0;

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

    public double getSrmcMWh() {
        return srmcMWh;
    }

    public void setSrmcMWh(double srmcMWh) {
        this.srmcMWh = srmcMWh;
    }

    public double getStorageOfferFactor() {
        return storageOfferFactor;
    }

    public void setStorageOfferFactor(double storageOfferFactor) {
        this.storageOfferFactor = storageOfferFactor;
    }

    public void setReferencePriceMWh(double referencePriceMWh) {
        this.referencePriceMWh = referencePriceMWh;
    }

    public double getBasePriceMWh() {
        return basePriceMWh;
    }

    public double getMarketPriceCap() {
        return marketPriceCap;
    }

    //Storage that buys low and sells high trades in the primary market whatever its size (announced batteries are rolled out in small parts)
    public Boolean getInPrimaryMarket() { return isArbitrageStorage() ? Boolean.TRUE : inPrimaryMarket; }

    public Boolean getInSecondaryMarket() { return isArbitrageStorage() ? Boolean.FALSE : inSecondaryMarket; }

    public void setInPrimaryMarket( boolean v ) { inPrimaryMarket = v; }

    public void setInSecondaryMarket( boolean v ) { inSecondaryMarket = v; }

    public double getAvailableCapacity() { return availableCapacity; }
}