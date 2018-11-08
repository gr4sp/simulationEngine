package core;

import sim.util.Double2D;

public class Storage implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int id;
	private String storageName;
	private int type; //integer since is the foreign key to the storageType table
	private double outputCapacity; //continuous output capacity on kW
    private int ownership;
	private double storageCapacity; //storage capacity on kWh.storage capacity at the beginning of the battery life in kWh.
	//This varies during the cycle life. it needs to be multiply by the capacity fade percentage during the cycle life.
	private double cycleLife; //life in cycles.
	private double costRange;
	
	//private Double2D distanceTodemand;
	
	//TODO: Ownership and Management
	
	public Storage(int id, String storageName, int type, double outputCapacity, double storageCapacity, int ownership, double cycleLife,  double costRange){
		this.id = id;
		this.storageName = storageName;
		this.type = type;
		this.outputCapacity = outputCapacity;
		this.storageCapacity = storageCapacity;
		this.cycleLife = cycleLife;
		this.costRange = costRange;
	}
	@Override
	public String toString() {
		return "Storage [id=" + id + ", name=" + storageName +", type=" + type + ", outputCapacity=" + outputCapacity + ", storageCapacity="
				+ storageCapacity + ", cycleLife=" + cycleLife + ", costRange=" + costRange + "]";
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStorageName() {
		return storageName;
	}

	public void setStorageName(String storagename) {
		this.storageName = storagename;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
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

	public double getCycleLife() {
		return cycleLife;
	}

	public void setCycleLife(double cycleLife) {
		this.cycleLife = cycleLife;
	}

	public double getCostRange() {
		return costRange;
	}

	public void setCostRange(double costRange) {
		this.costRange = costRange;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	}