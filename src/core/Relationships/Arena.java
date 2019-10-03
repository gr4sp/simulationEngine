package core.Relationships;

import core.Gr4spSim;
import core.Social.ConsumptionActor;
import core.Technical.Generator;
import core.Technical.Spm;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.time.YearMonth;
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

    private MeritOrder spot;
    private ArrayList<Contract> bilateral; //can be billing with retailers, PPAs and other types of OTCs with two known parties involved.
    private ArrayList<Contract> fiTs;

    //Each Arena has ONLY one of the following:
    //Merit order rules for wholesale market
    //PPA - Contracts Act-Act duration over the counter
    //FeedInTariff - Contracts Act-Act over the counter

    private Random randomGenerator;


    public Arena(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        if (type.equalsIgnoreCase("OTC") || type.equalsIgnoreCase("Retail"))
            bilateral = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("fiTs"))
            fiTs = new ArrayList<Contract>();
        if (type.equalsIgnoreCase("Spot"))
            spot = new MeritOrder();

        randomGenerator = new Random();

        rounds = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public MeritOrder getSpot() {
        return spot;
    }

    public ArrayList<Contract> getBilateral() {
        return bilateral;
    }

    public ArrayList<Contract> getFiTs() {
        return fiTs;
    }


    /**
     * This tariff represents price for $/MWh
     */
    public Contract getEndConsumerTariff(SimState state) {
        float tariff = 0;
        Contract selectedContract = null;

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
                for (Contract c : availableTariffsContracts) {
                    if (c.getPricecKWh() == tariff) {
                        selectedContract = c;
                        break;
                    }
                }
                break;
            case MIN:
                tariff = Collections.min(availableTariffs);
                for (Contract c : availableTariffsContracts) {
                    if (c.getPricecKWh() == tariff) {
                        selectedContract = c;
                        break;
                    }
                }
                break;
            case RND:
                int index = randomGenerator.nextInt(availableTariffs.size());
                tariff = availableTariffs.get(index);
                selectedContract = availableTariffsContracts.get(index);
                break;
        }

        return selectedContract;
    }

    /**
     * This tariff represents price for $/MWh -> c/KWh (divide by 10)
     */
    public double getTariff(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();

        //If Spot Market hasn't started yet, get historic prices
        if (data.getStartSpotMarketDate().after(today)) {
            return getEndConsumerTariff(state).getPricecKWh();
        }

        //If spot is on, get the price market
        double spotPrice = 0;
        for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
            Arena a = entry.getValue();
            if (a.type.equalsIgnoreCase("Spot")) {
                spotPrice = a.spot.getMarketPrice();
            }
        }
        // TODO: 16/09/2019 check percentage of contribution on tariffs from regulated networks (transmission and distribution),
        //  competitive markets (spot and retail), environmental policies and fix charges.
        double networkChargesFix = 0;
        double transmissionCharges = 0.12;
        double distributionCharges = 0.76;
        double retailerExtra = 0;
        double environmental_policies = 0.12;

        double spotPriceCentsKWh = spotPrice; /// 10.0;
        return spotPriceCentsKWh;

//        double tariff = spotPriceCentsKWh + (transmissionCharges * spotPriceCentsKWh) + (distributionCharges * spotPriceCentsKWh) +
//                (environmental_policies * spotPriceCentsKWh) + retailerExtra + networkChargesFix;
//        return tariff;
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

            Calendar c = Calendar.getInstance();
            c.setTime(today);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentMonth = c.get(Calendar.MONTH) + 1;
            int currentYear = c.get(Calendar.YEAR);


            /**
             * Create a bid for each Generator that participates in the SPOT market
             * Capacity factors of each generator vary depending on the season.  Data on this capacity factors is obtained from
             * the "Generation_Information_VIC" (May 2019)
             */

            this.spot.clearBidders();
            for (Generator g : activeGensSPOT) {

                //only consider Scheduled (S)
                if( g.getDispatchTypeDescriptor().equals("S") == false ) continue;

                g.updateHistoricCapacityFactor(state);
                double availableCapacity = g.computeMonthlyAvailableCapacity(state);
                double dollarMWh = g.priceMWhLCOE();

                Bid b = new Bid(null, g, dollarMWh, availableCapacity);
                this.spot.addBidder(b);
            }

            /**
             * Compute spot price using current consumption - consumption met by non scheduled gen
             */


            double totalDemand = data.getMonthly_demand_register().get(today);

            // Substract from Consumption the amount supplied by generators smaller than 30MW
            double availableCapacityNonScheduled = 0;
            for (Map.Entry<Integer, Vector<Generator>> entry : data.getGen_register().entrySet()) {
                Vector<Generator> gens = entry.getValue();
                for (Generator g : gens) {
                    //Has started today or earlier?
                    if (g.getStart().before(today) || g.getStart().equals(today)) {
                        //Has not finished operations?
                        if (g.getEnd().after(today)) {
                            //Check nominal capacity is smaller than 30 MW in order to know participation in spot Market
                            if (g.getMaxCapacity() < 30) {
                                availableCapacityNonScheduled += g.computeMonthlyAvailableCapacity(state);
                            }
                        }
                    }
                }
            }

            //remove consumption met by non scheduled generation (see assumptions in Word Document)
            totalDemand -= availableCapacityNonScheduled;

            this.spot.computeMarketPrice(totalDemand);

            Generator lastGenBid = (Generator) this.getSpot().getSuccessfulBids().get(this.getSpot().getSuccessfulBids().size()-1).asset;
            System.out.println("Price - " + today + ": " + this.spot.getMarketPrice() + " - Last Bid in "+ lastGenBid.getFuelSourceDescriptor() + "Gen Name: " +lastGenBid.getName() + "Gen Id: " +lastGenBid.getId()  +" Historic Capacity Factor: "+ lastGenBid.getHistoricCapacityFactor() );

            /**
             * Compute spot Emissions Intensity using successful bidders
             */

            spot.computeGenEmissionIntensity(currentYear);

            //Update the counter on how many steps the spot has been running
            rounds++;

        }

    }


}
