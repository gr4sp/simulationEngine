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

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;


public class Spm extends SimplePortrayal2D implements Steppable, Asset {
    private static final long serialVersionUID = 1;

    private int id;
    //For the Technical layout
    //Generators
    private Bag generators;

    //Contained SPMs
    private Bag spms_contained;

    //Distribution Network
    private Bag network_assets;

    //Storage
    private Bag storages;
    private boolean hasStorage;

    //Interface: Connection Point
    private Bag spm_interface;
    //For the societal layout

    //Some performance metrics
    //Efficiency
    private double efficiency;

    //Embodied GHG Emissions
    private double embGHG;

    //Costs
    private double installCosts;
    private double maintenanceCosts;


    //Visualization Parameters
    public double diameter;

    //Ownerships
    //TODO: add list of owners

    ArrayList<ActorAssetRelationship> assetRelationships;



    public Spm(int id, Bag spms_contained, Bag generators, Bag network_assets, Bag storages, Bag spm_interface) {
        this.id = id;
        this.spms_contained = spms_contained;
        this.generators = generators;
        this.network_assets = network_assets;
        this.hasStorage = (storages.isEmpty() == false);
        this.storages = storages;
        this.spm_interface = spm_interface;
        this.diameter = 50.0;
        this.assetRelationships = new ArrayList<>();

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
        return "Spm [id=" + id + ", spms_contained=" + spms_contained + ", generators=" + generators + ", network_assets=" + network_assets + ", storages=" + storages
                + ", hasStorage=" + hasStorage + ", spm_interface=" + spm_interface +
                "efficiency=" + efficiency + ", embGHG=" + embGHG + ", installCosts=" + installCosts + ", maintenanceCosts=" + maintenanceCosts
                + "]";
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

    public Bag getSpms_contained() {
        return spms_contained;
    }

    public void setSpms_contained(Bag spms_contained) {
        this.spms_contained = spms_contained;
    }

    public Bag getGenerators() {
        return generators;
    }


    public void setGenerators(Bag generators) {
        this.generators = generators;
    }


    public Bag getNetwork_assets() {
        return network_assets;
    }


    public void setNetwork_assets(Bag network_assets) {
        this.network_assets = network_assets;
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


    public Bag getSpm_interface() {
        return spm_interface;
    }


    public void setSpm_interface(Bag spm_interface) {
        this.spm_interface = spm_interface;
    }


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


    public void step(SimState state) {
        Gr4spSim data = (Gr4spSim) state;

        System.out.println(this);
        // position = position + velocity
        //Double2D pos = tut.balls.getObjectLocation(this);
        //Double2D newpos = new Double2D(pos.x+velocityx, pos.y + velocityy);
        //tut.balls.setObjectLocation(this,newpos);

    }

    private Color getColorForValue(int idSpm) {

        switch (idSpm) {
            case 1:
                return Color.red;
            case 2:
                return Color.green;
            case 3:
                return Color.orange;
            case 4:
                return Color.blue;
            case 5:
                return Color.GRAY;
            case 6:
                return Color.LIGHT_GRAY;
            case 7:
                return Color.magenta;
            case 8:
                return Color.yellow;
            case 9:
                return Color.black;
            case 10:
                return Color.darkGray;
            case 11:
                return Color.cyan;
            case 12:
                return Color.pink;
        }
        return Color.white;
    }

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
        double width = info.draw.width * this.diameter;
        double height = info.draw.height * this.diameter;

        Color c = new Color(getColorForValue(id).getRed(), getColorForValue(id).getGreen(), getColorForValue(id).getBlue(), 200);
        graphics.setColor(c);

        int x = (int) (info.draw.x - width / 2.0D);
        int y = (int) (info.draw.y - height / 2.0D);
        int w = (int) width;
        int h = (int) height;
        graphics.fillOval(x, y, w, h);


    }

    public boolean hitObject(Object object, DrawInfo2D range) {
        double SLOP = 1.0D;
        double width = range.draw.width * this.diameter;
        double height = range.draw.height * this.diameter;
        Ellipse2D.Double ellipse = new Ellipse2D.Double(range.draw.x - width / 2.0D - 1.0D, range.draw.y - height / 2.0D - 1.0D, width + 2.0D, height + 2.0D);
        return ellipse.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
    }

}
    
