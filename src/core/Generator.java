package core;

import sim.util.Double2D;

public class Generator implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int id;//tech id
	private String fuelSource; //fossil fuel, renewable, biomass, waste, surplus, other.
	private String name;//name of power plant
	private String fuelSourceDescriptor; //fuel source descriptor: according to AEMO it can be: brown coal, black coal, natural gas, Diesel, waste coal mine gas, landfill methane, etc. 
	private String ownership;//public, private, PPP, Cooperative?
	private String techTypeDescriptor; //combustion steam-subcritical, combustion Open Cycle Gas turbines (OCGT) phtovoltaic flat panet, hydro-gravity, hydro-run of river, solar PV, wind onshore/offshore,
	private double maxCapacity; //maximum capacity in kW
	private double unitSize;//unit size - dimensions. Depending on the range can fall into small/medium or large generation unit. //TODO: find the possible ranges!
	private double efficiency; //0-1 efficiency of conversion
	private int lifecycle; //life cycle in years
	private double constructionPeriod;//construction period
	//private double capex; //capital costs in AUD/capacity unit (kW)
	private double fixedCosts; //fixed costs operation and maintenance in AUD/capacity unit per year
	private double peakContribFactor; //peak contribution factor in percentage
	
	private Double2D location;//coordinates where the generation is located

	//TODO: Ownership and Management definition
	
	public Generator(int id, String fuelSourceDescriptor, String fuelSource, String name, 
			String ownership, String techTypeDescriptor, double maxCapacity, String size, double efficiency, int lifecycle, double constructionPeriod,
			double fixedCosts, double peakContribFactor){
		this.id = id;
		this.fuelSourceDescriptor = fuelSourceDescriptor;
		this.fuelSource = fuelSource;
		this.name = name;
		this.maxCapacity = maxCapacity;
		this.efficiency = efficiency;		
		this.lifecycle = lifecycle;
		this.constructionPeriod = constructionPeriod;
		//this.capex = capex;
		this.fixedCosts = fixedCosts;
		this.peakContribFactor = peakContribFactor;
	}
	public Generator(int id, String name, double maxCapacity, double efficiency){
		this.id = id;
		//this.fuelSourceDescriptor = fuelSourceDescriptor;
		//this.fuelSource = fuelSource;
		this.name = name;
		this.maxCapacity = maxCapacity;
		this.efficiency = efficiency;
		//this.lifecycle = lifecycle;
		//this.constructionPeriod = constructionPeriod;
		//this.capex = capex;
		//this.fixedCosts = fixedCosts;
		//this.peakContribFactor = peakContribFactor;
	}
	
	@Override
	public String toString() {
		return "Generator [id=" + id + ", fuelSource=" + fuelSource + ", name=" + name + ", fuelSourceDescriptor="
				+ fuelSourceDescriptor + ", ownership=" + ownership + ", techTypeDescriptor=" + techTypeDescriptor
				+ ", maxCapacity=" + maxCapacity + ", unitSize=" + unitSize + ", efficiency=" + efficiency
				+ ", lifecycle=" + lifecycle + ", constructionPeriod=" + constructionPeriod + ", fixedCosts=" + fixedCosts + ", " +
				"peakContribFactor=" + peakContribFactor + ", location=" + location
				+ "]";//need to include capex in this constructor if used
	}
	
	public int getId() {
		return id;
	}



	public void setId(int id) {
		this.id = id;
	}



	public String getfuelSourceDescriptor() {
		return fuelSourceDescriptor;
	}



	public void setfuelSourceDescriptor(String fuelSourceDescriptor) {
		this.fuelSourceDescriptor = fuelSourceDescriptor;
	}



	public String getfuelSource() {
		return fuelSource;
	}



	public void setfuelSource(String fuelSource) {
		this.fuelSource = fuelSource;
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



	//public double getCapex() {
	//	return capex;
	//}

	//public void setCapex(double capex) {
	//	this.capex = capex;
	//}



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


	public String getOwnership() {
		return ownership;
	}



	public void setOwnership(String ownership) {
		this.ownership = ownership;
	}



	public String getFuelSourceDescriptor() {
		return fuelSourceDescriptor;
	}



	public void setFuelSourceDescriptor(String fuelSourceDescriptor) {
		this.fuelSourceDescriptor = fuelSourceDescriptor;
	}



	public String getTechTypeDescriptor() {
		return techTypeDescriptor;
	}



	public void setTechTypeDescriptor(String techTypeDescriptor) {
		this.techTypeDescriptor = techTypeDescriptor;
	}



	public double getUnitSize() {
		return unitSize;
	}



	public void setUnitSize(double unitSize) {
		this.unitSize = unitSize;
	}
	

}