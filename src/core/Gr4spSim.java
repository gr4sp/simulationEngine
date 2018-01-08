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
        String url = "jdbc:sqlite:generators.db";


        String sql = "SELECT id, name, maxcap, efficiency  FROM Technology WHERE name = '" + name + "' ";
        Bag gens = new Bag();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("id") + "\t" +
                        rs.getString("name") + "\t" +
                        rs.getDouble("maxcap")+"\t"+
                        rs.getDouble("efficiency"));

                Generator gen = createGenerator( rs.getInt("id"), rs.getString("name"), rs.getDouble("maxcap"), rs.getDouble("efficiency") );
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

	//Creation of new storage type storage
	Storage createStorage() {
		//numSt is the number of storage units e.g. batteries
		int numSt = numStorage;
		String[] Stype = {"solid state", "flow batteries", "flywheels", "compressed air", "thermal", "pumped hydro"};
		double outCap = 3.3;
		double strCap = 6.4;
		int cycleL = 4000;
		String[] costRange = {"low", "average", "high"};

		//generate random number to choose the type and cost range
		int idType = random.nextInt(Stype.length);
		int idcostRange = random.nextInt(costRange.length);

		Storage s = new Storage(numSt, Stype[idType], outCap, strCap, cycleL, costRange[idcostRange]);
		//increases the number of generators
		numStorage += 1;
		return s;
	}

	//creation of type of energy grid

	EnergyGrid createEnergyGrid() {
		int numG = numGrid;
		String[] GType = {"Transmission", "Distribution", "Microgrid", "Building"};
		double GridEff = 0.75;
		double gridL = 0.2;
		int numNodes = 2;
		int typeId = random.nextInt(GType.length);


		EnergyGrid Grid = new EnergyGrid(numG, GType[typeId], GridEff, gridL, numNodes);

		return Grid;

	}

	//creation of random interface or connection point. This interface changes as well depending on the scale of study. A house with
	//own gene4ration off the grid will have the same house (smart metered or not) as connection point, a neighborhood can have one or more feeders and sub-stations,
	//it is basically the closest point where supply meets demand without going through any significant extra technological "treatment"
	//TODO knowledge and energy hubs within prosumer communities, where to include them?
	ConnectionPoint createConnectionPoint() {
		String[] Ctype = {""};
		String[] Cdescription = {""};
		Double2D d = new Double2D();
		ConnectionPoint C = new ConnectionPoint(0, Cdescription[0], d);
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

	//create random SPM
	public void createSpm(int numSpms) {
		double efficiency = 0.80; //efficiency all SPM
		// make the SPMs
		for (int i = 0; i < numSpms; i++) {
			//We have to create the generators. We need to create a Bag (Array)
			Bag generators = new Bag();

			//Randomly generate between 0 to 5 generators
			int numGen = random.nextInt(5);

			//Creating numGen generators to be added into the SPM
		/*	for (int j = 0; j < numGen; j++) {
				Generator gen = createGenerator();
				generators.add(gen);
			}*/

			//Create Storage

			boolean hasStorage = random.nextBoolean();

			Bag storages = new Bag();
			if (hasStorage) {
				Storage storage = createStorage();
				storages.add(storage);
			}

			//Create 4 Grids

			Bag grids = new Bag();

			int numGrids = 4;
			for (int j = 0; j < numGrids; j++) {
				EnergyGrid grid = createEnergyGrid();
				grids.add(grid);
			}
			//Create organisations
			Bag organisations = new Bag();
			int numOrganisations = 3;
			for (int k = 0; k < numOrganisations; k++) {
				SocialOrgStructure organisation = createSocialOrgs();
				organisations.add(organisation);
			}
			//Create Connection Point
			ConnectionPoint conn = createConnectionPoint();

			//Create random embGHG

			double embGHG = random.nextDouble() * 100.0;

			//Create SPM
			Spm spmx = new Spm(i, generators, grids, storages, conn, organisations, efficiency , embGHG);
			schedule.scheduleRepeating(spmx);

			layout.setObjectLocation(spmx, new Double2D(random.nextDouble() * 100, random.nextDouble() * 100));


		}
	}


    public void start()
    {
	    super.start();  
        layout = new Continuous2D(10.0, 600.0, 600.0);
        Bag gens = selectGenTech("Coal fired");
        //createSpm( 10 );
        //createSimpleTechSpm(); //creation of a simple technical spm

    }
    
    public static void main(String[] args)
    {
    	doLoop(Gr4spSim.class, args);


    	System.exit(0);
    }   

}
