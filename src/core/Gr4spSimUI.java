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
    // NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    // ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

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
    JFrame unmetDemandMwhChartFrame;
    JFrame unmetDemandHoursChartFrame;
    JFrame unmetDemandDaysChartFrame;
    JFrame maxUnmetDemandMwhPerDayChartFrame;

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

        // this.nodePortrayal.setField(gr4sp.layout);
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

        if (consumtionChartFrame != null)
            consumtionChartFrame.dispose();
        consumtionChartFrame = null;

        if (tariffChartFrame != null)
            tariffChartFrame.dispose();
        tariffChartFrame = null;

        if (wholesaleChartFrame != null)
            wholesaleChartFrame.dispose();
        wholesaleChartFrame = null;

        if (ghgChartFrame != null)
            ghgChartFrame.dispose();
        ghgChartFrame = null;

        if (genCapacityFactorInSpotChartFrame != null)
            genCapacityFactorInSpotChartFrame.dispose();
        genCapacityFactorInSpotChartFrame = null;

        if (genCapacityFactorOffSpotChartFrame != null)
            genCapacityFactorOffSpotChartFrame.dispose();
        genCapacityFactorOffSpotChartFrame = null;

        if (systemProductionOffSpotChartFrame != null)
            systemProductionOffSpotChartFrame.dispose();
        systemProductionOffSpotChartFrame = null;

        if (systemProductionInSpotChartFrame != null)
            systemProductionInSpotChartFrame.dispose();
        systemProductionInSpotChartFrame = null;

        if (systemProductionAggSpotChartFrame != null)
            systemProductionAggSpotChartFrame.dispose();
        systemProductionAggSpotChartFrame = null;

        if (numActiveActorsChartFrame != null)
            numActiveActorsChartFrame.dispose();
        numActiveActorsChartFrame = null;

        if (numDomesticConsumersChartFrame != null)
            numDomesticConsumersChartFrame.dispose();
        numDomesticConsumersChartFrame = null;

        if (PriceGenAvgChartFrame != null)
            PriceGenAvgChartFrame.dispose();
        PriceGenAvgChartFrame = null;

        if (unmetDemandMwhChartFrame != null)
            unmetDemandMwhChartFrame.dispose();
        unmetDemandMwhChartFrame = null;

        if (unmetDemandHoursChartFrame != null)
            unmetDemandHoursChartFrame.dispose();
        unmetDemandHoursChartFrame = null;

        if (unmetDemandDaysChartFrame != null)
            unmetDemandDaysChartFrame.dispose();
        unmetDemandDaysChartFrame = null;

        if (maxUnmetDemandMwhPerDayChartFrame != null)
            maxUnmetDemandMwhPerDayChartFrame.dispose();
        maxUnmetDemandMwhPerDayChartFrame = null;
    }
}
