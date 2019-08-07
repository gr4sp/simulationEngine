package core;

import core.Policies.EndConsumerTariff;
import core.Policies.SimPolicies;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.System.exit;


public class Gr4spSim extends SimState {
    private static final long serialVersionUID = 1;


    public Continuous2D layout;
    HashMap<Integer, Vector<Spm>> spm_register;
    HashMap<Integer, Vector<Generator>> gen_register;
    HashMap<Integer, Vector<NetworkAssets>> network_register;
    HashMap<Integer, Actor> actor_register;
    HashMap<Integer, Arena> arena_register;

    //Consumption per Domestic Costumer
    HashMap<Date, Double> monthly_consumption_register;
    //Total Generation GWh per month
    HashMap<Date, Generation> monthly_generation_register;

    //Number of costumers
    HashMap<Date, Integer> monthly_domestic_consumers_register;
    //CPI conversion
    HashMap<Date, Float> cpi_conversion;


    ArrayList<ConsumptionActor> consumptionActors = new ArrayList<ConsumptionActor>(); //this is the array to be traverse to get the total consumption
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
        monthly_consumption_register = new HashMap<>();
        monthly_generation_register = new HashMap<>();
        monthly_domestic_consumers_register = new HashMap<>();
        cpi_conversion = new HashMap<>();

        layout = new Continuous2D(10.0, 600.0, 600.0);
        policies = new SimPolicies();
        simulParametres();

    }

    private void simulParametres() {

        /**
         * Simulation Date Range
         */
        String startDate = "1926-01-01";
        String endDate = "2019-01-01";

        /**
         * Population and scale
         */

        //0.9/100 of Vic popultaion
        //Only Melbourne cityareaCode
        //areaCode = "M1";
        //populationPercentageAreacCode = 0.009;

        //Include regional with all framework indicatorss
        areaCode = "VIC";
        populationPercentageAreacCode = 1;

        //Max number of Dwellings represented in a single ConsumerUnit, attached to a SPM
        //It is a measure to control aggregation of dwellings per SPM
        //New ConsumerUnits are created according to monthly population growth in the simulation Data monthly_domestic_consumers_register
        //maxHouseholdsPerConsumerUnit = 1000;

        //Adds all households as a single actor connected to a single SPM
        maxHouseholdsPerConsumerUnit = Integer.MAX_VALUE;

        //Percentage of Total Consumption Historic Data that goes into domestic use
        domesticConsumptionPercentage = 0.3;

        /**
         * Public Policies
         */

        policies.setEndConsumerTariffs(EndConsumerTariff.MIN);


        //Save start and end date in the simulator state, which is this classs Gr4spSim
        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
            this.startSimDate = stringToDate.parse(startDate);
            this.simCalendar.setTime(this.startSimDate);
            this.endSimDate = stringToDate.parse(endDate);

        } catch (ParseException e) {
            System.err.println("Cannot parse Start Date: " + e.toString());
        }
    }

    /**
     * Getters and Setters
     */
    public Calendar getSimCalendar() {
        return simCalendar;
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

    /**
     * select all rows in the Generation Technologies table
     */
    public ArrayList<Generator> selectGenTech(String idgen) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; // url for sqlite "jdbc:sqlite:Spm_archetypes.db";


        SimpleDateFormat DateToString = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT id, powerstation, Owner, installedcapacitymw, technologytype, fueltype, dispatchtype, locationcoordinates, startdate, enddate, emissionsfactor" +
                " FROM generationassets WHERE id = '" + idgen + "' AND startdate <= '" + DateToString.format(this.endSimDate) + "'" +
                " AND enddate > '" + DateToString.format(this.startSimDate) + "';";

        ArrayList<Generator> gens = new ArrayList<Generator>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
              /*  System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("PowerStation") + "\t" +
                        rs.getString("Owner") + "\t" +
                        rs.getDouble("InstalledCapacityMW") + "\t" +
                        rs.getString("TechnologyType") + "\t" +
                        rs.getString("FuelType") + "\t" +
                        rs.getString("DispatchType") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("enddate") + "\t" +
                        rs.getString("locationcoordinates") + "\t" +
                        rs.getString("emissionsfactor"));
*/

                Generator gen = new Generator(rs.getInt("id"),
                        rs.getString("PowerStation"),
                        rs.getString("Owner"),
                        rs.getDouble("InstalledCapacityMW"),
                        rs.getString("TechnologyType"),
                        rs.getString("FuelType"),
                        rs.getString("DispatchType"),
                        rs.getString("locationCoordinates"),
                        rs.getDate("startdate"),
                        rs.getDate("enddate"),
                        rs.getFloat("emissionsfactor")
                );

                int idGen = rs.getInt("id");
                //Get from Map the vector of GENERATORS with key = idGen, and add the new Generator to the vector
                if (!gen_register.containsKey(idGen))
                    gen_register.put(idGen, new Vector<>());

                gen_register.get(idGen).add(gen);

                numGenerators += 1;

                //Add gen to ArrayList<Generator>. This will be used in the constructor of SPM
                gens.add(gen);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return gens;
    }


    public ArrayList<Storage> selectStorage(String idst) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT storage_id, storage_name, storagetype, storageoutputcap , storagecapacity, ownership," +
                " storage_cyclelife, storage_costrange  FROM storage WHERE storage_id = '" + idst + "' ";
        ArrayList<Storage> strs = new ArrayList<Storage>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*System.out.println("\t" + rs.getInt("storage_id") + "\t" +
                        rs.getString("storage_name") + "\t" +
                        rs.getInt("storageType") + "\t" +
                        rs.getDouble("storageOutputCap") + "\t" +
                        rs.getDouble("storageCapacity") + "\t" +
                        rs.getInt("Ownership") + "\t" +
                        rs.getDouble("storage_cycleLife") + "\t" +
                        rs.getDouble("storage_costRange"));
*/
                Storage str = new Storage(rs.getInt("storage_id"),
                        rs.getString("storage_name"),
                        rs.getInt("StorageType"),
                        rs.getDouble("storageOutputCap"),
                        rs.getDouble("storageCapacity"),
                        rs.getInt("Ownership"),
                        rs.getDouble("storage_cycleLife"),
                        rs.getDouble("storage_costRange"));

                numStorage += 1;
                strs.add(str);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return strs;
    }


    //Select and create the type of energy grid
    public ArrayList<NetworkAssets> selectNetwork(String id) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT networkassets.id as netid, networkassettype.type as type, networkassettype.subtype as subtype, " +
                "networkassettype.grid as grid, assetname, grid_node_name, location_mb, gridlosses, gridvoltage, owner, startdate, enddate " +
                " FROM networkassets " +
                "JOIN networkassettype ON networkassets.assettype = networkassettype.id " +
                "and  networkassets.id = '" + id + "' ";
        ArrayList<NetworkAssets> nets = new ArrayList<NetworkAssets>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
               /* System.out.println("\t" + rs.getInt("netId") + "\t" +
                        rs.getString("type") + "\t" +
                        rs.getString("subtype") + "\t" +
                        rs.getString("grid") + "\t" +
                        rs.getString("assetName") + "\t" +
                        rs.getString("grid_node_name") + "\t" +
                        rs.getString("location_MB") + "\t" +
                        rs.getDouble("gridLosses") + "\t" +
                        rs.getInt("gridVoltage") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("enddate") + "\t" +
                        rs.getString("owner"));
*/
                int idNet = rs.getInt("netId");
                NetworkAssets grid = new NetworkAssets(idNet,
                        rs.getString("type"),
                        rs.getString("subtype"),
                        rs.getString("grid"),
                        rs.getString("assetName"),
                        rs.getString("grid_node_name"),
                        rs.getString("location_MB"),
                        rs.getDouble("gridLosses"),
                        rs.getInt("gridVoltage"),
                        rs.getString("owner"),
                        rs.getDate("startdate"),
                        rs.getDate("enddate"));
                nets.add(grid);

                //Get from Map the vector of Networks with key = idNet, and add the new Network to the vector
                if (!network_register.containsKey(idNet))
                    network_register.put(idNet, new Vector<>());

                network_register.get(idNet).add(grid);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return nets;
    }


    //creation of random interface or connection point. This interface changes as well depending on the scale of study. A house with
    //own gene4ration off the grid will have the same house (smart metered or not) as connection point, a neighborhood can have one or more feeders and sub-stations,
    //it is basically the closest point where supply meets demand without going through any significant extra technological "treatment"
    //TODO knowledge and energy hubs within prosumer communities, where to include them?
    public ArrayList<ConnectionPoint> selectConnectionPoint(String name) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT cpoint_id, cpoint_name, cpoint_type, distancetodemand, cpoint_locationcode, cpoint_owner, ownership FROM connectionpoint WHERE cpoint_name = '" + name + "' ";
        ArrayList<ConnectionPoint> cpoints = new ArrayList<ConnectionPoint>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
               /* System.out.println("\t" + rs.getInt("cpoint_id") + "\t" +
                        rs.getString("cpoint_name") + "\t" +
                        rs.getInt("CPoint_Type") + "\t" +
                        rs.getDouble("distanceToDemand") + "\t" +
                        rs.getInt("cpoint_locationCode") + "\t" +
                        rs.getString("cpoint_owner") + "\t" +
                        rs.getInt("Ownership"));*/

                ConnectionPoint cpoint = new ConnectionPoint(rs.getInt("cpoint_id"),
                        rs.getString("cpoint_name"),
                        rs.getInt("CPoint_Type"),
                        rs.getDouble("distanceToDemand"),
                        rs.getInt("cpoint_locationCode"),
                        rs.getString("cpoint_owner"),
                        rs.getInt("Ownership"));

                numcpoints += 1;
                cpoints.add(cpoint);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cpoints;
    }


    /*//creation of societal organisations attached to the SPM

    SocialOrgStructure createSocialOrgs() {
        int OrgId = numOrg;
        String[] OrgType = {"Private", "Public", "Partnertship"};
        String[] subOrgType = {"Individual", "Cooperative", "Governmental", "Private company"};
        String[] OrgEntity = {"Non-for profit", "For profit", "Hybrid"};
        int numPeople = 20;
        String[] OrgActivity = {"Generator", "Network operator", "Retailer"};
        boolean liability = true;
        boolean taXemp = false;

        //generate random number to choose the type, subtype, entity and activity
        int idOrgType = random.nextInt(OrgType.length);
        int idOrgSub = random.nextInt(subOrgType.length);
        int idEntity = random.nextInt(OrgEntity.length);
        int idOrgActiv = random.nextInt(OrgActivity.length);

        SocialOrgStructure SO = new SocialOrgStructure(numOrg, OrgType[idOrgType], subOrgType[idOrgSub],
                OrgEntity[idEntity], numPeople, liability, taXemp, OrgActivity[idOrgActiv]);
        numOrg += 1;
        return SO;
    }

    */

    //Functions to convert readings from text in DB to Actor type, etc.
    public ActorAssetRelationshipType stringToActorAssetTypeRelType(String actorRelType) {

        if (actorRelType.equalsIgnoreCase("OWN"))
            return ActorAssetRelationshipType.OWN;
        if (actorRelType.equalsIgnoreCase("LEASE"))
            return ActorAssetRelationshipType.LEASE;
        if (actorRelType.equalsIgnoreCase("USE"))
            return ActorAssetRelationshipType.USE;


        return ActorAssetRelationshipType.OTHER;
    }

    public ActorActorRelationshipType stringToActorActorTypeRelType(String actorRelType) {


        if (actorRelType.equalsIgnoreCase("BILLING"))
            return ActorActorRelationshipType.BILLING;
        if (actorRelType.equalsIgnoreCase("OTC"))
            return ActorActorRelationshipType.OTC;
        if (actorRelType.equalsIgnoreCase("ETF"))
            return ActorActorRelationshipType.ETF;
        if (actorRelType.equalsIgnoreCase("SPOT"))
            return ActorActorRelationshipType.SPOT;
        if (actorRelType.equalsIgnoreCase("ACCESS_FEE"))
            return ActorActorRelationshipType.ACCESS_FEE;
        if (actorRelType.equalsIgnoreCase("COMMISSION_FEE"))
            return ActorActorRelationshipType.COMMISSION_FEE;
        if (actorRelType.equalsIgnoreCase("P2P"))
            return ActorActorRelationshipType.P2P;
        if (actorRelType.equalsIgnoreCase("OWNS"))
            return ActorActorRelationshipType.OWNS;

        return ActorActorRelationshipType.OTHER;
    }

    public ActorType stringToActorType(String actorType) {
        if (actorType == null)
            return ActorType.TBD;

        if (actorType.equalsIgnoreCase("RETAILER"))
            return ActorType.MARKETLOADSCHD;
        if (actorType.equalsIgnoreCase("MARKETGENSCHD"))
            return ActorType.MARKETGENSCHD;
        if (actorType.equalsIgnoreCase("MARKETNETWORKPROV"))
            return ActorType.MARKETNETWORKPROV;
        if (actorType.equalsIgnoreCase("HDCONSUMER"))
            return ActorType.HDCONSUMER;
        if (actorType.equalsIgnoreCase("REGULATOR"))
            return ActorType.REGULATOR;
        if (actorType.equalsIgnoreCase("IMPLEMENT"))
            return ActorType.IMPLEMENT;
        if (actorType.equalsIgnoreCase("GOVAUTHORITY"))
            return ActorType.GOVAUTHORITY;
        if (actorType.equalsIgnoreCase("INVESTOR"))
            return ActorType.INVESTOR;
        if (actorType.equalsIgnoreCase("BROKER"))
            return ActorType.BROKER;
        if (actorType.equalsIgnoreCase("SPECULATOR"))
            return ActorType.SPECULATOR;
        else {
            return ActorType.OTHER;
        }
    }

    public BusinessStructure stringToBusinessStructure(String businessStructure) {
        if (businessStructure == null)
            return BusinessStructure.TBD;

        if (businessStructure.equalsIgnoreCase("INCORPASSOCIATION"))
            return BusinessStructure.INCORPASSOCIATION;
        if (businessStructure.equalsIgnoreCase("PTYLTD"))
            return BusinessStructure.PTYLTD;
        if (businessStructure.equalsIgnoreCase("PUBLICCOMP"))
            return BusinessStructure.PUBLICCOMP;
        if (businessStructure.equalsIgnoreCase("TRUST"))
            return BusinessStructure.TRUST;
        if (businessStructure.equalsIgnoreCase("SOLETRADER"))
            return BusinessStructure.SOLETRADER;
        if (businessStructure.equalsIgnoreCase("LIMITEDPARTNERSHIP"))
            return BusinessStructure.LIMITEDPARTNERSHIP;
        if (businessStructure.equalsIgnoreCase("COOPERATIVE"))
            return BusinessStructure.COOPERATIVE;
        if (businessStructure.equalsIgnoreCase("INFORMAL"))
            return BusinessStructure.INFORMAL;
        if (businessStructure.equalsIgnoreCase("GRASSROOTS"))
            return BusinessStructure.GRASSROOTS;
        if (businessStructure.equalsIgnoreCase("NOTFORPROF"))
            return BusinessStructure.NOTFORPROF;
        if (businessStructure.equalsIgnoreCase("STATE"))
            return BusinessStructure.STATE;
        else {
            return BusinessStructure.OTHER;
        }
    }


    public GovRole stringToGovRole(String govRole) {
        if (govRole == null)
            return GovRole.TBD;

        if (govRole.equalsIgnoreCase("RULEFOLLOW"))
            return GovRole.RULEFOLLOW;
        if (govRole.equalsIgnoreCase("RULEMAKER"))
            return GovRole.RULEMAKER;
        if (govRole.equalsIgnoreCase("RULEIMPLEMENT"))
            return GovRole.RULEIMPLEMENT;
        else {

            return GovRole.OTHER;
        }
    }

    public OwnershipModel stringToOwnershipModel(String ownership) {
        if (ownership == null)
            return OwnershipModel.TBD;

        if (ownership.equalsIgnoreCase("SHARES"))
            return OwnershipModel.SHARES;
        if (ownership.equalsIgnoreCase("DONATION_BASED"))
            return OwnershipModel.DONATION_BASED;
        if (ownership.equalsIgnoreCase("COMMUNITY_INVEST"))
            return OwnershipModel.COMMUNITY_INVEST;
        if (ownership.equalsIgnoreCase("COMM_DEV_PARTNER"))
            return OwnershipModel.COMM_DEV_PARTNER;
        if (ownership.equalsIgnoreCase("COMM_COUNCIL_PARTNER"))
            return OwnershipModel.COMM_COUNCIL_PARTNER;
        if (ownership.equalsIgnoreCase("MULTIHOUSEHOLD"))
            return OwnershipModel.MULTIHOUSEHOLD;
        if (ownership.equalsIgnoreCase("INDIVIDUAL"))
            return OwnershipModel.INDIVIDUAL;
        if (ownership.equalsIgnoreCase("STATE"))
            return OwnershipModel.STATE;
        else {
            return OwnershipModel.OTHER;
        }
    }

    public void
    selectGenerationHistoricData(String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Consumption per month
         * */
        String sql = "SELECT date, volumeweightedprice_dollarmwh, temperaturec, solar_roofpv_dollargwh, solar_roofpv_gwh," +
                "solar_utility_dollargwh, solar_utility_gwh, wind_dollargwh, wind_gwh, hydro_dollargwh," +
                "hydro_gwh, battery_disch_dollargwh, battery_disch_gwh, gas_ocgt_dollargwh, gas_ocgt_gwh," +
                "gas_steam_dollargwh, gas_steam_gwh, browncoal_dollargwh, browncoal_gwh, imports_dollargwh," +
                "imports_gwh, exports_dollargwh, exports_gwh, total_gen_gwh" +
                " FROM  generation_demand_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";


        //volumeweightedprice_dollarMWh, temperaturec, solar_roofpv_dollargwh, solar_roofpv_gwh, solar_utility_dollargwh, solar_utility_gwh, wind_dollargwh, wind_gwh, hydro_dollargwh, hydro_gwh, battery_disch_dollargwh, battery_disch_gwh, gas_ocgt_dollargwh, gas_ocgt_gwh,gas_steam_dollargwh, gas_steam_gwh, browncoal_dollargwh, browncoal_gwh, imports_dollargwh, imports_gwh, exports_dollargwh, exports_gwh, total_gen_gwh
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {


 /*               System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("temperaturec") + "\t" +
                        rs.getString("total_gen_gwh"));
*/

                Date d = rs.getDate("date");

                Generation genData = new Generation(
                        rs.getDate("date"), rs.getFloat("volumeweightedprice_dollarmwh"), rs.getFloat("temperaturec"),
                        rs.getFloat("solar_roofpv_dollargwh"), rs.getFloat("solar_roofpv_gwh"), rs.getFloat("solar_utility_dollargwh"),
                        rs.getFloat("solar_utility_gwh"), rs.getFloat("wind_dollargwh"), rs.getFloat("wind_gwh"), rs.getFloat("hydro_dollargwh"),
                        rs.getFloat("hydro_gwh"), rs.getFloat("battery_disch_dollargwh"), rs.getFloat("battery_disch_gwh"), rs.getFloat("gas_ocgt_dollargwh"),
                        rs.getFloat("gas_ocgt_gwh"), rs.getFloat("gas_steam_dollargwh"), rs.getFloat("gas_steam_gwh"), rs.getFloat("browncoal_dollargwh"),
                        rs.getFloat("browncoal_gwh"), rs.getFloat("imports_dollargwh"), rs.getFloat("imports_gwh"), rs.getFloat("exports_dollargwh"),
                        rs.getFloat("exports_gwh"), rs.getFloat("total_gen_gwh")
                );

                //If monthly gen datapoint doesn't exist, create it
                if (!monthly_generation_register.containsKey(d)) {
                    monthly_generation_register.put(d, genData);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void
    selectConsumption(String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Consumption per month
         * */
        String sql = "SELECT date, total_demand_gwh" +
                " FROM  generation_demand_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("total_demand_gwh"));


                Date d = rs.getDate("date");
                Double gwh = rs.getDouble("total_demand_gwh");

                //If monthly consumption doesn't exist, create it
                if (!monthly_consumption_register.containsKey(d)) {
                    monthly_consumption_register.put(d, gwh);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        /**
         * Load domestic consumers
         * */
        sql = "SELECT date, domesticconsumers" +
                " FROM  domestic_consumers WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
  /*              System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("domesticconsumers"));
*/

                Date d = rs.getDate("date");
                int consumers = rs.getInt("domesticconsumers");

                //If arena doesn't exist, create it
                if (!monthly_domestic_consumers_register.containsKey(d)) {
                    monthly_domestic_consumers_register.put(d, consumers);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        /**
         * Compute linear Monthly growth from Yearly data
         * */
        HashMap<Date, Integer> newMonthlyData = new HashMap<>();
        for (Map.Entry<Date, Integer> entry : monthly_domestic_consumers_register.entrySet()) {
            Date currentDate = entry.getKey();
            Integer consumers = entry.getValue();

            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);

            //Get info about next year
            c.add(Calendar.YEAR, 1);
            Date nextYear = c.getTime();
            Integer consumersNextYear = monthly_domestic_consumers_register.get(nextYear);

            if (consumers == null || consumersNextYear == null) continue;

            Integer increment = (consumersNextYear - consumers) / 12;

            //Set calendar to current Date again
            c.setTime(currentDate);

            //Increment date by 1 month, and 1 increment of population, until we filled the 12 months
            for (int i = 0; i < 11; i++) {

                //add 1 month
                c.add(Calendar.MONTH, 1);
                Date newMonth = c.getTime();

                //add new consumers
                consumers += increment;

                //store the new data point
                newMonthlyData.put(newMonth, consumers);

            }
        }

        //Add new monthly data into our domestic consumer register
        for (Map.Entry<Date, Integer> entry : newMonthlyData.entrySet()) {
            monthly_domestic_consumers_register.put(entry.getKey(), entry.getValue());
        }

        /**
         * Update Total Consumption with monthly domestic consumers
         * and Percentage of domestic usage
         * */

        for (Date month : monthly_consumption_register.keySet()) {

            double gwh = monthly_consumption_register.get(month);
            Integer consumers = monthly_domestic_consumers_register.get(month);

            if (consumers == null) continue;

            //convert to kwh and only domestic demand
            double newkwh = (gwh * 1000000.0 * domesticConsumptionPercentage) / (double) consumers;
            //to check total demand in KWh
            //double newkwh = (gwh * 1000000);

            monthly_consumption_register.put(month, newkwh);
        }
    }

    /**
     * select all rows in the Actor table
     */
    public void
    selectArenaAndContracts(String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");


        String sql = "SELECT arenas.name as arenaname, arenas.type as arenatype, seller, buyer, assetused, pricemwh, startdate, enddate, capacitycontracted, arenaid" +
                " FROM contracts, arenas WHERE " +
                "contracts.arenaid = arenas.id AND contracts.startdate <= '" + endDate + "'" +
                " AND contracts.enddate > '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("arenaid") + "\t" +
                        rs.getString("arenaname") + "\t" +
                        rs.getString("arenatype") + "\t" +
                        rs.getString("seller") + "\t" +
                        rs.getString("buyer") + "\t" +
                        rs.getString("pricemwh") + "\t" +
                        rs.getString("assetused") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("enddate") + "\t" +
                        rs.getString("capacitycontracted"));


                int arenaId = rs.getInt("arenaid");

                //If arena doesn't exist, create it
                if (!arena_register.containsKey(arenaId)) {
                    Arena arena = new Arena(arenaId, rs.getString("arenaname"), rs.getString("arenatype"));
                    arena_register.put(arenaId, arena);
                }

                Arena arena = arena_register.get(arenaId);

                //Actor seller, Actor buyer, Asset assetUsed, float priceMWh, Date start, Date end, float capacityContracted

                Contract contract = new Contract("",
                        rs.getInt("seller"),
                        rs.getInt("buyer"),
                        rs.getInt("assetused"),
                        rs.getFloat("pricemwh"),
                        (float) 0.0,
                        rs.getDate("startdate"),
                        rs.getDate("enddate"),
                        rs.getFloat("capacitycontracted"));


                //Add contract to arena
                if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
                    arena.getBilateral().add(contract);
                }
                if (arena.getType().equalsIgnoreCase("FiTs")) {
                    arena.getFiTs().add(contract);
                }

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void
    selectArena() {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");


        String sql = "SELECT arenas.name as arenaname, arenas.type as arenatype, id" +
                " FROM  arenas ;";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("arenaname") + "\t" +
                        rs.getString("arenatype"));


                int arenaId = rs.getInt("id");

                //If arena doesn't exist, create it
                if (!arena_register.containsKey(arenaId)) {
                    Arena arena = new Arena(arenaId, rs.getString("arenaname"), rs.getString("arenatype"));
                    arena_register.put(arenaId, arena);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

   /* public void
    selectDemandTemp() {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM");


        String sql = "SELECT date, demandmw" +
                " FROM  totaldemandvictoria ;";

        HashMap<String,ArrayList<Double>> dataset = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                Date date = rs.getDate("date");
                String sd = stringToDate.format(date);

                if (!dataset.containsKey(sd)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    dataset.put(sd, yearData);
                    System.out.println(sd);
                }

                dataset.get(sd).add( rs.getDouble("demandmw") );


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


        for(String sd : dataset.keySet()) {
            ArrayList<Double> yearData = dataset.get(sd);

            Double totalDemand = 0.0;
            Double sizeData = (double) yearData.size();
            for(Double mw : yearData){
                totalDemand +=mw;
            }
            //GW
            totalDemand /= 1000.0;
            //GWh
            totalDemand /= (2.0);

            System.out.println(sd+", "+totalDemand);
        }
        exit(0);
    }*/

    public void
    selectTariffs(String startDate, String endDate, String areaCode) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT date, conversion_variable" +
                " FROM cpi_conversion;";

        //Loading CPI Conversion data
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getFloat("conversion_variable"));


                Date date = rs.getDate("date");
                Float conversion_rate = rs.getFloat("conversion_variable");

                cpi_conversion.put(date, conversion_rate);


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


        sql = "SELECT id, casestudy_area, date, tariff_name, average_dckwh, chargeperroom_dmonth, secvij_gdgr_aud_quarter, standingcharge_aud_year" +
                " FROM tariffdata WHERE" +
                " casestudy_area = '" + areaCode + "'" +
                " AND date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        //If areaCode is at state scale, do not filter tariffs by smaller areacodes, take all tariffs available
        //Agent will select a tariff according to simulation policy. For more details See SimulParameters()
        if(areaCode == "VIC")
            sql = "SELECT id, casestudy_area, date, tariff_name, average_dckwh, chargeperroom_dmonth, secvij_gdgr_aud_quarter, standingcharge_aud_year" +
                    " FROM tariffdata WHERE" +
                    " date <= '" + endDate + "'" +
                    " AND date >= '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("casestudy_area") + "\t" +
                        rs.getString("date") + "\t" +
                        rs.getString("tariff_name") + "\t" +
                        rs.getString("average_dckwh") + "\t" +
                        rs.getString("chargeperroom_dmonth") + "\t" +
                        rs.getString("secvij_gdgr_aud_quarter") + "\t" +
                        rs.getString("standingcharge_aud_year"));

                //add tariffs to the Retail arena
                int arenaId = 2;

                Arena arena = arena_register.get(arenaId);

                Date cStartDate = rs.getDate("date");

                //Compute end Date 1 year after
                Calendar cEndDate = Calendar.getInstance();
                try {
                    Date date = stringToDate.parse(rs.getString("date"));
                    cEndDate.setTime(date);
                } catch (ParseException e) {
                    System.err.println("Cannot parse Start Date: " + e.toString());
                }
                cEndDate.add(Calendar.YEAR, 1);

                //Get CPI conversion
                float conversion_rate = cpi_conversion.get(cStartDate);

                //Compute serviceFee
                Float chargeperroom_dmonth = rs.getFloat("chargeperroom_dmonth") * conversion_rate;
                Float secvij_gdgr_aud_quarter = rs.getFloat("secvij_gdgr_aud_quarter") * conversion_rate;
                Float standingcharge_aud_year = rs.getFloat("standingcharge_aud_year") * conversion_rate;

                float serviceFee = 0;

                if (chargeperroom_dmonth != null)
                    serviceFee += chargeperroom_dmonth;
                if (secvij_gdgr_aud_quarter != null)
                    serviceFee += secvij_gdgr_aud_quarter / 3.0;
                if (standingcharge_aud_year != null)
                    serviceFee += standingcharge_aud_year / 12.0;

                //Actor seller, Actor buyer, Asset assetUsed, float priceMWh, Date start, Date end, float capacityContracted

                /**
                 * NEED TO USE FORMULAT RBA TO UPDATE PRICES!
                 * */

                Contract contract = new Contract(
                        rs.getString("tariff_name"),
                        0,
                        arena.EndConsumer,
                        0,
                        rs.getFloat("average_dckwh") * conversion_rate,
                        serviceFee,
                        cStartDate,
                        cEndDate.getTime(),
                        0);


                //Add contract to arena
                if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
                    arena.getBilateral().add(contract);
                }
                if (arena.getType().equalsIgnoreCase("FiTs")) {
                    arena.getFiTs().add(contract);
                }

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * select all rows in the Actor table
     */
    public void
    selectActors(String tableName, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT " + tableName + ".id as id, actortype.name as typename, " + tableName + ".name as name, govrole, businessstructure, ownershipmodel, startdate, actorschange.changedate" +
                " FROM " + tableName + ", actorschange, actortype WHERE " +
                "actortype.id = actors.type AND actors.id = actorschange.idactora AND actors.startdate <= '" + endDate + "'" +
                " AND actorschange.changedate > '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("typename") + "\t" +
                        rs.getString("name") + "\t" +
                        rs.getString("govrole") + "\t" +
                        rs.getString("businessStructure") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("changedate") + "\t" +
                        rs.getString("ownershipModel"));


                Actor actor = new Actor(rs.getInt("id"),
                        ActorType.valueOf(rs.getString("typename")),
                        rs.getString("name"),
                        stringToGovRole(rs.getString("govrole")),
                        stringToBusinessStructure(rs.getString("businessStructure")),
                        stringToOwnershipModel(rs.getString("ownershipModel")),
                        rs.getDate("startdate"),
                        rs.getDate("changedate"));

                int idActor = rs.getInt("id");

                //Insert in Map the actor with key = id, and add the new actor
                if (!actor_register.containsKey(idActor))
                    actor_register.put(idActor, actor);
                else
                    System.err.println("Two actors cannot have the same ID");


                numActors += 1;

                //Add actor to Array. This will be used in the constructor of SPM
                //actors.add(actor);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        //return actors;
    }

    public void
    selectActorActorRelationships(String tableName) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT Actor1, Actor2, RelType" +
                " FROM " + tableName;


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("Actor1") + "\t" +
                        rs.getInt("Actor2") + "\t" +
                        rs.getString("RelType"));


                Actor act1 = actor_register.get(rs.getInt("Actor1"));
                Actor act2 = actor_register.get(rs.getInt("Actor2"));

                ActorActorRelationship actorRel = new ActorActorRelationship(act1, act2, stringToActorActorTypeRelType(rs.getString("RelType")));

                actorActorRelationships.add(actorRel);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void
    selectActorAssetRelationships(String tableName) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres";//"jdbc:sqlite:Spm_archetypes.db";

        for (Map.Entry<Integer, Actor> entry : actor_register.entrySet()) {
            Actor actor = entry.getValue();

            String sql = "SELECT actorid, assetid, reltype, assettype, percentage" +
                    " FROM " + tableName + " WHERE actorid = " + actor.getId();


            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                // loop through the result set
                while (rs.next()) {
                    System.out.println("\t" + rs.getInt("ActorId") + "\t" +
                            rs.getInt("AssetId") + "\t" +
                            rs.getDouble("Percentage") + "\t" +
                            rs.getString("AssetType") + "\t" +
                            rs.getString("RelType"));


                    //Actor actor = actor_register.get(rs.getInt("ActorId"));
                    Vector<Asset> asset;

                    double percentage = rs.getDouble("Percentage");
                    String assetType = rs.getString("AssetType");

                    if (assetType.equalsIgnoreCase("Generation")) {
                        Vector<Generator> gens = gen_register.get(rs.getInt("AssetId"));

                        //Need to make sure that the SPM_gen_mapping is correct
                        //For this we need to create another SPM for 90s, and make sure that it uses
                        //The gen from the 90s.
                        if (gens == null) {
                            System.out.println("Actor " + actor.getName() + " - Generation Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (Generator gen : gens) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, gen,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);

                                //Add actorAsset relationship into the asset list
                                gen.addAssetRelationship(actorRel);
                                //Add all actorAsset relationships into the simulation engine global asset list
                                actorAssetRelationships.add(actorRel);

                            }
                        }
                    } else if (assetType.equalsIgnoreCase("Network asset")) {
                        Vector<NetworkAssets> nets = network_register.get(rs.getInt("AssetId"));
                        if (nets == null) {
                            System.out.println("Actor " + actor.getName() + " - Network Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (NetworkAssets net : nets) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, net,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                                net.addAssetRelationship(actorRel);
                                actorAssetRelationships.add(actorRel);

                            }
                        }
                    } else if (assetType.equalsIgnoreCase("SPM")) {
                        Vector<Spm> spms = spm_register.get(rs.getInt("AssetId"));
                        if (spms == null) {
                            System.out.println("Actor " + actor.getName() + " - SPM Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (Spm spm : spms) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, spm,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                                spm.addAssetRelationship(actorRel);
                                actorAssetRelationships.add(actorRel);

                            }
                        }
                    }

                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }


    Spm createSpm(int idSpmActor) {

        /**
         *  Check if spm.id is shared. If so, check if we already created an object for that spm.id.
         *  If it's the first time this spm.id is needed, then we create its object,
         *  If it already exists, we just retrieve the object from the spm_register
         */
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        String spmGenSql = "SELECT id, spm.shared, spm_contains, generation, network_assets, interface, storage, description FROM spm WHERE spm.id = '" + idSpmActor + "' ";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGenSql)) {

            // loop through the result set
            while (rs.next()) {

                int shared = rs.getInt("shared");

                //If it is shared and
                //we already created the spm, then the id should exist in the spm_register
                //if so, we can return the existing object
                if (shared == 1 && spm_register.containsKey(idSpmActor)) {
                    return spm_register.get(idSpmActor).firstElement();
                }

                /**
                 *  Get list of SPMs recursively from DB. Base case when there's no spm_contains.contained_id
                 */
                ArrayList<Spm> spms_contained = new ArrayList<Spm>();

                String[] spm_contains = rs.getString("spm_contains").split(",");
                for (String id : spm_contains) {
                    if (id.equalsIgnoreCase("")) break;
                    int contained_id = Integer.parseInt(id);

                    //Create SPM if contained_id is different than NULL
                    //Base Case of recursion
                    if (contained_id != 0) {
                        //System.out.println("SPM '" + idSpmActor + "' contains:" + contained_id);

                        // Recursive call to load the contained SPMs before creating the current SPM with idSpmActor
                        Spm spm_returned = createSpm(contained_id);

                        // Add Spms created to the ArrayList<Spm>
                        spms_contained.add(spm_returned);
                    }
                }

                /**
                 *  Get list of Generators from DB
                 */

                ArrayList<Generator> gens = new ArrayList<Generator>();

                String[] gen_contained = rs.getString("generation").split(",");
                for (String id : gen_contained) {
                    if (id.equalsIgnoreCase("")) break;
                    //System.out.println("SPM '" + rs.getString("id") + "' Generators:");

                    ArrayList<Generator> genSpm = selectGenTech(id);
                    gens.addAll(genSpm);
                }

                /**
                 * Get list of STORAGE from DB
                 */

                ArrayList<Storage> strs = new ArrayList<Storage>();
                String[] st_contained = rs.getString("storage").split(",");
                for (String id : st_contained) {
                    if (id.equalsIgnoreCase("")) break;
                    //System.out.println("SPM '" + rs.getString("id") + "' Storages:");

                    ArrayList<Storage> SpmStrs = selectStorage(id);
                    strs.addAll(SpmStrs);
                }

                /**
                 * Get list of NetworkAssets from DB
                 */

                ArrayList<NetworkAssets> networkAssets = new ArrayList<NetworkAssets>();
                String[] net_contained = rs.getString("network_assets").split(",");
                for (String id : net_contained) {
                    if (id.equalsIgnoreCase("")) break;
                    //System.out.println("SPM '" + rs.getString("id") + "' Grids:");

                    ArrayList<NetworkAssets> SpmNet = selectNetwork(id);
                    networkAssets.addAll(SpmNet);
                }


                /**
                 * Create new SPM
                 */
                Spm spmx = new Spm(idSpmActor, spms_contained, gens, networkAssets, strs, null);

                //Get from Map the vector of SPM with key = idSpmActor, and add the new spm to the vector
                if (!spm_register.containsKey(idSpmActor))
                    spm_register.put(idSpmActor, new Vector<>());

                spm_register.get(idSpmActor).add(spmx);

                return spmx;

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }


//        /**
//         * Get list of CONNECTION POINTS from DB
//         */
//        String spmCptSql = "SELECT Spm.id, Spm.name, ConnectionPoint.cpoint_id, ConnectionPoint.cpoint_name FROM Spm as Spm JOIN Spm_connection_mapping join ConnectionPoint " +
//                "on ConnectionPoint.cpoint_id = Spm_connection_mapping.connectionId and Spm.id = Spm_connection_mapping.spmId and Spm.name = '" + name + "' ";
//        Bag cpoints = new Bag();
//
//        try (Connection conn = DriverManager.getConnection(url);
//             Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery(spmCptSql)) {
//            Boolean printed = false;
//            // loop through the result set
//            while (rs.next()) {
//                if (!printed) {
//                    System.out.println("SPM '" + rs.getString("name") + "' Connection Points:");
//                    printed = true;
//                }
//                Bag SpmCpoints = selectConnectionPoint( rs.getString("cpoint_name"));
//                cpoints.addAll(SpmCpoints);
//            }
//        } catch (SQLException e) {
//            System.out.println(e.getMessage());
//        }


        //Bag cpoints = selectConnectionPoint("BK Brunswick");
        //double embGHG = random.nextDouble() * 100.0; todo: this has to be calculated according to the components of the spm!
        //double efficiency = 0.80; //efficiency all SPM

        return null;

    }

    public void loadCaseStudy(String tableName) {
        /**
         * Get the SPMs for EndUse Case study
         */

        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres";//"jdbc:sqlite:Spm_archetypes.db";

        String spmSql = "SELECT id, location_code, id_spm, spm_name, location_area, " +
                "category_type, dwelling_type  FROM " + tableName;

        int spmId = 0;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmSql)) {

            // loop through the result set
            while (rs.next()) {

                int endUseId = rs.getInt("id");
                String locationCode = rs.getString("location_code");
                int idSpm = rs.getInt("id_spm");
                String spmName = rs.getString("Spm_name");
                String categoryType = rs.getString("Category_type");
                String dwellingType = rs.getString("Dwelling_type");

                Spm spmEndUse = createSpm(idSpm);

                Enduse eu = new Enduse(endUseId, locationCode, spmEndUse, spmName, categoryType, dwellingType);

                //Add Random Location of EU in the layout
                Double2D euLoc = new Double2D(random.nextDouble() * 1000, random.nextDouble() * 1000);
                layout.setObjectLocation(eu, euLoc);

                //Use same Loc for the SPM
                Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
                Spm spmeu = eu.getSpm_end_use();

                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                addSPMLocation(spmeu, spmLoc, spmeu.diameter);

            }

        } catch (SQLException e) {

            System.out.println(e.getMessage());

        }
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

    void loadActor() {
    /*    ConsumerUnit householdA = new ConsumerUnit(1, ActorType.HDCONSUMER, "ConsumerUnit A", GovRole.RULEFOLLOW, BusinessStructure.INFORMAL, OwnershipModel.INDIVIDUAL, 1, true,0, false, 1);
        //662 from 2016ABS census for Australia (http://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/036)
        //82.6 kWh weekly average from "ConsumerUnit energy expenditure as proportion of gross income by family composition" data (4670.0 ConsumerUnit Energy consumption survey, 2012)
        consumptionActors.add(householdA);

        ConsumerUnit householdB = new ConsumerUnit(2, ActorType.HDCONSUMER, "core.ConsumerUnit B", GovRole.RULEFOLLOW, BusinessStructure.INFORMAL, OwnershipModel.INDIVIDUAL, 1, true,1, true,
                662 );
        consumptionActors.add(householdB);

        addNewActorActorRel(householdA, householdB, ActorActorRelationshipType.P2P);

        addNewActorAssetRel(householdA, spm_register.get(1).firstElement(), ActorAssetRelationshipType.OWN, 100);

        Actor electricityProducerA = new Actor(3, ActorType.MARKETGENSCHD, "AGL Energy Limited", GovRole.RULEFOLLOW, BusinessStructure.PUBLICCOMP, OwnershipModel.SHARES);
        Actor retailerA = new Actor(4, ActorType.RETAILER, "Lumo", GovRole.RULEFOLLOW, BusinessStructure.PTYLTD, OwnershipModel.SHARES);
        Actor networkOperator = new Actor(5, ActorType.MARKETNETWORKPROV, "AusNet", GovRole.RULEFOLLOW, BusinessStructure.PTYLTD, OwnershipModel.SHARES);
        //Actor broker = new Actor...

        addNewActorActorRel(householdA, retailerA, ActorActorRelationshipType.BILLING);
        addNewActorActorRel(retailerA, networkOperator, ActorActorRelationshipType.ACCESS_FEE);

        for (Generator gen : gen_register.get(12)) {
            addNewActorAssetRel(electricityProducerA, gen, ActorAssetRelationshipType.LEASE, 100.0);
        }
        System.out.println("foo");
        */
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

    public void generateHouseholds() {

        Date today = getCurrentSimDate();

        //Get Population today (this sim step)
        int householdsToday = monthly_domestic_consumers_register.get(today);

        //Get Last month date (last sim state)
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.MONTH, -1);
        Date lastStepTime = c.getTime();

        //Get Population before (last sim step)
        int householdsBefore = 0;
        if (monthly_domestic_consumers_register.containsKey(lastStepTime))
            householdsBefore = monthly_domestic_consumers_register.get(lastStepTime);

        //Get growth. Need to create ConsumerUnits if possitive
        int householdsGrowth = householdsToday - householdsBefore;


        int numHouseholds = (int) (householdsGrowth * populationPercentageAreacCode);


        //Get Last ConsumerUnit, which may have space for more households
        ConsumerUnit consumer = null;
        if(consumptionActors.size() >= 1)
            consumer = (ConsumerUnit) consumptionActors.get(consumptionActors.size() - 1);

        int householdsLeft = numHouseholds;

        if(consumer != null) {
            //If all new households fit in existing SPM, then just add them
            if ((maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds()) > numHouseholds) {
                consumer.setNumberOfHouseholds(consumer.getNumberOfHouseholds() + numHouseholds);
                return;
            }
            //If not all households fit, increase SPM to full, and create a new SPM with remaining households
            else if ((maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds()) <= numHouseholds) {
                int decrease = maxHouseholdsPerConsumerUnit - consumer.getNumberOfHouseholds();
                numHouseholds -= decrease;
                householdsLeft = numHouseholds;
                consumer.setNumberOfHouseholds(maxHouseholdsPerConsumerUnit);
            }
        }

        for (int i = 0; i < numHouseholds; i = i + maxHouseholdsPerConsumerUnit) {

            boolean hasGas = false;

            System.out.println("Num Actors: " + numActors);

            int numPeople = 2;
            String name = "Household";

            int householdsCreated = maxHouseholdsPerConsumerUnit;
            if(householdsLeft < maxHouseholdsPerConsumerUnit) householdsCreated = householdsLeft;

            Actor actor = new ConsumerUnit(numActors++, ActorType.HDCONSUMER, name,
                    GovRole.RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, householdsCreated, hasGas, true, 0, today);

            consumptionActors.add((ConsumptionActor) actor);

            householdsLeft -= householdsCreated;

            //Schedule the actor in order to make decissions at every step
            this.schedule.scheduleRepeating((Actor) actor);

            //Id of conventional SPM
            int idSpm = 1;

            Spm spm = createSpm(idSpm);

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
            Double2D euLoc = new Double2D((i * 80) % 1600, ((int) i / 20 + 1) * 80);
            layout.setObjectLocation(actor, euLoc);

            //Use same Loc for the SPM
            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
            for (ActorAssetRelationship rel : actor.assetRelationships) {
                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                //Only draw SPMs in the meantime
                if (Spm.class.isInstance(rel.getAsset()))
                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
            }


        }

    }
/*
    public void generateHouseholds() {

        Date today = getCurrentSimDate();

        //Get Population today (this sim step)
        int householdsToday = monthly_domestic_consumers_register.get(today);

        //Get Last month date (last sim state)
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.MONTH,-1);
        Date lastStepTime = c.getTime();

        //Get Population before (last sim step)
        int householdsBefore = 0;
        if(monthly_domestic_consumers_register.containsKey(lastStepTime))
            householdsBefore = monthly_domestic_consumers_register.get(lastStepTime);

        //Get growth. Need to create ConsumerUnits if possitive
        int householdsGrowth = householdsToday - householdsBefore;



        int numHouseholds = (int) (householdsGrowth * populationPercentageAreacCode);


        double gasUsersPercentage = 20;
        double onePerson = 20;
        double twoPerson = 20;
        double threePerson = 20;
        double fourPerson = 20;
        double fivePlusPerson = 20;



        double TotalPercentage = onePerson + twoPerson + threePerson + fourPerson + fivePlusPerson;
        if (TotalPercentage != 100) {
            System.err.println("Total Percentage sums up to " + TotalPercentage);
            System.exit(1);
        }


        int onePersonHouseholds = (int) (numHouseholds * onePerson) / 100;
        int twoPersonHouseholds = (int) (numHouseholds * twoPerson) / 100;
        int threePersonHouseholds = (int) (numHouseholds * threePerson) / 100;
        int fourPersonHouseholds = (int) (numHouseholds * fourPerson) / 100;
        int fivePlusPersonHouseholds = (int) (numHouseholds * fivePlusPerson) / 100;


        for (int i = 0; i < numHouseholds; i=i+maxHouseholdsPerConsumerUnit) {

            //To round Up
            int householdsLeft = numHouseholds - i;
            //if( householdsLeft < maxHouseholdsPerConsumerUnit) maxHouseholdsPerConsumerUnit = householdsLeft;

            boolean hasGas = (random.nextGaussian() * 100 <= gasUsersPercentage);

            System.out.println("Num Actors: "+ numActors);

            int numPeople = 1;
            String name = "OnePersonHousehold";

            if ((i >= onePersonHouseholds) &&
                    (i < onePersonHouseholds + twoPersonHouseholds)) {
                numPeople = 2;
                name = "TwoPersonHousehold";
            }

            if ((i >= onePersonHouseholds + twoPersonHouseholds) &&
                    (i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds)) {
                numPeople = 3;
                name = "ThreePersonHousehold";
            }

            if ((i >= onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds) &&
                    (i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds)) {
                numPeople = 4;
                name = "FourPersonHousehold";
            }

            if ((i >= onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds) &&
                    (i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds + fivePlusPersonHouseholds)) {
                numPeople = 5;
                name = "FivePlusPersonHousehold";
            }

            Actor actor = new ConsumerUnit(numActors++, ActorType.HDCONSUMER, name,
                    GovRole.RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, maxHouseholdsPerConsumerUnit, hasGas, true, 0, today);

            consumptionActors.add((ConsumptionActor) actor);

            //Schedule the actor in order to make decissions at every step
            this.schedule.scheduleRepeating((Actor) actor);

            //Id of conventional SPM
            int idSpm = 1;

            Spm spm = createSpm(idSpm);

            *//**
     * Actor Asset rel
     * *//*
            //create new relationship btw the household and SPM
            ActorAssetRelationship actorSpmRel = new ActorAssetRelationship(actor, spm, ActorAssetRelationshipType.USE, 100);

            //Store relationship with assets in the actor object
            actor.addAssetRelationship(actorSpmRel);
            //Store relationship in the global array of all actor asset rel
            actorAssetRelationships.add(actorSpmRel);

            *//**
     * Actor Actor rel
     * *//*
            //create new relationship and contract btw the household and Retailer
            ActorActorRelationship actorActHouseRetailRel = new ActorActorRelationship(actor, actor_register.get(4), ActorActorRelationshipType.BILLING);

            //Store relationship with actor in the actor object
            actor.addContract(actorActHouseRetailRel);
            //Store relationship in the global array of all actor asset rel
            actorActorRelationships.add(actorActHouseRetailRel);


            */

    /**
     * The code below is only for visualization
     **//*

            //Add Random Location of EU in the layout
            Double2D euLoc = new Double2D((i * 80) % 1600, ((int) i / 20 + 1) * 80);
            layout.setObjectLocation(actor, euLoc);

            //Use same Loc for the SPM
            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
            for (ActorAssetRelationship rel : actor.assetRelationships) {
                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                //Only draw SPMs in the meantime
                if (Spm.class.isInstance(rel.getAsset()))
                    addSPMLocation((Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
            }


        }

    }*/
    public void start() {
        super.start();

        //selectDemandTemp();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = stringToDate.format(this.startSimDate);
        String endDate = stringToDate.format(this.endSimDate);

        selectArena();
        selectTariffs(startDate, endDate, areaCode);

        selectConsumption(startDate, endDate);
        selectGenerationHistoricData(startDate, endDate);

        selectActors("actors", startDate, endDate);

        generateHouseholds();

        //selectActorActorRelationships("actoractor93");

        selectActorAssetRelationships("actorasset");//from https://www.secv.vic.gov.au/history/


     /*   for (ConsumptionActor ca : this.consumptionActors) {
            this.schedule.scheduleRepeating((Actor) ca);
        }
*/

    }


    public static void main(String[] args) {
        doLoop(Gr4spSim.class, args);


        exit(0);
    }

}
