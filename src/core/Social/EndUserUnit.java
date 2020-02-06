package core.Social;

import core.Relationships.ActorAssetRelationship;
import core.Relationships.Arena;
import core.Relationships.Contract;
import core.Gr4spSim;
import core.Technical.Spm;
import org.jfree.data.xy.XYDataItem;
import sim.engine.SimState;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EndUserUnit extends Actor implements EndUserActor, java.io.Serializable  {
    private int numberOfPerson;
    private int numberOfHouseholds;
    private HashMap<Date, Integer > newHousholdsPerDate;
    private boolean family;
    private boolean hasGas;
    private double income; //median weekly income according to ABS census 2016
    public double currentConsumption;
    public double surplusMWh;
    public double currentEmissions;
    public float currentTariff;
    public Contract currentTariffContract;
    public Date creationDate;


    public EndUserUnit(int actor, ActorType actorType, String name, GovRole mainRole, BusinessStructure businessType,
                       OwnershipModel ownershipModel, int numberOfPerson, int numberOfHouseholds, boolean gas,
                       boolean family, double income, Date CreationDate ) {
        super(actor, actorType, name, mainRole, businessType, ownershipModel);
        this.numberOfPerson = numberOfPerson;
        this.family = family;
        this.income = income;
        this.hasGas = gas;
        this.currentConsumption = 0.0;
        this.currentEmissions = 0.0;
        this.surplusMWh = 0.0;
        this.numberOfHouseholds = numberOfHouseholds;
        this.creationDate = CreationDate;
        this.newHousholdsPerDate = new HashMap<>();
        this.newHousholdsPerDate.put(creationDate,numberOfHouseholds);
    }

    public float getCurrentTariff() {
        return currentTariff;
    }

    public double getCurrentConsumption() {
        return currentConsumption;
    }

    public double getCurrentEmissions() {
        return currentEmissions;
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

    public int getNumberOfHouseholds() {
        return numberOfHouseholds;
    }

    public void setNumberOfHouseholds(int numberOfHouseholds) {
        this.numberOfHouseholds = numberOfHouseholds;
    }

    public void setNumberOfNewHouseholds(Date d, int numberOfNewHouseholds) { this.newHousholdsPerDate.put(d,numberOfNewHouseholds);}

    public boolean isHasGas() {
        return hasGas;
    }

    public void setHasGas(boolean hasGas) {
        this.hasGas = hasGas;
    }

    private void computeTariff(SimState simState){

        Gr4spSim data = (Gr4spSim) simState;

        int currentMonth = (int)simState.schedule.getSteps()%12;

        Date today = data.getCurrentSimDate();
        Arena spotArena = null;
        Arena retail = null;

        //Finds the Retail and Spot Arena, and asks for his currentTariff, following the Simulation policy defined in Gr4spSim.Start()
        for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
            Arena a = entry.getValue();

            if (a.getType().equalsIgnoreCase("Spot")) {
                spotArena = a;
            }
            if (a.getType().equalsIgnoreCase("Retail")) {
                retail = a;
            }
        }

        //If Spot Market hasn't started yet, get historic prices
        if (data.getStartSpotMarketDate().after(today)) {
            //Historic tariff is given in c/KWh
            this.currentTariff = (float) retail.getTariff(simState);

        }
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(data.getStartSpotMarketDate());
            c.add(Calendar.MONTH, 5);
            Date spotStartSixMonthAfter = c.getTime();

            //Lets run the first 6 months of the spot market to be able to get an average of wholesale price
            //Get tariff from historic for the first 6 months of starting the spot
            if(spotStartSixMonthAfter.after(today)){
                //Historic tariff is given in c/KWh
                this.currentTariff = retail.getEndConsumerTariff(simState);
            }
            //Update tariff every 6 months
            else if( (currentMonth + 1) % 6 == 0 ){

                //Find out wholesale price contribution percentage to the tariff (other values given in yaml file)
                float wholesalePriceComponent = (float)1.0 - (float)(data.settings.getUsageTariff("transmissionCosts")
                        + data.settings.getUsageTariff("distributionCosts")
                        + data.settings.getUsageTariff("retailCosts")
                        + data.settings.getUsageTariff("environmentalCosts"));

                //Wholesale component cannot fall below 0.01
                if(wholesalePriceComponent < 0.01) wholesalePriceComponent = (float)0.01;

                //Get CPI conversion
                c.setTime(today);
                c.set(Calendar.MONTH, 0);
                Date cpiDate = c.getTime();
                float conversion_rate = data.getCpi_conversion().get(cpiDate);

                //Compute Average Wholesale Price using the last 6 months values saved in SaveData.java
                //Current month price hasn't been stored yet, so we initialize it with the price for the current month
                // Convert $/MWh -> c/KWh
                double avgWholesalePrice = (float) (spotArena.getTariff(simState) * conversion_rate) / (float) 10.0;
                int nmonths = 1;
                for (nmonths = 1 ; nmonths < 6 ; nmonths++) {

                    //Get Wholesale price from the past nmonth(s)
                    int idx = data.saveData.PriceGenAvgSeries.get(1).getItems().size() - nmonths;
                    XYDataItem item = (XYDataItem) data.saveData.PriceGenAvgSeries.get(1).getItems().get(idx);
                    double wholesale = item.getYValue();

                    // Convert $/MWh -> c/KWh
                    float wholesalePrice = (float) (wholesale * conversion_rate) / (float) 10.0;
                    avgWholesalePrice += wholesalePrice / wholesalePriceComponent;
                }
                avgWholesalePrice /= nmonths;

                this.currentTariff = (float) avgWholesalePrice;
            }
            else{
                //Use the tariff from this semester
                int idx = data.saveData.tariffUsageConsumptionActorSeries.get(0).getItems().size() - 1;
                if(idx > 0){
                    //Make sure there's data for the semester. If starting after spotStartDate, there may not be any data for the first 5 months
                    XYDataItem item = (XYDataItem) data.saveData.tariffUsageConsumptionActorSeries.get(0).getItems().get(idx);
                    double tariffSemester = item.getYValue();
                    this.currentTariff = (float) tariffSemester;
                }else{
                    this.currentTariff = (float) 0.0;
                }

            }



        }


    }

    @Override
    public void step(SimState simState) {

        //System.out.println(simState.schedule.getTime()+"STEP!!!!"+currentConsumption);

        Gr4spSim data = (Gr4spSim) simState;

        int currentMonth = (int)simState.schedule.getSteps()%12;

        Date today = data.getCurrentSimDate();

        //MWH
        this.currentConsumption = (data.getMonthly_consumption_register().get(today));
        this.currentConsumption = this.currentConsumption * numberOfHouseholds;

        //Find SPM used by Consumer
        for(ActorAssetRelationship actorSpmRel : data.getActorAssetRelationships()){
            if(actorSpmRel.getActor().getId() == this.getId() && actorSpmRel.getAsset() instanceof Spm){
                Spm spm = (Spm) actorSpmRel.getAsset();

                double consumption = this.currentConsumption;

                //Reduce from consumption the onsite generation in order to compute Emissions
                double onSiteGenerationTotalHouseholds = spm.getOnsiteGeneration(data, today, this.newHousholdsPerDate);

                //Save the surplus energy
                this.surplusMWh = onSiteGenerationTotalHouseholds - consumption;

                //remove from consumption the MWh generated onsite
                consumption -= onSiteGenerationTotalHouseholds;

                //If onsite Generation is greater than consumption, set consumption to 0
                //Because we have not created a market to sell the surplus to other SPMs
                if(consumption < 0) {
                    consumption = 0;
                }

                 spm.computeIndicators(simState,consumption);
                this.currentEmissions = spm.currentEmissions;

//                //Compute emissions multiplying Emission factor t-CO2/MWh * consumption in MWh
//                Generation genData = data.getMonthly_generation_register().get(today);
//                this.currentEmissions = genData.computeGenEmissionIntensity(spm) * consumption;
//
//                //Apply network losses to the computation of GHG emissions
//                double networkLosses = spm.computeRecursiveNetworksLosses(spm);
//                this.currentEmissions *= (1+networkLosses);

                //System.out.println("Population for " + today + " of "+numberOfHouseholds+ " with consumption "+ this.currentConsumption+" create total of "+ this.currentEmissions +"GHG (t/co2)");
            }
        }

        computeTariff(simState);
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

