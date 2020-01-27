package core.Relationships;

import core.Gr4spSim;
import core.Technical.Generator;
import core.Technical.Spm;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.*;

public class Arena implements Steppable {
    private int id;
    private String name;
    private String type;
    private int rounds;

    //Constant value that represents End-Consumer contracts. 999 is the value assigned to contracts DB to end consumer
    public static final int EndConsumer = 999;

    //    private ArrayList<EligibilityRule> eligibilityRules;

    //    private float transactionFee; //percentage fee

    private MeritOrder primarySpot;
    private double avgMonthlyPricePrimarySpot;
    private double avgMonthlyPriceSecondarySpot;
    private double avgMonthlyPriceOffSpot;

    private double avgMonthlyDemandPrimarySpot;
    private double avgMonthlyDemandSecondarySpot;
    private double avgMonthlyDemandOffSpot;

    private MeritOrder secondarySpot; //Merit order type of market at the distribution level.
    private ArrayList<Contract> bilateral; //can be billing with retailers, PPAs and other types of OTCs with two known parties involved.
    private ArrayList<Contract> fiTs;

    //Each Arena has ONLY one of the following:
    //Merit order rules for wholesale market
    //PPA - Contracts Act-Act duration over the counter
    //FeedInTariff - Contracts Act-Act over the counterarenas

    private Random randomGenerator;


    public Arena(int id, String name, String type, Gr4spSim state) {
        Gr4spSim data = (Gr4spSim) state;

        this.id = id;
        this.name = name;
        this.type = type;
        if (type.equalsIgnoreCase("OTC") || type.equalsIgnoreCase("Retail"))
            bilateral = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("fiTs"))
            fiTs = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("Spot")) {
            if(data.settings.existsMarket("primary"))
                primarySpot = new MeritOrder("Primary");
            else
                primarySpot = null;
            if(data.settings.existsMarket("secondary"))
                secondarySpot = new MeritOrder("secondary");
            else
                secondarySpot = null;
        }
        randomGenerator = new Random();

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

    public MeritOrder getPrimarySpot() {
        return primarySpot;
    }

    public MeritOrder getSecondarySpot() {
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
    /**
     * This tariff represents price for $/MWh
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
     * This tariff represents price for $/MWh -> c/KWh (divide by 10)
     */
    public double getTariff(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        //If Spot Market hasn't started yet, get historic prices
        if (data.getStartSpotMarketDate().after(today)) {
            return getEndConsumerTariff(state);
        }

        //If spot is on, get the price market
        double price = 0;
        for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
            Arena a = entry.getValue();
            if (a.type.equalsIgnoreCase("Spot")) {

                //Weighted average between secondaryPrice and unmet demand supplied by the primary spot
                if(data.settings.existsMarket("secondary")) {
                    price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                            avgMonthlyPriceSecondarySpot * avgMonthlyDemandSecondarySpot +
                            avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot ) /
                            (avgMonthlyDemandPrimarySpot + avgMonthlyDemandSecondarySpot + avgMonthlyDemandOffSpot);

                }
                else
                    price = (avgMonthlyPricePrimarySpot * avgMonthlyDemandPrimarySpot +
                            avgMonthlyPriceOffSpot * avgMonthlyDemandOffSpot ) /
                            (avgMonthlyDemandPrimarySpot  + avgMonthlyDemandOffSpot);
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
    public void createBids(SimState state, Date currentTime){

        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        ArrayList<Generator> activeGensSPOT = new ArrayList<Generator>();
        ArrayList<Generator> activeGensOutSPOT = new ArrayList<Generator>();

        /**
         * Go through each SPM and check if active generators today have more than 30MW
         */

        for (Map.Entry<Integer, Vector<Spm>> entry : data.getSpm_register().entrySet()) {
            Vector<Spm> spm_gens = entry.getValue();
            for (Spm s : spm_gens) {

                //Get Active Gens Today
                s.getActiveGensThisSPM(today, activeGensSPOT, activeGensOutSPOT);

            }
        }

        /**
         * Create a bid for each Generator that participates in the SPOT market
         */

        this.primarySpot.clearBidders();
        if(data.settings.existsMarket("secondary")) {
            this.secondarySpot.clearBidders();
        }

        for (Generator g : activeGensSPOT) {

            //only consider Scheduled (S)
            g.updateHistoricCapacityFactor(state);
            double availableCapacity = g.computeAvailableCapacity(state, currentTime);
            if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") ){
                availableCapacity = g.getSolarSurplusCapacity();
            }

            double dollarMWh = g.priceMWhLCOE();

            if( data.settings.isMarketPaticipant( g.getDispatchTypeDescriptor(),"primary", g.getMaxCapacity() ) ) {
                Bid b = new Bid(null, g, dollarMWh, availableCapacity);
                this.primarySpot.addBidder(b);
                g.setBidsInSpot(g.getBidsInSpot() + 1);
            }else{
                if( data.settings.isMarketPaticipant( g.getDispatchTypeDescriptor(),"secondary", g.getMaxCapacity() ) ) {
                    Bid b = new Bid(null, g, dollarMWh, availableCapacity);
                    this.secondarySpot.addBidder(b);
                    g.setBidsInSpot(g.getBidsInSpot() + 1);
                }
            }
        }

    }

    private void applyInflation(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        for (Map.Entry<Integer, Vector<Generator>> entry : data.getGen_register().entrySet()) {
            Vector<Generator> gens = entry.getValue();
            for (Generator g : gens) {

                //Apply annual inflation
                g.setPriceMinMWh(g.getPriceMinMWh() * (1 + data.settings.getAnnualInflation()));
                g.setPriceMaxMWh(g.getPriceMaxMWh() * (1 + data.settings.getAnnualInflation()));

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


            Calendar c = Calendar.getInstance();
            c.setTime(today);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentMonth = c.get(Calendar.MONTH) + 1;
            int currentYear = c.get(Calendar.YEAR);

            if (currentMonth == 1 && currentYear > data.settings.getBaseYearConsumptionForecast()) {
                applyInflation(state);
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


            while(c.getTime().before(nextMonth)) {

                Date currentTime = c.getTime();

                if(data.getHalfhour_demand_register().containsKey(currentTime)) {
                    double newDemand = data.getHalfhour_demand_register().get(currentTime);
                    //Check if it is above 0. If it's 0, just take the previous one as it is likely to be due to misleading data
                    if( newDemand > 0.001 )
                        totalDemand = newDemand;
                }
//                else{
//                    System.out.println("\t\t\t" + currentTime + " For some reason this key is lost, use last available value ");
//                }

                //double totalDemand = data.getMonthly_demand_register().get(today);

                //System.out.println(currentTime + " Demand: " + totalDemand);

                createBids(state, currentTime);

                /**
                 * Compute spot price using current consumption - consumption met by non scheduled gen
                 */

                // Substract from Consumption the amount supplied by generators smaller than 30MW
                double availableCapacityOffMarket = 0;
                double priceOffMarket = 0;
                double consumptionSuppliedBySolar = 0;
                //(This for is to include the restrictions on which generators participate in the bidding process)
                for (Map.Entry<Integer, Vector<Generator>> entry : data.getGen_register().entrySet()) {
                    Vector<Generator> gens = entry.getValue();
                    for (Generator g : gens) {
                        //Has started today or earlier?
                        if (g.getStart().before(today) || g.getStart().equals(today)) {
                            //Has not finished operations?
                            if (g.getEnd().after(today)) {
                                double capacity = g.computeAvailableCapacity(state, currentTime);

                                //Add consumption supplied already by solar so it can be removed from the demand
                                if(g.getFuelSourceDescriptor().equalsIgnoreCase("Solar") ){
                                    consumptionSuppliedBySolar += capacity - g.getSolarSurplusCapacity();
                                }

                                if( data.settings.isMarketPaticipant( g.getDispatchTypeDescriptor(), "primary", g.getMaxCapacity() ) == false &&
                                        data.settings.isMarketPaticipant( g.getDispatchTypeDescriptor(), "secondary", g.getMaxCapacity() ) == false ){

                                    g.setMonthlyGeneratedMWh( g.getMonthlyGeneratedMWh() + capacity );

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
                                    g.setBidsOffSpot( g.getBidsOffSpot() + 1 );
                                    g.setHistoricCapacityFactor( g.getHistoricGeneratedMWh() / (g.getMaxCapacity() * (g.getBidsOffSpot())) );


                                }
                            }
                        }
                    }
                }

                //remove consumption met by non scheduled generation (see assumptions in Word Document)
                double totalDemandWholesale = totalDemand - availableCapacityOffMarket - consumptionSuppliedBySolar;

                //If non scheduled covered more than the demand, set the demand of the wholesale to 0
                if(totalDemandWholesale < 0.0 ) totalDemandWholesale = 0.0;

                double totalDemandResidential = 0.0;
                if(data.settings.existsMarket("secondary")) {
                    //Send residential demand (30% typically, see YAML) to secondary market, and remove it from primary market
                    totalDemandResidential = totalDemandWholesale * data.settings.getDomesticConsumptionPercentage();
                    totalDemandWholesale -= totalDemandResidential;


                    this.secondarySpot.computeMarketPrice(totalDemandResidential, data, currentTime);

                    //Add unmet demand by secondary market into the demand of the primary market
                    if (this.secondarySpot.getUnmetDemand() > 0.0) {
                        totalDemandWholesale += this.secondarySpot.getUnmetDemand();
                        totalDemandResidential -= this.secondarySpot.getUnmetDemand();
                    }
                }

                this.primarySpot.computeMarketPrice(totalDemandWholesale, data, currentTime);



                //Generator lastGenBid = (Generator) this.getSpot().getSuccessfulBids().get(this.getSpot().getSuccessfulBids().size()-1).asset;
                //System.out.println("Price - " + currentTime + ": " + this.spot.getMarketPrice() + " - Last Bid in "+ lastGenBid.getFuelSourceDescriptor() + "Gen Name: " +lastGenBid.getName()
                //        + "Gen Id: " +lastGenBid.getId()  +" Historic Capacity Factor: "+ lastGenBid.getHistoricCapacityFactor() );

                /**
                 * Compute spot Emissions Intensity using successful bidders
                 */

                primarySpot.computeGenEmissionIntensity(currentYear);


               if(totalDemandWholesale + totalDemandResidential > 0.0001) {
                   avgMonthlyDemandPrimarySpot = (avgMonthlyDemandPrimarySpot * num_half_hours) + totalDemandWholesale;
                   avgMonthlyDemandOffSpot = (avgMonthlyDemandOffSpot * num_half_hours ) + availableCapacityOffMarket;

                   avgMonthlyPricePrimarySpot = (avgMonthlyPricePrimarySpot * num_half_hours) + this.primarySpot.getMarketPrice() ;
                   avgMonthlyPriceOffSpot = (avgMonthlyPriceOffSpot * num_half_hours ) + priceOffMarket;
                   if(data.settings.existsMarket("secondary")) {
                       avgMonthlyPriceSecondarySpot = (avgMonthlyPriceSecondarySpot * num_half_hours) + this.secondarySpot.getMarketPrice();
                       avgMonthlyDemandSecondarySpot = (avgMonthlyDemandSecondarySpot * num_half_hours) + totalDemandResidential;
                   }
//                   if(data.settings.existsMarket("secondary")) {
//                       monthlyAveragePrice = (monthlyAveragePrice * num_half_hours) + (this.primarySpot.getMarketPrice() * totalDemandWholesale + this.secondarySpot.getMarketPrice() * totalDemandResidential) / (totalDemandWholesale + totalDemandResidential);
//                       monthlyAverageEmissions = (monthlyAverageEmissions * num_half_hours) + (this.primarySpot.getEmissionsIntensity() * totalDemandWholesale + this.secondarySpot.getEmissionsIntensity() * totalDemandResidential) / (totalDemandWholesale + totalDemandResidential);
//                   }
//                   else{
//                       monthlyAveragePrice = (monthlyAveragePrice * num_half_hours) + (this.primarySpot.getMarketPrice() * totalDemandWholesale) / (totalDemandWholesale);
//                       monthlyAverageEmissions = (monthlyAverageEmissions * num_half_hours) + (this.primarySpot.getEmissionsIntensity() * totalDemandWholesale ) / (totalDemandWholesale );
//                       }
                }else{
                    data.LOGGER.warning("demand in Spots was 0!! all covered off market");
                }

//                if (Double.isNaN(monthlyAveragePrice)) {
//                    System.out.println(monthlyAveragePrice);
//                }

                //Update the counter on how many steps the spot has been running
                rounds++;
                num_half_hours++;

                if(totalDemandWholesale + totalDemandResidential > 0.0001) {
                    avgMonthlyPricePrimarySpot /= num_half_hours;
                    avgMonthlyPriceSecondarySpot /= num_half_hours;
                    avgMonthlyPriceOffSpot /= num_half_hours;

                    avgMonthlyDemandPrimarySpot /= num_half_hours;
                    avgMonthlyDemandSecondarySpot /= num_half_hours;
                    avgMonthlyDemandOffSpot /= num_half_hours;
                }



                //Add 30 min to get next demand
                c.add(Calendar.MINUTE, 30);


//                //----------DEBUG AND REMOVE--------------
//                // creating a Calendar object
//                Date date = new Date(120, 6, 0);
//
//                if(currentTime.after(date) )
//                    System.out.println("Foo");
//                //-------------------------------------------------

            }


            //Currently not in use for reporting emissions, but it just captures the emissions of the succesful biders.
            primarySpot.setMarketPrice(avgMonthlyPricePrimarySpot);
            primarySpot.setDemand(avgMonthlyDemandPrimarySpot);
            if(data.settings.existsMarket("secondary")) {

                secondarySpot.setMarketPrice(avgMonthlyPriceSecondarySpot);
                secondarySpot.setDemand(avgMonthlyDemandSecondarySpot);
            }


        }

    }


}
