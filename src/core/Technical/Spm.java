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


public class Spm extends SimplePortrayal2D implements Steppable, Asset, java.io.Serializable  {
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
    private double genAvailable;
    private double genEmissionIntensityIndex;

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

        this.genAvailable = 0;
        this.genEmissionIntensityIndex = 0;

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

//    //Get currently active generators into 2 arrays, the ones in the spot market, and non spot market
//    public void getActiveGensThisSPM(Date today, ArrayList<Generator> inSpotMarket, ArrayList<Generator> outSpotMarket) {
//        for (Generator g : generators) {
//            //Has started today or earlier?
//            if (g.getStart().before(today) || g.getStart().equals(today)) {
//                //Has not finished operations?
//                if (g.getEnd().after(today)) {
//                    //Check nominal capacity is greater than 30 MW in order to know participation in spot Market
//                    if (g.getMaxCapacity() >= 30)
//                        inSpotMarket.add(g);
//                    else
//                        outSpotMarket.add(g);
//                }
//            }
//        }
//    }

    //Get currently active generators in 1 array, this is used before the spot market
    public void getActiveGensThisSPM(Date today, ArrayList<Generator> gens) {
        for (Generator g : generators) {
            //Has started today or earlier?
            if (g.getStart().before(today) || g.getStart().equals(today)) {
                //Has not finished operations?
                if (g.getEnd().after(today)) {
                        gens.add(g);
                }
            }
        }
    }


    //Get currently active generators in 2 lists as in/out spot market
//    public void getActiveGensAllSPM(Date today, ArrayList<Generator> inSpotMarket, ArrayList<Generator> outSpotMarket) {
//
//        getActiveGensThisSPM(today, inSpotMarket, outSpotMarket);
//
//        //Recursively get active generators contained in SPM
//        for (Spm scontained : spms_contained) {
//            scontained.getActiveGensAllSPM(today, inSpotMarket, outSpotMarket);
//        }
//    }

    //Get currently active generators in 1 list, used before spot market
    public void getActiveGensAllSPM(Date today, ArrayList<Generator> gens) {

        getActiveGensThisSPM(today, gens);

        //Recursively get active generators contained in SPM
        for (Spm scontained : spms_contained) {
            scontained.getActiveGensAllSPM(today, gens);
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
    public double computeNetworksLosses(Date today) {
        double networkLoss = 0.0;

        //Compute average loss across networks of current SPM
        if (network_assets.size() > 0) {
            for (NetworkAssets n : network_assets) {
                //Has started today or earlier?
                if (n.getStart().before(today) || n.getStart().equals(today)) {
                    //Has not finished operations?
                    if (n.getEnd().after(today)) {
                        networkLoss += n.getGridLosses();
                    }
                }
            }
            networkLoss /= network_assets.size();
        }

        return networkLoss;
    }


    //Recursively get Total available Generation in each spm contained in this SPM
    public double getTotalGenerationInSPMs() {

        double totalGeneration = this.getGenAvailable();

        for (Spm scontained : spms_contained) {
            totalGeneration += scontained.getTotalGenerationInSPMs();
        }

        return totalGeneration;
    }

    //Recursively get normalize each contained SPM with Total Capacity
    public void normalizeEmissionIntensityIndexRecursively(double totalCapacity) {
        this.genEmissionIntensityIndex /= totalCapacity;

        for (Spm scontained : spms_contained) {
            scontained.normalizeEmissionIntensityIndexRecursively(totalCapacity);
        }
    }

    /**
     * If spot market has started, combine emissions intensity from spot and out of spot, normalized by total capacity used.
     * Compute emissionIntensity based on Merit Order bids in Spot Market
     *
     * @param state
     * @param gensInSpmActive
     */
    public void computeCDEIISpotMarket(SimState state, ArrayList<Generator> gensInSpmActive) {
        Gr4spSim data = (Gr4spSim) state;

        /**
         * In SPOT
         */
        double totalGenerationInSpot = 0.0;
        double CDEInSpot = 0.0;

        Calendar c = Calendar.getInstance();
        c.setTime(data.getCurrentSimDate());
        int currentYear = c.get(Calendar.YEAR);

        //Get Arena from Gr4spSim
        Arena a = data.getArenaByName("spot");

        //Compute capacity from successful bidders from THIS SPM .
        /**
         * Primary Spot
         * */
        for (Generator g : gensInSpmActive) {
                if( ! g.getInPrimaryMarket() ) continue;

            //Total MWh generated by a generator in this SPM  *  its emmission intensity
            CDEInSpot += g.getMonthlyGeneratedMWh() * g.getEmissionsFactor(currentYear);
            totalGenerationInSpot += g.getMonthlyGeneratedMWh();

        }
        /**
         * Secondary Spot
         * */
        if(data.settings.existsMarket("secondary")) {
            for (Generator g : gensInSpmActive) {
                if( ! g.getInSecondaryMarket() ) continue;

                //Total MWh generated by a generator in this SPM  *  its emmission intensity
                CDEInSpot += g.getMonthlyGeneratedMWh() * g.getEmissionsFactor(currentYear);
                totalGenerationInSpot += g.getMonthlyGeneratedMWh();

            }
        }
        /**
         * Out SPOT
         */

        //Get the emission factor of generators within THIS spm OUT spot market
        double totalGenerationOutSpot = 0.0;
        double CDEOutSpot = 0.0;

        //It uses the same available capacity (capacity factors) used by bidders in markets
        for (Generator g : gensInSpmActive) {
            if( g.getInPrimaryMarket() || g.getInSecondaryMarket() ) continue;

            CDEOutSpot += g.getMonthlyGeneratedMWh() * g.getEmissionsFactor(currentYear);
            totalGenerationOutSpot += g.getMonthlyGeneratedMWh();
        }

        //Normalize using the total capacity
        this.genAvailable = totalGenerationInSpot + totalGenerationOutSpot;

        //CDE (not normalized yet - see function normalizeEmissionIntensityIndexRecursively() )
        this.genEmissionIntensityIndex = CDEInSpot + CDEOutSpot;
    }

    /**
     * Computes Indicators and starts with recursion level 0 by default
     *
     * @param state
     * @param consumption
     */
    public void computeIndicators(SimState state, double consumption) {
        computeIndicators(state, consumption, 0);
    }

    /**
     * Recursive computation of indicators
     *
     * @param state
     * @param consumption
     * @param recursionLevel
     */
    public void computeIndicators(SimState state, double consumption, int recursionLevel) {
        Gr4spSim data = (Gr4spSim) state;



        Date today = data.getCurrentSimDate();




        /**
         * Get the emission factor of generators
         */

        //If spot hasn't started, get historic information of generation per fuel type using all generators
        if (data.getStartSpotMarketDate().after(today)) {

            ArrayList<Generator> activeGens = new ArrayList<Generator>();
            ArrayList<Generator> allActivegens = new ArrayList<Generator>();

            //Get Active gens in this SPM
            this.getActiveGensThisSPM(today, activeGens);

            //Get ALL Active gens in this SPM, and in the contained SPMs
            this.getActiveGensAllSPM(today, allActivegens);


            //GET ALL CAPACITY
            float sumCapacityALLSPM = 0;
            for (Generator g : allActivegens)
                sumCapacityALLSPM += g.getMaxCapacity();

            //GET CAPACITY THIS SPM
            float sumCapacityThisSPM = 0;
            for (Generator g : activeGens)
                sumCapacityThisSPM += g.getMaxCapacity();

            //Proportion of energy used to normalize the CDE into CDEII
            //As we do not have the energy generated by each generation unit in the historical data,
            //We use the ratio of (nominal capacity to of this SPM vs the nominal capacity of all SPMS) to normalize the contribution of each SPM
            float proportionGeneration = sumCapacityThisSPM / sumCapacityALLSPM;

            //compute Emission Factor t-CO2/MWh from historic generation contributions
            Generation genData = data.getMonthly_generation_register().get(today);


            this.genEmissionIntensityIndex = genData.computeGenEmissionIntensity(activeGens, today) * proportionGeneration;

        } else {

            ArrayList<Generator> gensInSpmActive = new ArrayList<Generator>();

            //Get Active gens In/Out spot market in this SPM
            this.getActiveGensThisSPM(today, gensInSpmActive);

            //If spot market has started, combine emissions intensity from spot and out of spot, normalized by total capacity used
            //Compute emissionIntensity based on Merit Order bids in Spot Market
            this.computeCDEIISpotMarket(data, gensInSpmActive);

        }


        //Compute network losses
        double networkLosses = computeNetworksLosses(today);

        //Base Case, when an SPM doesn't contain any other SPM but contains generators
        this.genEmissionIntensityIndex = (1 / (1 - networkLosses)) * this.genEmissionIntensityIndex;

        //Recursion through SPMs

        for (Spm s_inside : spms_contained) {
            s_inside.computeIndicators(state, consumption, recursionLevel + 1);
            this.genEmissionIntensityIndex += (1 / (1 - networkLosses)) * s_inside.getGenEmissionIntensityIndex();
        }

        //When recursion has finished, compute normalized CDEII
        if (recursionLevel == 0) {

            //If spot has not started, The historic data is already normalized with the total generation
            if (data.getStartSpotMarketDate().after(today)) {
                //Direct Operational Generators Emissions = Generated E * EmissionIntensityIndex (CDEIII)
                //Emission factor t-CO2/MWh * consumption in MWh
                this.currentEmissions = consumption * this.getGenEmissionIntensityIndex();
            }else{
                //If spot has started, normalize the EmissionIntensityIndex
                double totalGeneration = this.getTotalGenerationInSPMs();
                this.normalizeEmissionIntensityIndexRecursively(totalGeneration);
                //Direct Operational Generators Emissions = Generated E * EmissionIntensityIndex (CDEIII)
                //Emission factor t-CO2/MWh * consumption in MWh
                this.currentEmissions = consumption * this.getGenEmissionIntensityIndex();
            }



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

    public double getGenAvailable() {
        return genAvailable;
    }

    public double getGenEmissionIntensityIndex() {
        return genEmissionIntensityIndex;
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

    public void addGenerator(Generator generator) {
        this.generators.add(generator);
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
    
