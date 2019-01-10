package core;


import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


public class Gr4spSim extends SimState {
    private static final long serialVersionUID = 1;


    public Continuous2D layout;
    HashMap<Integer, Vector<Spm>> spm_register;
    ArrayList<ConsumptionActor> consumptionActors = new ArrayList<ConsumptionActor>(); //this is the array to be traverse to get the total consumption


    //Counter for the unique id each time a storage unit is created and other storage related variables
    private int numGenerators;
    private int numStorage;
    //counter for energy grid
    private int numGrid;
    private int numcpoints;

    //counter organisation structures
    private int numOrg;


    public Gr4spSim(long seed) {
        super(seed);
        //Num generator, storage, grid to generate unique id
        numGenerators = 0; //for real example, it is going to be the number of generators supplying the area under study at the scale under study.
        numStorage = 0;
        numGrid = 0;
        numOrg = 0;
    }

    /**
     * select all rows in the Generation Technologies table
     */
    public Bag selectGenTech(String name) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT id, PowerStation, Owner, InstalledCapacity_MW, TechnologyType, FuelType, DispatchType, location_Coordinates" +
                " FROM GenerationAssets WHERE PowerStation = '" + name + "' ";

        Bag gens = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("PowerStation") + "\t" +
                        rs.getString("Owner") + "\t" +
                        rs.getDouble("InstalledCapacity_MW") + "\t" +
                        rs.getString("TechnologyType") + "\t" +
                        rs.getString("FuelType") + "\t" +
                        rs.getString("DispatchType") + "\t" +
                        rs.getString("location_Coordinates"));


                Generator gen = new Generator(rs.getInt("id"),
                        rs.getString("PowerStation"),
                        rs.getString("Owner"),
                        rs.getDouble("InstalledCapacity_MW"),
                        rs.getString("TechnologyType"),
                        rs.getString("FuelType"),
                        rs.getString("DispatchType"),
                        rs.getString("location_Coordinates"));

                numGenerators += 1;
                gens.add(gen);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return gens;
    }


    public Bag selectStorage(String name) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT storage_id, storage_name, StorageType, storageOutputCap , storageCapacity, Ownership, storage_cycleLife, storage_costRange  FROM Storage WHERE storage_name = '" + name + "' ";
        Bag strs = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("storage_id") + "\t" +
                        rs.getString("storage_name") + "\t" +
                        rs.getInt("StorageType") + "\t" +
                        rs.getDouble("storageOutputCap") + "\t" +
                        rs.getDouble("storageCapacity") + "\t" +
                        rs.getInt("Ownership") + "\t" +
                        rs.getDouble("storage_cycleLife") + "\t" +
                        rs.getDouble("storage_costRange"));

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
    public Bag selectNetwork(String id) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT NetworkAssets.id as netId, NetworkAssetType.type as type, NetworkAssetType.subtype as subtype, NetworkAssetType.grid as grid, assetName, grid_node_name, " +
                "location_MB, gridLosses, gridVoltage, owner  FROM NetworkAssets JOIN NetworkAssetType " +
                "WHERE NetworkAssets.assetType = NetworkAssetType.id and NetworkAssets.id = '" + id + "' ";
        Bag nets = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("netId") + "\t" +
                        rs.getString("type") + "\t" +
                        rs.getString("subtype") + "\t" +
                        rs.getString("grid") + "\t" +
                        rs.getString("assetName") + "\t" +
                        rs.getString("grid_node_name") + "\t" +
                        rs.getString("location_MB") + "\t" +
                        rs.getDouble("gridLosses") + "\t" +
                        rs.getInt("gridVoltage") + "\t" +
                        rs.getString("owner"));

                NetworkAssets grid = new NetworkAssets(rs.getInt("netId"),
                        rs.getString("type"),
                        rs.getString("subtype"),
                        rs.getString("grid"),
                        rs.getString("assetName"),
                        rs.getString("grid_node_name"),
                        rs.getString("location_MB"),
                        rs.getDouble("gridLosses"),
                        rs.getInt("gridVoltage"),
                        rs.getString("owner"));
                nets.add(grid);

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
    public Bag selectConnectionPoint(String name) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT cpoint_id, cpoint_name, CPoint_type, distanceToDemand, cpoint_locationCode, cpoint_owner, Ownership FROM ConnectionPoint WHERE cpoint_name = '" + name + "' ";
        Bag cpoints = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("cpoint_id") + "\t" +
                        rs.getString("cpoint_name") + "\t" +
                        rs.getInt("CPoint_Type") + "\t" +
                        rs.getDouble("distanceToDemand") + "\t" +
                        rs.getInt("cpoint_locationCode") + "\t" +
                        rs.getString("cpoint_owner") + "\t" +
                        rs.getInt("Ownership"));

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


    //creation of societal organisations attached to the SPM

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


    Spm createSpm(int idSpmEndUse) {

        /**
         *  Get list of SPMs recursively from DB. Base case when there's no spm_contains.contained_id
         */

        String url = "jdbc:sqlite:Spm_archetypes.db";

        String spmGenSql = "SELECT Spm_contains.contained_id FROM Spm_contains WHERE Spm_contains.id_Spm = '" + idSpmEndUse + "' ";

        Bag spms_contained = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGenSql)) {

            // loop through the result set
            while (rs.next()) {

                int contained_id = rs.getInt("contained_id");

                //Create SPM if contained_id is different than NULL
                //Base Case of recursion
                if (contained_id != 0) {
                    System.out.println("SPM '" + idSpmEndUse + "' contains:" + rs.getInt("contained_id"));

                    // Recursive call to load the contained SPMs before creating the current SPM with idSpmEndUse
                    Spm spm_returned = createSpm(rs.getInt("contained_id"));

                    // Add Spms created to the Bag
                    spms_contained.add(spm_returned);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }

        /**
         *  Get list of Generators from DB
         */

        spmGenSql = "SELECT Spm.id, Spm.name, GenerationAssets.id, GenerationAssets.PowerStation FROM Spm as Spm JOIN Spm_gen_mapping join GenerationAssets on " +
                "GenerationAssets.id = Spm_gen_mapping.genId and Spm.id = Spm_gen_mapping.spmId and Spm.id = '" + idSpmEndUse + "' ";

        Bag gens = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGenSql)) {

            Boolean printed = false;
            // loop through the result set
            while (rs.next()) {

                if (!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Generators:");
                    printed = true;
                }

                Bag genSpm = selectGenTech(rs.getString("PowerStation"));
                gens.addAll(genSpm);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }


        /**
         * Get list of STORAGE from DB
         */
        String spmStrSql = "SELECT Spm.id, Spm.name, Storage.storage_id, Storage.storage_name FROM Spm as Spm JOIN Spm_storage_mapping join Storage " +
                "on Storage.storage_id = Spm_storage_mapping.storageId and Spm.id = Spm_storage_mapping.spmId and Spm.id = '" + idSpmEndUse + "' ";
        Bag strs = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmStrSql)) {
            Boolean printed = false;

            // loop through the result set
            while (rs.next()) {


                if (!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Storages:");
                    printed = true;
                }
                Bag SpmStrs = selectStorage(rs.getString("storage_name"));
                strs.addAll(SpmStrs);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        /**
         * Get list of NetworkAssets from DB
         */
        String spmGridSql = "SELECT Spm.id, Spm.name, NetworkAssets.id as netId FROM Spm as Spm JOIN Spm_networkTimaru_mapping join NetworkAssets " +
                "on NetworkAssets.id = Spm_networkTimaru_mapping.network_assetId and Spm.id = Spm_networkTimaru_mapping.spmId and Spm.id = '" + idSpmEndUse + "' ";
        Bag networkAssets = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGridSql)) {
            Boolean printed = false;

            // loop through the result set
            while (rs.next()) {
                if (!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Grids:");
                    printed = true;
                }
                Bag SpmNet = selectNetwork(rs.getString("netId"));
                networkAssets.addAll(SpmNet);
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

        /**
         * Create new SPM
         */
        Spm spmx = new Spm(idSpmEndUse, spms_contained, gens, networkAssets, strs, null);

        //Get from Map the vector of SPM with key = idSpmEndUse, and add the new spm to the vector
        if (!spm_register.containsKey(idSpmEndUse))
            spm_register.put(idSpmEndUse, new Vector<>());

        spm_register.get(idSpmEndUse).add(spmx);

        return spmx;


    }

    public void loadCaseStudy(String tableName) {
        /**
         * Get the SPMs for EndUse Case study
         */

        String url = "jdbc:sqlite:Spm_archetypes.db";

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
        for (int i = 0; i < s.getSpms_contained().numObjs; i++) {
            Spm spmc = (Spm) s.getSpms_contained().getValue(i);
            spmc.diameter = diameter - 10;
            addSPMLocation(spmc, loc, spmc.diameter);
            loc = loc.add(shift);

        }
    }

    void loadActorConsumption() {
        Household householdA = new Household(1, "core.Household", "core.Household A", "Rule follower", "household",1, 0, false,
                662, "residential", 82.6);
        //662 from 2016ABS census for Australia (http://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/036)
        //82.6 kWh weekly average from "Household energy expenditure as proportion of gross income by family composition" data (4670.0 Household Energy consumption survey, 2012)
        consumptionActors.add(householdA);

        Household householdB = new Household(1, "core.Household", "core.Household B", "Rule follower", "household",1, 1, true,
                662, "residential", 82.6 * 1.2);
        consumptionActors.add(householdB);

        addNewActorActorRel(householdA, householdB, ActorActorRelationshipType.P2P);

        addNewActorAssetRel(householdA, spm_register.get(1).firstElement(), ActorAssetRelationshipType.OWN, 100);

        ElectricityProducer electricityProducerA = new ElectricityProducer(1, "Electricity producer", "AGL Energy Limited", "Rule follower",
                "Public company", "Large");

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

    public void start() {
        super.start();
        layout = new Continuous2D(10.0, 600.0, 600.0);
        spm_register = new HashMap<>();
        loadCaseStudy("SPMsTimaruSt");
        /*createSpm( "inner city");
        createSpm("individual");
        createSpm("primary");*/
        System.out.println(layout.toString());
        //loadCaseStudy("SPMsTimaruSt");

        loadActorConsumption();
    }

    public static void main(String[] args) {
        doLoop(Gr4spSim.class, args);


        System.exit(0);
    }

}
