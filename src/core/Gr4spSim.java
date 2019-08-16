package core;

import core.Policies.EndConsumerTariff;
import core.Policies.SimPolicies;
import core.Relationships.*;
import core.Social.*;
import core.Technical.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
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
import static java.lang.System.setOut;


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

    //Solar Exposure in MJ/m^2 daily mean per month
    HashMap<Date, Float> solar_exposure;

    // Solar installation in kw per month in Australia

    HashMap<Date, Float> solar_installation_kw;

    //this is the array to be traversed to get the total consumption
    ArrayList<ConsumptionActor> consumptionActors = new ArrayList<ConsumptionActor>();

    //this is the array to be traverse to get the total conventional consumption
    ArrayList<ConsumptionActor> conventionalConsumptionActors = new ArrayList<ConsumptionActor>();

    //this is the array to be traverse to get the total non conventional consumption
    ArrayList<ConsumptionActor> nonConventionalConsumptionActors = new ArrayList<ConsumptionActor>();

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

    //Class to manage all the data generated in the simulation
    public SaveData saveData;


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
        solar_exposure = new HashMap<>();
        solar_installation_kw = new HashMap<>();

        layout = new Continuous2D(10.0, 600.0, 600.0);
        policies = new SimPolicies();

        simulParametres();

        saveData = new SaveData(this);

    }

    private void simulParametres() {

        /**
         * Simulation Date Range
         */
        String startDate = "2000-01-01";
        String endDate = "2018-01-01";

        /**
         * Population and scale
         */

        //To use M1(only Melbourne cityareaCode), populationPercentageAreacCode = 0.009; assuming 0.9% of Vic popultaion,

        //Include regional with all framework indicators, 100% Population data
        areaCode = "VIC";
        populationPercentageAreacCode = 1;

        //Max number of Dwellings represented in a single ConsumerUnit, attached to a SPM
        //It is a measure to control aggregation of dwellings per SPM
        //New ConsumerUnits are created according to monthly population growth in the simulation Data monthly_domestic_consumers_register
        //maxHouseholdsPerConsumerUnit = 1000;

        //Adds all households as a single actor connected to a single SPM
        maxHouseholdsPerConsumerUnit = Integer.MAX_VALUE;

        //Percentage of Total Consumption Historic Data that goes into domestic use
        domesticConsumptionPercentage = 1;

        /**
         * Public Policies
         */

        // goes from 0.00 to 1.0, represents percentage of monthly uptake and uses a normal gaussian distribution to simulate the uptake
        // for example, 0.01 represents 1% per month, around 12% a year
        double uptakeRate = 0.0;//0.005;
        policies.getAccelerateSolarPV().setMonthlyHousholdsPercentageConversion(uptakeRate);

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

    public HashMap<Date, Float> getCpi_conversion() {
        return cpi_conversion;
    }

    public HashMap<Date, Double> getMonthly_consumption_register() {
        return monthly_consumption_register;
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

    public HashMap<Date, Float> getSolar_exposure() {
        return solar_exposure;
    }

    public HashMap<Date, Float> getSolar_installation_kw() {
        return solar_installation_kw;
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

    /**
     * Create SPM function
     * */

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

                    ArrayList<Generator> genSpm = LoadData.selectGenTech(this, id);
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

                    ArrayList<Storage> SpmStrs = LoadData.selectStorage(this, id);
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

                    ArrayList<NetworkAssets> SpmNet = LoadData.selectNetwork(this, id);
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
     * */

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

        //Get Last conventional ConsumerUnit, which may have space for more households
        ConsumerUnit consumer = null;
        if (conventionalConsumptionActors.size() >= 1)
            consumer = (ConsumerUnit) conventionalConsumptionActors.get(conventionalConsumptionActors.size() - 1);

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
            Actor actor = new ConsumerUnit(numActors++, ActorType.HDCONSUMER, name,
                    GovRole.RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, householdsCreated, hasGas, true, 0, today);

            consumptionActors.add((ConsumptionActor) actor);
            conventionalConsumptionActors.add((ConsumptionActor) actor);

            householdsLeft -= householdsCreated;

            //Schedule the actor in order to make decissions at every step
            this.schedule.scheduleRepeating((Actor) actor);

            //Id of conventional SPM
            int idSpm = 1;

            //Create new SPM
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

    /**
     *  Decrease conventional households, without deleting the ConsumerUnit if it gets down to 0 households
     * */

    void decreaseConventionalHouseholds(int numHouseholds) {
        //Get Last conventional ConsumerUnit, which may have space for more households
        ConsumerUnit conventionalConsumer = null;
        int lastConvConsumer = conventionalConsumptionActors.size() - 1;

        Date today = getCurrentSimDate();


        //while there are still houses to remove from consumer units
        while (numHouseholds > 0) {

            //if index is positive, retrieve consumption actor to decrease households
            if (lastConvConsumer >= 0)
                conventionalConsumer = (ConsumerUnit) conventionalConsumptionActors.get(lastConvConsumer);

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
     * */

    public void convertIntoNonConventionalHouseholds(double conversionPercentage) {

        Date today = getCurrentSimDate();

        //Get CONVENTIONAL Population today (this sim step)
        int householdsToday = 0;

        //sum population of each conventional consumer unit
        for (ConsumptionActor c : conventionalConsumptionActors) {
            ConsumerUnit cu = (ConsumerUnit) c;
            householdsToday += cu.getNumberOfHouseholds();
        }

        int numHouseholdsConverted = (int) (householdsToday * conversionPercentage);


        //Get Last Non conventional ConsumerUnit, which may has space for more households
        ConsumerUnit NonConventionalConsumer = null;
        if (nonConventionalConsumptionActors.size() >= 1)
            NonConventionalConsumer = (ConsumerUnit) nonConventionalConsumptionActors.get(nonConventionalConsumptionActors.size() - 1);

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

            Actor actor = new ConsumerUnit(numActors++, ActorType.HDCONSUMER, name,
                    GovRole.RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, householdsCreated, hasGas, true, 0, today);

            //Decrease conventional households, after increasing the non-conventional
            decreaseConventionalHouseholds(householdsCreated);


            consumptionActors.add((ConsumptionActor) actor);
            nonConventionalConsumptionActors.add((ConsumptionActor) actor);

            householdsLeft -= householdsCreated;

            //Schedule the actor in order to make decisions at every step
            this.schedule.scheduleRepeating((Actor) actor);

            //Id of Non Conventional SPM
            int idSpm = 2;

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

    /**
     * Load Data From DataBase
     * */

    public void loadData() {
        //selectDemandTemp();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String startDate = stringToDate.format(this.startSimDate);
        String endDate = stringToDate.format(this.endSimDate);

        LoadData.selectArena(this);
        LoadData.selectTariffs(this, startDate, endDate, areaCode);

        LoadData.selectConsumption(this, startDate, endDate);
        LoadData.selectGenerationHistoricData(this, startDate, endDate);
        LoadData.selectSolarExposure(this);
        LoadData.selectSolarInstallation(this);

        LoadData.selectActors(this, "actors", startDate, endDate);

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
        this.schedule.scheduleRepeating(0.0,1,saveData);


    }


    public static void main(String[] args) {
        doLoop(Gr4spSim.class, args);


        exit(0);
    }

}
