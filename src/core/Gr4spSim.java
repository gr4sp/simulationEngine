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
    HashMap<Integer, Vector<Generator>> gen_register;
    HashMap<Integer, Vector<NetworkAssets>> network_register;
    HashMap<Integer, Actor> actor_register;

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


    public Gr4spSim(long seed) {
        super(seed);
        //Num generator, storage, grid to generate unique id
        numGenerators = 0; //for real example, it is going to be the number of generators supplying the area under study at the scale under study.
        numStorage = 0;
        numGrid = 0;
        numOrg = 0;
        numActors = 0;
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

                int idGen = rs.getInt("id");
                //Get from Map the vector of GENERATORS with key = idGen, and add the new Generator to the vector
                if (!gen_register.containsKey(idGen))
                    gen_register.put(idGen, new Vector<>());

                gen_register.get(idGen).add(gen);

                numGenerators += 1;

                //Add gen to bag. This will be used in the constructor of SPM
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
                        rs.getString("owner"));
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

        if (actorType.equalsIgnoreCase("RETAILER"))
            return ActorType.RETAILER;
        if (actorType.equalsIgnoreCase("ELECPROD"))
            return ActorType.ELECPROD;
        if (actorType.equalsIgnoreCase("NETWORKOP"))
            return ActorType.NETWORKOP;
        if (actorType.equalsIgnoreCase("HOUSEHOLD"))
            return ActorType.HOUSEHOLD;
        if (actorType.equalsIgnoreCase("REGULATOR"))
            return ActorType.REGULATOR;
        if (actorType.equalsIgnoreCase("IMPLEMENT"))
            return ActorType.IMPLEMENT;
        if (actorType.equalsIgnoreCase("GOVERNM"))
            return ActorType.GOVERNM;
        if (actorType.equalsIgnoreCase("OMUSD"))
            return ActorType.OMUSD;
        if (actorType.equalsIgnoreCase("ÏNVESTOR"))
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

    /**
     * select all rows in the Actor table
     */
    public void
    selectActors(String tableName) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT id, actorType, actorName, role, businessStructure, ownershipModel" +
                " FROM " + tableName;

        //Bag actors = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("actorType") + "\t" +
                        rs.getString("actorName") + "\t" +
                        rs.getString("role") + "\t" +
                        rs.getString("businessStructure") + "\t" +
                        rs.getString("ownershipModel"));


                Actor actor = new Actor(rs.getInt("id"),
                        stringToActorType(rs.getString("actorType")),
                        rs.getString("actorName"),
                        stringToGovRole(rs.getString("role")),
                        stringToBusinessStructure(rs.getString("businessStructure")),
                        stringToOwnershipModel(rs.getString("ownershipModel")));

                int idActor = rs.getInt("id");

                //Insert in Map the actor with key = id, and add the new actor
                if (!actor_register.containsKey(idActor))
                    actor_register.put(idActor, actor);
                else
                    System.err.println("Two actors cannot have the same ID");


                numActors += 1;

                //Add actor to bag. This will be used in the constructor of SPM
                //actors.add(actor);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        //return actors;
    }

    public void
    selectActorActorRelationships(String tableName) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT Actor1, Actor2, RelType" +
                " FROM " + tableName;

        //Bag actors = new Bag();
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

                ActorActorRelationship actorRel = new ActorActorRelationship( act1, act2, stringToActorActorTypeRelType(rs.getString("RelType")));

                actorActorRelationships.add(actorRel);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void
    selectActorAssetRelationships(String tableName) {
        String url = "jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT ActorId, AssetId, RelType, AssetType, Percentage" +
                " FROM " + tableName;

        //Bag actors = new Bag();
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


                Actor actor = actor_register.get(rs.getInt("ActorId"));
                Vector<Asset> asset;

                double percentage = rs.getDouble("Percentage");
                String assetType = rs.getString("AssetType");

                if(assetType.equalsIgnoreCase("Generation") ) {
                    Vector<Generator> gens = gen_register.get(rs.getInt("AssetId"));

                    //Need to make sure that the SPM_gen_mapping is correct
                    //For this we need to create another SPM for 90s, and make sure that it uses
                    //The gen from the 90s.

                    for(Generator gen : gens ){
                        ActorAssetRelationship actorRel = new ActorAssetRelationship( actor, gen,
                                stringToActorAssetTypeRelType(rs.getString("RelType")),percentage);

                        //Add actorAsset relationship into the asset list
                        gen.addAssetRelationship(actorRel);
                        //Add all actorAsset relationships into the simulation engine global asset list
                        actorAssetRelationships.add(actorRel);

                    }
                }
                else if(assetType.equalsIgnoreCase("Network asset") ) {
                    Vector<NetworkAssets> nets = network_register.get(rs.getInt("AssetId"));

                    for (NetworkAssets net : nets) {
                        ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, net,
                                stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                        net.addAssetRelationship(actorRel);
                        actorAssetRelationships.add(actorRel);

                    }
                }
                else if(assetType.equalsIgnoreCase("SPM") ) {
                    Vector<Spm> spms = spm_register.get(rs.getInt("AssetId"));

                    for (Spm spm : spms) {
                        ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, spm,
                                stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                        spm.addAssetRelationship(actorRel);
                        actorAssetRelationships.add(actorRel);

                    }
                }

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    Spm createSpm(int idSpmActor) {

        /**
         *  Check if spm.id is shared. If so, check if we already created an object for that spm.id.
         *  If it's the first time this spm.id is needed, then we create its object,
         *  If it already exists, we just retrieve the object from the spm_register
         */
        String url = "jdbc:sqlite:Spm_archetypes.db";

        String spmGenSql = "SELECT Spm.shared FROM Spm WHERE Spm.id = '" + idSpmActor + "' ";

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
                else
                    break;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }

        /**
         *  Get list of SPMs recursively from DB. Base case when there's no spm_contains.contained_id
         */


        url = "jdbc:sqlite:Spm_archetypes.db";

        spmGenSql = "SELECT Spm_contains.contained_id FROM Spm_contains WHERE Spm_contains.id_Spm = '" + idSpmActor + "' ";

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
                    System.out.println("SPM '" + idSpmActor + "' contains:" + rs.getInt("contained_id"));

                    // Recursive call to load the contained SPMs before creating the current SPM with idSpmActor
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
                "GenerationAssets.id = Spm_gen_mapping.genId and Spm.id = Spm_gen_mapping.spmId and Spm.id = '" + idSpmActor + "' ";

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
                "on Storage.storage_id = Spm_storage_mapping.storageId and Spm.id = Spm_storage_mapping.spmId and Spm.id = '" + idSpmActor + "' ";
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
        String spmGridSql = "SELECT Spm.id, Spm.name, NetworkAssets.id as netId FROM Spm as Spm JOIN Spm_network_mapping join NetworkAssets " +
                "on NetworkAssets.id = Spm_network_mapping.network_assetId and Spm.id = Spm_network_mapping.spmId and Spm.id = '" + idSpmActor + "' ";
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
        Spm spmx = new Spm(idSpmActor, spms_contained, gens, networkAssets, strs, null);

        //Get from Map the vector of SPM with key = idSpmActor, and add the new spm to the vector
        if (!spm_register.containsKey(idSpmActor))
            spm_register.put(idSpmActor, new Vector<>());

        spm_register.get(idSpmActor).add(spmx);

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

    void loadActor() {
    /*    Household householdA = new Household(1, ActorType.HOUSEHOLD, "Household A", GovRole.RULEFOLLOW, BusinessStructure.INFORMAL, OwnershipModel.INDIVIDUAL, 1, true,0, false, 1);
        //662 from 2016ABS census for Australia (http://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/036)
        //82.6 kWh weekly average from "Household energy expenditure as proportion of gross income by family composition" data (4670.0 Household Energy consumption survey, 2012)
        consumptionActors.add(householdA);

        Household householdB = new Household(2, ActorType.HOUSEHOLD, "core.Household B", GovRole.RULEFOLLOW, BusinessStructure.INFORMAL, OwnershipModel.INDIVIDUAL, 1, true,1, true,
                662 );
        consumptionActors.add(householdB);

        addNewActorActorRel(householdA, householdB, ActorActorRelationshipType.P2P);

        addNewActorAssetRel(householdA, spm_register.get(1).firstElement(), ActorAssetRelationshipType.OWN, 100);

        Actor electricityProducerA = new Actor(3, ActorType.ELECPROD, "AGL Energy Limited", GovRole.RULEFOLLOW, BusinessStructure.PUBLICCOMP, OwnershipModel.SHARES);
        Actor retailerA = new Actor(4, ActorType.RETAILER, "Lumo", GovRole.RULEFOLLOW, BusinessStructure.PTYLTD, OwnershipModel.SHARES);
        Actor networkOperator = new Actor(5, ActorType.NETWORKOP, "AusNet", GovRole.RULEFOLLOW, BusinessStructure.PTYLTD, OwnershipModel.SHARES);
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

    public void generateHouseholdsNineties() {

        Scanner read = new Scanner(System.in);


        System.out.println("Introduce the number of Households: ");
        int numHouseholds = read.nextInt();
        read.nextLine();

        System.out.println("Introduce % of Household with Gas [0..100]: ");
        double gasUsersPercentage = read.nextDouble();
        read.nextLine();

        double percentageRemaining = 100;

        System.out.println("Introduce % of Household with 1 person [0..100]: ");
        double onePerson = read.nextDouble();
        read.nextLine();
        percentageRemaining -= onePerson;

        System.out.println("Introduce % of Household with 2 person [0.." + percentageRemaining + "]: ");
        double twoPerson = read.nextDouble();
        read.nextLine();
        percentageRemaining -= twoPerson;

        System.out.println("Introduce % of Household with 3 person [0.." + percentageRemaining + "]: ");
        double threePerson = read.nextDouble();
        read.nextLine();
        percentageRemaining -= threePerson;

        System.out.println("Introduce % of Household with 4 person [0.." + percentageRemaining + "]: ");
        double fourPerson = read.nextDouble();
        read.nextLine();
        percentageRemaining -= fourPerson;

        System.out.println("Introduce % of Household with 5 person [0.." + percentageRemaining + "]: ");
        double fivePlusPerson = read.nextDouble();
        read.nextLine();

        System.out.println("Introduce initial month [1..12]: ");
        int month = read.nextInt();
        read.nextLine();

        read.close();

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

        int idActor = 1000;

        for (int i = 0; i < numHouseholds; i++) {

            boolean hasGas = (random.nextGaussian() * 100 <= gasUsersPercentage);

            int numPeople = 1;
            String name = "OnePersonHousehold";

            if( (i >= onePersonHouseholds) &&
                    ( i < onePersonHouseholds + twoPersonHouseholds ) ) {
                numPeople = 2;
                name = "TwoPersonHousehold";
            }

            if( (i >= onePersonHouseholds + twoPersonHouseholds) &&
                    ( i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds) ){
                numPeople = 3;
                name = "ThreePersonHousehold";
            }

            if( (i >= onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds ) &&
                    ( i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds) ){
                numPeople = 4;
                name = "FourPersonHousehold";
            }

            if( (i >= onePersonHouseholds+ twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds ) &&
                    ( i < onePersonHouseholds + twoPersonHouseholds + threePersonHouseholds + fourPersonHouseholds + fivePlusPersonHouseholds) ){
                numPeople = 5;
                name = "FivePlusPersonHousehold";
            }

            Actor actor = new Household(idActor++, ActorType.HOUSEHOLD, "OnePersonHousehold",
                    GovRole.RULEFOLLOW, BusinessStructure.OTHER, OwnershipModel.INDIVIDUAL,
                    numPeople, hasGas, true, 0);

            consumptionActors.add((ConsumptionActor) actor);

            //Id of conventional 90s SPM
            int idSpm = 13;

            Spm spm = createSpm(idSpm);

            /**
             * Actor Asset rel
             * */
            //create new relationship btw the household and SPM
            ActorAssetRelationship actorSpmRel = new ActorAssetRelationship( actor,spm,ActorAssetRelationshipType.USE,100);

            //Store relationship with assets in the actor object
            actor.addAssetRelationship(actorSpmRel);
            //Store relationship in the global array of all actor asset rel
            actorAssetRelationships.add(actorSpmRel);

            /**
             * Actor Actor rel
             * */
            //create new relationship and contract btw the household and Retailer
            ActorActorRelationship actorActHouseRetailRel = new ActorActorRelationship( actor,actor_register.get(4),ActorActorRelationshipType.BILLING);

            //Store relationship with actor in the actor object
            actor.addContract(actorActHouseRetailRel);
            //Store relationship in the global array of all actor asset rel
            actorActorRelationships.add(actorActHouseRetailRel);


            /**
             * The code below is only for visualization
             **/

            //Add Random Location of EU in the layout
            Double2D euLoc = new Double2D(random.nextDouble() * 1000, random.nextDouble() * 1000);
            layout.setObjectLocation(actor, euLoc);

            //Use same Loc for the SPM
            Double2D spmLoc = new Double2D(euLoc.x + 0, euLoc.y + 0);
            for (ActorAssetRelationship rel : actor.assetRelationships) {
                //Add all SPMs location for this EU recursively, decreasing the diameter and
                //shifting when more than one smp is contained
                //Only draw SPMs in the meantime
                if( Spm.class.isInstance( rel.getAsset()) )
                    addSPMLocation( (Spm) rel.getAsset(), spmLoc, rel.getAsset().diameter());
            }


        }

    }

    public void start() {
        super.start();
        layout = new Continuous2D(10.0, 600.0, 600.0);
        spm_register = new HashMap<>();
        gen_register = new HashMap<>();
        network_register = new HashMap<>();
        actor_register = new HashMap<>();

        //loadCaseStudy("SPM");
            /*createSpm( "inner city");
            createSpm("individual");
            createSpm("primary");*/
        System.out.println(layout.toString());
        //loadCaseStudy("SPMsTimaruSt");
        //loadActor();

        selectActors("Actor90s");

        generateHouseholdsNineties();

        selectActorActorRelationships("ActorActor90s");

        selectActorAssetRelationships("ActorAsset90s");//from https://www.secv.vic.gov.au/history/

    }

    public static void main(String[] args) {
        doLoop(Gr4spSim.class, args);


        System.exit(0);
    }

}
