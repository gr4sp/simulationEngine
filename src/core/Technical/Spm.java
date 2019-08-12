/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
//using the sim.engine, sim.portrayal, sim.util, sim.field.network, sim.field.continuous from Mason to create the SPMs

package core.Technical;

import core.Relationships.ActorAssetRelationship;
import core.Gr4spSim;
import sim.engine.*;
import sim.portrayal.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class Spm extends SimplePortrayal2D implements Steppable, Asset {
    private static final long serialVersionUID = 1;

    private int id;
    //For the Technical layout
    //Generators

    private ArrayList<Generator> generators;

    //Contained SPMs
    private ArrayList<Spm> spms_contained;

    //Distribution Network
    private ArrayList<NetworkAssets> network_assets;

    //Storage
    private ArrayList<Storage> storages;
    private boolean hasStorage;

    //Interface: Connection Point
    private ArrayList<ConnectionPoint> spm_interface;
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



    public Spm(int id, ArrayList<Spm> spms_contained, ArrayList<Generator> generators, ArrayList<NetworkAssets> network_assets, ArrayList<Storage> storages, ArrayList<ConnectionPoint> spm_interface) {
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

    //Get onsite generation
    public double getOnsiteGeneration(Gr4spSim data, Date today, HashMap<Date, Integer> newHouseholdsPerDate){
        double generationKWh = 0.0;

        for(Generator g : generators){
            generationKWh += g.getGeneration(data, today, newHouseholdsPerDate);
        }

        for (Storage s : storages){
            generationKWh += s.getGeneration(data, today, newHouseholdsPerDate);
        }

        return generationKWh;
    }

    //Get currently active generators
    public ArrayList<Generator> getActiveGens(Date today){
        ArrayList<Generator> activeGens = new ArrayList<Generator>();
        for(Generator g : generators){
            //Has started today or earlier?
            if( g.getStart().before(today) || g.getStart().equals(today) ){
                //Has not finished operations?
                if(g.getEnd().after(today)){
                    activeGens.add(g);
                }
            }
        }

        //Recursively get active generators contained in SPM
        for(Spm scontained : spms_contained){
            ArrayList<Generator> activeGensContained = scontained.getActiveGens(today);
            activeGens.addAll(activeGensContained);
        }

        return activeGens;
    }

    //Calculates recursively the network losses from Generation to Consumer through each level of SPM traversed
    public double computeNetworksLosses(Spm spm){
        double networkLoss = 0.0;

        //Compute average loss across networks of current SPM
        if(spm.network_assets.size() > 0) {
            for (NetworkAssets n : spm.network_assets) {
                networkLoss += n.getGridLosses();
            }
            networkLoss /= spm.network_assets.size();
        }
        //Sum the (avg, if contains more than 1 spm as a direct child) Network Losses of Contained SPMs
        //Each level of SPM is averaged, but each depth is summed, as it's a measure of distance
        if(spm.spms_contained.size() > 0) {
            for (Spm s : spm.spms_contained) {
                networkLoss += spm.computeNetworksLosses(s);
            }
            networkLoss /= spm.spms_contained.size();
        }

        return networkLoss;
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

    public ArrayList<Spm> getSpms_contained() {
        return spms_contained;
    }

    public void setSpms_contained(ArrayList<Spm> spms_contained) {
        this.spms_contained = spms_contained;
    }

    public ArrayList<Generator> getGenerators() {
        return generators;
    }


    public void setGenerators(ArrayList<Generator> generators) {
        this.generators = generators;
    }





    public boolean isHasStorage() {
        return hasStorage;
    }


    public void setHasStorage(boolean hasStorage) {
        this.hasStorage = hasStorage;
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
    
