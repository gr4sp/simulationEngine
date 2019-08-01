package core;

import sim.engine.SimState;

import java.util.Map;

public class Household extends Actor implements ConsumptionActor {
    private int numberOfPerson;
    private boolean family;
    private boolean hasGas;
    private double income; //median weekly income according to ABS census 2016
    public double currentConsumption;
    public float currentTariff;
    public Contract currentTariffContract;

    public Household(int actor, ActorType actorType, String name, GovRole mainRole, BusinessStructure businessType, OwnershipModel ownershipModel, int numberOfPerson, boolean gas,
                     boolean family, double income) {
        super(actor, actorType, name, mainRole, businessType, ownershipModel);
        this.numberOfPerson = numberOfPerson;
        this.family = family;
        this.income = income;
        this.hasGas = gas;
        this.currentConsumption = 0.0;
    }

    public float getCurrentTariff() {
        return currentTariff;
    }

    public double getCurrentConsumption() {
        return currentConsumption;
    }

    public void setCurrentConsumption(double currentConsumption) {
        this.currentConsumption = currentConsumption;
    }

    public boolean isFamily() {
        return family;
    }

    public void setFamily(boolean family) {
        this.family = family;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public int getNumberOfPerson() {
        return numberOfPerson;
    }

    public void setNumberOfPerson(int numberOfPerson) {
        this.numberOfPerson = numberOfPerson;
    }

    public boolean isHasGas() {
        return hasGas;
    }

    public void setHasGas(boolean hasGas) {
        this.hasGas = hasGas;
    }

    @Override
    public void step(SimState simState) {

        //System.out.println(simState.schedule.getTime()+"STEP!!!!"+currentConsumption);

        Gr4spSim data = (Gr4spSim) simState;

        int currentMonth = (int)simState.schedule.getSteps()%12;

        this.currentConsumption = data.monthly_consumption_register.get(data.getCurrentSimDate());
        //this.currentConsumption = computeConsumption( currentMonth );

        //Finds the Retail Arena, and asks for his currentTariff, following the Simulation policy defined in Gr4spSim.Start()
        for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
            Arena a = entry.getValue();

            if(a.getType().equalsIgnoreCase("Retail") ) {
                this.currentTariffContract = a.getEndConsumerTariff(simState);
                this.currentTariff = this.currentTariffContract.getPricecKWh();
                //Need to take into account service Fee when computing costs
            }
        }
    }

    @Override
    public double computeConsumption(int month) {

        double[][] noGas = new double[][]{
                {505.6666667, 505.6666667, 505.6666667, 448.6666667, 448.6666667, 448.6666667, 892.3333333, 892.3333333, 892.3333333, 584.6666667, 584.6666667, 584.6666667},
                {618.3333333, 618.3333333, 618.3333333, 606.3333333, 606.3333333, 606.3333333, 962.3333333, 962.3333333, 962.3333333, 679.3333333, 679.3333333, 679.3333333},
                {773.3333333, 773.3333333, 773.3333333, 773.6666667, 773.6666667, 773.6666667, 1117, 1117, 1117, 800, 800, 800},
                {866.3333333, 866.3333333, 866.3333333, 860.6666667, 860.6666667, 860.6666667, 1309.333333, 1309.333333, 1309.333333, 966, 966, 966},
                {866.3333333, 866.3333333, 866.3333333, 860.6666667, 860.6666667, 860.6666667, 1309.333333, 1309.333333, 1309.333333, 966, 966, 966}
        };

        double[][] gas = new double[][]{
                {296.3333333, 296.3333333, 296.3333333, 339.3333333, 339.3333333, 339.3333333, 492.3333333, 492.3333333, 492.3333333, 309.6666667, 309.6666667, 309.6666667},
                {409.3333333, 409.3333333, 409.3333333, 497.3333333, 497.3333333, 497.3333333, 562.6666667, 562.6666667, 562.6666667, 404.3333333, 404.3333333, 404.3333333},
                {564.3333333, 564.3333333, 564.3333333, 664.6666667, 664.6666667, 664.6666667, 717, 717, 717, 525, 525, 525},
                {657.3333333, 657.3333333, 657.3333333, 751.3333333, 751.3333333, 751.3333333, 909.3333333, 909.3333333, 909.3333333, 691, 691, 691},
                {657.3333333, 657.3333333, 657.3333333, 751.3333333, 751.3333333, 751.3333333, 909.3333333, 909.3333333, 909.3333333, 691, 691, 691}
        };
        int row = this.numberOfPerson - 1;

        //if more than 5 people, just take the last row
        if (row > 4) row = 4;


        if( this.hasGas ){
            return gas[row][month];
        }
        else{
            return noGas[row][month];
        }

        /*Double consumptionTotal;
        //ArrayList <Double> onSiteGeneration; //this value should be taken from onsite generation of household as weekly average per year
        if (consumptionOnly == false) {
            for (int i=0; i<consumptionTotal.size(); i++){
            consumptionTotal = onSiteGeneration.get(i) - consumptionProfile.getConsumption().get(i);
            return consumptionTotal;
        } else {
            return consumptionProfile.getConsumption();
        }*/
    }
}

