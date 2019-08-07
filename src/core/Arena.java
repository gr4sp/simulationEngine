package core;

import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

public class Arena implements Steppable {
    private int id;
    private String name;
    private String type;

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
        if(type.equalsIgnoreCase("OTC") || type.equalsIgnoreCase("Retail"))
            bilateral = new ArrayList<Contract>();
        if(type.equalsIgnoreCase("fiTs"))
            fiTs = new ArrayList<Contract>();
        if(type.equalsIgnoreCase("Spot"))
            spot = new MeritOrder();

        randomGenerator = new Random();
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
     * */
    public Contract getEndConsumerTariff(SimState state){
        float tariff = 0;
        Contract selectedContract = null;

        Gr4spSim data = (Gr4spSim) state;

        ArrayList<Float> availableTariffs = new ArrayList<Float>();
        ArrayList<Contract> availableTariffsContracts = new ArrayList<Contract>();

        // Find available tariffs for endConsumers based on current simulation date
        for ( Contract c : bilateral) {

            //EndConsumer contract?
            if( c.getBuyerId() == EndConsumer){

                Date today = data.getCurrentSimDate();

                //Is the contract active today? including starting date [start, end)
                if( ( today.after( c.getStart() ) || today.equals(c.getStart())) && today.before( c.getEnd() ) ){
                    availableTariffs.add( c.getPricecKWh() );
                    availableTariffsContracts.add(c);
                }

            }
        }

        //Select Tariff based on current Policy
        switch ( data.getPolicies().getEndConsumerTariff()  ){
            case MAX:
                tariff = Collections.max( availableTariffs );
                for ( Contract c : availableTariffsContracts) {
                    if (c.getPricecKWh() == tariff ) {
                        selectedContract = c;
                        break;
                    }
                }
                break;
            case MIN:
                tariff = Collections.min( availableTariffs );
                for ( Contract c : availableTariffsContracts) {
                    if (c.getPricecKWh() == tariff ) {
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

    @Override
    public void step(SimState state) {
        Gr4spSim data = (Gr4spSim) state;




    }
}
