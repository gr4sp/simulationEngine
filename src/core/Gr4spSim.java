package core;

import com.esotericsoftware.yamlbeans.YamlReader;
import core.Policies.SimPolicies;
import core.Relationships.*;
import core.Social.*;
import core.Technical.*;
import core.settings.Settings;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.field.network.Network;
//import sim.util.Double2D;

import java.io.*;
import java.nio.file.Paths;
import java.security.Permission;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

import sim.portrayal.network.stats.*;
import sim.portrayal.network.*;

import core.Gr4spLogger;

import static core.Social.GovRole.*;
import static java.lang.System.exit;

class MySecurityManager extends SecurityManager {
    @Override public void checkExit(int status) {
        throw new SecurityException();
    }

    @Override public void checkPermission(Permission perm) {
        // Allow other activities by default
    }
}

public class Gr4spSim extends SimState implements java.io.Serializable {
    private static final long serialVersionUID = 1;

    public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    //public Continuous2D layout;
//    SocialNetworkInspector networkInspector = new SocialNetworkInspector();
//    public Network actorsNetwork = new Network();


    HashMap<Integer, Vector<Spm>> spm_register;
    HashMap<Integer, Vector<Generator>> gen_register; // If a new Generator is added, make its id to be numGenerators+1
    HashMap<Integer, Vector<NetworkAssets>> network_register;
    HashMap<Integer, Actor> actor_register;
    HashMap<Integer, Arena> arena_register;

    //Demand
    HashMap<Date, Double> halfhour_demand_register;

    //forecast ISP maximum and minimum demand (MW) from 2019 to 2050; various scenarios
    HashMap<Integer, Float> maximum_demand_forecast_register;
    HashMap<Integer, Float> minimum_demand_forecast_register;

    //Forecast ISP consumption from 2019 to 2050 in GWh for various scenarios and regions
    HashMap<Integer, Float> annual_forecast_consumption_register;
    HashMap<Integer, Float> annual_forecast_rooftopPv_register;

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

    //Tariff contribution of the wholesale
    HashMap<Integer, Float> tariff_contribution_wholesale_register;

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

    //Population percentage over VIC population
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
    public static String outputID;



    public Gr4spSim(long seed ) {

        super(seed);

        //Generate Unique ID to represent all generated data Files (SaveData and Logger)
//        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
//        Calendar cal = Calendar.getInstance();
//        outputID = sdf.format(cal.getTime())+"_seed"+seed;

        outputID = "_seed_"+seed;

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

        halfhour_demand_register = new HashMap<>();
        monthly_consumption_register = new HashMap<>();

        total_monthly_consumption_register = new HashMap<>();
        monthly_generation_register = new HashMap<>();
        monthly_domestic_consumers_register = new HashMap<>();
        cpi_conversion = new HashMap<>();
        tariff_contribution_wholesale_register = new HashMap<>();
        halfhour_solar_exposure = new HashMap<>();
        solar_aggregated_kw = new HashMap<>();
        solar_number_installs = new HashMap<>();
        solar_system_capacity_kw = new HashMap<>();
        annual_forecast_consumption_register = new HashMap<>();
        annual_forecast_rooftopPv_register = new HashMap<>();
        maximum_demand_forecast_register = new HashMap<>();
        minimum_demand_forecast_register = new HashMap<>();

        //layout = new Continuous2D(10.0, 600.0, 600.0);
        policies = new SimPolicies();

        simulParametres();

        //Setup Logger
        try {
            //Setup logging level
            Level level = Level.INFO;
            if(settings.logLevel.equalsIgnoreCase("OFF") )
                level = Level.OFF;
            if(settings.logLevel.equalsIgnoreCase("WARNING"))
                level = Level.WARNING;

            Gr4spLogger.setup(outputID, settings.folderOutput, level);

        }catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problems with creating the log files");
        }



        saveData = new SaveData(this);

    }


    private void simulParametres() {

        try {
            

            String pathSRC = Paths.get(".").toAbsolutePath().normalize().toString();
            pathSRC = pathSRC.split("gr4sp")[0];
            yamlFileName = "VIC";
            String sysName = System.getProperty("os.name");

            String folderYaml = pathSRC;
            if( System.getProperty("os.name").contains("Windows") )
                folderYaml+="gr4sp\\simulationSettings\\"+yamlFileName+".yaml";
            else
                folderYaml+="gr4sp/simulationSettings/"+yamlFileName+".yaml";

            YamlReader reader = new YamlReader(new FileReader(folderYaml));

            settings = reader.read(Settings.class);
            settings.computeSolarEfficiency();


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

    public HashMap<Date, Float> getCpi_conversion() { return cpi_conversion; }

    public HashMap<Integer, Float> getTariff_contribution_wholesale_register() { return tariff_contribution_wholesale_register; }

    public HashMap<Date, Double> getHalfhour_demand_register() {
        return halfhour_demand_register;
    }

    public HashMap<Date, Double> getMonthly_consumption_register() {
        return monthly_consumption_register;
    }


    public HashMap<Date, Double> getTotal_monthly_consumption_register() {
        return total_monthly_consumption_register;
    }

    public HashMap<Date, Integer> getMonthly_domestic_consumers_register() { return monthly_domestic_consumers_register; }

    public double getDomesticConsumptionPercentage() {
        return domesticConsumptionPercentage;
    }

    public HashMap<Date, Generation> getMonthly_generation_register() {
        return monthly_generation_register;
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

    public HashMap<Integer, Float> getAnnual_forecast_consumption_register() { return annual_forecast_consumption_register; }

    public void setAnnual_forecast_consumption_register(HashMap<Integer, Float> annual_forecast_consumption_register) { this.annual_forecast_consumption_register = annual_forecast_consumption_register; }

    public HashMap<Integer, Float> getAnnual_forecast_rooftopPv_register() { return annual_forecast_rooftopPv_register; }

    public void setAnnual_forecast_rooftopPv_register(HashMap<Integer, Float> annual_forecast_rooftopPv_register) { this.annual_forecast_rooftopPv_register = annual_forecast_rooftopPv_register; }

    public HashMap<Integer, Float> getMaximum_demand_forecast_register() {
        return maximum_demand_forecast_register;
    }

    public void setMaximum_demand_forecast_register(HashMap<Integer, Float> maximum_demand_forecast_register) { this.maximum_demand_forecast_register = maximum_demand_forecast_register; }

    public HashMap<Integer, Float> getMinimum_demand_forecast_register() {
        return minimum_demand_forecast_register;
    }

    public void setMinimum_demand_forecast_register(HashMap<Integer, Float> minimum_demand_forecast_register) { this.minimum_demand_forecast_register = minimum_demand_forecast_register; }

    public Arena getArenaByName(String arenaName) {
        for (Map.Entry<Integer, Arena> entry : this.getArena_register().entrySet()) {
            Arena a = entry.getValue();
            if (a.getType().equalsIgnoreCase(arenaName)) {
                return a;
            }
        }

        return null;
    }


//    void addSPMLocation(Spm s, Double2D loc, double diameter) {
//        //layout.setObjectLocation(s, loc);
//        Double2D shift = new Double2D(10, 10);
//
//
//        for (Spm spmc : s.getSpms_contained()) {
//            spmc.diameter = diameter - 10;
//            addSPMLocation(spmc, loc, spmc.diameter);
//            loc = loc.add(shift);
//
//        }
//    }


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

            LOGGER.info("Num Actors: " + numActors);

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

            //Schedule the actor in order to make decisions at every step
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
//            Double2D euLoc = new Double2D((i * 80) % 1600, (i / 20 + 1) * 80);
//            layout.setObjectLocation(actor, euLoc);
//
//            //Use same Loc for the SPM
//            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
//            for (ActorAssetRelationship rel : actor.assetRelationships) {
//                //Add all SPMs location for this EU recursively, decreasing the diameter and
//                //shifting when more than one smp is contained
//                //Only draw SPMs in the meantime
//                if (rel.getAsset() instanceof Spm)
//                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
//            }


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

            LOGGER.info("Num Actors: " + numActors);

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
//            Double2D euLoc = new Double2D((i * 80) % 1600, (i / 20 + 1) * 80);
//            layout.setObjectLocation(actor, euLoc);
//
//            //Use same Loc for the SPM
//            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
//            for (ActorAssetRelationship rel : actor.assetRelationships) {
//                //Add all SPMs location for this EU recursively, decreasing the diameter and
//                //shifting when more than one smp is contained
//                //Only draw SPMs in the meantime
//                if (rel.getAsset() instanceof Spm)
//                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
//            }


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

        LoadData.selectDemandHalfHour(this, startDate, endDate);
        LoadData.selectForecastConsumption(this);
        LoadData.selectForecastSolarUptake(this);
        LoadData.selectForecastEnergyEfficency(this);
        LoadData.selectForecastOnsiteGeneration(this);
        LoadData.selectConsumption(this, startDate, this.settings.getStartDateSpotMarket(), endDate);
        LoadData.selectGenerationHistoricData(this, startDate, endDate);
        LoadData.selectHalfHourSolarExposure(this);
        LoadData.createHalfHourSolarExposureForecast(this);
        LoadData.selectSolarInstallation(this);
        LoadData.createSolarInstallationForecast(this);
        LoadData.selectActors(this,  startDate, endDate);

        LoadData.selectMaximumDemandForecast(this);
        LoadData.selectMinimumDemandForecast(this);

        //loadNetwork();
        //selectActorActorRelationships("actoractor93");

        //LoadData.selectActorAssetRelationships(this, "actorasset");//from https://www.secv.vic.gov.au/history/
    }

//    private void loadNetwork(){
//
//        int numActiveActors = 0;
//        for (Integer integer : actor_register.keySet()) {
//            Actor actor = actor_register.get(integer);
//            // if start of actor is after current date (it started) and has not ended (change date is after current date)
//            if (simCalendar.getTime().after(actor.getStart()) && actor.getChangeDate().after(simCalendar.getTime())) {
//                this.actorsNetwork.addNode(actor);
//                numActiveActors++;
//            }
//        }
//
//        System.out.println(numActiveActors);
//    }

    private void serialize(){

        HashMap<String, Object> dataToSerialize = new HashMap<String, Object>();
        dataToSerialize.put("spm_register", spm_register);
        dataToSerialize.put("gen_register", gen_register);
        dataToSerialize.put("network_register", network_register);
        dataToSerialize.put("actor_register", actor_register);
        dataToSerialize.put("arena_register", arena_register);

        dataToSerialize.put("halfhour_demand_register", halfhour_demand_register);
        dataToSerialize.put("maximum_demand_forecast_register", maximum_demand_forecast_register);
        dataToSerialize.put("minimum_demand_forecast_register", minimum_demand_forecast_register);

        dataToSerialize.put("annual_forecast_consumption_register", annual_forecast_consumption_register);
        dataToSerialize.put("annual_forecast_rooftopPv_register", annual_forecast_rooftopPv_register);
        dataToSerialize.put("monthly_consumption_register", monthly_consumption_register);
        dataToSerialize.put("total_monthly_consumption_register", total_monthly_consumption_register);

        dataToSerialize.put("monthly_generation_register", monthly_generation_register);
        dataToSerialize.put("monthly_domestic_consumers_register", monthly_domestic_consumers_register);
        dataToSerialize.put("cpi_conversion", cpi_conversion);
        dataToSerialize.put("halfhour_solar_exposure", halfhour_solar_exposure);

        dataToSerialize.put("solar_number_installs", solar_number_installs);
        dataToSerialize.put("solar_aggregated_kw", solar_aggregated_kw);
        dataToSerialize.put("solar_system_capacity_kw", solar_system_capacity_kw);

        dataToSerialize.put("numGenerators", numGenerators);
        dataToSerialize.put("numStorage", numStorage);
        dataToSerialize.put("numGrid", numGrid);
        dataToSerialize.put("numcpoints", numcpoints);
        dataToSerialize.put("numOrg", numOrg);
        dataToSerialize.put("numActors", numActors);

        dataToSerialize.put("consumptionActors", consumptionActors);
        dataToSerialize.put("conventionalConsumptionActors", conventionalConsumptionActors);
        dataToSerialize.put("nonConventionalConsumptionActors", nonConventionalConsumptionActors);
        dataToSerialize.put("actorActorRelationships", actorActorRelationships);
        dataToSerialize.put("actorAssetRelationships", actorAssetRelationships);




         try {
            FileOutputStream fileOut = new FileOutputStream(this.settings.folderOutput+"/SimStateDB.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(dataToSerialize);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    private void deserialize(){

        try {

            FileInputStream fis = new FileInputStream(this.settings.folderOutput+"/SimStateDB.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);

            HashMap<String,Object> retreived = (HashMap<String,Object>)ois.readObject();
            fis.close();

            spm_register = (HashMap<Integer, Vector<Spm>> ) retreived.get("spm_register");

            gen_register = (HashMap<Integer, Vector<Generator>> ) retreived.get("gen_register");
            network_register = (HashMap<Integer, Vector<NetworkAssets>> ) retreived.get("network_register");
            actor_register = (HashMap<Integer, Actor>) retreived.get("actor_register");
            arena_register = (HashMap<Integer, Arena>) retreived.get("arena_register");

            halfhour_demand_register = (HashMap<Date, Double>) retreived.get("halfhour_demand_register");
            maximum_demand_forecast_register = (HashMap<Integer, Float> ) retreived.get("maximum_demand_forecast_register");
            minimum_demand_forecast_register = (HashMap<Integer, Float> ) retreived.get("minimum_demand_forecast_register");
            annual_forecast_consumption_register = (HashMap<Integer, Float> ) retreived.get("annual_forecast_consumption_register");
            annual_forecast_rooftopPv_register = (HashMap<Integer, Float> ) retreived.get("annual_forecast_rooftopPv_register");

            monthly_consumption_register = (HashMap<Date, Double>) retreived.get("monthly_consumption_register");
            total_monthly_consumption_register = (HashMap<Date, Double>) retreived.get("total_monthly_consumption_register");

            monthly_generation_register = (HashMap<Date, Generation>) retreived.get("monthly_generation_register");
            monthly_domestic_consumers_register = (HashMap<Date, Integer>) retreived.get("monthly_domestic_consumers_register");
            cpi_conversion = (HashMap<Date, Float>) retreived.get("cpi_conversion");
            halfhour_solar_exposure = (HashMap<Date, Float>) retreived.get("halfhour_solar_exposure");

            solar_number_installs = (HashMap<Date, Integer>) retreived.get("solar_number_installs");
            solar_aggregated_kw = (HashMap<Date, Integer>) retreived.get("solar_aggregated_kw");
            solar_system_capacity_kw = (HashMap<Date, Float>) retreived.get("solar_system_capacity_kw");

            numGenerators = (int) retreived.get("numGenerators");
            numStorage = (int) retreived.get("numStorage");
            numGrid = (int) retreived.get("numGrid");
            numcpoints = (int) retreived.get("numcpoints");
            numOrg = (int) retreived.get("numOrg");
            numActors = (int) retreived.get("numActors");


            consumptionActors = (ArrayList<EndUserActor>) retreived.get("consumptionActors");
            conventionalConsumptionActors = (ArrayList<EndUserActor>) retreived.get("conventionalConsumptionActors");
            nonConventionalConsumptionActors = (ArrayList<EndUserActor>) retreived.get("nonConventionalConsumptionActors");
            actorActorRelationships = (ArrayList<ActorActorRelationship>) retreived.get("actorActorRelationships");
            actorAssetRelationships = (ArrayList<ActorAssetRelationship>) retreived.get("actorAssetRelationships");


        } catch (IOException | ClassNotFoundException i) {
            i.printStackTrace();
        }

    }

    public void start() {
        super.start();
        loadData();
        generateHouseholds();

//        serialize();
//        deserialize();

        saveData.plotSeries(this);
        this.schedule.scheduleRepeating(policies);
        this.schedule.scheduleRepeating(0.0, 2, saveData);

        for (Map.Entry<Integer, Arena> entry : arena_register.entrySet()) {
            Arena a = entry.getValue();
            this.schedule.scheduleRepeating(0.0, 0, a);
        }


    }

    public void runFromPythonEMA() {
        //Before running the external Command
        MySecurityManager secManager = new MySecurityManager();
        System.setSecurityManager(secManager);

        NumberFormat rateFormat = NumberFormat.getInstance();
        rateFormat.setMaximumFractionDigits(5);
        rateFormat.setMinimumIntegerDigits(1);

        SimState state = this;
        try {

            state.start();
            long oldClock = System.currentTimeMillis();

            while(true){
                if (!state.schedule.step(state)) break;

                //Print info
                if (state.schedule.getSteps() % 12 == 0L) {
                    long clock = System.currentTimeMillis();
                    SimState.printlnSynchronized("Job" + this.outputID + ": " + "Steps: " + state.schedule.getSteps() + " Time: " + getCurrentSimDate() + " Rate: " + rateFormat.format(1000.0D * (double)(12) / (double)(clock - oldClock)));
                    LOGGER.info("Job" + this.outputID + ": " + "Steps: " + state.schedule.getSteps() + " Time: " + getCurrentSimDate() +" Rate: " + rateFormat.format(1000.0D * (double)(12) / (double)(clock - oldClock)));
                    oldClock = clock;
                }
            }
            state.finish();
        } catch (SecurityException e) {
            //Do something if the external code used System.exit()
            //System.out.println("We avoided the Exit!");
        }

    }

//    public static void runAsPythonEMA() {
//        //Before running the external Command
//        MySecurityManager secManager = new MySecurityManager();
//        System.setSecurityManager(secManager);
//
//        NumberFormat rateFormat = NumberFormat.getInstance();
//        rateFormat.setMaximumFractionDigits(5);
//        rateFormat.setMinimumIntegerDigits(1);
//
//        long oldClock = System.currentTimeMillis();
//
//        SimState state = null;
//        try {
//            Random rand = new Random();
//            state = new Gr4spSim( rand.nextInt() );
//            state.start();
//            Gr4spSim data  = (Gr4spSim) state;
//            while(true){
//                if (!state.schedule.step(state)) break;
//
//                //Print info
//                if (state.schedule.getSteps() % 12 == 0L) {
//                    long clock = System.currentTimeMillis();
//                    SimState.printlnSynchronized("Job " + data.outputID + ": " + "Steps: " + state.schedule.getSteps() + " Time: " + data.getCurrentSimDate() +
//                            " Rate: " + rateFormat.format(1000.0D * (double)(12) / (double)(clock - oldClock)));
//                    LOGGER.info("Job " + data.outputID + ": " + "Steps: " + state.schedule.getSteps() + " Time: " + data.getCurrentSimDate() +
//                            " Rate: " + rateFormat.format(1000.0D * (double)(12) / (double)(clock - oldClock)));
//                    oldClock = clock;
//                }
//            }
//            state.finish();
//        } catch (SecurityException e) {
//            //Do something if the external code used System.exit()
//            //System.out.println("We avoided the Exit!");
//        }
//
//    }

    public static void main(String[] args) {

        //Before running the external Command
        MySecurityManager secManager = new MySecurityManager();
        System.setSecurityManager(secManager);

        try {

//            Gr4spSim data = new Gr4spSim(0);
//            data.runFromPythonEMA();
//            data = new Gr4spSim(1);
//            data.runFromPythonEMA();

            doLoop(Gr4spSim.class, args);
        } catch (SecurityException e) {
            //Do something if the external code used System.exit()
            //System.out.println("We avoided the Exit!");
        }


        //exit(0);
    }

}


