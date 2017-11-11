package core;


import sim.engine.*;
import sim.field.continuous.Continuous2D;


public class DataManager extends SimState {
	private static final long serialVersionUID = 1;
	
	public Continuous2D dummyField;
	public DataManager(long seed) {
		super(seed);
	}
	
    public void start()
    {
	    super.start();  
        dummyField = new Continuous2D(1000.0, 600.0, 600.0);

    }
    
    public static void main(String[] args)
    {
    	doLoop(DataManager.class, args);
    	System.exit(0);
    }   

}
