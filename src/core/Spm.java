/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
//using the sim.engine, sim.portrayal, sim.util, sim.field.network, sim.field.continuous from Mason to create the SPMs

package core;
import sim.engine.*;
import sim.portrayal.*;
import sim.util.*;
import sim.field.network.*;
import sim.field.continuous.*;
import java.awt.*;
import java.awt.geom.*;


public class Spm extends SimplePortrayal2D implements Steppable
    {
    private static final long serialVersionUID = 1;

    private int idSpm;
    //For the Technical layout
    //Generators
    private Bag generators;
    
    //Distribution Network
    private Bag grid;
    
    //Storage
    private Bag storages;
    private boolean hasStorage;
    
    //Interface: Connection Point
    private ConnectionPoint connectionPoint;
    //For the societal layout

	private Bag organisations;

	//Some performance metrics
    //Efficiency
    private double efficiency;
    
    //Embodied GHG Emissions
    private double embGHG;
    
    //Costs
    private double installCosts;
    private double maintenanceCosts;
        
    
    //Ownerships
    //TODO: add list of owners
        
    
    public Spm(int idSpm, Bag generators, Bag grid, Bag storages, ConnectionPoint connectionPoint,
			   Bag organisations,double efficiency, double embGHG){
    	this.idSpm = idSpm;
    	this.generators = generators;
    	this.grid = grid;
    	this.hasStorage =  ( storages.isEmpty() == false );
    	this.storages = storages;
    	this.connectionPoint = connectionPoint;
    	this.organisations = organisations;
    	this.efficiency = efficiency;
    	this.embGHG = embGHG;
    	
        
        }
    
        
    @Override
	public String toString() {
		return "Spm [idSpm=" + idSpm + ", generators=" + generators + ", grid=" + grid + ", storages=" + storages
				+ ", hasStorage=" + hasStorage + ", connectionPoint=" + connectionPoint + ", organisations=" + organisations+"," +
				"efficiency=" + efficiency + ", embGHG=" + embGHG + ", installCosts=" + installCosts + ", maintenanceCosts=" + maintenanceCosts
				+ "]";
	}


	public Bag getGenerators() {
		return generators;
	}


	public void setGenerators(Bag generators) {
		this.generators = generators;
	}


	public Bag getGrid() {
		return grid;
	}


	public void setGrid(Bag grid) {
		this.grid = grid;
	}


	public Bag getStorages() {
		return storages;
	}


	public void setStorages(Bag storages) {
		this.storages = storages;
	}


	public boolean isHasStorage() {
		return hasStorage;
	}


	public void setHasStorage(boolean hasStorage) {
		this.hasStorage = hasStorage;
	}


	public ConnectionPoint getConnectionPoint() {
		return connectionPoint;
	}


	public void setConnectionPoint(ConnectionPoint connectionPoint) {
		this.connectionPoint = connectionPoint;
	}

	public Bag getOrganisations() {return organisations;}

	public void setOrganisations(Bag organisations) {this.organisations = organisations;}

	public double getEfficiency() {
		return efficiency;
	}


	public void setEfficiency(double efficiency) {
		this.efficiency = efficiency;
	}


	public double getEmbGHG() {
		return embGHG;
	}


	public void setEmbGHG(double embGHG) {
		this.embGHG = embGHG;
	}


	public double getInstallCosts() {
		return installCosts;
	}


	public void setInstallCosts(double installCosts) {
		this.installCosts = installCosts;
	}


	public double getMaintenanceCosts() {
		return maintenanceCosts;
	}


	public void setMaintenanceCosts(double maintenanceCosts) {
		this.maintenanceCosts = maintenanceCosts;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}


	public void step(SimState state)
        {
        Gr4spSim data = (Gr4spSim) state;
        
        System.out.println(this);
        // position = position + velocity
        //Double2D pos = tut.balls.getObjectLocation(this);
        //Double2D newpos = new Double2D(pos.x+velocityx, pos.y + velocityy);
        //tut.balls.setObjectLocation(this,newpos);
        
        }
    
	
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double width = info.draw.width;
        final double height = info.draw.height;

        graphics.setColor(Color.blue);

        final int x = (int)(info.draw.x - width / 2.0);
        final int y = (int)(info.draw.y - height / 2.0);
        final int w = (int)(width);
        final int h = (int)(height);

        // draw centered on the origin
        graphics.fillOval(x,y,w,h);
        }
    
    
    }
    
