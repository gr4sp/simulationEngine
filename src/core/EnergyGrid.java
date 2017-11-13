package core;

import sim.util.Double2D;

public class EnergyGrid implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int type;
	private String description;
	private double efficiency;
	private int nodes;
	
	//TODO: Ownership and Management
	
	public EnergyGrid(int type, String description, double efficiency, int nodes){
		this.type = type;
		this.description = description;
		this.efficiency = efficiency;		
		this.nodes = nodes;
	}
	
	
	
	public int getType() {
		return type;
	}



	public void setType(int type) {
		this.type = type;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public double getEfficiency() {
		return efficiency;
	}



	public void setEfficiency(double efficiency) {
		this.efficiency = efficiency;
	}



	public int getNodes() {
		return nodes;
	}



	public void setNodes(int nodes) {
		this.nodes = nodes;
	}



	public static long getSerialversionuid() {
		return serialVersionUID;
	}



	public String toString() { return "" + description; }
}