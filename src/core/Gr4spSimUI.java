package core;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.awt.Color;
import javax.swing.JFrame;

import sim.engine.Steppable;

import org.jfree.data.xy.XYSeries;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.util.media.chart.TimeSeriesChartGenerator;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.ChartPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;




public class Gr4spSimUI extends GUIState {
    public Display2D display;
    public JFrame displayFrame;
    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public TimeSeriesChartGenerator chart;
    JFrame chartFrame;
    ArrayList<XYSeries> consumptionActorSeries;    // the data series we'll add to

    public static void main(String[] args) {
        (new Gr4spSimUI()).createController();
    }

    public Gr4spSimUI() {
        super(new Gr4spSim(System.currentTimeMillis()));

        consumptionActorSeries = new ArrayList<XYSeries>();
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
        plotSeries();
    }

    public void load(SimState state) {
        super.load(state);
        this.setupPortrayals();
    }

    public void setupPortrayals() {
        Gr4spSim gr4sp = (Gr4spSim) this.state;
        //this.edgePortrayal.setField(new SpatialNetwork2D(gr4sp.balls, gr4sp.bands));
        //this.edgePortrayal.setPortrayalForAll(new BandPortrayal2D());
        this.nodePortrayal.setField(gr4sp.layout);
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
        //this.display.attach(this.edgePortrayal, "Bands");
        this.display.attach(this.nodePortrayal, "End Users and SPMs");

        plotting(c);
    }

    public void plotSeries(){
        chart.removeAllSeries();

        Gr4spSim data = (Gr4spSim) state;
        for(int i = 0; i < data.consumptionActors.size(); i++) {
            Household h = (Household) data.consumptionActors.get(i);
            XYSeries series = new org.jfree.data.xy.XYSeries(
                    i+" - #p:"+ h.getNumberOfPerson()+" Gas:" +h.isHasGas(),
                    false);

            chart.addSeries(series, null);

            consumptionActorSeries.add(series);

        }

        scheduleRepeatingImmediatelyAfter(new Steppable()
        {
            public void step(SimState state)
            {
                // at this stage we're adding data to our chart.  We
                // need an X value and a Y value.  Typically the X
                // value is the schedule's timestamp.  The Y value
                // is whatever data you're extracting from your
                // simulation.  For purposes of illustration, let's
                // extract the number of steps from the schedule and
                // run it through a sin wave.

                Gr4spSim data = (Gr4spSim) state;

                double x = state.schedule.getSteps();

                // now add the data
                if (x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
                {
                    for(int i = 0; i < data.consumptionActors.size(); i++) {
                        consumptionActorSeries.get(i).add(x, data.consumptionActors.get(i).getCurrentConsumption(), true);
                    }
                    // we're in the model thread right now, so we shouldn't directly
                    // update the chart.  Instead we request an update to occur the next
                    // time that control passes back to the Swing event thread.
                    chart.updateChartLater(state.schedule.getSteps());
                }
            }
        });

    }

    public void plotting(Controller c){

        chart = new sim.util.media.chart.TimeSeriesChartGenerator();
        chart.setTitle("TimeSeries Households Consumption");
        chart.setXAxisLabel("Month");
        chart.setYAxisLabel("kW");

        // perhaps you might move the chart to where you like.
        chartFrame = new JFrame("TimeSeries Households Consumption");

        chartFrame.setVisible(true);
        chartFrame.setSize(800, 800);
        c.registerFrame(chartFrame);

        chartFrame.add(new ChartPanel(chart.getChart()),BorderLayout.CENTER);

    }

    public void finish()
    {
        super.finish();

        chart.update(state.schedule.getSteps(), true);
        chart.repaint();
        chart.stopMovie();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
        File f = new File("plots/HouseholdConsumption"+sdf.format(cal.getTime())+".png");
        try {
            f.createNewFile();
            ChartUtilities.saveChartAsPNG(f,
                    chart.getChart(),
                    chartFrame.getWidth(),
                    chartFrame.getHeight());
        }catch (IOException ex) {
            System.out.println(ex);
        }

    }

    public void quit() {
        super.quit();
        if (this.displayFrame != null) {
            this.displayFrame.dispose();
        }

        this.displayFrame = null;
        this.display = null;

        chart.update(state.schedule.getSteps(), true);
        chart.repaint();
        chart.stopMovie();
        if (chartFrame != null)	chartFrame.dispose();
        chartFrame = null;
    }
}


