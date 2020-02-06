package core;

import java.awt.Color;
import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;

import org.jfree.chart.ChartPanel;

import java.awt.BorderLayout;


public class Gr4spSimUI extends GUIState implements java.io.Serializable {
    public Display2D display;
    public JFrame displayFrame;
    //NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    //ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();


    JFrame consumtionChartFrame;
    JFrame tariffChartFrame;
    JFrame wholesaleChartFrame;
    JFrame ghgChartFrame;
    JFrame genCapacityFactorInSpotChartFrame;
    JFrame genCapacityFactorOffSpotChartFrame;
    JFrame systemProductionInSpotChartFrame;
    JFrame systemProductionOffSpotChartFrame;
    JFrame systemProductionAggSpotChartFrame;
    JFrame numDomesticConsumersChartFrame;
    JFrame numActiveActorsChartFrame;
    JFrame PriceGenAvgChartFrame;




    public static void main(String[] args) {
        (new Gr4spSimUI()).createController();
    }

    public Gr4spSimUI() {
        super(new Gr4spSim(System.currentTimeMillis()));

    }

    public Gr4spSimUI(SimState state) {
        super(state);
    }

    public static String getName() {
        return "Gr4spSim: Generic Recursive Representation of Systems for Service Provision";
    }

    public Object getSimulationInspectedObject() {
        return this.state;
    }

    public void start() {
        super.start();
        this.setupPortrayals();

    }

    public void load(SimState state) {
        super.load(state);
        this.setupPortrayals();
    }

    public void setupPortrayals() {
        Gr4spSim gr4sp = (Gr4spSim) this.state;

        //this.nodePortrayal.setField(gr4sp.layout);
        this.display.reset();
        this.display.setBackdrop(Color.white);
        this.display.repaint();
    }

    public void init(Controller c) {
        super.init(c);
        this.display = new Display2D(1000.0D, 1000.0D, this);
        this.display.setClipping(false);
        this.displayFrame = this.display.createFrame();
        this.displayFrame.setTitle("Gr4spSim Display");
        c.registerFrame(this.displayFrame);
        this.displayFrame.setVisible(true);
        //this.display.attach(this.nodePortrayal, "End Users and SPMs");

        plotting(c);
    }




    public void plotting(Controller c) {

        Gr4spSim data = (Gr4spSim) state;

        // create the frames for every time series that we want to plot

        consumtionChartFrame = new JFrame("Total Households Consumption (MWh) area code: " + data.getAreaCode());
        consumtionChartFrame.setVisible(true);
        consumtionChartFrame.setSize(800, 800);
        c.registerFrame(consumtionChartFrame);
        consumtionChartFrame.add(new ChartPanel(data.saveData.consumptionChart.getChart()), BorderLayout.CENTER);

        tariffChartFrame = new JFrame("Households Tariffs (c/KWh) area code: " + data.getAreaCode());
        tariffChartFrame.setVisible(true);
        tariffChartFrame.setSize(800, 800);
        c.registerFrame(tariffChartFrame);
        tariffChartFrame.add(new ChartPanel(data.saveData.TariffUsageChart.getChart()), BorderLayout.CENTER);

        wholesaleChartFrame = new JFrame("Wholesale Price ($/MWh) area code: " + data.getAreaCode());
        wholesaleChartFrame.setVisible(true);
        wholesaleChartFrame.setSize(800, 800);
        c.registerFrame(wholesaleChartFrame);
        wholesaleChartFrame.add(new ChartPanel(data.saveData.WholesaleChart.getChart()), BorderLayout.CENTER);

        ghgChartFrame = new JFrame("Total Households GHG (MtCO2-e/MWh) area code: " + data.getAreaCode());
        ghgChartFrame.setVisible(true);
        ghgChartFrame.setSize(800, 800);
        c.registerFrame(ghgChartFrame);
        ghgChartFrame.add(new ChartPanel(data.saveData.ghgChart.getChart()), BorderLayout.CENTER);

        numDomesticConsumersChartFrame = new JFrame("Number of Domestic Consumers (households) area code: " + data.getAreaCode());
        numDomesticConsumersChartFrame.setVisible(true);
        numDomesticConsumersChartFrame.setSize(800, 800);
        c.registerFrame(numDomesticConsumersChartFrame);
        numDomesticConsumersChartFrame.add(new ChartPanel(data.saveData.numDomesticConsumersChart.getChart()), BorderLayout.CENTER);

        numActiveActorsChartFrame = new JFrame("Number of Actors area code: " + data.getAreaCode());
        numActiveActorsChartFrame.setVisible(true);
        numActiveActorsChartFrame.setSize(800, 800);
        c.registerFrame(numActiveActorsChartFrame);
        numActiveActorsChartFrame.add(new ChartPanel(data.saveData.numActorsChart.getChart()), BorderLayout.CENTER);

        genCapacityFactorInSpotChartFrame = new JFrame("Spot Historic Generators Capacity Factors: " + data.getAreaCode());
        genCapacityFactorInSpotChartFrame.setVisible(true);
        genCapacityFactorInSpotChartFrame.setSize(800, 800);
        c.registerFrame(genCapacityFactorInSpotChartFrame);
        genCapacityFactorInSpotChartFrame.add(new ChartPanel(data.saveData.genCapacityFactorInSpotChart.getChart()), BorderLayout.CENTER);

        genCapacityFactorOffSpotChartFrame = new JFrame("Off-Spot Historic Generators Capacity Factors: " + data.getAreaCode());
        genCapacityFactorOffSpotChartFrame.setVisible(true);
        genCapacityFactorOffSpotChartFrame.setSize(800, 800);
        c.registerFrame(genCapacityFactorOffSpotChartFrame);
        genCapacityFactorOffSpotChartFrame.add(new ChartPanel(data.saveData.genCapacityFactorOffSpotChart.getChart()), BorderLayout.CENTER);

        systemProductionOffSpotChartFrame = new JFrame("Off-Spot System Production " + data.getAreaCode());
        systemProductionOffSpotChartFrame.setVisible(true);
        systemProductionOffSpotChartFrame.setSize(800, 800);
        c.registerFrame(systemProductionOffSpotChartFrame);
        systemProductionOffSpotChartFrame.add(new ChartPanel(data.saveData.systemProductionOffSpotChart.getChart()), BorderLayout.CENTER);

        systemProductionInSpotChartFrame = new JFrame("Spot System Production " + data.getAreaCode());
        systemProductionInSpotChartFrame.setVisible(true);
        systemProductionInSpotChartFrame.setSize(800, 800);
        c.registerFrame(systemProductionInSpotChartFrame);
        systemProductionInSpotChartFrame.add(new ChartPanel(data.saveData.systemProductionInSpotChart.getChart()), BorderLayout.CENTER);

        systemProductionAggSpotChartFrame = new JFrame("Aggregated System Production " + data.getAreaCode());
        systemProductionAggSpotChartFrame.setVisible(true);
        systemProductionAggSpotChartFrame.setSize(800, 800);
        c.registerFrame(systemProductionAggSpotChartFrame);
        systemProductionAggSpotChartFrame.add(new ChartPanel(data.saveData.systemProductionAggChart.getChart()), BorderLayout.CENTER);


        PriceGenAvgChartFrame = new JFrame("Average Price ($/MWh) area code: " + data.getAreaCode());
        PriceGenAvgChartFrame.setVisible(true);
        PriceGenAvgChartFrame.setSize(800, 800);
        c.registerFrame(PriceGenAvgChartFrame);
        PriceGenAvgChartFrame.add(new ChartPanel(data.saveData.PriceGenAvgChart.getChart()), BorderLayout.CENTER);


    }



    public void finish() {
        super.finish();

    }

    public void quit() {
        super.quit();
        if (this.displayFrame != null) {
            this.displayFrame.dispose();
        }

        this.displayFrame = null;
        this.display = null;

        if (consumtionChartFrame != null) consumtionChartFrame.dispose();
        consumtionChartFrame = null;

        if (tariffChartFrame != null) tariffChartFrame.dispose();
        tariffChartFrame = null;

        if (wholesaleChartFrame != null) wholesaleChartFrame.dispose();
        wholesaleChartFrame = null;

        if (ghgChartFrame != null) ghgChartFrame.dispose();
        ghgChartFrame = null;

        if (genCapacityFactorInSpotChartFrame != null) genCapacityFactorInSpotChartFrame.dispose();
        genCapacityFactorInSpotChartFrame = null;

        if (genCapacityFactorOffSpotChartFrame != null) genCapacityFactorOffSpotChartFrame.dispose();
        genCapacityFactorOffSpotChartFrame = null;

        if (systemProductionOffSpotChartFrame != null) systemProductionOffSpotChartFrame.dispose();
        systemProductionOffSpotChartFrame = null;

        if (systemProductionInSpotChartFrame != null) systemProductionInSpotChartFrame.dispose();
        systemProductionInSpotChartFrame = null;

        if (systemProductionAggSpotChartFrame != null) systemProductionAggSpotChartFrame.dispose();
        systemProductionAggSpotChartFrame = null;

        if (numActiveActorsChartFrame != null) numActiveActorsChartFrame.dispose();
        numActiveActorsChartFrame = null;

        if (numDomesticConsumersChartFrame != null) numDomesticConsumersChartFrame.dispose();
        numDomesticConsumersChartFrame = null;

        if (PriceGenAvgChartFrame != null) PriceGenAvgChartFrame.dispose();
        PriceGenAvgChartFrame = null;
    }
}


