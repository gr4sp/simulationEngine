package core;

import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.awt.Color;
import javax.swing.JFrame;

import org.jfree.data.xy.*;
import sim.engine.Steppable;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.util.media.chart.TimeSeriesChartGenerator;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.ChartPanel;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Date;

import org.jfree.data.xy.XYSeries;


public class Gr4spSimUI extends GUIState {
    public Display2D display;
    public JFrame displayFrame;
    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public TimeSeriesChartGenerator consumptionChart;
    public TimeSeriesChartGenerator tariffChart;
    public TimeSeriesChartGenerator ghgChart;
    public TimeSeriesChartGenerator numDomesticConsumersChart;
    JFrame consumtionChartFrame;
    JFrame tariffChartFrame;
    JFrame ghgChartFrame;
    JFrame numDomesticConsumersChartFrame;


    ArrayList<XYSeries> consumptionActorSeries;    // the data series we'll add to
    ArrayList<XYSeries> tariffConsumptionActorSeries;    // the data series we'll add to
    ArrayList<XYSeries> ghgConsumptionActorSeries;    // the data series we'll add to
    ArrayList<XYSeries> numDomesticConsumersSeries;

    public static void main(String[] args) {
        (new Gr4spSimUI()).createController();
    }

    public Gr4spSimUI() {
        super(new Gr4spSim(System.currentTimeMillis()));

        consumptionActorSeries = new ArrayList<XYSeries>();
        tariffConsumptionActorSeries = new ArrayList<XYSeries>();
        ghgConsumptionActorSeries = new ArrayList<XYSeries>();
        numDomesticConsumersSeries = new ArrayList<XYSeries>();
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

    public void plotSeries() {
        consumptionChart.removeAllSeries();
        tariffChart.removeAllSeries();
        ghgChart.removeAllSeries();
        numDomesticConsumersChart.removeAllSeries();

        Gr4spSim data = (Gr4spSim) state;

        //Add series to store data about consumption, Tariffs and GHG
        for (int i = 0; i < data.consumptionActors.size(); i++) {
            Household h = (Household) data.consumptionActors.get(i);

            //i + " - #p:" + h.getNumberOfPerson() + " Gas:" + h.isHasGas(),
            XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                    i,
                    false);
            consumptionChart.addSeries(seriesConsumption, null);
            consumptionActorSeries.add(seriesConsumption);

            XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                    i,
                    false);
            tariffChart.addSeries(seriesTariff, null);
            tariffConsumptionActorSeries.add(seriesTariff);

            XYSeries seriesGHG = new org.jfree.data.xy.XYSeries(
                    i,
                    false);
            ghgChart.addSeries(seriesGHG, null);
            ghgConsumptionActorSeries.add(seriesGHG);

            XYSeries seriesDomesticConsumers = new org.jfree.data.xy.XYSeries(
                    i,
                    false);
            numDomesticConsumersChart.addSeries(seriesDomesticConsumers, null);
            numDomesticConsumersSeries.add(seriesDomesticConsumers);

        }

        /**
         * This is executed right after a simulation step is completed
         */
        scheduleRepeatingImmediatelyAfter(new Steppable() {
            public void step(SimState state) {

                Gr4spSim data = (Gr4spSim) state;

                double x = state.schedule.getSteps();
                Date currentDate = data.getSimCalendar().getTime();
                float year = data.getSimCalendar().get(Calendar.YEAR);
                //normalize month btw [0.0,1.0]
                float month = data.getSimCalendar().get(Calendar.MONTH) / (float)12.0;
                //sum normalized month
                year += month;

                // now add the data to the time series
                if (x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION) {
                    for (int i = 0; i < data.consumptionActors.size(); i++) {

                        consumptionActorSeries.get(i).add(year, data.consumptionActors.get(i).getCurrentConsumption(), true);
                        tariffConsumptionActorSeries.get(i).add(year, data.consumptionActors.get(i).getCurrentTariff(), true);
                        ghgConsumptionActorSeries.get(i).add(year, 0, true);
                        numDomesticConsumersSeries.get(i).add(year, data.monthly_domestic_consumers_register.get(currentDate), true);
                    }

                    // we're in the model thread right now, so we shouldn't directly
                    // update the consumptionChart.  Instead we request an update to occur the next
                    // time that control passes back to the Swing event thread.
                    consumptionChart.updateChartLater(state.schedule.getSteps());
                    tariffChart.updateChartLater(state.schedule.getSteps());
                    ghgChart.updateChartLater(state.schedule.getSteps());
                    numDomesticConsumersChart.updateChartLater(state.schedule.getSteps());
                }

                // Update Current simulation date
                data.advanceCurrentSimDate();

                //Finish simulation if endDate is reached
                if (data.getCurrentSimDate().after(data.getEndSimDate()) || data.getCurrentSimDate().equals(data.getEndSimDate())) {
                    finish();
                }

            }
        });

    }

    public void plotting(Controller c) {

        consumptionChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        consumptionChart.setTitle("TimeSeries Households Consumption (kWh)");
        consumptionChart.setXAxisLabel("Year");
        consumptionChart.setYAxisLabel("kWh");

        tariffChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        tariffChart.setTitle("TimeSeries Households Tariff (c/KWh)");
        tariffChart.setXAxisLabel("Year");
        tariffChart.setYAxisLabel("c/KWh");

        ghgChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        ghgChart.setTitle("TimeSeries Households GHG (tCO2-e/KWh)");
        ghgChart.setXAxisLabel("Year");
        ghgChart.setYAxisLabel("tCO2-e/KWh");

        numDomesticConsumersChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        numDomesticConsumersChart.setTitle("TimeSeries Number of Domestic Consumers (households)");
        numDomesticConsumersChart.setXAxisLabel("Year");
        numDomesticConsumersChart.setYAxisLabel("tCO2-e/KWh");


        // perhaps you might move the consumptionChart to where you like.
        consumtionChartFrame = new JFrame("TimeSeries Households Consumption (kWh)");
        consumtionChartFrame.setVisible(true);
        consumtionChartFrame.setSize(800, 800);
        c.registerFrame(consumtionChartFrame);
        consumtionChartFrame.add(new ChartPanel(consumptionChart.getChart()), BorderLayout.CENTER);

        tariffChartFrame = new JFrame("TimeSeries Households Tariffs (c/KWh)");
        tariffChartFrame.setVisible(true);
        tariffChartFrame.setSize(800, 800);
        c.registerFrame(tariffChartFrame);
        tariffChartFrame.add(new ChartPanel(tariffChart.getChart()), BorderLayout.CENTER);

        ghgChartFrame = new JFrame("TimeSeries Households GHG (tCO2-e/KWh)");
        ghgChartFrame.setVisible(true);
        ghgChartFrame.setSize(800, 800);
        c.registerFrame(ghgChartFrame);
        ghgChartFrame.add(new ChartPanel(ghgChart.getChart()), BorderLayout.CENTER);

        numDomesticConsumersChartFrame = new JFrame("TimeSeries Number of Domestic Consumers (households)");
        numDomesticConsumersChartFrame.setVisible(true);
        numDomesticConsumersChartFrame.setSize(800, 800);
        c.registerFrame(numDomesticConsumersChartFrame);
        numDomesticConsumersChartFrame.add(new ChartPanel(numDomesticConsumersChart.getChart()), BorderLayout.CENTER);


    }

    public void saveData() {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
        Calendar cal = Calendar.getInstance();
        //final String dir = System.getProperty("user.dir");
        //System.out.println("current dir = " + dir);

        try (
                Writer writer = Files.newBufferedWriter(Paths.get("../csv/SimData" + sdf.format(cal.getTime()) + ".csv"));

                CSVWriter csvWriter = new CSVWriter(writer,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
        ) {
            String[] headerRecord = {"Household", "Time (month)", "Consumption (KWh)", "Tariff ($/KWh)", "GHG Emissions (tCO2-e/KWh)", "Number of Domestic Consumers (households)"};
            csvWriter.writeNext(headerRecord);

            Gr4spSim data = (Gr4spSim) state;
            for (int i = 0; i < data.consumptionActors.size(); i++) {
                XYSeries cseries = consumptionActorSeries.get(i);
                XYSeries tseries = tariffConsumptionActorSeries.get(i);
                XYSeries gseries = ghgConsumptionActorSeries.get(i);
                XYSeries dseries = numDomesticConsumersSeries.get(i);


                Calendar c = Calendar.getInstance();
                c.setTime(data.getStartSimDate());

                for (int t = 0; t < cseries.getItems().size(); t++) {
                    //for (Object o : series.getItems()) {

                    XYDataItem citem = (XYDataItem) cseries.getItems().get(t);
                    XYDataItem titem = (XYDataItem) tseries.getItems().get(t);
                    XYDataItem gitem = (XYDataItem) gseries.getItems().get(t);
                    XYDataItem ditem = (XYDataItem) dseries.getItems().get(t);
                    double kwh = citem.getYValue();
                    double price = titem.getYValue();
                    double emissions = gitem.getYValue();
                    double consumers = ditem.getYValue();

                    SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                    String[] record = {Integer.toString(i), dateToString.format(c.getTime()), Double.toString(kwh), Double.toString(price), Double.toString(emissions), Double.toString(consumers)};
                    csvWriter.writeNext(record);

                    c.add(Calendar.MONTH, 1);
                }
            }


        } catch (IOException ex) {
            System.out.println(ex);
        }


    }

    public void finish() {
        super.finish();

        consumptionChart.update(state.schedule.getSteps(), true);
        consumptionChart.repaint();
        consumptionChart.stopMovie();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
        File fc = new File("../plots/HouseholdConsumption" + sdf.format(cal.getTime()) + ".png");
        File ft = new File("../plots/HouseholdTariff" + sdf.format(cal.getTime()) + ".png");
        File fg = new File("../plots/HouseholdGHG" + sdf.format(cal.getTime()) + ".png");
        File fd = new File("../plots/NumberHouseholds" + sdf.format(cal.getTime()) + ".png");
        try {
            fc.createNewFile();
            ChartUtilities.saveChartAsPNG(fc,
                    consumptionChart.getChart(),
                    consumtionChartFrame.getWidth(),
                    consumtionChartFrame.getHeight());

            ft.createNewFile();
            ChartUtilities.saveChartAsPNG(ft,
                    tariffChart.getChart(),
                    tariffChartFrame.getWidth(),
                    tariffChartFrame.getHeight());

            fg.createNewFile();
            ChartUtilities.saveChartAsPNG(fg,
                    ghgChart.getChart(),
                    ghgChartFrame.getWidth(),
                    ghgChartFrame.getHeight());
            fd.createNewFile();
            ChartUtilities.saveChartAsPNG(fd,
                    numDomesticConsumersChart.getChart(),
                    numDomesticConsumersChartFrame.getWidth(),
                    numDomesticConsumersChartFrame.getHeight());
        } catch (IOException ex) {
            System.out.println(ex);
        }

        saveData();

    }

    public void quit() {
        super.quit();
        if (this.displayFrame != null) {
            this.displayFrame.dispose();
        }

        this.displayFrame = null;
        this.display = null;

        consumptionChart.update(state.schedule.getSteps(), true);
        consumptionChart.repaint();
        consumptionChart.stopMovie();
        if (consumtionChartFrame != null) consumtionChartFrame.dispose();
        consumtionChartFrame = null;

        tariffChart.update(state.schedule.getSteps(), true);
        tariffChart.repaint();
        tariffChart.stopMovie();
        if (tariffChartFrame != null) tariffChartFrame.dispose();
        tariffChartFrame = null;

        ghgChart.update(state.schedule.getSteps(), true);
        ghgChart.repaint();
        ghgChart.stopMovie();
        if (ghgChartFrame != null) ghgChartFrame.dispose();
        ghgChartFrame = null;

        numDomesticConsumersChart.update(state.schedule.getSteps(), true);
        numDomesticConsumersChart.repaint();
        numDomesticConsumersChart.stopMovie();
        if (numDomesticConsumersChartFrame != null) numDomesticConsumersChartFrame.dispose();
        numDomesticConsumersChartFrame = null;
    }
}


