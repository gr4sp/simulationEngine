package core;

import com.esotericsoftware.yamlbeans.YamlReader;
import core.Policies.SimPolicies;
import core.Relationships.*;
import core.Social.*;
import core.Technical.*;
import core.settings.Settings;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;

import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static core.Social.GovRole.*;
import static java.lang.System.exit;


public class Gr4spSim extends SimState {
    private static final long serialVersionUID = 1;


    public Continuous2D layout;
    HashMap<Integer, Vector<Spm>> spm_register;
    HashMap<Integer, Vector<Generator>> gen_register; // If a new Generator is added, make its id to be numGenerators+1
    HashMap<Integer, Vector<NetworkAssets>> network_register;
    HashMap<Integer, Actor> actor_register;
    HashMap<Integer, Arena> arena_register;

    //Demand
    HashMap<Date, Double> monthly_demand_register;
    HashMap<Date, Double> halfhour_demand_register;

    //forecast ISP maximum and minimum demand (MW) from 2019 to 2050; various scenarios
    HashMap<Integer, Float> maximum_demand_forecast_register;
    HashMap<Integer, Float> minimum_demand_forecast_register;

    //Forecast ISP consumption from 2019 to 2050 in GWh for various scenarios and regions
    HashMap<Integer, Float> annual_forecast_consumption_register;

    //Consumption per Domestic Costumer
    HashMap<Date, Double> monthly_consumption_register;

    //Total monthly Consumption
    HashMap<Date, Double> total_monthly_consumption_register;


    //Total Generation GWh per month
    HashMap<Date, Generation> monthly_generation_register;

    //Number of costumers
    HashMap<Date, Integer> monthly_domestic_consumers_register;
    //CPI conversion
    HashMap<Date, Float> cpi_conversion;

    //Solar Exposure in KWh/m^2 (MJ/m^2 in DB) monthly mean
    HashMap<Date, Float> monthly_solar_exposure;

    //Solar Exposure in KWh/m^2 30min mean
    HashMap<Date, Float> halfhour_solar_exposure;

    // Solar installation in kw per month in Australia
    HashMap<Date, Integer> solar_number_installs;
    HashMap<Date, Integer> solar_aggregated_kw;
    HashMap<Date, Float> solar_system_capacity_kw;

    //this is the array to be traversed to get the total consumption
    ArrayList<EndUserActor> consumptionActors = new ArrayList<EndUserActor>();

    //this is the array to be traverse to get the total conventional consumption
    ArrayList<EndUserActor> conventionalConsumptionActors = new ArrayList<EndUserActor>();

    //this is the array to be traverse to get the total non conventional consumption
    ArrayList<EndUserActor> nonConventionalConsumptionActors = new ArrayList<EndUserActor>();

    ArrayList<ActorActorRelationship> actorActorRelationships = new ArrayList<ActorActorRelationship>();
    ArrayList<ActorAssetRelationship> actorAssetRelationships = new ArrayList<ActorAssetRelationship>();

    //Counter for the unique id each time a storage unit is created and other storage related variables
    private int numGenerators;
    private int numStorage;
    //counter for energy grid
    private int numGrid;
    private int numcpoints;

    //counter organisation structures
    private int numOrg;

    //couter for actors
    private int numActors;

    //Date representing the start of the simulation, and the final date of the simulation
    private Calendar simCalendar;
    private Date startSimDate;
    private Date endSimDate;
    private Date startSpotMarketDate;

    //Area Code for Simulation [M1,M2,R1,R2]
    private String areaCode;

    //Poplation percentage over VIC population
    private double populationPercentageAreacCode;

    //Max num Dwellings in ConsumerUnit
    private int maxHouseholdsPerConsumerUnit;

    //Percentage of Domestic Consumption over Total Consumption. Used in selectConsumption()
    private double domesticConsumptionPercentage;

    //Policies used for the simulation
    private SimPolicies policies;

    //Class to manage all the data generated in the simulation
    public SaveData saveData;

    //Simulation Settings specified via YAML file
    public Settings settings;
    public String yamlFileName;


    public Gr4spSim(long seed) {

        super(seed);

        //Num generator, storage, grid to generate unique id
        numGenerators = 0; //for real example, it is going to be the number of generators supplying the area under study at the scale under study.
        numStorage = 0;
        numGrid = 0;
        numOrg = 0;
        numActors = 0;

        simCalendar = Calendar.getInstance();

        spm_register = new HashMap<>();
        gen_register = new HashMap<>();
        network_register = new HashMap<>();
        actor_register = new HashMap<>();
        arena_register = new HashMap<>();
        monthly_demand_register = new HashMap<>();
        halfhour_demand_register = new HashMap<>();
        monthly_consumption_register = new HashMap<>();
        total_monthly_consumption_register = new HashMap<>();
        monthly_generation_register = new HashMap<>();
        monthly_domestic_consumers_register = new HashMap<>();
        cpi_conversion = new HashMap<>();
        monthly_solar_exposure = new HashMap<>();
        halfhour_solar_exposure = new HashMap<>();
        solar_aggregated_kw = new HashMap<>();
        solar_number_installs = new HashMap<>();
        solar_system_capacity_kw = new HashMap<>();
        annual_forecast_consumption_register = new HashMap<>();
        maximum_demand_forecast_register = new HashMap<>();
        minimum_demand_forecast_register = new HashMap<>();

        layout = new Continuous2D(10.0, 600.0, 600.0);
        policies = new SimPolicies();

        simulParametres();

        saveData = new SaveData(this);

    }

    private void simulParametres() {

        try {

            yamlFileName = "BAUVIC";
            YamlReader reader = new YamlReader(new FileReader("core/settings/"+yamlFileName+".yaml"));
            settings = reader.read(Settings.class);
            System.out.println(settings);


            /**
             * Simulation Date Range
             */

            //Save start and end date in the simulator state, which is this classs Gr4spSim
            SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

            this.startSimDate = stringToDate.parse(settings.getStartDate());
            this.simCalendar.setTime(this.startSimDate);
            this.endSimDate = stringToDate.parse(settings.getEndDate());

            this.startSpotMarketDate = stringToDate.parse(settings.getStartDateSpotMarket());

            /**
             * Population and scale
             */

            //To use M1(only Melbourne cityareaCode), populationPercentageAreacCode = 0.009; assuming 0.9% of Vic population,
            //Include regional with all framework indicators, 100% Population data
            areaCode = settings.getAreaCode();
            populationPercentageAreacCode = settings.getPopulationPercentageAreacCode();
            //Adds all households as a single actor connected to a single SPM
            maxHouseholdsPerConsumerUnit = settings.getMaxHouseholdsPerConsumerUnit();
            //Percentage of Total Consumption Historic Data that goes into domestic use
            domesticConsumptionPercentage = settings.getDomesticConsumptionPercentage();

            //Max number of Dwellings represented in a single ConsumerUnit, attached to a SPM
            //It is a measure to control aggregation of dwellings per SPM
            //New ConsumerUnits are created according to monthly population growth in the simulation Data monthly_domestic_consumers_register
            //maxHouseholdsPerConsumerUnit = 1000;


            /**
             * Public Policies
             */

            // goes from 0.00 to 1.0, represents percentage of monthly uptake and uses a normal gaussian distribution to simulate the uptake
            // for example, 0.01 represents 1% per month, around 12% a year
            double uptakeRate = settings.getUptakeRate();
            policies.getAccelerateSolarPV().setMonthlyHousholdsPercentageConversion(uptakeRate);
            policies.setEndConsumerTariffs(settings.getEndConsumerTariff());

        } catch (ParseException | com.esotericsoftware.yamlbeans.YamlException | java.io.FileNotFoundException e) {
            System.out.println("Problems reading YAML file!" + e.toString());
            exit(0);
        }


    }

    /**
     * Getters and Setters
     */


    public Calendar getSimCalendar() {
        return simCalendar;
    }

    public Date getStartSpotMarketDate() {
        return startSpotMarketDate;
    }

    public Date getStartSimDate() {
        return startSimDate;
    }

    public Date getCurrentSimDate() {
        return simCalendar.getTime();
    }

    public void advanceCurrentSimDate() {
        simCalendar.add(Calendar.MONTH, 1);
    }

    public Date getEndSimDate() {
        return endSimDate;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setEndSimDate(Date endSimDate) {
        this.endSimDate = endSimDate;
    }

    public SimPolicies getPolicies() {
        return policies;
    }

    public void setPolicies(SimPolicies policies) {
        this.policies = policies;
    }

    public HashMap<Integer, Arena> getArena_register() {
        return arena_register;
    }

    public HashMap<Date, Float> getCpi_conversion() {
        return cpi_conversion;
    }

    public HashMap<Date, Double> getMonthly_demand_register() {
        return monthly_demand_register;
    }

    public HashMap<Date, Double> getHalfhour_demand_register() {
        return halfhour_demand_register;
    }

    public HashMap<Date, Double> getMonthly_consumption_register() {
        return monthly_consumption_register;
    }


    public HashMap<Date, Double> getTotal_monthly_consumption_register() {
        return total_monthly_consumption_register;
    }

    public HashMap<Date, Integer> getMonthly_domestic_consumers_register() {
        return monthly_domestic_consumers_register;
    }

    public double getDomesticConsumptionPercentage() {
        return domesticConsumptionPercentage;
    }

    public HashMap<Date, Generation> getMonthly_generation_register() {
        return monthly_generation_register;
    }

    public HashMap<Date, Float> getMonthly_solar_exposure() {
        return monthly_solar_exposure;
    }

    public HashMap<Date, Float> getHalfhour_solar_exposure() {
        return halfhour_solar_exposure;
    }

    public HashMap<Date, Integer> getSolar_aggregated_kw() {
        return solar_aggregated_kw;
    }

    public HashMap<Date, Integer> getSolar_number_installs() {
        return solar_number_installs;
    }

    public HashMap<Date, Float> getSolar_system_capacity_kw() {
        return solar_system_capacity_kw;
    }

    public HashMap<Integer, Actor> getActor_register() {
        return actor_register;
    }

    public int getNumActors() {
        return numActors;
    }

    public void setNumActors(int numActors) {
        this.numActors = numActors;
    }

    public HashMap<Integer, Vector<Generator>> getGen_register() {
        return gen_register;
    }

    public ArrayList<ActorAssetRelationship> getActorAssetRelationships() {
        return actorAssetRelationships;
    }

    public HashMap<Integer, Vector<NetworkAssets>> getNetwork_register() {
        return network_register;
    }

    public HashMap<Integer, Vector<Spm>> getSpm_register() {
        return spm_register;
    }

    public ArrayList<ActorActorRelationship> getActorActorRelationships() {
        return actorActorRelationships;
    }

    public int getNumGenerators() {
        return numGenerators;
    }

    public void setNumGenerators(int numGenerators) {
        this.numGenerators = numGenerators;
    }

    public int getNumStorage() {
        return numStorage;
    }

    public void setNumStorage(int numStorage) {
        this.numStorage = numStorage;
    }

    public int getNumGrid() {
        return numGrid;
    }

    public void setNumGrid(int numGrid) {
        this.numGrid = numGrid;
    }

    public int getNumcpoints() {
        return numcpoints;
    }

    public void setNumcpoints(int numcpoints) {
        this.numcpoints = numcpoints;
    }

    public int getNumOrg() {
        return numOrg;
    }

    public double getPopulationPercentageAreacCode() {
        return populationPercentageAreacCode;
    }

    public int getMaxHouseholdsPerConsumerUnit() {
        return maxHouseholdsPerConsumerUnit;
    }

    public ArrayList<EndUserActor> getConsumptionActors() {
        return consumptionActors;
    }

    public HashMap<Integer, Float> getAnnual_forecast_consumption_register() {
        return annual_forecast_consumption_register;
    }

    public void setAnnual_forecast_consumption_register(HashMap<Integer, Float> annual_forecast_consumption_register) {
        this.annual_forecast_consumption_register = annual_forecast_consumption_register;
    }

    public HashMap<Integer, Float> getMaximum_demand_forecast_register() {
        return maximum_demand_forecast_register;
    }

    public void setMaximum_demand_forecast_register(HashMap<Integer, Float> maximum_demand_forecast_register) {
        this.maximum_demand_forecast_register = maximum_demand_forecast_register;
    }

    public HashMap<Integer, Float> getMinimum_demand_forecast_register() {
        return minimum_demand_forecast_register;
    }

    public void setMinimum_demand_forecast_register(HashMap<Integer, Float> minimum_demand_forecast_register) {
        this.minimum_demand_forecast_register = minimum_demand_forecast_register;
    }

    public Arena getArenaByName(String arenaName) {
        for (Map.Entry<Integer, Arena> entry : this.getArena_register().entrySet()) {
            Arena a = entry.getValue();
            if (a.getType().equalsIgnoreCase(arenaName)) {
                return a;
            }
        }

        return null;
    }


    void addSPMLocation(Spm s, Double2D loc, double diameter) {
        layout.setObjectLocation(s, loc);
        Double2D shift = new Double2D(10, 10);


        for (Spm spmc : s.getSpms_contained()) {
            spmc.diameter = diameter - 10;
            addSPMLocation(spmc, loc, spmc.diameter);
            loc = loc.add(shift);

        }
    }


    void addNewActorActorRel(Actor act1, Actor act2, ActorActorRelationshipType type) {

        ActorActorRelationship newRel = new ActorActorRelationship(act1, act2, type);

        act1.addContract(newRel);
        act2.addContract(newRel);
    }

    void addNewActorAssetRel(Actor actor, Asset asset, ActorAssetRelationshipType type, double percentage) {

        ActorAssetRelationship newRel = new ActorAssetRelationship(actor, asset, type, percentage);

        actor.addAssetRelationship(newRel);
        asset.addAssetRelationship(newRel);
    }

    /**
     * Generate Conventional ConsumerUnit
     */

    public void generateHouseholds() {

        Date today = getCurrentSimDate();

        //Get Population today (this sim step)
        int householdsToday = monthly_domestic_consumers_register.get(today);

        //Get Last month date (last sim state)
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        int currentYear = c.get(Calendar.YEAR);

        c.add(Calendar.MONTH, -1);
        Date lastStepTime = c.getTime();


        //Get Population before (last sim step)
        int householdsBefore = 0;
        if (monthly_domestic_consumers_register.containsKey(lastStepTime))
            householdsBefore = monthly_domestic_consumers_register.get(lastStepTime);

        //Get growth. Need to create ConsumerUnits if possitive
        int householdsGrowth = householdsToday - householdsBefore;


        int numHouseholds = (int) (householdsGrowth * populationPercentageAreacCode);

        //If currentYear is after the forecast, then do not adapt the population percentage, as the forecast already has data for regions.
        if( settings.getBaseYearConsumptionForecast() >= currentYear)
            numHouseholds = householdsGrowth;

        //Get Last conventional ConsumerUnit, which may have space for more households
        EndUserUnit consumer = null;
        if (conventionalConsumptionActors.size() >= 1)
            consumer = (EndUserUnit) conventionalConsumptionActors.get(conventionalConsumptionActors.size() - 1);

        int householdsLeft = numHouseholds;

        if (consumer != null) {
            //If all new households fit in existing SPM, then just add them
            if ((maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds()) > numHouseholds) {
                consumer.setNumberOfHouseholds(consumer.getNumberOfHouseholds() + numHouseholds);
                consumer.setNumberOfNewHouseholds(today, numHouseholds);

                return;
            }
            //If not all households fit, increase SPM to full, and create a new SPM with remaining households
            else if ((maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds()) <= numHouseholds) {
                int decrease = maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds();
                numHouseholds -= decrease;
                householdsLeft = numHouseholds;

                consumer.setNumberOfNewHouseholds(today, decrease);
                consumer.setNumberOfHouseholds(maxHouseholdsPerConsumerUnit);
            }
        }

        for (int i = 0; i < numHouseholds; i = i + maxHouseholdsPerConsumerUnit) {

            boolean hasGas = false;

            System.out.println("Num Actors: " + numActors);

            int numPeople = 2;
            String name = "Household Conventional " + conventionalConsumptionActors.size();

            int householdsCreated = maxHouseholdsPerConsumerUnit;
            if (householdsLeft < maxHouseholdsPerConsumerUnit) householdsCreated = householdsLeft;


            //Create new Consumer Unit
            Actor actor = new EndUserUnit(numActors++, ActorType.HDCONSUMER, name,
                    stringToGovRole("RULEFOLLOWER"), BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, householdsCreated, hasGas, true, 0, today);

            consumptionActors.add((EndUserActor) actor);
            conventionalConsumptionActors.add((EndUserActor) actor);

            householdsLeft -= householdsCreated;

            //Schedule the actor in order to make decissions at every step
            this.schedule.scheduleRepeating(0.0, 1, actor);

            //Id of conventional SPM
            int idSpm = 1;

            //Create new SPM
            Spm spm = LoadData.createSpm(this, idSpm);

            /**
             * Actor Asset rel
             * */
            //create new relationship btw the household and SPM
            ActorAssetRelationship actorSpmRel = new ActorAssetRelationship(actor, spm, ActorAssetRelationshipType.USE, 100);

            //Store relationship with assets in the actor object
            actor.addAssetRelationship(actorSpmRel);
            //Store relationship in the global array of all actor asset rel
            actorAssetRelationships.add(actorSpmRel);

            /**
             * Actor Actor rel
             * */
 /*           //create new relationship and contract btw the household and Retailer
            ActorActorRelationship actorActHouseRetailRel = new ActorActorRelationship(actor, actor_register.get(4), ActorActorRelationshipType.BILLING);

            //Store relationship with actor in the actor object
            actor.addContract(actorActHouseRetailRel);
            //Store relationship in the global array of all actor asset rel
            actorActorRelationships.add(actorActHouseRetailRel);

*/
            /**
             * The code below is only for visualization
             **/

            //Add Random Location of EU in the layout
            Double2D euLoc = new Double2D((i * 80) % 1600, (i / 20 + 1) * 80);
            layout.setObjectLocation(actor, euLoc);

            //Use same Loc for the SPM
            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
            for (ActorAssetRelationship rel : actor.assetRelationships) {
                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                //Only draw SPMs in the meantime
                if (rel.getAsset() instanceof Spm)
                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
            }


        }

    }

    /**
     * Decrease conventional households, without deleting the ConsumerUnit if it gets down to 0 households
     */

    void decreaseConventionalHouseholds(int numHouseholds) {
        //Get Last conventional ConsumerUnit, which may have space for more households
        EndUserUnit conventionalConsumer = null;
        int lastConvConsumer = conventionalConsumptionActors.size() - 1;

        Date today = getCurrentSimDate();


        //while there are still houses to remove from consumer units
        while (numHouseholds > 0) {

            //if index is positive, retrieve consumption actor to decrease households
            if (lastConvConsumer >= 0)
                conventionalConsumer = (EndUserUnit) conventionalConsumptionActors.get(lastConvConsumer);

            //how many households remain if we remove them all from the last consumer unit?
            int householdsLeft = conventionalConsumer.getNumberOfHouseholds() - numHouseholds;

            //if positive, we can just remove them from consumer unit, and we are done
            if (householdsLeft > 0) {
                conventionalConsumer.setNumberOfNewHouseholds(today, -numHouseholds);
                conventionalConsumer.setNumberOfHouseholds(householdsLeft);
                break;
            }
            //if negative, then we have to set the consumer unit to 0, and repeat with the next consumer unit
            else {

                //store the decrease in population in consumer unit
                conventionalConsumer.setNumberOfNewHouseholds(today, -conventionalConsumer.getNumberOfHouseholds());

                //set households to 0
                conventionalConsumer.setNumberOfHouseholds(0);

                //Convert households left into a positive number
                householdsLeft *= -1;

                //get index of next consumerUnit
                lastConvConsumer -= 1;
            }

            numHouseholds = householdsLeft;
        }
    }

    /**
     * Move housholds from Conventional ConsumerUnits to NonConventional Consumer Units
     */

    public void convertIntoNonConventionalHouseholds(double conversionPercentage) {

        Date today = getCurrentSimDate();

        //Get CONVENTIONAL Population today (this sim step)
        int householdsToday = 0;

        //sum population of each conventional consumer unit
        for (EndUserActor c : conventionalConsumptionActors) {
            EndUserUnit cu = (EndUserUnit) c;
            householdsToday += cu.getNumberOfHouseholds();
        }

        int numHouseholdsConverted = (int) (householdsToday * conversionPercentage);


        //Get Last Non conventional ConsumerUnit, which may has space for more households
        EndUserUnit NonConventionalConsumer = null;
        if (nonConventionalConsumptionActors.size() >= 1)
            NonConventionalConsumer = (EndUserUnit) nonConventionalConsumptionActors.get(nonConventionalConsumptionActors.size() - 1);

        int householdsLeft = numHouseholdsConverted;

        if (NonConventionalConsumer != null) {
            //If all new households fit in existing SPM, then just add them
            if ((maxHouseholdsPerConsumerUnit - NonConventionalConsumer.getNumberOfHouseholds()) > numHouseholdsConverted) {
                NonConventionalConsumer.setNumberOfHouseholds(NonConventionalConsumer.getNumberOfHouseholds() + numHouseholdsConverted);
                NonConventionalConsumer.setNumberOfNewHouseholds(today, numHouseholdsConverted);

                //Decrease conventional households, after increasing the non-conventional
                decreaseConventionalHouseholds(numHouseholdsConverted);
                return;
            }
            //If not all households fit, increase SPM to full, and create a new SPM with remaining households
            else if ((maxHouseholdsPerConsumerUnit - NonConventionalConsumer.getNumberOfHouseholds()) <= numHouseholdsConverted) {
                //Compute how many housholds fit in existing SPM
                int decrease = maxHouseholdsPerConsumerUnit - NonConventionalConsumer.getNumberOfHouseholds();
                //Decrease conventional households, after increasing the non-conventional
                decreaseConventionalHouseholds(decrease);

                //update how many households remain
                numHouseholdsConverted -= decrease;
                householdsLeft = numHouseholdsConverted;

                NonConventionalConsumer.setNumberOfNewHouseholds(today, decrease);
                NonConventionalConsumer.setNumberOfHouseholds(maxHouseholdsPerConsumerUnit);
            }
        }

        //If there are still houses remaining, then create a new consumer unit with fresh capacity
        for (int i = 0; i < numHouseholdsConverted; i = i + maxHouseholdsPerConsumerUnit) {

            boolean hasGas = true;

            System.out.println("Num Actors: " + numActors);

            int numPeople = 2;
            String name = "Household Non Conventional " + nonConventionalConsumptionActors.size();

            int householdsCreated = maxHouseholdsPerConsumerUnit;
            if (householdsLeft < maxHouseholdsPerConsumerUnit) householdsCreated = householdsLeft;

            Actor actor = new EndUserUnit(numActors++, ActorType.HDCONSUMER, name,
                    RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, householdsCreated, hasGas, true, 0, today);

            //Decrease conventional households, after increasing the non-conventional
            decreaseConventionalHouseholds(householdsCreated);


            consumptionActors.add((EndUserActor) actor);
            nonConventionalConsumptionActors.add((EndUserActor) actor);

            householdsLeft -= householdsCreated;

            //Schedule the actor in order to make decisions at every step
            this.schedule.scheduleRepeating(0.0, 1, actor);

            //Id of Non Conventional SPM
            int idSpm = 2;

            Spm spm = LoadData.createSpm(this, idSpm);

            /**
             * Actor Asset rel
             * */
            //create new relationship btw the household and SPM
            ActorAssetRelationship actorSpmRel = new ActorAssetRelationship(actor, spm, ActorAssetRelationshipType.USE, 100);

            //Store relationship with assets in the actor object
            actor.addAssetRelationship(actorSpmRel);
            //Store relationship in the global array of all actor asset rel
            actorAssetRelationships.add(actorSpmRel);

            /**
             * Actor Actor rel
             * */
            //create new relationship and contract btw the household and Retailer
            ActorActorRelationship actorActHouseRetailRel = new ActorActorRelationship(actor, actor_register.get(4), ActorActorRelationshipType.BILLING);

            //Store relationship with actor in the actor object
            actor.addContract(actorActHouseRetailRel);
            //Store relationship in the global array of all actor asset rel
            actorActorRelationships.add(actorActHouseRetailRel);


            /**
             * The code below is only for visualization
             **/

            //Add Random Location of EU in the layout
            Double2D euLoc = new Double2D((i * 80) % 1600, (i / 20 + 1) * 80);
            layout.setObjectLocation(actor, euLoc);

            //Use same Loc for the SPM
            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
            for (ActorAssetRelationship rel : actor.assetRelationships) {
                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                //Only draw SPMs in the meantime
                if (rel.getAsset() instanceof Spm)
                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
            }


        }

    }

    /**
     * Load Data From DataBase
     */

    public void loadData() {
        //selectDemandTemp();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = stringToDate.format(this.startSimDate);
        String endDate = stringToDate.format(this.endSimDate);

        LoadData.selectArena(this);
        LoadData.selectTariffs(this, startDate, endDate, areaCode);

        LoadData.selectDemand(this, startDate, endDate);
        LoadData.selectDemandHalfHour(this, startDate, endDate);
        LoadData.selectForecastConsumption(this);
        LoadData.selectConsumption(this, startDate, endDate);
        LoadData.selectGenerationHistoricData(this, startDate, endDate);
        LoadData.selectMonthlySolarExposure(this);
        LoadData.selectHalfHourSolarExposure(this);
        LoadData.createHalfHourSolarExposureForecast(this);
        LoadData.selectSolarInstallation(this);
        LoadData.selectActors(this,  startDate, endDate);

        LoadData.selectMaximumDemandForecast(this);
        LoadData.selectMinimumDemandForecast(this);

        //selectActorActorRelationships("actoractor93");

        LoadData.selectActorAssetRelationships(this, "actorasset");//from https://www.secv.vic.gov.au/history/
    }

    public void start() {
        super.start();
        simulParametres();
        loadData();
        generateHouseholds();
        saveData.plotSeries(this);
        this.schedule.scheduleRepeating(policies);
        this.schedule.scheduleRepeating(0.0, 2, saveData);

        for (Map.Entry<Integer, Arena> entry : arena_register.entrySet()) {
            Arena a = entry.getValue();
            this.schedule.scheduleRepeating(0.0, 0, a);
        }


    }


    public static void main(String[] args) {
        doLoop(Gr4spSim.class, args);


        exit(0);
    }

}
