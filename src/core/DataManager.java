package core;


import sim.app.tutorial5.Ball;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;


public class DataManager extends SimState {
	private static final long serialVersionUID = 1;
	
	//It's a counter that we are going to use to create unique id every time a generator is created
	private int numGenerators;
	
	private Continuous2D layout;
	public DataManager(long seed) {
		super(seed);
		numGenerators = 0;
	}
	
	Generator createRandomGenerator(){
		
		// id is the number of generators
		int id = numGenerators;
		
		String[] fuelSourceDescriptions = { "brown coal", "black coal", "natural gas", "Diesel", 
				"waste coal mine gas", "landfill methane", "solar", "wind", "hydro" };
		String[] fuelSource = {"fossil fuel", "renewable", "biomass", "waste", "surplus", "other"};
		double size;
		String[] name = {"name from database"};
		String[] ownership = {"public", "private", "public private partnership"};//need something to specify the organization it belongs to if it does.
		String [] techTypeDescriptor = {"combustion steam-subcritical",}; 
		
		
		
		// generate a random number btw 0 and 9 to choose the desc randomly
		int idDesc = random.nextInt( fuelSourceDescriptions.length );
		int idfuelSource = random.nextInt( fuelSource.length );
		int idName = random.nextInt( name.length );
		int idOwnership = random.nextInt( ownership.length );
		
		
		
		//Generator gen = new Generator(id, fuelSourceDescriptions[idDesc],fuelSource[idfuelSource],name,);
		Generator gen ;
		//increase counter number of generators
		numGenerators+=1;		
		
		return gen;
		
	}
	
	public Storage createRandomStorage() {
		Storage s;
		
		return s;
	}
	
	//create random SPM
	public void createaRandomSpm ( int numSpms ) {
	
		 Steppable[] s = new Steppable[numSpms];
	        
	        // make the SPMs
	        for(int i=0; i<numSpms;i++)
	            {
	        		//We have to create the generators. We need to create a Bag (Array)
	        		Bag generators = new Bag();
	        		
	        		//Randomly generate betrween 0 to 5 generators
	        		int numGen = random.nextInt(5);
	        		
	        		//Creating numGen generators to be added into the SPM
	        		for( int j = 0; j < numGen; j++) {
	        			Generator gen = createRandomGenerator();
	        			generators.add( gen );
	        		}
	        		
	        		//Create Storage
	        		
	        		boolean hasStorage = random.nextBoolean();
	        		
	        		Storage storage = null;
	        		if( hasStorage ) {
	        			storage = createRandomStorage();
	        		}
	        		
	        		//Create Grid
	        			
	        		// must be final to be used in the anonymous class below
	        		final Spm spmx = new Spm(generators, gridType, gridNetwork, hasStorage, storageType, connectionPoint, efficiency, embGHG) 
	            }
	      
	        /*      
	            balls.setObjectLocation(ball,
	                new Double2D(random.nextDouble() * 100,
	                    random.nextDouble() * 100));
	            bands.addNode(ball);
	            schedule.scheduleRepeating(ball);
	            
	            // schedule the balls to compute their force after everyone's moved
	            s[i] = new Steppable()
	                {
	                private static final long serialVersionUID = 1;

	                public void step(SimState state) { ball.computeForce(state); }
	                };
	            }*/
	            
	        // add the sequence
	        schedule.scheduleRepeating(Schedule.EPOCH,1,new Sequence(s),1);
	}
	
    public void start()
    {
	    super.start();  
        layout = new Continuous2D(10.0, 600.0, 600.0);
        createRandomSpm( 10 );

    }
    
    public static void main(String[] args)
    {
    	doLoop(DataManager.class, args);
    	System.exit(0);
    }   

}
