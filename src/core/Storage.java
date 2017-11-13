package core;

import sim.util.Double2D;

public class Storage implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int type;
	private String description;
	private Double2D distanceTodemand;
	
	//TODO: Ownership and Management
	
	public Storage(int type, String description, Double2D distanceTodemand ){ 
		this.type = type;
		this.description = description;
		this.distanceTodemand = distanceTodemand;
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



	public Double2D getDistanceTodemand() {
		return distanceTodemand;
	}



	public void setDistanceTodemand(Double2D distanceTodemand) {
		this.distanceTodemand = distanceTodemand;
	}



	public static long getSerialversionuid() {
		return serialVersionUID;
	}



	public String toString() { return "" + description; }
}