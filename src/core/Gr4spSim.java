package core;

import com.esotericsoftware.yamlbeans.YamlReader;
import core.Policies.SimPolicies;
import core.Relationships.*;
import core.Social.*;
import core.Technical.*;
import core.settings.Settings;
import sim.engine.*;
//import sim.util.Double2D;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static String url = "jdbc:postgresql://localhost:5432/gr4spdb?user=postgres";
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

    // Variable to get historic RE before market. In Victoria RE was only hydro
    HashMap<Date, Double> monthly_renewable_historic_register;


    //Total monthly Consumption
    HashMap<Date, Double> total_monthly_consumption_register;


    //Total Generation GWh per month
    HashMap<Date, Generation> monthly_generation_register;

    //Number of costumers
    HashMap<Date, Integer> monthly_domestic_consumers_register;

    //CPI conversion
    HashMap<Date, Float> cpi_conversion;

    //Annual Inflation
    HashMap<Integer, Float> annual_inflation;


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

    ArrayList<ActorAssetRelationship> actorAssetRelationships = new ArrayList<ActorAssetRelationship>();

    //Counter for the unique id each time a storage unit is created and other storage related variables
    private int numGenerators;
    private int numStorage;
    //counter for energy grid
    private int numGrid;
    private int numcpoints;

    //counter organisation structures
    private int numOrg;

    //counter for actors
    private int numActors;

    //Date representing the start of the simulation, and the final date of the simulation
    private Calendar simCalendar;
    private Date startSimDate;
    private Date endSimDate;
    private Date startSpotMarketDate;
    private Date baseYearForecastDate;

    //Policies used for the simulation
    private SimPolicies policies;

    //Class to manage all the data generated in the simulation
    public SaveData saveData;

    //Simulation Settings specified via YAML file
    public Settings settings;
    public Settings settingsAfterBaseYear;

    public String yamlFileName;
    public static String outputID;

    public Gr4spSim(long seed ) {
    //1607932481652L
        super(seed);

        //Generate Unique ID to represent all generated data Files (SaveData and Logger)
//        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
//        Calendar cal = Calendar.getInstance();
//        outputID = sdf.format(cal.getTime())+"_seed"+seed;

        outputID = "_seed_" + seed;

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
        monthly_renewable_historic_register = new HashMap<>();

        total_monthly_consumption_register = new HashMap<>();
        monthly_generation_register = new HashMap<>();
        monthly_domestic_consumers_register = new HashMap<>();
        cpi_conversion = new HashMap<>();
        annual_inflation = new HashMap<>();
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
            throw new RuntimeException("Problems creating the log files");
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

            // Load Future Yaml, if it doesn't exist, use the same yaml file
            String folderYamlfuture = pathSRC;
            if( System.getProperty("os.name").contains("Windows") )
                folderYamlfuture+="gr4sp\\simulationSettings\\"+yamlFileName+"future.yaml";
            else
                folderYamlfuture+="gr4sp/simulationSettings/"+yamlFileName+"future.yaml";

            Path p = Paths.get(folderYamlfuture);
            if( Files.exists(p) )
                reader = new YamlReader(new FileReader(folderYamlfuture));

            settingsAfterBaseYear = reader.read(Settings.class);


            /**
             * Simulation Date Range
             */

            //Save start and end date in the simulator state, which is this classs Gr4spSim
            SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

            this.startSimDate = stringToDate.parse(settings.getStartDate());
            this.simCalendar.setTime(this.startSimDate);
            this.endSimDate = stringToDate.parse(settings.getEndDate());

            this.startSpotMarketDate = stringToDate.parse(settings.getStartDateSpotMarket());
            this.baseYearForecastDate = stringToDate.parse(settings.getBaseYearConsumptionForecast() + "-01-01");



            /**
             * Public Policies
             */

            // goes from 0.00 to 1.0, represents percentage of monthly uptake and uses a normal gaussian distribution to simulate the uptake
            // for example, 0.01 represents 1% per month, around 12% a year
            policies.setEndConsumerTariffs(settings.getEndConsumerTariff());

        } catch (ParseException | com.esotericsoftware.yamlbeans.YamlException | java.io.FileNotFoundException e) {
            System.out.println("Problems reading YAML file!" + e.toString());
            exit(0);
        }


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


        int numHouseholds = (int) (householdsGrowth * getPopulationPercentageAreaCode());

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
            if ((getMaxHouseholdsPerConsumerUnit() - consumer.getNumberOfHouseholds()) > numHouseholds) {
                consumer.setNumberOfHouseholds(consumer.getNumberOfHouseholds() + numHouseholds);
                consumer.setNumberOfNewHouseholds(today, numHouseholds);

                return;
            }
            //If not all households fit, increase SPM to full, and create a new SPM with remaining households
            else if ((getMaxHouseholdsPerConsumerUnit() - consumer.getNumberOfHouseholds()) <= numHouseholds) {
                int decrease = getMaxHouseholdsPerConsumerUnit() - consumer.getNumberOfHouseholds();
                numHouseholds -= decrease;
                householdsLeft = numHouseholds;

                consumer.setNumberOfNewHouseholds(today, decrease);
                consumer.setNumberOfHouseholds(getMaxHouseholdsPerConsumerUnit());
            }
        }

        for (int i = 0; i < numHouseholds; i = i + getMaxHouseholdsPerConsumerUnit()) {

            boolean hasGas = false;

            LOGGER.info("Num Actors: " + numActors);

            int numPeople = 2;
            String name = "Household Conventional " + conventionalConsumptionActors.size();

            int householdsCreated = getMaxHouseholdsPerConsumerUnit();
            if (householdsLeft < getMaxHouseholdsPerConsumerUnit()) householdsCreated = householdsLeft;


            //Create new Consumer Unit
            Actor actor = new EndUserUnit(numActors++, name,
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
        LoadData.selectTariffs(this, startDate, endDate, getAreaCode());
        LoadData.selectInflation( this );

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

//        LoadData.selectMaximumDemandForecast(this);
//        LoadData.selectMinimumDemandForecast(this);

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

        dataToSerialize.put("gen_register", gen_register);

        dataToSerialize.put("spm_register", spm_register);
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
        dataToSerialize.put("annual_inflation", annual_inflation);
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
        dataToSerialize.put("actorAssetRelationships", actorAssetRelationships);


        //more info here https://www.toolsqa.com/rest-assured/serialization-and-deserialization-in-java/

         try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.settings.folderOutput+"/SimStateDB.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(dataToSerialize);
            objectOutputStream.close();
            fileOutputStream.close();
         } catch (FileNotFoundException i) {
             i.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    private HashMap<String, Object> deserialize(){

        try {

            FileInputStream fileInputStream = new FileInputStream(this.settings.folderOutput+"/SimStateDB.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            HashMap<String,Object> retrieved = (HashMap<String,Object>)objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            gen_register = (HashMap<Integer, Vector<Generator>> ) retrieved.get("gen_register");

            spm_register = (HashMap<Integer, Vector<Spm>> ) retrieved.get("spm_register");

            network_register = (HashMap<Integer, Vector<NetworkAssets>> ) retrieved.get("network_register");
            actor_register = (HashMap<Integer, Actor>) retrieved.get("actor_register");
            arena_register = (HashMap<Integer, Arena>) retrieved.get("arena_register");

            halfhour_demand_register = (HashMap<Date, Double>) retrieved.get("halfhour_demand_register");
            maximum_demand_forecast_register = (HashMap<Integer, Float> ) retrieved.get("maximum_demand_forecast_register");
            minimum_demand_forecast_register = (HashMap<Integer, Float> ) retrieved.get("minimum_demand_forecast_register");
            annual_forecast_consumption_register = (HashMap<Integer, Float> ) retrieved.get("annual_forecast_consumption_register");
            annual_forecast_rooftopPv_register = (HashMap<Integer, Float> ) retrieved.get("annual_forecast_rooftopPv_register");

            monthly_consumption_register = (HashMap<Date, Double>) retrieved.get("monthly_consumption_register");
            total_monthly_consumption_register = (HashMap<Date, Double>) retrieved.get("total_monthly_consumption_register");

            monthly_generation_register = (HashMap<Date, Generation>) retrieved.get("monthly_generation_register");
            monthly_domestic_consumers_register = (HashMap<Date, Integer>) retrieved.get("monthly_domestic_consumers_register");
            cpi_conversion = (HashMap<Date, Float>) retrieved.get("cpi_conversion");
            annual_inflation = (HashMap<Integer, Float>) retrieved.get("annual_inflation");
            halfhour_solar_exposure = (HashMap<Date, Float>) retrieved.get("halfhour_solar_exposure");

            solar_number_installs = (HashMap<Date, Integer>) retrieved.get("solar_number_installs");
            solar_aggregated_kw = (HashMap<Date, Integer>) retrieved.get("solar_aggregated_kw");
            solar_system_capacity_kw = (HashMap<Date, Float>) retrieved.get("solar_system_capacity_kw");

            numGenerators = (int) retrieved.get("numGenerators");
            numStorage = (int) retrieved.get("numStorage");
            numGrid = (int) retrieved.get("numGrid");
            numcpoints = (int) retrieved.get("numcpoints");
            numOrg = (int) retrieved.get("numOrg");
            numActors = (int) retrieved.get("numActors");


            consumptionActors = (ArrayList<EndUserActor>) retrieved.get("consumptionActors");
            conventionalConsumptionActors = (ArrayList<EndUserActor>) retrieved.get("conventionalConsumptionActors");
            nonConventionalConsumptionActors = (ArrayList<EndUserActor>) retrieved.get("nonConventionalConsumptionActors");
            actorAssetRelationships = (ArrayList<ActorAssetRelationship>) retrieved.get("actorAssetRelationships");


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
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

    public Date getBaseYearForecastDate() {
        return baseYearForecastDate;
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
        return settings.getAreaCode();
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

    public HashMap<Integer, Float> getAnnual_inflation() { return annual_inflation; }

    public HashMap<Integer, Float> getTariff_contribution_wholesale_register() { return tariff_contribution_wholesale_register; }

    public HashMap<Date, Double> getHalfhour_demand_register() {
        return halfhour_demand_register;
    }

    public HashMap<Date, Double> getMonthly_consumption_register() {
        return monthly_consumption_register;
    }

    public HashMap<Date, Double> getMonthly_renewable_historic_register() {
        return monthly_renewable_historic_register;
    }

    public HashMap<Date, Double> getTotal_monthly_consumption_register() {
        return total_monthly_consumption_register;
    }

    public HashMap<Date, Integer> getMonthly_domestic_consumers_register() { return monthly_domestic_consumers_register; }

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

    public double getPopulationPercentageAreaCode() {
        return settings.getPopulationPercentageAreacCode();
    }

    public int getMaxHouseholdsPerConsumerUnit() {
        return settings.getMaxHouseholdsPerConsumerUnit();
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


    public void start() {
        super.start();

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
    public void finish() {
        super.finish();
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
//            return;
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


