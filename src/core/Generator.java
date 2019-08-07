package core;

import sim.util.Double2D;

import java.util.ArrayList;
import java.util.Date;

/*** Definition of the class generator ****/

public class Generator implements java.io.Serializable, Asset{
    private static final long serialVersionUID = 1;

    private int id;//tech id
    private String name;//name of power plant
    private String fuelSourceDescriptor; //fuel source descriptor: according to AEMO it can be: brown coal, black coal, natural gas, Diesel, waste coal mine gas, landfill methane, etc.
    private String ownership;//public, private, PPP, Cooperative?
    private String ownerName;//name of the owner
    private String techTypeDescriptor; //combustion steam-subcritical, combustion Open Cycle Gas turbines (OCGT) phtovoltaic flat panet, hydro-gravity, hydro-run of river, solar PV, wind onshore/offshore,
    private String dispatchTypeDescriptor; // scheduled or non-scheduled
    private double maxCapacity; //maximum capacity in MW

    private double efficiency; //0-1 efficiency of conversion
    private int lifecycle; //life cycle in years
    //private double constructionPeriod;//construction period
    //private double capex; //capital costs in AUD/capacity unit (kW)
    //private double fixedCosts; //fixed costs operation and maintenance in AUD/capacity unit per year
    //private double peakContribFactor; //peak contribution factor in percentage
    private double emissionsFactor;

    private Double2D location;//coordinates where the generation is located

    //Start and end of operations
    private Date start;
    private Date end;

    //Visualization Parameters
    public double diameter;

    //TODO: Ownership and Management definition
    ArrayList<ActorAssetRelationship> assetRelationships;


    /**Create here other constructors that call the main constructor to include fix values to the parameters and make the class more flexible:
  * public Generator (); this *include here the parameters values.**/

    public Generator(int genId, String genName, String owner, Double gencap, String techType, String fuelType, String dispachType, String locationCoord, Date start, Date end, double emissionsfactor) {
        this.id = genId;
        this.name = genName;
        this.ownerName = owner;
        this.maxCapacity = gencap;
        this.techTypeDescriptor = techType;
        this.fuelSourceDescriptor = fuelType;
        this.dispatchTypeDescriptor = dispachType;
        this.assetRelationships = new ArrayList<>();
        this.diameter = 50.0;
        this.start = start;
        this.end = end;
        this.emissionsFactor = emissionsfactor;

        if (locationCoord != null) {
            String[] coord = locationCoord.split("\\,");
            this.location = new Double2D(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
        } else
            this.location = null;

        //default lifecycle of 30 years
        this.lifecycle = 30;
    }

    @Override
    public double electricityIn() {
        return 0;
    }

    @Override
    public double electricityOut() {
        return 0;
    }


    @Override
    public double diameter() {
        return diameter;
    }

    @Override
    public String toString() {
        return "Generator [id=" + id +  ", name=" + name + ", fuelSourceDescriptor="
                + fuelSourceDescriptor + ", ownership=" + ownership + ", techTypeDescriptor=" + techTypeDescriptor
                + ", maxCapacity=" + maxCapacity + ", efficiency=" + efficiency
                + ", lifecycle=" + lifecycle + ", " +
                 ", location=" + location
                + "]";//need to include capex in this constructor if used
    }

    @Override
    public void addAssetRelationship( ActorAssetRelationship newAssetRel){
        this.assetRelationships.add(newAssetRel);
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


    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public double getEmissionsFactor() {
        return emissionsFactor;
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




}