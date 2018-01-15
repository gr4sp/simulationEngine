package core;

import sim.util.Double2D;

public class EnergyGrid implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	private int gridId;
	private int type;
	private double efficiency;
	private double gridLosses;
	private int ownership;
	private int nodes;//not used for now
	
	//TODO: Ownership and Management
	
	public EnergyGrid(int gridId, int type, double efficiency, double gridLosses, int ownership){
		this.gridId = gridId;
		this.type = type;
		this.efficiency = efficiency;	
		this.ownership = ownership;
	}
	
	
	
	public int getGridId() {
		return gridId;
	}



	public void setGridId(int gridId) {
		this.gridId = gridId;
	}



	public int getType() {
		return type;
	}



	public void setType(int type) {
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



	public int getOwnership() {
		return ownership;
	}



	public void setOwnership(int ownership) {
		this.ownership = ownership;
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