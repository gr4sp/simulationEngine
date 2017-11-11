package core;

import sim.util.Double2D;

public class Generators implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int id;//tech id
	private String description;//renewable or non-re
	public String technologyType; //coal fired, gas, solar PV, etc.
	public String name;//name of power plant
	public double maxCapacity; //maximum capacity in kW
	public double efficiency; //0-1 efficiency of conversion
	public int lifecycle; //life cycle in years
	public double constructionPeriod;//construction period
	public double capex; //capital costs in AUD/capacity unit (kW)
	public double fixedCosts; //fixed costs operation and maintenance in AUD/capacity unit per year
	public double peakContribFactor; //peak contribution factor in percentage
	
	public Double2D location;//coordinates where the generation is located
	
	//TODO: Ownership and Management
	
	public Generators(int id, String description, String technologyType, String name, 
			double maxCapacity, double efficiency, int lifecycle, double constructionPeriod,
			double capex, double fixedCosts, double peakContribFactor){
		this.technologyType = technologyType;
		this.id = id;
		this.description = description;
		this.technologyType = technologyType;
		this.name = name;
		this.maxCapacity = maxCapacity;
		this.efficiency = efficiency;		
		this.lifecycle = lifecycle;
		this.constructionPeriod = constructionPeriod;
		this.capex = capex;
		this.fixedCosts = fixedCosts;
		this.peakContribFactor = peakContribFactor;
	}
	
	
	
	public int getId() {
		return id;
	}



	public void setId(int id) {
		this.id = id;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public String getTechnologyType() {
		return technologyType;
	}



	public void setTechnologyType(String technologyType) {
		this.technologyType = technologyType;
	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public double getMaxCapacity() {
		return maxCapacity;
	}



	public void setMaxCapacity(double maxCapacity) {
		this.maxCapacity = maxCapacity;
	}



	public double getEfficiency() {
		return efficiency;
	}



	public void setEfficiency(double efficiency) {
		this.efficiency = efficiency;
	}



	public int getLifecycle() {
		return lifecycle;
	}



	public void setLifecycle(int lifecycle) {
		this.lifecycle = lifecycle;
	}



	public double getConstructionPeriod() {
		return constructionPeriod;
	}



	public void setConstructionPeriod(double constructionPeriod) {
		this.constructionPeriod = constructionPeriod;
	}



	public double getCapex() {
		return capex;
	}



	public void setCapex(double capex) {
		this.capex = capex;
	}



	public double getFixedCosts() {
		return fixedCosts;
	}



	public void setFixedCosts(double fixedCosts) {
		this.fixedCosts = fixedCosts;
	}



	public double getPeakContribFactor() {
		return peakContribFactor;
	}



	public void setPeakContribFactor(double peakContribFactor) {
		this.peakContribFactor = peakContribFactor;
	}



	public Double2D getLocation() {
		return location;
	}



	public void setLocation(Double2D location) {
		this.location = location;
	}



	public String toString() { return "" + description; }
}