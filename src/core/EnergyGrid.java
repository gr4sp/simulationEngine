package core;

import sim.util.Double2D;

public class EnergyGrid implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	private int gridId;
	private String type;
	private double efficiency;
	private double gridLosses;
	private int nodes;
	
	//TODO: Ownership and Management
	
	public EnergyGrid(int gridId, String type, double efficiency, double gridLosses, int nodes){
		this.gridId = gridId;
		this.type = type;
		this.efficiency = efficiency;	
		this.nodes = nodes;
	}
	
	
	
	public int getGridId() {
		return gridId;
	}



	public void setGridId(int gridId) {
		this.gridId = gridId;
	}



	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}


	public double getEfficiency() {
		return efficiency;
	}



	public void setEfficiency(double efficiency) {
		this.efficiency = efficiency;
	}



	public double getGridLosses() {
		return gridLosses;
	}



	public void setGridLosses(double gridLosses) {
		this.gridLosses = gridLosses;
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



	@Override
	public String toString() {
		return "EnergyGrid [gridId=" + gridId + ", type=" + type + ", efficiency=" + efficiency + ", gridLosses="
				+ gridLosses + ", nodes=" + nodes + "]";
	}
}