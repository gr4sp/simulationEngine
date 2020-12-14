package core.Social;

import core.Relationships.ActorAssetRelationship;
import core.Relationships.Arena;
import core.Gr4spSim;
import core.Technical.Spm;
import org.jfree.data.xy.XYDataItem;
import sim.engine.SimState;

import java.util.*;

public class EndUserUnit extends Actor implements EndUserActor, java.io.Serializable  {
    private int numberOfPerson;
    private int numberOfHouseholds;
    private HashMap<Date, Integer > newHousholdsPerDate;
    private boolean family;
    private boolean hasGas;
    private double income;
    public double currentConsumption;
    public double currentEmissions;
    public float currentTariff;
    public Date creationDate;


    public EndUserUnit(int actor, String name, int numberOfPerson, int numberOfHouseholds, boolean gas,
                       boolean family, double income, Date CreationDate ) {
        super(actor, name);
        this.numberOfPerson = numberOfPerson;
        this.family = family; //not used
        this.income = income; //not used
        this.hasGas = gas; //not used
        this.currentConsumption = 0.0;
        this.currentEmissions = 0.0;
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
            this.currentTariff = (float) retail.getWholesalePrice(simState);

        }
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(data.getStartSpotMarketDate());
            c.add(Calendar.MONTH, 11);
            Date spotStartTwelveMonthAfter = c.getTime();


            //Lets run the first 12 months of the spot market to be able to get an average of wholesale price
            //Get tariff from historic for the first 6 months of starting the spot
            if(spotStartTwelveMonthAfter.after(today)){
                //Historic tariff is given in c/KWh
                this.currentTariff = retail.getEndConsumerTariff(simState);
            }
            //Update tariff every 12 months
            else if( currentMonth == 0 ){
            //else if( currentMonth == 0 ){

                float wholesalePriceComponent = 0;

                c.setTime(today);
                c.set(Calendar.MONTH, 0);
                int year = c.get(Calendar.YEAR)-1;

                //If wholesale contribution is given by historic data (DB table historic_tariff_contribution)
                if( data.getTariff_contribution_wholesale_register().containsKey(year) ) {
                    wholesalePriceComponent = data.getTariff_contribution_wholesale_register().get(year);
                }
                else {
                    //Find out wholesale price contribution percentage to the tariff (other values given in yaml file)
                    if( today.after(data.getBaseYearForecastDate()))
                        wholesalePriceComponent = (float) data.settingsAfterBaseYear.getUsageTariff("wholesaleContribution");
                    else
                        wholesalePriceComponent = (float) data.settings.getUsageTariff("wholesaleContribution");
                }
                //Wholesale component cannot fall below 0.01 and above 1.0
                if(wholesalePriceComponent < 0.01) wholesalePriceComponent = (float)0.01;
                if(wholesalePriceComponent > 1.00) wholesalePriceComponent = (float)1.0;

                //Get CPI conversion
                Date cpiDate = c.getTime();
                float conversion_rate = data.getCpi_conversion().get(cpiDate);

                //Compute Average Wholesale Price using the last 12 months values saved in SaveData.java
                //Current month price hasn't been stored yet, so we initialize it with the price for the current month
                // Convert $/MWh -> c/KWh
                //double avgWholesalePrice = (float) (spotArena.getWholesalePrice(simState) * conversion_rate) / (float) 10.0;
                double avgWholesalePrice = (float) 0.0;
                int nmonths = 0;

                for (nmonths = 0 ; nmonths < 6 ; nmonths++) {

                    //Get Wholesale price from the past nmonth(s)
                    int idx = data.saveData.PriceGenAvgSeries.get(1).getItems().size() - nmonths - 1;
                    XYDataItem item = (XYDataItem) data.saveData.PriceGenAvgSeries.get(1).getItems().get(idx);
                    double wholesale = item.getYValue();

                    // Convert $/MWh -> c/KWh
                    float wholesalePrice = (float) (wholesale * conversion_rate) / (float) 10.0;

                    avgWholesalePrice += wholesalePrice ;
                }
                avgWholesalePrice /= nmonths;

                this.currentTariff = (float) avgWholesalePrice / wholesalePriceComponent;


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


                //If onsite Generation is greater than consumption, set consumption to 0
                //Because we have not created a market to sell the surplus to other SPMs
                if(consumption < 0) {
                    consumption = 0;
                }

                 spm.computeIndicators(simState,consumption);
                this.currentEmissions = spm.currentEmissions;

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

