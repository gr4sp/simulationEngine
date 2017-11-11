package core;

import sim.util.Double2D;

public class ConnectionPoint implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	public int type;
	public String description;
	public Double2D distanceTo;
	
	//TODO: Ownership and Management
	
	public ConnectionPoint( ){ 
		
	}
	
	
	
	public String toString() { return "" + description; }
}