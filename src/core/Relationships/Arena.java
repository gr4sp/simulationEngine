package core.Relationships;

import core.Gr4spSim;
import core.Technical.Generator;
import core.Technical.Spm;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.*;

public class Arena implements Steppable,  java.io.Serializable  {
    private int id;
    private String name;
    private String type;
    private int rounds;

    //Constant value that represents End-Consumer contracts. 999 is the value assigned to contracts DB to end consumer
    public static final int EndConsumer = 999;

    //    private ArrayList<EligibilityRule> eligibilityRules;

    //    private float transactionFee; //percentage fee
    private SpotMarket primarySpot;
    private double avgMonthlyPricePrimarySpot;
    private double avgMonthlyPriceSecondarySpot;
    private double avgMonthlyPriceOffSpot;

    private double avgMonthlyDemandPrimarySpot;
    private double avgMonthlyDemandSecondarySpot;
    private double avgMonthlyDemandOffSpot;

    // Statistics of reliability, Data per month.
    // When demand is unmet, is assumed to be imported at a high price: marketPrice *= importFactor; -> SpotMarket.java:157
    private double unmetDemandMwhPrimary;
    private double unmetDemandHours;
    private double unmetDemandDays;
    private double maxUnmetDemandMwhPerHourPrimary;

    // Statistics of reliability for secondary market
    private double unmetDemandMwhSecondary;
    private double unmetDemandHoursSecondary;
    private double unmetDemandDaysSecondary;
    private double maxUnmetDemandMwhPerHourSecondary;


    private SpotMarket secondarySpot; //Merit order type of market at the distribution level.
    private ArrayList<Contract> bilateral; //can be billing with retailers, PPAs and other types of OTCs with two known parties involved.
    private ArrayList<Contract> fiTs;

    //Each Arena has ONLY one of the following:
    //Merit order rules for spot market
    //PPA - Contracts Act-Act duration over the counter
    //FeedInTariff - Contracts Act-Act over the counterarenas

    private Random randomGenerator;
    boolean existsSecondary;
    Calendar c;


    public Arena(int id, String name, String type, Gr4spSim state) {
        Gr4spSim data = state;

        this.id = id;
        this.name = name;
        this.type = type;
        if (type.equalsIgnoreCase("OTC") || type.equalsIgnoreCase("Retail"))
            bilateral = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("fiTs"))
            fiTs = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("Spot")) {
            if(data.settings.existsMarket("primary"))
                primarySpot = new SpotMarket("Primary");
            else
                primarySpot = null;
            if(data.settings.existsMarket("secondary"))
                secondarySpot = new SpotMarket("secondary");
            else
                secondarySpot = null;
        }
        randomGenerator = new Random();

        if(data.settings.existsMarket("secondary"))
            existsSecondary = true;
        else
            existsSecondary = false;

        c = Calendar.getInstance();

        rounds = 0;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRounds() {
        return rounds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public SpotMarket getPrimarySpot() {
        return primarySpot;
    }

    public SpotMarket getSecondarySpot() {
        return secondarySpot;
    }

    public ArrayList<Contract> getBilateral() {
        return bilateral;
    }

    public ArrayList<Contract> getFiTs() {
        return fiTs;
    }

    public double getAvgMonthlyPricePrimarySpot() { return avgMonthlyPricePrimarySpot; }

    public double getAvgMonthlyPriceSecondarySpot() { return avgMonthlyPriceSecondarySpot; }

    public double getAvgMonthlyPriceOffSpot() { return avgMonthlyPriceOffSpot; }

    public double getUnmetDemandMwhPrimary() { return unmetDemandMwhPrimary; }

    public double getUnmetDemandHours() {  return unmetDemandHours;  }

    public double getUnmetDemandDays() {  return unmetDemandDays;  }

    public double getMaxUnmetDemandMwhPerHourPrimary() {  return maxUnmetDemandMwhPerHourPrimary;  }

    public double getUnmetDemandMwhSecondary() { return unmetDemandMwhSecondary;  }

    public double getUnmetDemandHoursSecondary() { return unmetDemandHoursSecondary; }

    public double getUnmetDemandDaysSecondary() {  return unmetDemandDaysSecondary; }

    public double getMaxUnmetDemandMwhPerHourSecondary() { return maxUnmetDemandMwhPerHourSecondary; }

    /**
     * This tariff represents price for c/KWh
     */
    public Float getEndConsumerTariff(SimState state) {
        float tariff = 0;

        Gr4spSim data = (Gr4spSim) state;

        ArrayList<Float> availableTariffs = new ArrayList<Float>();
        ArrayList<Contract> availableTariffsContracts = new ArrayList<Contract>();

        // Find available tariffs for endConsumers based on current simulation date
        for (Contract c : bilateral) {

            //EndConsumer contract?
            if (c.getBuyerId() == EndConsumer) {

                Date today = data.getCurrentSimDate();

                //Is the contract active today? including starting date [start, end)
                if ((today.after(c.getStart()) || today.equals(c.getStart())) && today.before(c.getEnd())) {
                    availableTariffs.add(c.getPricecKWh());
                    availableTariffsContracts.add(c);
                }

            }
        }

        //Select Tariff based on current Policy
        switch (data.getPolicies().getEndConsumerTariff()) {
            case MAX:
                tariff = Collections.max(availableTariffs);
                break;
            case MIN:
                tariff = Collections.min(availableTariffs);
                break;
            case AVG:
                tariff = (float) availableTariffs.stream()
                        .mapToDouble(a -> a)
                        .average()
                        .orElse(0);
                break;
            case RND:
                int index = randomGenerator.nextInt(availableTariffs.size());
                tariff = availableTariffs.get(index);
                break;
        }

        return tariff;
    }

    /**
     * Wholesale price in $/MWh
     */
    public double getWholesalePrice(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        //If Spot Market hasn't started yet, get historic prices of tariffs as wholesale is not available
        if (data.getStartSpotMarketDate().after(today)) {
            return getEndConsumerTariff(state);
        }

        //If spot is on, get the market price
        double price = 0;
        if(today.after(data.getBaseYearForecastDate())){
            for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
                Arena a = entry.getValue();
                if (a.type.equalsIgnoreCase("Spot")) {

                    //Weighted average between secondarySpot Price and unmet demand supplied by the primary spot
                    if (data.settingsAfterBaseYear.existsMarket("secondary")) {
                        price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                                avgMonthlyPriceSecondarySpot * avgMonthlyDemandSecondarySpot +
                                avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot) /
                                (avgMonthlyDemandPrimarySpot + avgMonthlyDemandSecondarySpot + avgMonthlyDemandOffSpot);

                    } else
                        price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                                avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot) /
                                (avgMonthlyDemandPrimarySpot + avgMonthlyDemandOffSpot);

                }
            }
        }
        else {
            for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
                Arena a = entry.getValue();
                if (a.type.equalsIgnoreCase("Spot")) {

                    //Weighted average between secondaryPrice and unmet demand supplied by the primary spot
                    if (data.settings.existsMarket("secondary")) {
                        price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                                avgMonthlyPriceSecondarySpot * avgMonthlyDemandSecondarySpot +
                                avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot) /
                                (avgMonthlyDemandPrimarySpot + avgMonthlyDemandSecondarySpot + avgMonthlyDemandOffSpot);

                    } else
                        price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                                avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot) /
                                (avgMonthlyDemandPrimarySpot + avgMonthlyDemandOffSpot);

                }
            }
        }
        //$/MWh
        return price;

    }

    /**
     * Create a bid for each Generator that participates in the SPOT market
     * Capacity factors of each generator vary depending on the season.  Data on max capacity is obtained from
     * the "Generation_Information_VIC" (May 2019)
    */
    public void createBids(SimState state, Date currentTime, int currentMonth, ArrayList<Generator> activeGens){

        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        /**
         * Create a bid for each Generator that participates in the SPOT market
         */

        this.primarySpot.clearBidders();
        if(existsSecondary) {
            this.secondarySpot.clearBidders();
        }


        for (Generator g : activeGens) {

            //only consider Scheduled (S)
            g.updateHistoricCapacityFactor(state);
            double availableCapacity = g.getAvailableCapacity();
            if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") ){
                availableCapacity = g.getSolarSurplusCapacity();
            }

            double dollarMWh = g.priceMWhLCOE();
            //Don't add bids of 0 capacity
            if(availableCapacity != 0) {
                if (g.getInPrimaryMarket()) {
                    Bid b = new Bid(null, g, dollarMWh, availableCapacity);
                    this.primarySpot.addBidder(b);
                    g.setBidsInSpot(g.getBidsInSpot() + 1, availableCapacity);
                } else {
                    if (g.getInSecondaryMarket()) {
                        Bid b = new Bid(null, g, dollarMWh, availableCapacity);
                        this.secondarySpot.addBidder(b);
                        g.setBidsInSpot(g.getBidsInSpot() + 1, availableCapacity);
                    }
                }
            }
        }

    }

    private void applyInflation(SimState state, int currentYear) {
        Gr4spSim data = (Gr4spSim) state;
        double inflation = 1.0;

        // Use historic inflation data up to the base year.
        if( currentYear <= data.settings.getBaseYearConsumptionForecast())
            inflation += data.getAnnual_inflation().get(currentYear);
        else
            inflation += data.settingsAfterBaseYear.getAnnualInflation();

        //Update base price settings for future new generation
        data.settingsAfterBaseYear.applyInflationToAllGenBasePrice(inflation);
        data.settings.applyInflationToAllGenBasePrice(inflation);

        //Update existing generator prices
        for (Map.Entry<Integer, Vector<Generator>> entry : data.getGen_register().entrySet()) {
            Vector<Generator> gens = entry.getValue();
            for (Generator g : gens) {

                g.setBasePriceMWh(g.getBasePriceMWh() * inflation);
                g.setMarketPriceCap(g.getMarketPriceCap() * inflation);

            }
        }
    }

    @Override
    public void step(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        if (type.equalsIgnoreCase("Spot")) {

            //Do not run Spot maket until its start date
            if (data.getStartSpotMarketDate().after(today)) {
                return;
            }

            c.setTime(today);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentMonth = c.get(Calendar.MONTH) + 1;
            int currentYear = c.get(Calendar.YEAR);

            //Update markets setting after the baseyear
            if( currentYear == data.settings.getBaseYearConsumptionForecast() && currentMonth == 1){
                if(data.settingsAfterBaseYear.existsMarket("primary")) {
                    if (primarySpot == null)
                        primarySpot = new SpotMarket("Primary");
                }
                else
                    primarySpot = null;

                if(data.settingsAfterBaseYear.existsMarket("secondary")) {
                    if (secondarySpot == null)
                        secondarySpot = new SpotMarket("secondary");
                }
                else
                    secondarySpot = null;

                if(data.settingsAfterBaseYear.existsMarket("secondary"))
                    existsSecondary = true;
                else
                    existsSecondary = false;
            }

            /**
             * Apply inflation in January
             * */
            if (currentMonth == 1 ) {
                applyInflation(state, currentYear);
            }

            /**
             * Get all the half hour info of the month if available
             * */

            c.add(Calendar.MONTH, 1);
            Date nextMonth = c.getTime();

            c.setTime(today);

            avgMonthlyPriceOffSpot = 0;
            avgMonthlyPricePrimarySpot = 0;
            avgMonthlyPriceSecondarySpot = 0;
            avgMonthlyDemandOffSpot = 0;
            avgMonthlyDemandPrimarySpot = 0;
            avgMonthlyDemandSecondarySpot = 0;

            unmetDemandMwhPrimary = 0;
            unmetDemandMwhSecondary = 0;
            unmetDemandHours = 0;
            unmetDemandHoursSecondary = 0;
            unmetDemandDays = 0;
            unmetDemandDaysSecondary = 0;
            maxUnmetDemandMwhPerHourPrimary = 0;
            maxUnmetDemandMwhPerHourSecondary = 0;

            double unmetDemandMwhPrimaryDay = 0;
            double unmetDemandMwhSecondaryDay = 0;

            double unmetDemandMwhPrimaryHour = 0;
            double unmetDemandMwhSecondaryHour = 0;


            int num_half_hours = 1;
            this.rounds = num_half_hours;
            double totalDemand  = 0;

            //Reset Monthly generation data for each Generator
            for (Map.Entry<Integer, Vector<Generator>> entry : data.getGen_register().entrySet()) {
                Vector<Generator> gens = entry.getValue();
                for (Generator g : gens) {
                    g.setMonthlyGeneratedMWh(0.0);
                }
            }


            ArrayList<Generator> activeGens = new ArrayList<Generator>();

            /**
             * Go through each SPM and check if active generators today have more than 30MW
             */

            for (Map.Entry<Integer, Vector<Spm>> entry : data.getSpm_register().entrySet()) {
                Vector<Spm> spm_gens = entry.getValue();
                for (Spm s : spm_gens) {

                    //Get Active Gens Today
                    s.getActiveGensThisSPM(today, activeGens);

                }
            }

            /**
             * Compute Available Capacities for this month. Solar available capacity is recomputed every 30 min below
             * */


            for (Generator g : activeGens) {
                if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") == false) {
                    g.computeAvailableCapacity(state, today, today, currentMonth);
                }
            }

            double consumption = data.getMonthly_consumption_register().get(today);

            double monthDemand = 0.0;
            double monthDemandRemoved = 0.0;

            while(c.getTime().before(nextMonth)) {

                Date currentTime = c.getTime();

                if(data.getHalfhour_demand_register().containsKey(currentTime)) {
                    double newDemand = data.getHalfhour_demand_register().get(currentTime);
                    //Check if it is above 0. If it's 0, just take the previous one as it is likely to be due to misleading data
                    if( newDemand > 0.001 )
                        totalDemand = newDemand;
                    monthDemand += totalDemand;
                }
//                else{
//                    System.out.println("\t\t\t" + currentTime + " For some reason this key is lost, use last available value ");
//                }

                //double totalDemand = data.getMonthly_demand_register().get(today);

                //System.out.println(currentTime + " Demand: " + totalDemand);

                createBids(state, currentTime, currentMonth, activeGens);

                /**
                 * Compute spot price using current consumption - consumption met by non scheduled gen
                 */

                // Substract from Consumption the amount supplied by generators smaller than 30MW
                double availableCapacityOffMarket = 0;
                double priceOffMarket = 0;
                double consumptionSuppliedBySolar = 0;
                //(This for is to include the restrictions on which generators participate in the bidding process)
                for (Generator g : activeGens) {
                    double capacity = g.getAvailableCapacity();

                    //Add consumption supplied already by solar so it can be removed from the demand
                    //Recompute capacity for solar every 30 min
                    if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") ){
                        g.computeAvailableSolarCapacity(state, today, currentTime, currentMonth, consumption);
                        capacity = g.getAvailableCapacity();
                        consumptionSuppliedBySolar += capacity; //- g.getSolarSurplusCapacity();
                    }

                    if( g.getInPrimaryMarket() == false && g.getInSecondaryMarket()  == false ){

                        g.setMonthlyGeneratedMWh( g.getMonthlyGeneratedMWh() + (capacity / 2.0) ); //30min production

                        //If going through Off spot (OTC), then account only for surplus
                        if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") ){
                            priceOffMarket = (priceOffMarket*availableCapacityOffMarket) + (g.getSolarSurplusCapacity() * g.priceMWhLCOE());
                            availableCapacityOffMarket += g.getSolarSurplusCapacity();
                            if(availableCapacityOffMarket > 0.0)
                                priceOffMarket /= availableCapacityOffMarket;
                        }
                        else {
                            priceOffMarket = (priceOffMarket*availableCapacityOffMarket) + (capacity * g.priceMWhLCOE());
                            availableCapacityOffMarket += capacity;
                            if(availableCapacityOffMarket > 0.0)
                                priceOffMarket /= availableCapacityOffMarket;

                        }

                        //Historic Capacity Factor Off Spot non scheduled
                        g.setHistoricGeneratedMWh( g.getHistoricGeneratedMWh() + capacity );
                        g.setNumTimesUsedOffSpot( g.getNumTimesUsedOffSpot() + 1 );
                        g.setHistoricCapacityFactor( g.getHistoricGeneratedMWh() / (g.getMaxCapacity() * (g.getNumTimesUsedOffSpot())) );


                    }
                }


                //remove consumption met by off Market generation (see assumptions in Word Document)
                double totalDemandWholesale = totalDemand - availableCapacityOffMarket - consumptionSuppliedBySolar;

                monthDemandRemoved += (totalDemand - totalDemandWholesale);

                //If non scheduled covered more than the demand, set the demand of the wholesale to 0
                if(totalDemandWholesale < 0.0 )
                    totalDemandWholesale = 0.0;

                double totalDemandResidential = 0.0;
                if( currentYear < data.settings.getBaseYearConsumptionForecast() ) {
                    if (data.settings.existsMarket("secondary")) {
                        //Send residential demand (30% typically, see YAML) to secondary market, and remove it from primary market
                        totalDemandResidential = totalDemandWholesale * data.settings.getDomesticConsumptionPercentage();
                        totalDemandWholesale -= totalDemandResidential;


                        this.secondarySpot.computeMarketPrice(totalDemandResidential, data, currentTime, currentYear);

                        //Add unmet demand by secondary market into the demand of the primary market
                        if (this.secondarySpot.getUnmetDemand() > 0.0) {
                            totalDemandWholesale += this.secondarySpot.getUnmetDemand();
                            totalDemandResidential -= this.secondarySpot.getUnmetDemand();
                        }
                    }
                }
                else{
                    if(data.settingsAfterBaseYear.existsMarket("secondary")) {
                        //Send residential demand (30% typically, see YAML) to secondary market, and remove it from primary market
                        totalDemandResidential = totalDemandWholesale * data.settingsAfterBaseYear.getDomesticConsumptionPercentage();
                        totalDemandWholesale -= totalDemandResidential;


                        this.secondarySpot.computeMarketPrice(totalDemandResidential, data, currentTime, currentYear);

                        //Add unmet demand by secondary market into the demand of the primary market
                        if (this.secondarySpot.getUnmetDemand() > 0.0) {
                            totalDemandWholesale += this.secondarySpot.getUnmetDemand();
                            totalDemandResidential -= this.secondarySpot.getUnmetDemand();
                        }
                    }
                }

                this.primarySpot.computeMarketPrice(totalDemandWholesale, data, currentTime, currentYear);



                //Generator lastGenBid = (Generator) this.getSpot().getSuccessfulBids().get(this.getSpot().getSuccessfulBids().size()-1).asset;
                //System.out.println("Price - " + currentTime + ": " + this.spot.getMarketPrice() + " - Last Bid in "+ lastGenBid.getFuelSourceDescriptor() + "Gen Name: " +lastGenBid.getName()
                //        + "Gen Id: " +lastGenBid.getId()  +" Historic Capacity Factor: "+ lastGenBid.getHistoricCapacityFactor() );

                /**
                 * Update spot Average Monthly Price and Demand
                 */


                if(totalDemandWholesale + totalDemandResidential > 0.0001) {
                   avgMonthlyDemandPrimarySpot = (avgMonthlyDemandPrimarySpot * num_half_hours) + totalDemandWholesale;
                   avgMonthlyPricePrimarySpot = (avgMonthlyPricePrimarySpot * num_half_hours) + this.primarySpot.getMarketPrice() ;
                    if( currentYear < data.settings.getBaseYearConsumptionForecast() ) {
                        if (data.settings.existsMarket("secondary")) {
                            avgMonthlyPriceSecondarySpot = (avgMonthlyPriceSecondarySpot * num_half_hours) + this.secondarySpot.getMarketPrice();
                            avgMonthlyDemandSecondarySpot = (avgMonthlyDemandSecondarySpot * num_half_hours) + totalDemandResidential;
                        }
                    }else{
                        if(data.settingsAfterBaseYear.existsMarket("secondary")) {
                            avgMonthlyPriceSecondarySpot = (avgMonthlyPriceSecondarySpot * num_half_hours) + this.secondarySpot.getMarketPrice();
                            avgMonthlyDemandSecondarySpot = (avgMonthlyDemandSecondarySpot * num_half_hours) + totalDemandResidential;
                        }
                    }

                }else{
                    data.LOGGER.warning(currentTime + " - " +"demand in Spots was 0!! all covered off market");
                }

                if(availableCapacityOffMarket > 0.0001){
                   avgMonthlyDemandOffSpot = (avgMonthlyDemandOffSpot * num_half_hours ) + availableCapacityOffMarket;
                   avgMonthlyPriceOffSpot = (avgMonthlyPriceOffSpot * num_half_hours ) + priceOffMarket;
                }

                //Update the counter on how many steps the spot has been running
                rounds++;
                num_half_hours++;

                if(totalDemandWholesale + totalDemandResidential > 0.0001) {
                    avgMonthlyPricePrimarySpot /= num_half_hours;
                    avgMonthlyPriceSecondarySpot /= num_half_hours;

                    avgMonthlyDemandPrimarySpot /= num_half_hours;
                    avgMonthlyDemandSecondarySpot /= num_half_hours;
                }
                if(availableCapacityOffMarket > 0.0001) {
                    avgMonthlyPriceOffSpot /= num_half_hours;
                    avgMonthlyDemandOffSpot /= num_half_hours;
                }

                // Statistics about unmet demand
                double unmetPrimary = this.primarySpot.getUnmetDemand() / 2.0; //30min;

                double unmetSecondary = 0.0;
                if( currentYear < data.settings.getBaseYearConsumptionForecast() ) {
                    if (data.settings.existsMarket("secondary"))
                        unmetSecondary = this.secondarySpot.getUnmetDemand() / 2.0; //30min;
                }else{
                    if(data.settingsAfterBaseYear.existsMarket("secondary"))
                        unmetSecondary = this.secondarySpot.getUnmetDemand() / 2.0; //30min;
                }

                //Update Unmet demand month
                unmetDemandMwhPrimary += unmetPrimary;
                unmetDemandMwhSecondary += unmetSecondary;
                
                //Unmet demand last 24h
                unmetDemandMwhPrimaryDay += unmetPrimary;
                unmetDemandMwhSecondaryDay += unmetSecondary;

                //Unmet demand last 1h
                unmetDemandMwhPrimaryHour += unmetPrimary;
                unmetDemandMwhSecondaryHour += unmetSecondary;

                //New day
                if(c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0){
                    
                    //Update numdays with unmet demand
                    if( unmetDemandMwhPrimaryDay > 0.0001 ) unmetDemandDays++;
                    if( unmetDemandMwhSecondaryDay > 0.0001 ) unmetDemandDaysSecondary++;

                    unmetDemandMwhPrimaryDay = 0;
                    unmetDemandMwhSecondaryDay = 0;
                    
                }

                //New Hour
                if(c.get(Calendar.MINUTE) == 0){

                    //Update numhours unmet demand
                    if( unmetDemandMwhPrimaryHour > 0.0001 )
                        unmetDemandHours++;
                    if( unmetDemandMwhSecondaryHour > 0.0001 ) unmetDemandHoursSecondary++;

                    //Update max unmet demand per hour
                    if( maxUnmetDemandMwhPerHourPrimary < unmetDemandMwhPrimaryHour ) maxUnmetDemandMwhPerHourPrimary = unmetDemandMwhPrimaryHour;
                    if( maxUnmetDemandMwhPerHourSecondary < unmetDemandMwhSecondaryHour ) maxUnmetDemandMwhPerHourSecondary = unmetDemandMwhSecondaryHour;

                    unmetDemandMwhPrimaryHour = 0;
                    unmetDemandMwhSecondaryHour = 0;
                }

                //Add 30 min to get next demand
                c.add(Calendar.MINUTE, 30);


//                //----------DEBUG AND REMOVE--------------
//                // creating a Calendar object
//                Date date = new Date(124, 8, 0);
//
//                if(currentTime.after(date) )
//                    System.out.println("Foo");
//                //-------------------------------------------------

            }
//            //CODE TO DEBUG and CHECK that GENERATORS PRODUCTION EQUALS THE TOTAL DEMAND after all 30min BIDDING
//            monthDemand = monthDemand / 2000.0;
//            monthDemandRemoved = monthDemandRemoved / 2000.0;
//
//            double totalMonthGenerators = 0.0;
//            double totalMonthGeneratorsOFF = 0.0;
//            for (Integer integer : data.getGen_register().keySet()) {
//                Vector<Generator> gens = data.getGen_register().get(integer);
//                for (int i = 0; i < gens.size(); i++) {
//                    if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) )
//                        totalMonthGenerators += gens.get(i).getMonthlyGeneratedMWh();
//                    else
//                        totalMonthGeneratorsOFF += gens.get(i).getMonthlyGeneratedMWh();
//                }
//            }
//            totalMonthGenerators = totalMonthGenerators / 1000.0;
//            totalMonthGeneratorsOFF = totalMonthGeneratorsOFF / 1000.0;

            primarySpot.setMarketPrice(avgMonthlyPricePrimarySpot);
            primarySpot.setDemand(avgMonthlyDemandPrimarySpot); // MWh
            if( currentYear < data.settings.getBaseYearConsumptionForecast() ) {
                if (data.settings.existsMarket("secondary")) {

                    secondarySpot.setMarketPrice(avgMonthlyPriceSecondarySpot);
                    secondarySpot.setDemand(avgMonthlyDemandSecondarySpot);
                }
            }else{
                if(data.settingsAfterBaseYear.existsMarket("secondary")) {

                    secondarySpot.setMarketPrice(avgMonthlyPriceSecondarySpot);
                    secondarySpot.setDemand(avgMonthlyDemandSecondarySpot);
                }
            }

        }

    }


}
