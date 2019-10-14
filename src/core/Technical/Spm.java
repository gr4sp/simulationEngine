/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
//using the sim.engine, sim.portrayal, sim.util, sim.field.network, sim.field.continuous from Mason to create the SPMs

package core.Technical;

import core.Relationships.ActorAssetRelationship;
import core.Gr4spSim;
import core.Relationships.Arena;
import core.Relationships.Bid;
import sim.engine.*;
import sim.portrayal.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.*;


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

    //Generation metrics
    private double genCapacityAvailable;
    private double genEemissionFactor;

    //Costs
    private double installCosts;
    private double maintenanceCosts;


    //Visualization Parameters
    public double diameter;

    //Indicators
    public double currentEmissions; //operational emissions
    public float currentPrice;
    public float currentReliability;


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

        this.currentEmissions = 0;
        this.currentPrice = 0;
        this.currentReliability = 0;

        this.genCapacityAvailable=0;
        this.genEemissionFactor=0;

    }

    //Get onsite generation
    public double getOnsiteGeneration(Gr4spSim data, Date today, HashMap<Date, Integer> newHouseholdsPerDate) {
        double generationKWh = 0.0;

        for (Generator g : generators) {
            generationKWh += g.getGeneration(data, today, newHouseholdsPerDate);
        }

        for (Storage s : storages) {
            generationKWh += s.getGeneration(data, today, newHouseholdsPerDate);
        }

        return generationKWh;
    }

    //Get currently active generators into 2 arrays, the ones in the spot market, and non spot market
    public void getActiveGensThisSPM(Date today, ArrayList<Generator> inSpotMarket, ArrayList<Generator> outSpotMarket) {
        for (Generator g : generators) {
            //Has started today or earlier?
            if (g.getStart().before(today) || g.getStart().equals(today)) {
                //Has not finished operations?
                if (g.getEnd().after(today)) {
                    //Check nominal capacity is greater than 30 MW in order to know participation in spot Market
                    if (g.getMaxCapacity() >= 30)
                        inSpotMarket.add(g);
                    else
                        outSpotMarket.add(g);
                }
            }
        }

    }


    //Get currently active generators
    public void getActiveGensAllSPM(Date today, ArrayList<Generator> inSpotMarket, ArrayList<Generator> outSpotMarket) {

        getActiveGensThisSPM(today, inSpotMarket, outSpotMarket);

        //Recursively get active generators contained in SPM
        for (Spm scontained : spms_contained) {
            scontained.getActiveGensAllSPM(today, inSpotMarket, outSpotMarket);
        }
    }

    //Calculates recursively the network losses from Generation to Consumer through each level of SPM traversed
    public double computeRecursiveNetworksLosses(Spm spm) {
        double networkLoss = 0.0;

        //Compute average loss across networks of current SPM
        if (spm.network_assets.size() > 0) {
            for (NetworkAssets n : spm.network_assets) {
                networkLoss += n.getGridLosses();
            }
            networkLoss /= spm.network_assets.size();
        }
        //Sum the (avg, if contains more than 1 spm as a direct child) Network Losses of Contained SPMs
        //Each level of SPM is averaged, but each network loss of an SPM is summed
        if (spm.spms_contained.size() > 0) {
            for (Spm s : spm.spms_contained) {
                networkLoss += spm.computeRecursiveNetworksLosses(s);
            }
        }

        return networkLoss;
    }

    //Calculates the network losses of current SPM
    public double computeNetworksLosses() {
        double networkLoss = 0.0;

        //Compute average loss across networks of current SPM
        if (network_assets.size() > 0) {
            for (NetworkAssets n : network_assets) {
                networkLoss += n.getGridLosses();
            }
            networkLoss /= network_assets.size();
        }

        return networkLoss;
    }


    //Recursively get Total available Capacity in each spm contained in this SPM
    public double getTotalCapacityInSPMs() {

        double totalCapacity = this.getGenCapacityAvailable();

        for (Spm scontained : spms_contained) {
            totalCapacity += scontained.getTotalCapacityInSPMs();
        }

        return totalCapacity;
    }

    //Recursively get normalize each contained SPM with Total Capacity
    public void normalizeEmissionFactorRecursively(double totalCapacity) {
        this.genEemissionFactor /= totalCapacity;

        for (Spm scontained : spms_contained) {
            scontained.normalizeEmissionFactorRecursively(totalCapacity);
        }
    }

    /**
     * If spot market has started, combine emissions intensity from spot and out of spot, normalized by total capacity used.
     * Compute emissionIntensity based on Merit Order bids in Spot Market
     * @param state
     * @param gensInSpotMarket
     * @param gensOutSpotMarket
     */
    public void computeEmissionsFactorActiveSpotMarket(SimState state, ArrayList<Generator> gensInSpotMarket, ArrayList<Generator> gensOutSpotMarket){
        Gr4spSim data = (Gr4spSim) state;

        /**
         * In SPOT
         */
        double capacityInSpot = 0.0;
        double emissionFactorGenerationInSpot = 0.0;

        Calendar c = Calendar.getInstance();
        c.setTime(data.getCurrentSimDate());
        int currentYear = c.get(Calendar.YEAR);

        //Get Arena from Gr4spSim
        Arena a = data.getArenaByName("spot");

        //Compute capacity from successful bidders from THIS SPM .
        ArrayList<Bid> bids = a.getSpot().getSuccessfulBids();
        for (Bid b : bids) {
            Generator g = (Generator) b.asset;

            //if the bid has been made by a generator in this SPM, then count its emmission intensity
            if (gensInSpotMarket.contains(g)) {

                emissionFactorGenerationInSpot += b.capacity * g.getEmissionsFactor(currentYear);
                capacityInSpot += b.capacity;
            }
        }

        /**
         * Out SPOT
         */

        //Get the emission factor of generators within THIS spm OUT spot market
        double capacityOutSpot = 0.0;
        double emissionFactorGenerationOutSpot = 0.0;

        //It uses the same available capacity (capacity factors) used by bidders in markets
        for (Generator g : gensOutSpotMarket) {
            double capacityAvailable = g.computeAvailableCapacity(data);
            emissionFactorGenerationOutSpot += capacityAvailable * g.getEmissionsFactor(currentYear);
            capacityOutSpot += capacityAvailable;
        }

        //Normalize using the total capacity
        genCapacityAvailable = capacityInSpot + capacityOutSpot;

        //CDEII
        this.genEemissionFactor = emissionFactorGenerationInSpot + emissionFactorGenerationOutSpot;
    }

    /**
     * Computes Indicators and starts with recursion level 0 by default
     * @param state
     * @param consumption
     */
    public void computeIndicators(SimState state, double consumption) {
        computeIndicators(state, consumption, 0);
    }

    /**
     * Recursive computation of indicators
     * @param state
     * @param consumption
     * @param recursionLevel
     */
    public void computeIndicators(SimState state, double consumption, int recursionLevel) {
        Gr4spSim data = (Gr4spSim) state;
        ArrayList<Generator> gensInSpotMarket = new ArrayList<Generator>();
        ArrayList<Generator> gensOutSpotMarket = new ArrayList<Generator>();

        Date today = data.getCurrentSimDate();


        //Get Active gens In/Out spot market in this SPM
        this.getActiveGensThisSPM(today, gensInSpotMarket, gensOutSpotMarket);

        /**
         * Get the emission factor of generators
         */

        //If spot hasn't started, get historic information of generation per fuel type using all generators
        if (data.getStartSpotMarketDate().after(today)) {
            ArrayList<Generator> activegens = new ArrayList<Generator>();
            activegens.addAll(gensInSpotMarket);
            activegens.addAll(gensOutSpotMarket);

            //compute Emission Factor t-CO2/MWh from historic generation contributions
            Generation genData = data.getMonthly_generation_register().get(today);
            this.genEemissionFactor = genData.computeGenEmissionIntensity(activegens, today);
            for (Generator g : activegens) {
                genCapacityAvailable += g.computeAvailableCapacity(data);
            }
            //CDEII
            this.genEemissionFactor = this.genCapacityAvailable * this.genEemissionFactor;

        } else {

            //If spot market has started, combine emissions intensity from spot and out of spot, normalized by total capacity used
            //Compute emissionIntensity based on Merit Order bids in Spot Market
            this.computeEmissionsFactorActiveSpotMarket(data,gensInSpotMarket,gensOutSpotMarket);

        }


        //Compute LossFactor
        double lossFactor = computeNetworksLosses();

        //Base Case, when an SPM doesn't contain any other SPM
        this.genEemissionFactor = (1 / (1 - lossFactor)) * this.genEemissionFactor;

        //Recursion through SPMs

        for (Spm s_inside : spms_contained) {
            s_inside.computeIndicators(state, consumption, recursionLevel + 1);
            this.genEemissionFactor += (1 / (1 - lossFactor)) * s_inside.getGenEemissionFactor();
        }

        //When recursion has finished, compute normalized CDEII
        if (recursionLevel == 0) {

            double totalCapacity = this.getTotalCapacityInSPMs();
            this.normalizeEmissionFactorRecursively(totalCapacity);

            //Direct Operational Generators Emissions = Generated E * EmissionFactor
            //Emission factor t-CO2/MWh * consumption in MWh
            this.currentEmissions = consumption * this.getGenEemissionFactor();

        }


        currentPrice = 0;
        currentReliability = 0;
    }


//    //Get the emission factor of all genereators (This and contained SPM)
//    ArrayList<Generator> activeGensAllSPM = this.getActiveGensAllSPM(today);
//    double emissionFactorAllGenerators = genData.computeGenEmissionIntensity(activeGensAllSPM);

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
    public void addAssetRelationship(ActorAssetRelationship newAssetRel) {
        this.assetRelationships.add(newAssetRel);
    }

    public double getGenCapacityAvailable() {
        return genCapacityAvailable;
    }

    public double getGenEemissionFactor() {
        return genEemissionFactor;
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

        Date today = data.getCurrentSimDate();


    }

}
    
