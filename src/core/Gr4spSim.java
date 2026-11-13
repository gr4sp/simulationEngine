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
    /**
     * JDBC URL for the input database. Overridable with -Dgr4sp.db.url so a run can be
     * pointed at a database other than the default gr4spdb -- used to regenerate and
     * check the regression baseline against a clean restore of backupDB/, without
     * disturbing a working gr4spdb that may carry refreshed data.
     */
    public static String url = System.getProperty("gr4sp.db.url",
            "jdbc:postgresql://localhost:5432/gr4spdb?user=postgres");
    public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * The timezone the model runs in. GR4SP is date-driven throughout -- half-hourly
     * demand and solar series are bucketed into months and years, and the retail
     * tariff updates in January -- and every one of those steps used the JVM default
     * timezone, which is a property of the machine rather than of the model. The same
     * code, the same database and the same seed therefore produced different results
     * in different places: running the regression suite under UTC instead of Melbourne
     * moves annual hydro production by about 0.14% and shifts wholesale price,
     * emissions and renewable share with it.
     *
     * That made the published results dependent on where they were run, which is not
     * a property a reproducible model can have. Pinning it here fixes the results to
     * the timezone they were produced and reported in, so a reader anywhere obtains
     * the numbers in the article.
     *
     * NOTE: Australia/Melbourne observes daylight saving, while the NEM settles in
     * Australian Eastern Standard Time year-round (UTC+10, no DST). This constant
     * preserves the published behaviour; whether the model should instead run on NEM
     * market time is a separate question, recorded as roadmap item 12.
     */
    public static final String TIMEZONE = "Australia/Melbourne";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
    }


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
    //N23: irradiance on the rooftop array plane (null: rooftop PV uses the horizontal irradiance), with a one-entry
    //cache, since every rooftop generator asks for the same half-hour in turn
    RooftopIrradiance rooftopIrradiance;
    private Date rooftopExposureTime;
    private float rooftopExposureValue;
    //N23: rooftop installations from solarInstallationsFile (year, month, installations, kW; system capacity kW)
    List<double[]> solarInstallationsOverride = new ArrayList<>();

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
    //First date written to the outputs; null writes the whole run
    private Date reportFromDate;
    //Net exports by month (yyyy-MM) as a flat MW rate over the month; empty without the netExportsFile setting
    private Map<String, Double> netExportsMW = new HashMap<>();
    //Deviation from the monthly mean by calendar month (1-12) and half-hour of day (0-47), MW
    private double[][] netExportsShapeMW;
    //Fuel price by year for each fuelPriceFile, carbon price by month, and station name prefixes -> portfolio
    private Map<String, TreeMap<Integer, Double>> fuelPrices = new HashMap<>();
    private Map<String, Double> carbonPriceTonne = new HashMap<>();
    private List<String[]> portfolios = new ArrayList<>();
    private Map<String, String> portfolioOf = new HashMap<>();
    //Station parameter overrides (name prefix, parameter, value) and the consumer price index by year
    private List<String[]> generatorParameters = new ArrayList<>();
    private TreeMap<Integer, Double> cpi = new TreeMap<>();
    //Additive tariff components by calendar year of bills: {network, environmental, retail, gst_rate} (tariffComponentsFile)
    private TreeMap<Integer, double[]> tariffComponents = new TreeMap<>();
    //Interconnector import blocks (link,from,to,importMW,heatRate,fuel,VOM,emissions,waterValueFactor,lossFactor)
    //and the market price cap by start date (yyyy-MM-dd); both empty unless set
    private List<String[]> interconnectors = new ArrayList<>();
    private TreeMap<String, Double> shortfallPriceMWh = new TreeMap<>();

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
            

            //Settings live under the project root, which is the directory the
            //simulation is launched from. Do not derive the root from the folder
            //name: a clone can be called anything.
            Path settingsFolder = Paths.get("").toAbsolutePath().normalize().resolve("simulationSettings");
            yamlFileName = "VIC";

            Path folderYaml = settingsFolder.resolve(yamlFileName + ".yaml");

            YamlReader reader = new YamlReader(new FileReader(folderYaml.toFile()));

            settings = reader.read(Settings.class);
            settings.computeSolarEfficiency();
            if (settings.folderOutput == null || settings.folderOutput.trim().equals("auto")) {
                settings.folderOutput = System.getProperty("user.dir");
            }

            // Load Future Yaml, if it doesn't exist, use the same yaml file
            Path p = settingsFolder.resolve(yamlFileName + "future.yaml");
            if( Files.exists(p) )
                reader = new YamlReader(new FileReader(p.toFile()));

            settingsAfterBaseYear = reader.read(Settings.class);
            if (settingsAfterBaseYear.folderOutput == null || settingsAfterBaseYear.folderOutput.trim().equals("auto")) {
                settingsAfterBaseYear.folderOutput = settings.folderOutput;
            }


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
            this.reportFromDate = settings.getReportFrom() == null ? null : stringToDate.parse(settings.getReportFrom());
            if (settings.getNetExportsFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getNetExportsFile()));
                for (String line : lines.subList(1, lines.size())) {
                    String[] f = line.split(",", 3);
                    double hours = java.time.YearMonth.parse(f[0]).lengthOfMonth() * 24.0;
                    netExportsMW.put(f[0], Double.parseDouble(f[1]) / hours);
                }
            }
            if (settings.getNetExportsShapeFile() != null) {
                netExportsShapeMW = new double[13][48];
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getNetExportsShapeFile()));
                for (String line : lines.subList(1, lines.size())) {
                    String[] f = line.split(",", 4);
                    netExportsShapeMW[Integer.parseInt(f[0])][Integer.parseInt(f[1])] = Double.parseDouble(f[2]);
                }
            }
            if (settings.getShapeDemandUplift() && (netExportsShapeMW == null || settings.getNetExportsFile() != null))
                throw new IllegalArgumentException("shapeDemandUplift needs netExportsShapeFile and no netExportsFile (exports are already in the calibrated demand)");
            if (settings.getCarbonPriceFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getCarbonPriceFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 3);
                    carbonPriceTonne.put(f[0].trim(), Double.parseDouble(f[1].trim()));
                }
            }
            if (settings.getPortfolioFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getPortfolioFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 3);
                    portfolios.add(new String[]{f[0].trim(), f[1].trim()});
                }
            }
            if (settings.getSolarInstallationsFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getSolarInstallationsFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 6);
                    solarInstallationsOverride.add(new double[]{Double.parseDouble(f[0].trim()), Double.parseDouble(f[1].trim()),
                            Double.parseDouble(f[2].trim()), Double.parseDouble(f[3].trim()), Double.parseDouble(f[4].trim())});
                }
            }
            if (settings.getRooftopArrayTilt() > 0)
                rooftopIrradiance = new RooftopIrradiance(settings.getSolarStationLatitude(), settings.getSolarStationLongitude(),
                        settings.getRooftopArrayTilt(), settings.getRooftopArrayAzimuth(), 0.2, 10.0);
            if (settings.getGeneratorParametersFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getGeneratorParametersFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 4);
                    generatorParameters.add(new String[]{f[0].trim(), f[1].trim(), f[2].trim()});
                }
            }
            if (settings.getTariffComponentsFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getTariffComponentsFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 6);
                    tariffComponents.put(Integer.parseInt(f[0].trim()), new double[]{Double.parseDouble(f[1].trim()),
                            Double.parseDouble(f[2].trim()), Double.parseDouble(f[3].trim()), Double.parseDouble(f[4].trim())});
                }
            }
            if (settings.getCpiFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getCpiFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 3);
                    cpi.put(Integer.parseInt(f[0].trim()), Double.parseDouble(f[1].trim()));
                }
            }
            for (String fuelFile : settings.getFuelPriceFiles()) {
                TreeMap<Integer, Double> series = new TreeMap<>();
                List<String> lines = Files.readAllLines(settingsFolder.resolve(fuelFile));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 3);
                    series.put(Integer.parseInt(f[0].trim()), Double.parseDouble(f[1].trim()));
                }
                fuelPrices.put(fuelFile, series);
            }
            if (settings.getInterconnectorFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getInterconnectorFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 11);
                    String fuel = f[5].trim();
                    if (!fuel.isEmpty() && !fuel.matches("[-0-9.eE+]+") && !fuelPrices.containsKey(fuel)) {
                        TreeMap<Integer, Double> series = new TreeMap<>();
                        List<String> fl = Files.readAllLines(settingsFolder.resolve(fuel));
                        for (String l : fl.subList(1, fl.size())) {
                            if (l.isBlank() || l.startsWith("#")) continue;
                            String[] g = l.split(",", 3);
                            series.put(Integer.parseInt(g[0].trim()), Double.parseDouble(g[1].trim()));
                        }
                        fuelPrices.put(fuel, series);
                    }
                    interconnectors.add(new String[]{f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(), f[4].trim(),
                            fuel, f[6].trim(), f[7].trim(), f[8].trim(), f[9].trim()});
                }
            }
            if (settings.getShortfallPriceFile() != null) {
                List<String> lines = Files.readAllLines(settingsFolder.resolve(settings.getShortfallPriceFile()));
                for (String line : lines.subList(1, lines.size())) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] f = line.split(",", 3);
                    shortfallPriceMWh.put(f[0].trim(), Double.parseDouble(f[1].trim()));
                }
            }



            /**
             * Public Policies
             */

            // goes from 0.00 to 1.0, represents percentage of monthly uptake and uses a normal gaussian distribution to simulate the uptake
            // for example, 0.01 represents 1% per month, around 12% a year
            policies.setEndConsumerTariffs(settings.getEndConsumerTariff());

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problems reading the YAML settings in "
                    + Paths.get("").toAbsolutePath().normalize().resolve("simulationSettings")
                    + " - run the simulation from the project root", e);
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

    //CPI(year) / CPI(costPriceYear): turns real cost settings into the year's dollars (1 without cpiFile)
    public double getCostFactor(int year) {
        if (cpi.isEmpty() || settings.getCostPriceYear() <= 0) return 1.0;
        return cpiAt(year) / cpiAt(settings.getCostPriceYear());
    }

    //Last year in which cost settings follow the CPI file (later years hold its last value); MAX_VALUE without cpiFile
    public int getCostPriceLastYear() {
        if (cpi.isEmpty() || settings.getCostPriceYear() <= 0) return Integer.MAX_VALUE;
        return cpi.lastKey();
    }

    private double cpiAt(int year) {
        Map.Entry<Integer, Double> e = cpi.floorEntry(year);
        return (e != null ? e : cpi.firstEntry()).getValue();
    }

    //Apply the station parameters whose name prefix matches (later rows win)
    public void applyGeneratorParameters(Generator g) {
        for (String[] p : generatorParameters) {
            if (!g.getName().startsWith(p[0])) continue;
            double v = Double.parseDouble(p[2]);
            switch (p[1]) {
                case "heatRateGJperMWh": g.heatRateGJperMWh = v; break;
                case "variableOMMWh": g.variableOMMWh = v; break;
                case "fuelPriceGJ": g.fuelPriceGJ = v; break;
                case "forcedOutageRate": g.forcedOutageRate = v; break;
                case "outageHours": g.outageHours = v; break;
                case "availabilityFactor": g.availabilityFactor = v; break;
                default: throw new IllegalArgumentException("Unknown generator parameter " + p[1]);
            }
        }
    }

    //Fuel price in $/GJ for a year: the file's value, or its nearest year outside the range it covers
    public double getFuelPriceGJ(String file, int year) {
        TreeMap<Integer, Double> series = fuelPrices.get(file);
        Map.Entry<Integer, Double> e = series.floorEntry(year);
        return (e != null ? e : series.firstEntry()).getValue();
    }

    public boolean hasInterconnectors() { return !interconnectors.isEmpty(); }

    //Import blocks open on this date, each offering at the neighbour's short-run cost or a water value (see
    //Settings.interconnectorFile). lastMonthPriceMWh is last month's mean primary price
    public List<core.Relationships.SpotMarket.ImportOffer> getImportOffers(Date day, int year, double lastMonthPriceMWh) {
        List<core.Relationships.SpotMarket.ImportOffer> offers = new ArrayList<>();
        String d = new SimpleDateFormat("yyyy-MM-dd").format(day);
        double costFactor = getCostFactor(year), carbon = getCarbonPriceTonne(day);
        for (String[] r : interconnectors) {
            if (d.compareTo(r[1]) < 0 || (!r[2].isEmpty() && d.compareTo(r[2]) > 0)) continue;
            double mw = Double.parseDouble(r[3]);
            double waterValue = r[8].isEmpty() ? -1 : Double.parseDouble(r[8]);
            double loss = r[9].isEmpty() ? 1.0 : Double.parseDouble(r[9]);
            double price;
            if (waterValue >= 0) price = waterValue * lastMonthPriceMWh;
            else {
                double fuel = r[5].isEmpty() ? 0.0 : r[5].matches("[-0-9.eE+]+")
                        ? Double.parseDouble(r[5]) * costFactor : getFuelPriceGJ(r[5], year);
                price = Double.parseDouble(r[4]) * fuel + Double.parseDouble(r[6]) * costFactor
                        + Double.parseDouble(r[7]) * carbon;
            }
            offers.add(new core.Relationships.SpotMarket.ImportOffer(r[0], price * loss, mw));
        }
        return offers;
    }

    //Market price cap on this date, or -1 without shortfallPriceFile (or before its first row)
    public double getShortfallPriceMWh(Date time) {
        Map.Entry<String, Double> e = shortfallPriceMWh.floorEntry(new SimpleDateFormat("yyyy-MM-dd").format(time));
        return e == null ? -1.0 : e.getValue();
    }

    //Carbon price in $/t CO2-e for the month of this date (0 outside the file)
    public double getCarbonPriceTonne(Date time) {
        if (carbonPriceTonne.isEmpty()) return 0.0;
        return carbonPriceTonne.getOrDefault(new SimpleDateFormat("yyyy-MM").format(time), 0.0);
    }

    //Portfolio of a station: the first name prefix in portfolioFile that matches, otherwise the station itself
    public String getPortfolio(String stationName) {
        return portfolioOf.computeIfAbsent(stationName, n -> {
            for (String[] p : portfolios) if (n.startsWith(p[0])) return p[1];
            return n;
        });
    }

    //MW of net exports to add to wholesale demand in this half-hour (0 for months the file does not cover)
    public double getNetExportsMW(Date time) {
        if (netExportsMW.isEmpty()) return 0.0;
        Double monthly = netExportsMW.get(new SimpleDateFormat("yyyy-MM").format(time));
        if (monthly == null) return 0.0;
        return monthly + getNetExportsShapeMW(time);
    }

    //Net-export shape (deviation from the month's mean, MW) for the half-hour ending at time; 0 without the file.
    //Model times are period ends (AEMO SETTLEMENTDATE); the shape file is indexed by interval start
    public double getNetExportsShapeMW(Date time) {
        if (netExportsShapeMW == null) return 0.0;
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        c.add(Calendar.MINUTE, -30);
        return netExportsShapeMW[c.get(Calendar.MONTH) + 1][c.get(Calendar.HOUR_OF_DAY) * 2 + c.get(Calendar.MINUTE) / 30];
    }

    //Months before reportFrom run as spin-up and are left out of the output CSVs
    public boolean isReported(Date date) {
        return reportFromDate == null || !date.before(reportFromDate);
    }

    //A year is reported from the year that contains reportFrom
    public boolean isReportedYear(String year) {
        return reportFromDate == null || Integer.parseInt(year) >= reportFromDate.getYear() + 1900;
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

    public boolean hasTariffComponents() { return !tariffComponents.isEmpty(); }

    //Row of the bills of `year`; years after the last row hold it, years before the first take the first
    public double[] getTariffComponents(int year) {
        Map.Entry<Integer, double[]> e = tariffComponents.floorEntry(year);
        return (e != null ? e : tariffComponents.firstEntry()).getValue();
    }

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

    //Irradiance (kW/m2) a rooftop array receives in the half-hour stamped t: on its plane when rooftopArrayTilt is set,
    //the horizontal irradiance otherwise; 0 where the register has no value
    public float getRooftopExposure(Date t) {
        float ghi = getSolarExposure(t);
        if (rooftopIrradiance == null || ghi <= 0f) return ghi;
        if (rooftopExposureTime == null || t.getTime() != rooftopExposureTime.getTime()) {
            rooftopExposureValue = (float) rooftopIrradiance.planeOfArray(ghi, t);
            rooftopExposureTime = new Date(t.getTime());
        }
        return rooftopExposureValue;
    }

    //Horizontal irradiance (kW/m2) of the half-hour stamped t, 0 where the register has none. The key is looked up as a
    //java.util.Date: the register holds java.sql.Timestamp keys (database rows) and Date keys (filled and forecast
    //half-hours), and Timestamp.equals(Date) is false, so a Timestamp argument would miss the Date keys
    public float getSolarExposure(Date t) {
        Float v = halfhour_solar_exposure.get(t.getClass() == Date.class ? t : new Date(t.getTime()));
        return v == null ? 0f : v;
    }

    public List<double[]> getSolarInstallationsOverride() { return solarInstallationsOverride; }

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
        //Deferred demand calibration (solarShapeInDemand / forecastFromCalibratedDemand) needs the SPMs created above
        LoadData.finishDemandCalibration(this);

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


