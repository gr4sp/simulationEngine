package core;

import sim.util.Double2D;

public class Storage implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int id;
	private String type;
	private double outputCapacity; //continuous output capacity on kW
	private double storageCapacity; //storage capacity on kWh.storage capacity at the beginning of the battery life in kWh.
	//This varies during the cycle life. it needs to be multiply by the capacity fade percentage during the cycle life.
	private int cycleLife; //life in cycles.
	private String costRange;
	
	//private Double2D distanceTodemand;
	
	//TODO: Ownership and Management
	
	public Storage(int id, String type, double outputCapacity, double storageCapacity, int cycleLife,  String costRange){
		this.id = id;
		this.type = type;
		this.outputCapacity = outputCapacity;
		this.storageCapacity = storageCapacity;
		this.cycleLife = cycleLife;
		this.costRange = costRange;
	}
	
	
	


	public int getId() {
		return id;
	}





	public void setId(int id) {
		this.id = id;
	}





	public String getType() {
		return type;
	}





	public void setType(String type) {
		this.type = type;
	}





	public double getOutputCapacity() {
		return outputCapacity;
	}





	public void setOutputCapacity(double outputCapacity) {
		this.outputCapacity = outputCapacity;
	}





	public double getStorageCapacity() {
		return storageCapacity;
	}





	public void setStorageCapacity(double storageCapacity) {
		this.storageCapacity = storageCapacity;
	}





	public int getCycleLife() {
		return cycleLife;
	}





	public void setCycleLife(int cycleLife) {
		this.cycleLife = cycleLife;
	}





	public String getCostRange() {
		return costRange;
	}





	public void setCostRange(String costRange) {
		this.costRange = costRange;
	}





	public static long getSerialversionuid() {
		return serialVersionUID;
	}



	@Override
	public String toString() {
		return "Storage [id=" + id + ", type=" + type + ", outputCapacity=" + outputCapacity + ", storageCapacity="
				+ storageCapacity + ", cycleLife=" + cycleLife + ", costRange=" + costRange + "]";
	}
}