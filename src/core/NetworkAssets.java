package core;

import sim.util.Double2D;

import java.util.ArrayList;

public class NetworkAssets implements java.io.Serializable, Asset
{
	private static final long serialVersionUID = 1;
	private int netId;
	private String type;
	private String subtype;
	private String grid;
	private String assetName;
	private String grid_node_name;
	private Double2D location;//coordinates where the network assets are located
	private double gridLosses;
	private int gridVoltate;
	private String owner;

	//Visualization Parameters
	public double diameter;

	//TODO: Ownership and Management definition
	ArrayList<ActorAssetRelationship> assetRelationships;
	
	public NetworkAssets(int NetId, String type, String subtype, String grid, String assetName, String grid_node_name, String location_MB,
						 double gridLosses, int gridVoltage, String owner){
		this.netId = netId;
		this.type = type;
		this.subtype = subtype;
		this.grid = grid;
		this.assetName = assetName;
		this.grid_node_name = grid_node_name;
		this.gridLosses = gridLosses;
		this.gridVoltate = gridVoltage;
		this.owner = owner;
		this.diameter  = 50;

		this.assetRelationships = new ArrayList<>();


		if(location_MB != null) {
			String[] coord = location_MB.split("\\,");
			this.location = new Double2D(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
		}
		else
			this.location = null;

	}

	@Override
	public double electricityIn() {
		return 0;
	}

	@Override
	public double electricityOut() {
		return 0;
	}

	public void addAssetRelationship( ActorAssetRelationship newAssetRel){
		this.assetRelationships.add(newAssetRel);
	}

	@Override
	public double diameter() {
		return diameter;
	}


	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public int getNetId() {
		return netId;
	}

	public void setNetId(int netId) {
		this.netId = netId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public String getGrid() {
		return grid;
	}

	public void setGrid(String grid) {
		this.grid = grid;
	}

	public String getAssetName() {
		return assetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public String getGrid_node_name() {
		return grid_node_name;
	}

	public void setGrid_node_name(String grid_node_name) {
		this.grid_node_name = grid_node_name;
	}

	public Double2D getLocation() {
		return location;
	}

	public void setLocation(Double2D location) {
		this.location = location;
	}

	public double getGridLosses() {
		return gridLosses;
	}

	public void setGridLosses(double gridLosses) {
		this.gridLosses = gridLosses;
	}

	public int getGridVoltate() {
		return gridVoltate;
	}

	public void setGridVoltate(int gridVoltate) {
		this.gridVoltate = gridVoltate;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "NetworkAssets{" +
				"netId=" + netId +
				", type='" + type + '\'' +
				", subtype='" + subtype + '\'' +
				", grid='" + grid + '\'' +
				", assetName='" + assetName + '\'' +
				", grid_node_name='" + grid_node_name + '\'' +
				", location=" + location +
				", gridLosses=" + gridLosses +
				", gridVoltate=" + gridVoltate +
				", owner='" + owner + '\'' +
				'}';
	}
}