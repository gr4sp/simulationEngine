package core.Technical;

import core.Relationships.ActorAssetRelationship;
import sim.util.Double2D;

import java.util.ArrayList;
import java.util.Date;

public class NetworkAssets implements java.io.Serializable, Asset
{
	private static final long serialVersionUID = 1;
	private int netId;
	private String type;
	private String subtype;
	private String grid;
	private String assetName;
	private String ownername;
	private Double2D location;//coordinates where the network assets are located
	private double gridLosses;
	private String gridVoltate;
	private String owner;

	//Start and end of operations
	private Date start;
	private Date end;

	//Visualization Parameters
	public double diameter;

	//TODO: Ownership and Management definition
	ArrayList<ActorAssetRelationship> assetRelationships;
	
	public NetworkAssets(int netId, String type, String subtype, String grid, String assetName, String ownername, String location_MB,
						 double gridLosses, String gridVoltage, Date start, Date end){
		this.netId = netId;
		this.type = type;
		this.subtype = subtype;
		this.grid = grid;
		this.assetName = assetName;
		this.ownername = ownername;
		this.gridLosses = gridLosses;
		this.gridVoltate = gridVoltage;
		this.diameter  = 50;

		this.assetRelationships = new ArrayList<>();
		this.start = start;
		this.end = end;

		if(location_MB != null) {
			String[] coord = location_MB.split("\\,");
			this.location = new Double2D(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
		}
		else
			this.location = null;

	}

	public void addAssetRelationship( ActorAssetRelationship newAssetRel){
		this.assetRelationships.add(newAssetRel);
	}

	@Override
	public double diameter() {
		return diameter;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
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

	public String getOwnername() {
		return ownername;
	}

	public void setOwnername(String ownername) {
		this.ownername = ownername;
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

	public String getGridVoltate() {
		return gridVoltate;
	}

	public void setGridVoltate(String gridVoltate) {
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
				", ownername='" + ownername + '\'' +
				", location_MB=" + location +
				", gridLosses=" + gridLosses +
				", gridVoltate='" + gridVoltate +'\'' +
				'}';
	}
}