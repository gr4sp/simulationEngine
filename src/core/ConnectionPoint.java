package core;

import sim.util.Double2D;

public class ConnectionPoint implements java.io.Serializable
{
	private static final long serialVersionUID = 1;
	
	private int id;
	private String name;
	private int type;
	private int locationCode;
	private String owner;
	private int ownership;
	private String description;
	private Double distanceTodemand; //interface. e.g. from feeder to demand
	private Double2D coordinates;
	
	//TODO: Ownership and Management
	
	public ConnectionPoint (int id, String name, int type, Double distanceTodemand, int locationCode, String owner, int ownership){
		this.id = id;
		this.name = name;
		this.type = type;
		this.distanceTodemand = distanceTodemand;
		this.locationCode = locationCode;
		this.owner = owner;
		this.ownership = ownership;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	public String getname() {
		return name;
	}

	public void setname(String name) {
		this.name = name;
	}
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(int locationCode) {
		this.locationCode = locationCode;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getOwnership() {
		return ownership;
	}

	public void setOwnership(int ownership) {
		this.ownership = ownership;
	}

	public double getDistanceTodemand() {
		return distanceTodemand;
	}

	public void setDistanceTodemand(Double2D coordinates) {
		this.distanceTodemand = distanceTodemand;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String toString() { return "" + description; }
}