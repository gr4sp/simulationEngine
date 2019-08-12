package core;

import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import java.awt.Color;
import javax.swing.JFrame;

import core.Social.ConsumerUnit;
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


    ArrayList<XYSeries> consumptionActorSeries;    // the data series that will be added to
    ArrayList<XYSeries> tariffConsumptionActorSeries;    // the data series that will be added to
    ArrayList<XYSeries> ghgConsumptionActorSeries;    // the data series that will be added to
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

    private void addNewPlottingSeries(int startId, int numNewSeries) {
        Gr4spSim data = (Gr4spSim) state;

        int id = startId;
        //Add series to store data about consumption, Tariffs and GHG
        for (int i = 0; i < numNewSeries; i++) {
            ConsumerUnit h = (ConsumerUnit) data.consumptionActors.get(id+i);

            //i + " - #p:" + h.getNumberOfPerson() + " Gas:" + h.isHasGas(),
            XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            consumptionActorSeries.add(seriesConsumption);

            XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            tariffConsumptionActorSeries.add(seriesTariff);

            XYSeries seriesGHG = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            ghgConsumptionActorSeries.add(seriesGHG);

            XYSeries seriesDomesticConsumers = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            numDomesticConsumersChart.addSeries(seriesDomesticConsumers, null);
            numDomesticConsumersSeries.add(seriesDomesticConsumers);

            id++;

        }
    }

    public void plotSeries() {
        consumptionChart.removeAllSeries();
        tariffChart.removeAllSeries();
        ghgChart.removeAllSeries();
        numDomesticConsumersChart.removeAllSeries();

        Gr4spSim data = (Gr4spSim) state;

        //Add series to store data about consumption, Tariffs and GHG

        XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        consumptionChart.addSeries(seriesConsumption, null);
        consumptionActorSeries.add(seriesConsumption);

        XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        tariffChart.addSeries(seriesTariff, null);
        tariffConsumptionActorSeries.add(seriesTariff);

        XYSeries seriesGHG = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        ghgChart.addSeries(seriesGHG, null);
        ghgConsumptionActorSeries.add(seriesGHG);

        XYSeries seriesDomesticConsumers = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        numDomesticConsumersChart.addSeries(seriesDomesticConsumers, null);
        numDomesticConsumersSeries.add(seriesDomesticConsumers);

        for (int i = 0; i < data.consumptionActors.size(); i++) {
            ConsumerUnit h = (ConsumerUnit) data.consumptionActors.get(i);

            //i + " - #p:" + h.getNumberOfPerson() + " Gas:" + h.isHasGas(),
            XYSeries seriesConsumptionD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            consumptionActorSeries.add(seriesConsumptionD);

            XYSeries seriesTariffD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            tariffConsumptionActorSeries.add(seriesTariffD);

            XYSeries seriesGHGD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            ghgConsumptionActorSeries.add(seriesGHGD);

            XYSeries seriesDomesticConsumersD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);

            numDomesticConsumersChart.addSeries(seriesDomesticConsumersD, null);
            numDomesticConsumersSeries.add(seriesDomesticConsumersD);

        }

        /**
         * This is executed right after a simulation step is completed
         */
        scheduleRepeatingImmediatelyAfter(new Steppable() {
            public void step(SimState state) {


                //Add new series for plots if new consumer units were created
                int startIdForNewSeries = consumptionActorSeries.size()-1;
                int NewNumConsumers = data.consumptionActors.size() - startIdForNewSeries;
                if(NewNumConsumers > 0) {
                    addNewPlottingSeries(startIdForNewSeries, NewNumConsumers);
                }

                Gr4spSim data = (Gr4spSim) state;

                double x = state.schedule.getSteps();
                Date currentDate = data.getSimCalendar().getTime();
                float year = data.getSimCalendar().get(Calendar.YEAR);
                //normalize month btw [0.0,1.0]
                float month = data.getSimCalendar().get(Calendar.MONTH) / (float) 12.0;
                //sum normalized month
                year += month;

                // now add the data to the time series
                if (x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION) {
                    float sumConsumption = 0;
                    float averageTariff = 0;
                    double sumEmissions = 0;
                    int sumDwellings = 0;

                    for (int i = 0; i < data.consumptionActors.size(); i++) {

                        int id = i + 1;
                        //Save data for CSV
                        ConsumerUnit c = (ConsumerUnit) data.consumptionActors.get(i);
                        consumptionActorSeries.get(id).add(year, data.consumptionActors.get(i).getCurrentConsumption(), true);
                        tariffConsumptionActorSeries.get(id).add(year, data.consumptionActors.get(i).getCurrentTariff(), true);
                        ghgConsumptionActorSeries.get(id).add(year, data.consumptionActors.get(i).getCurrentEmissions(), true);
                        numDomesticConsumersSeries.get(id).add(year, c.getNumberOfHouseholds(), true);

                        //Average/sum data for plot
                        sumConsumption += data.consumptionActors.get(i).getCurrentConsumption();
                        averageTariff += data.consumptionActors.get(i).getCurrentTariff();
                        sumEmissions += data.consumptionActors.get(i).getCurrentEmissions();
                        sumDwellings += c.getNumberOfHouseholds();
                    }
                    averageTariff = averageTariff / data.consumptionActors.size();

                    consumptionActorSeries.get(0).add(year, sumConsumption, true);
                    tariffConsumptionActorSeries.get(0).add(year, averageTariff, true);
                    ghgConsumptionActorSeries.get(0).add(year, sumEmissions, true);
                    numDomesticConsumersSeries.get(0).add(year, sumDwellings, true);
                    //numDomesticConsumersSeries.get(0).add(year, data.monthly_domestic_consumers_register.get(currentDate), true);


                    // we're in the model thread right now, so we shouldn't directly
                    // update the consumptionChart/tariffChart/GHGChart/etc.  Instead we request an update to occur the next
                    // time that control passes back to the Swing event thread.
                    consumptionChart.updateChartLater(state.schedule.getSteps());
                    tariffChart.updateChartLater(state.schedule.getSteps());
                    ghgChart.updateChartLater(state.schedule.getSteps());
                    numDomesticConsumersChart.updateChartLater(state.schedule.getSteps());
                }

                // Update Current simulation date
                data.advanceCurrentSimDate();

                //Add new Consumers according to population growth
                data.generateHouseholds();

                //Finish simulation if endDate is reached
                if (data.getCurrentSimDate().after(data.getEndSimDate()) || data.getCurrentSimDate().equals(data.getEndSimDate())) {
                    finish();
                    saveData();
                    savePlots();
                }

            }
        });

    }


    public void plotting(Controller c) {

        Gr4spSim data = (Gr4spSim) state;

        consumptionChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        consumptionChart.setTitle("Total Households Consumption (MWh) area code: " + data.getAreaCode());
        consumptionChart.setXAxisLabel("Year");
        consumptionChart.setYAxisLabel("MWh");

        tariffChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        tariffChart.setTitle("Average Households Tariff (c/KWh) area code: " + data.getAreaCode());
        tariffChart.setXAxisLabel("Year");
        tariffChart.setYAxisLabel("c/KWh");

        ghgChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        ghgChart.setTitle("Total Households GHG (tCO2-e/MWh) area code: " + data.getAreaCode());
        ghgChart.setXAxisLabel("Year");
        ghgChart.setYAxisLabel("tCO2-e/MWh");

        numDomesticConsumersChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        numDomesticConsumersChart.setTitle("Number of Domestic Consumers (households) area code: " + data.getAreaCode());
        numDomesticConsumersChart.setXAxisLabel("Year");
        numDomesticConsumersChart.setYAxisLabel("Num. Households");


        // perhaps you might move the consumptionChart to where you like.
        consumtionChartFrame = new JFrame("Total Households Consumption (MWh) area code: " + data.getAreaCode());
        consumtionChartFrame.setVisible(true);
        consumtionChartFrame.setSize(800, 800);
        c.registerFrame(consumtionChartFrame);
        consumtionChartFrame.add(new ChartPanel(consumptionChart.getChart()), BorderLayout.CENTER);

        tariffChartFrame = new JFrame("Average Households Tariffs (c/KWh) area code: " + data.getAreaCode());
        tariffChartFrame.setVisible(true);
        tariffChartFrame.setSize(800, 800);
        c.registerFrame(tariffChartFrame);
        tariffChartFrame.add(new ChartPanel(tariffChart.getChart()), BorderLayout.CENTER);

        ghgChartFrame = new JFrame("Total Households GHG (tCO2-e/MWh) area code: " + data.getAreaCode());
        ghgChartFrame.setVisible(true);
        ghgChartFrame.setSize(800, 800);
        c.registerFrame(ghgChartFrame);
        ghgChartFrame.add(new ChartPanel(ghgChart.getChart()), BorderLayout.CENTER);

        numDomesticConsumersChartFrame = new JFrame("Number of Domestic Consumers (households) area code: " + data.getAreaCode());
        numDomesticConsumersChartFrame.setVisible(true);
        numDomesticConsumersChartFrame.setSize(800, 800);
        c.registerFrame(numDomesticConsumersChartFrame);
        numDomesticConsumersChartFrame.add(new ChartPanel(numDomesticConsumersChart.getChart()), BorderLayout.CENTER);


    }

    public void savePlots(){

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

                Writer writerMonthly = Files.newBufferedWriter(Paths.get("../csv/SimDataMonthlySummary" + sdf.format(cal.getTime()) + ".csv"));

                CSVWriter csvWriterMonthly = new CSVWriter(writerMonthly,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerYear = Files.newBufferedWriter(Paths.get("../csv/SimDataYearSummary" + sdf.format(cal.getTime()) + ".csv"));

                CSVWriter csvWriterYear = new CSVWriter(writerYear,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
        ) {
            String[] headerRecord = {"ConsumerUnit", "Time (month)", "Consumption (KWh)", "Tariff (c/KWh)", "GHG Emissions (tCO2-e)", "Number of Domestic Consumers (households)"};
            csvWriter.writeNext(headerRecord);

            String[] headerRecordYear = {"Time (Year)", "Consumption (KWh) per household", " Avg Tariff (c/KWh) per household", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)"};

            csvWriterYear.writeNext(headerRecordYear);

            String[] headerRecordMonthly = {"Time (Month)", "Consumption (KWh) per household", " Avg Tariff (c/KWh) per household", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)"};

            csvWriterMonthly.writeNext(headerRecordMonthly);


            Gr4spSim data = (Gr4spSim) state;

            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");

            HashMap<String, ArrayList<Double>> datasetGHGsummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPriceSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetKWhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetConsumersSummary = new HashMap<>();



            for (int i = 0; i <= data.consumptionActors.size(); i++) {

                XYSeries cseries = consumptionActorSeries.get(i);
                XYSeries tseries = tariffConsumptionActorSeries.get(i);
                XYSeries gseries = ghgConsumptionActorSeries.get(i);
                XYSeries dseries = numDomesticConsumersSeries.get(i);


                Calendar c = Calendar.getInstance();

                //First series starts from beginning simulation, the rest follows a consumtionUnit, which has a creationDate
                if (i == 0)
                    c.setTime(data.getStartSimDate());
                else {
                    ConsumerUnit consumer = (ConsumerUnit) data.consumptionActors.get(i - 1);
                    c.setTime(consumer.creationDate);
                }

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

                    //First series starts from beginning simulation, the rest follows a consumtionUnit, which has a creationDate
                    if (i == 0){
                        String[] record = {dateToString.format(c.getTime()), Double.toString(kwh/consumers), Double.toString(price), Double.toString(emissions/consumers), Double.toString(consumers)};
                        csvWriterMonthly.writeNext(record);
                    }else {
                        String[] record = {Integer.toString(i), dateToString.format(c.getTime()), Double.toString(kwh), Double.toString(price), Double.toString(emissions), Double.toString(consumers)};
                        csvWriter.writeNext(record);
                    }
                    //Year summary based on monthly data from the first series with id=0 that aggregates all SPM data
                    if (i == 0) {
                        String year = dateToYear.format(c.getTime());

                        if (!datasetGHGsummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetGHGsummary.put(year, yearData);
                        }

                        datasetGHGsummary.get(year).add(emissions / consumers);

                        if (!datasetPriceSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPriceSummary.put(year, yearData);
                        }

                        datasetPriceSummary.get(year).add(price);


                        if (!datasetKWhSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetKWhSummary.put(year, yearData);
                        }

                        datasetKWhSummary.get(year).add(kwh / consumers);

                        if (!datasetConsumersSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetConsumersSummary.put(year, yearData);
                        }

                        datasetConsumersSummary.get(year).add(consumers);

                    }

                    c.add(Calendar.MONTH, 1);
                }
            }
            csvWriter.close();
            csvWriterMonthly.close();

            //Save Year summary to csv file
            Object[] years  =  datasetGHGsummary.keySet().toArray();
            for (int i = years.length-1; i>= 0; i--) {
                String year = (String) years[i];
                ArrayList<Double> yearDataGHG = datasetGHGsummary.get(year);
                ArrayList<Double> yearDataPrice = datasetPriceSummary.get(year);
                ArrayList<Double> yearDataKWh = datasetKWhSummary.get(year);
                ArrayList<Double> yearDataConsumers = datasetConsumersSummary.get(year);


                Double totalGHG= 0.0;
                Double avgPrice= 0.0;
                Double totalKWh= 0.0;
                Double maxDwellings = 0.0;

                Double sizeData = (double) yearDataGHG.size();

                for (Double ghg : yearDataGHG) {
                    totalGHG += ghg;
                }

                for (Double p : yearDataPrice) {
                    avgPrice += p;
                }
                avgPrice /= (double)yearDataPrice.size();

                for (Double k : yearDataKWh) {
                    totalKWh += k;
                }

                for (Double con : yearDataConsumers) {
                    if(con > maxDwellings) maxDwellings = con;
                }

                String[] record = {year, Double.toString(totalKWh), Double.toString(avgPrice), Double.toString(totalGHG), Double.toString(maxDwellings)};
                csvWriterYear.writeNext(record);
            }

            csvWriterYear.close();

        } catch (IOException ex) {
            System.out.println(ex);
        }


    }

    public void finish() {
        super.finish();

        consumptionChart.update(state.schedule.getSteps(), true);
        consumptionChart.repaint();
        consumptionChart.stopMovie();

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


