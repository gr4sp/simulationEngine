package core;

import sim.util.Double2D;

public class Storage implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	public int type;
	public String description;
	public Double2D distanceTodemand;
	
	//TODO: Ownership and Management
	
	public Storage( ){ 
		
	}
	
	
	
	public String toString() { return "" + description; }
}