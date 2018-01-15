package core;


import sim.app.tutorial5.Ball;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Gr4spSim extends SimState {
	private static final long serialVersionUID = 1;


	private Continuous2D layout;

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
	public Bag selectGenTech( String name) {
        String url = "jdbc:sqlite:technologies.db";


        String sql = "SELECT gen_id, gen_name, gen_maxcap, gen_efficiency  FROM Generators WHERE gen_name = '" + name + "' ";
        Bag gens = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println( "\t" + rs.getInt("gen_id") + "\t" +
                        rs.getString("gen_name") + "\t" +
                        rs.getDouble("gen_maxcap")+"\t"+
                        rs.getDouble("gen_efficiency"));

                Generator gen = createGenerator( rs.getInt("gen_id"), rs.getString("gen_name"), rs.getDouble("gen_maxcap"), rs.getDouble("gen_efficiency") );
                gens.add(gen);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return gens;
	}
	Generator createGenerator(int genId, String genName, Double gencap, Double genefficiency) { //create generator with random data
		//capex = 50; //capital costs in AUD/capacity unit (kW)
		// numGen is the number of generators
		int numGen = numGenerators;

		String[] fuelSource = {"fossil fuel", "renewable", "biomass", "waste", "surplus", "other"};
		String[] name = {"name from database"};
		String[] fuelSourceDescriptions = {"brown coal", "black coal", "natural gas", "Diesel",
				"waste coal mine gas", "landfill methane", "solar", "wind", "hydro"};
		String[] ownership = {"public", "private", "public private partnership"};//need something to specify the organization it belongs to if it does.
		String[] techTypeDescriptor = {"combustion steam-subcritical", "combustion Open Cycle Gas turbines (OCGT)", "photovoltaic flat panel",
				"hydro-gravity", "hydro-run of river", "solar PV", "wind onshore/offshore"};
		double maxCap = 100;
		String[] size = {"Small", "Medium", "Large"};
		double effi = 0.75;
		int lifeC = 20;//life cycle in years
		double constPer = 365; //construction period in days?
		double fixC = 10; //fixed costs operation and maintenance in AUD/capacity unit per year;
		double peakF = 55; //peak contribution factor in percentage;

		// generate a random number to choose the description, the fuel source, the name, ownership, technology description randomly
		int idDesc = random.nextInt(fuelSourceDescriptions.length);
		int idfuelSource = random.nextInt(fuelSource.length);
		int idName = random.nextInt(name.length);
		int idOwnership = random.nextInt(ownership.length);
		int idtechTypeDescriptor = random.nextInt(techTypeDescriptor.length);
		int idSize = random.nextInt(size.length);

		//TODO: smart meters? considered as the connection of the socio-technical system, it can be an enabler for empower consumers to be active participants!
		//Creation of new generator type gen, it wont include for the moment the capex.
		//Generator gen = new Generator(numGen, fuelSourceDescriptions[idDesc], fuelSource[idfuelSource], name[idName], ownership[idOwnership],
		//		techTypeDescriptor[idtechTypeDescriptor], maxCap, size[idSize], effi, lifeC, constPer, fixC, peakF);
        Generator gen =  new Generator(genId, genName, gencap, genefficiency);
		//increase counter number of generators
		numGenerators += 1;

		return gen;
		//It's a counter that we are going to use to create unique id every time a generator is created
		//Counter for the unique id each time a storage unit is created and other storage related variables
	}
    public Bag selectStorage( String name) {
        String url = "jdbc:sqlite:technologies.db";


        String sql = "SELECT storage_id, storage_name, StorageType, storageOutputCap , storageCapacity, Ownership, storage_cycleLife, storage_costRange  FROM Storage WHERE storage_name = '" + name + "' ";
        Bag strs = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println(  "\t" + rs.getInt("storage_id") + "\t" +
                        rs.getString("storage_name") + "\t" +
                        rs.getInt("StorageType") + "\t" +
                        rs.getDouble("storageOutputCap") + "\t" +
                        rs.getDouble("storageCapacity") + "\t" +
                        rs.getInt("Ownership") + "\t" +
                        rs.getDouble("storage_cycleLife") + "\t" +
                        rs.getDouble("storage_costRange"));

                Storage str = createStorage(rs.getInt("storage_id"), rs.getString("storage_name"), rs.getInt("StorageType"), rs.getDouble("storageOutputCap"),
                        rs.getDouble("storageCapacity"), rs.getInt("Ownership"), rs.getDouble("storage_cycleLife"), rs.getDouble("storage_costRange"));
                strs.add(str);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return strs;
    }
	//Creation of new storage type storage
	Storage createStorage(int strId, String strName, int strType, double outputCapacity, double storageCapacity, int ownerStr, double cycleLife,  double costRange) {
		//numSt is the number of storage units e.g. batteries
		int numSt = numStorage;
		/*String Stype = {"solid state", "flow batteries", "flywheels", "compressed air", "thermal", "pumped hydro"};
		double outCap = 3.3;
		double strCap = 6.4;
		int cycleL = 4000;
		String[] costRange = {"low", "average", "high"};

		//generate random number to choose the type and cost range
		int idType = random.nextInt(Stype.length);
		int idcostRange = random.nextInt(costRange.length);
*/
		Storage str = new Storage(strId, strName, strType, outputCapacity, storageCapacity, ownerStr, cycleLife, costRange);
		//increases the number of generators
		numStorage += 1;
		return str;
	}

	//creation of type of energy grid
    public Bag selectGrid( String id) {
        String url = "jdbc:sqlite:technologies.db";


        String sql = "SELECT grid_id, Grid_Type, gridEfficiency, gridLosses, ownership  FROM Grid WHERE grid_id = '" + id + "' ";
        Bag grids = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("grid_id") + "\t" +
                        rs.getInt("Grid_Type") + "\t" +
                        rs.getDouble("gridEfficiency")+"\t"+
                        rs.getDouble("gridLosses")+ "\t" +
                        rs.getInt("Ownership"));

                EnergyGrid grid = createEnergyGrid( rs.getInt("grid_id"), rs.getInt("Grid_Type"), rs.getDouble("gridEfficiency"),
                        rs.getDouble("gridLosses"), rs.getInt("Ownership") );
                grids.add(grid);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return grids;
    }
	EnergyGrid createEnergyGrid(int gridId, int gridType, double gridEff, double gridLoss, int gridOwner) {
		int numG = numGrid;
		/*String[] GType = {"Transmission", "Distribution", "Microgrid", "Building"};
		double GridEff = 0.75;
		double gridL = 0.2;
		int numNodes = 2;
		int typeId = random.nextInt(GType.length);*/


		EnergyGrid Grid = new EnergyGrid(gridId, gridType, gridEff, gridLoss, gridOwner);

		return Grid;

	}

	//creation of random interface or connection point. This interface changes as well depending on the scale of study. A house with
	//own gene4ration off the grid will have the same house (smart metered or not) as connection point, a neighborhood can have one or more feeders and sub-stations,
	//it is basically the closest point where supply meets demand without going through any significant extra technological "treatment"
	//TODO knowledge and energy hubs within prosumer communities, where to include them?
    public Bag selectConnectionPoint( String name) {
        String url = "jdbc:sqlite:technologies.db";


        String sql = "SELECT cpoint_id, cpoint_name, CPoint_type, distanceToDemand, cpoint_locationCode, cpoint_owner, Ownership FROM ConnectionPoint WHERE cpoint_name = '" + name + "' ";
        Bag cpoints = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println(  "\t" +  rs.getInt("cpoint_id") + "\t" +
                        rs.getString("cpoint_name") + "\t" +
                        rs.getInt("CPoint_Type") + "\t" +
                        rs.getDouble("distanceToDemand")+"\t"+
                        rs.getInt("cpoint_locationCode")+ "\t" +
                        rs.getString("cpoint_owner")+ "\t" +
                        rs.getInt("Ownership"));

                ConnectionPoint cpoint = createConnectionPoint( rs.getInt("cpoint_id"), rs.getString("cpoint_name"), rs.getInt("CPoint_Type"), rs.getDouble("distanceToDemand"),
                        rs.getInt("cpoint_locationCode"), rs.getString("cpoint_owner"), rs.getInt("Ownership") );
                cpoints.add(cpoint);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cpoints;
    }

    ConnectionPoint createConnectionPoint(int cPtId, String cPtName, int cPtType, double cPtDistance, int cPtlocation, String cPtOwner, int cPtOwnership  ) {
		int numc = numcpoints ;
	    String[] Ctype = {""};
		String[] Cdescription = {""};
		Double2D d = new Double2D();
		/*d.x=10.0;
		d.y=10.0;*/
		ConnectionPoint C = new ConnectionPoint (cPtId, cPtName, cPtType,cPtDistance, cPtlocation, cPtOwner, cPtOwnership);
        numcpoints += 1;
		return C;

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
		numOrg+=1;
		return SO;
	}


    public void createSpm (String name)
    {
        /**
         * Get the SPM id
         */
                
        String url = "jdbc:sqlite:technologies.db";

        String spmSql = "SELECT Spm.id, Spm.name, Spm.description FROM Spm WHERE Spm.name = '" + name + "' ";

        int spmId = 0;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmSql)) {

           // loop through the result set
            while (rs.next()) {

                spmId = rs.getInt("id");

                System.out.println("SPM '" + rs.getInt("id") + "' SPM: ");

                }

            }
        catch (SQLException e){


        System.out.println(e.getMessage());

    }
        /**
         *  Get list of Generators from DB
         */
        String spmGenSql = "SELECT Spm.id, Spm.name, Generators.gen_id, Generators. gen_name FROM Spm as Spm JOIN Spm_gen_mapping join Generators on " +
                "Generators.gen_id = Spm_gen_mapping.genId and Spm.id = Spm_gen_mapping.spmId and Spm.name = '" + name + "' ";

        Bag gens = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGenSql)) {

            Boolean printed = false;
            // loop through the result set
            while (rs.next()) {

                    if(!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Generators:");
                    printed=true;
                }

                Bag genSpm = selectGenTech( rs.getString("gen_name") );
                gens.addAll(genSpm);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());

        }


        /**
         * Get list of STORAGES from DB
         */
        String spmStrSql = "SELECT Spm.id, Spm.name, Storage.storage_id, Storage.storage_name FROM Spm as Spm JOIN Spm_storage_mapping join Storage " +
                "on Storage.storage_id = Spm_storage_mapping.storageId and Spm.id = Spm_storage_mapping.spmId and Spm.name = '" + name + "' ";
        Bag strs = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmStrSql)) {
             Boolean printed = false;

             // loop through the result set
            while (rs.next()) {


                if(!printed) {
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
         * Get list of GRIDS from DB
         */
        String spmGridSql = "SELECT Spm.id, Spm.name, Grid.grid_id, Grid.Grid_Type FROM Spm as Spm JOIN Spm_grid_mapping join Grid " +
                "on Grid.grid_id = Spm_grid_mapping.gridId and Spm.id = Spm_grid_mapping.spmId and Spm.name = '" + name + "' ";
        Bag grids = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmGridSql)) {
             Boolean printed = false;

            // loop through the result set
            while (rs.next()) {
                if(!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Grids:");
                    printed = true;
                }
                Bag SpmGrid = selectGrid(rs.getString("grid_id"));
                grids.addAll(SpmGrid);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        /**
         * Get list of CONNECTION POINTS from DB
         */
        String spmCptSql = "SELECT Spm.id, Spm.name, ConnectionPoint.cpoint_id, ConnectionPoint.cpoint_name FROM Spm as Spm JOIN Spm_connection_mapping join ConnectionPoint " +
                "on ConnectionPoint.cpoint_id = Spm_connection_mapping.connectionId and Spm.id = Spm_connection_mapping.spmId and Spm.name = '" + name + "' ";
        Bag cpoints = new Bag();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(spmCptSql)) {
            Boolean printed = false;
            // loop through the result set
            while (rs.next()) {
                if (!printed) {
                    System.out.println("SPM '" + rs.getString("name") + "' Connection Points:");
                    printed = true;
                }
                Bag SpmCpoints = selectConnectionPoint( rs.getString("cpoint_name"));
                cpoints.addAll(SpmCpoints);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }



        //Bag cpoints = selectConnectionPoint("BK Brunswick");
        //double embGHG = random.nextDouble() * 100.0; todo: this has to be calculated according to the components of the spm!
        //double efficiency = 0.80; //efficiency all SPM

        /**
         * Create new SPM
         */
        Spm spmx = new Spm(spmId, gens, grids, strs, cpoints);
        layout.setObjectLocation(spmx, new Double2D(random.nextDouble() * 100, random.nextDouble() * 100));


    }


    public void start()
    {
	    super.start();  
        layout = new Continuous2D(10.0, 600.0, 600.0);
        createSpm( "inner city");
        createSpm("individual");
        createSpm("primary");
        System.out.println(layout.toString());
    }
    
    public static void main(String[] args)
    {
    	doLoop(Gr4spSim.class, args);


    	System.exit(0);
    }   

}
