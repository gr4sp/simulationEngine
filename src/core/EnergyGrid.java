package core;

import sim.util.Double2D;

public class EnergyGrid implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	public int type;
	public String description;
	public double efficiency;
	public int nodes;
	
	//TODO: Ownership and Management
	
	public EnergyGrid(int type, String description, double efficiency, int nodes){
		this.type = type;
		this.description = description;
		this.efficiency = efficiency;		
		this.nodes = nodes;
	}
	
	
	
	public String toString() { return "" + description; }
}