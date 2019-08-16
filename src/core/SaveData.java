package core;

import com.opencsv.CSVWriter;
import core.Social.ConsumerUnit;
import core.Technical.Spm;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.TimeSeriesChartGenerator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class SaveData implements Steppable {

    public TimeSeriesChartGenerator consumptionChart;
    public TimeSeriesChartGenerator tariffChart;
    public TimeSeriesChartGenerator ghgChart;
    public TimeSeriesChartGenerator numDomesticConsumersChart;


    public ArrayList<XYSeries> consumptionActorSeries;  // the data series that will be added to
    public ArrayList<XYSeries> tariffConsumptionActorSeries;// the data series that will be added to
    public ArrayList<XYSeries> ghgConsumptionActorSeries; // the data series that will be added to
    public ArrayList<XYSeries> ghgConsumptionSpmSeries; // the data series that will be added to
    public ArrayList<XYSeries> numDomesticConsumersSeries;

    public SaveData(SimState simState) {
        consumptionActorSeries = new ArrayList<XYSeries>();  // the data series that will be added to
        tariffConsumptionActorSeries = new ArrayList<XYSeries>();// the data series that will be added to
        ghgConsumptionActorSeries = new ArrayList<XYSeries>(); // the data series that will be added to
        ghgConsumptionSpmSeries = new ArrayList<XYSeries>(); // the data series that will be added to
        numDomesticConsumersSeries = new ArrayList<XYSeries>();
        initializeCharts(simState);

    }

    public void initializeCharts(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        consumptionChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        consumptionChart.setTitle("Total Households Consumption (MWh) area code: " + data.getAreaCode());
        consumptionChart.setXAxisLabel("Year");
        consumptionChart.setYAxisLabel("MWh");

        tariffChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        tariffChart.setTitle("Average Households Tariff (c/KWh) area code: " + data.getAreaCode());
        tariffChart.setXAxisLabel("Year");
        tariffChart.setYAxisLabel("c/KWh");

        ghgChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        ghgChart.setTitle("Total Households GHG (MtCO2-e/MWh) area code: " + data.getAreaCode());
        ghgChart.setXAxisLabel("Year");
        ghgChart.setYAxisLabel("MtCO2-e/MWh");

        numDomesticConsumersChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        numDomesticConsumersChart.setTitle("Number of Domestic Consumers (households) area code: " + data.getAreaCode());
        numDomesticConsumersChart.setXAxisLabel("Year");
        numDomesticConsumersChart.setYAxisLabel("Num. Households");

    }

    private void addNewPlottingSeries(SimState simState, int startId, int numNewSeries) {
        Gr4spSim data = (Gr4spSim) simState;

        int id = startId;
        //Add series to store data about consumption, Tariffs and GHG
        for (int i = 0; i < numNewSeries; i++) {
            ConsumerUnit h = (ConsumerUnit) data.consumptionActors.get(id + i);

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

    public void plotSeries(SimState simState) {
        consumptionChart.removeAllSeries();
        tariffChart.removeAllSeries();
        ghgChart.removeAllSeries();
        numDomesticConsumersChart.removeAllSeries();

        Gr4spSim data = (Gr4spSim) simState;

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


        for (Integer integer : data.spm_register.keySet()) {
            Vector<Spm> spms = data.spm_register.get(integer);
            for (int i = 0; i < spms.size(); i++) {
                XYSeries seriesGHGs = new org.jfree.data.xy.XYSeries(
                        spms.elementAt(i).getId(),
                        false);
                ghgConsumptionSpmSeries.add(seriesGHGs);
                ghgChart.addSeries(seriesGHGs, null);
            }
        }

    }

    /**
     * This is executed right after a simulation step is completed
     */

    @Override
    public void step(SimState simState) {

        Gr4spSim data = (Gr4spSim) simState;

        //Add new series for plots if new consumer units were created
        int startIdForNewSeries = consumptionActorSeries.size() - 1;
        int NewNumConsumers = data.consumptionActors.size() - startIdForNewSeries;
        if (NewNumConsumers > 0) {
            addNewPlottingSeries(simState, startIdForNewSeries, NewNumConsumers);
            //TODO: Should add a function to add plots for new SPMs created
        }


        double x = simState.schedule.getSteps();
        Date currentDate = data.getSimCalendar().getTime();
        float year = data.getSimCalendar().get(Calendar.YEAR);
        //normalize month btw [0.0,1.0]
        float month = data.getSimCalendar().get(Calendar.MONTH) / (float) 12.0;
        //sum normalized month
        year += month;

        // now add the data to the time series
        if (x >= simState.schedule.EPOCH && x < simState.schedule.AFTER_SIMULATION) {
            float sumConsumption = 0;
            float averageTariff = 0;
            double sumEmissions = 0;
            int sumDwellings = 0;

            int idSPM = 0;
            for (Integer integer : data.spm_register.keySet()) {
                Vector<Spm> spms = data.spm_register.get(integer);
                for (int i = 0; i < spms.size(); i++) {
                    ghgConsumptionSpmSeries.get(idSPM).add(year,spms.get(i).currentEmissions, true);
                    idSPM++;
                }
            }

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
            consumptionChart.updateChartLater(simState.schedule.getSteps());
            tariffChart.updateChartLater(simState.schedule.getSteps());
            ghgChart.updateChartLater(simState.schedule.getSteps());
            numDomesticConsumersChart.updateChartLater(simState.schedule.getSteps());
        }

        // Update Current simulation date
        data.advanceCurrentSimDate();

        //Add new Consumers according to population growth
        data.generateHouseholds();

        //Finish simulation if endDate is reached
        if (data.getCurrentSimDate().after(data.getEndSimDate()) || data.getCurrentSimDate().equals(data.getEndSimDate())) {
            finalizeCharts(simState);
            saveData(simState);
            savePlots();

            //Stop Simulation
            data.finish();
        }

    }

    public void finalizeCharts(SimState simState) {
        consumptionChart.update(simState.schedule.getSteps(), true);
        consumptionChart.repaint();
        consumptionChart.stopMovie();

        tariffChart.update(simState.schedule.getSteps(), true);
        tariffChart.repaint();
        tariffChart.stopMovie();

        ghgChart.update(simState.schedule.getSteps(), true);
        ghgChart.repaint();
        ghgChart.stopMovie();

        numDomesticConsumersChart.update(simState.schedule.getSteps(), true);
        numDomesticConsumersChart.repaint();
        numDomesticConsumersChart.stopMovie();

    }

    public void savePlots() {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
        File fc = new File("../plots/HouseholdConsumption" + sdf.format(cal.getTime()) + ".png");
        File ft = new File("../plots/HouseholdTariff" + sdf.format(cal.getTime()) + ".png");
        File fg = new File("../plots/HouseholdGHG" + sdf.format(cal.getTime()) + ".png");
        File fd = new File("../plots/NumberHouseholds" + sdf.format(cal.getTime()) + ".png");

        int width = 1920;
        int height = 1080;

        try {
            fc.createNewFile();
            ChartUtilities.saveChartAsPNG(fc,
                    consumptionChart.getChart(),
                    width,
                    height);

            ft.createNewFile();
            ChartUtilities.saveChartAsPNG(ft,
                    tariffChart.getChart(),
                    width,
                    height);

            fg.createNewFile();
            ChartUtilities.saveChartAsPNG(fg,
                    ghgChart.getChart(),
                    width,
                    height);
            fd.createNewFile();
            ChartUtilities.saveChartAsPNG(fd,
                    numDomesticConsumersChart.getChart(),
                    width,
                    height);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public void saveData(SimState simState) {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");
        Calendar cal = Calendar.getInstance();
        //final String dir = System.getProperty("user.dir");
        //System.out.println("current dir = " + dir);

        //TODO: Save SPMs indicators into CSV. Currently only Consumer Unit indicators are saved

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


            Gr4spSim data = (Gr4spSim) simState;

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
                    if (i == 0) {
                        String[] record = {dateToString.format(c.getTime()), Double.toString(kwh / consumers), Double.toString(price), Double.toString(emissions / consumers), Double.toString(consumers)};
                        csvWriterMonthly.writeNext(record);
                    } else {
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
            Object[] years = datasetGHGsummary.keySet().toArray();
            for (int i = years.length - 1; i >= 0; i--) {
                String year = (String) years[i];
                ArrayList<Double> yearDataGHG = datasetGHGsummary.get(year);
                ArrayList<Double> yearDataPrice = datasetPriceSummary.get(year);
                ArrayList<Double> yearDataKWh = datasetKWhSummary.get(year);
                ArrayList<Double> yearDataConsumers = datasetConsumersSummary.get(year);


                Double totalGHG = 0.0;
                Double avgPrice = 0.0;
                Double totalKWh = 0.0;
                Double maxDwellings = 0.0;

                Double sizeData = (double) yearDataGHG.size();

                for (Double ghg : yearDataGHG) {
                    totalGHG += ghg;
                }

                for (Double p : yearDataPrice) {
                    avgPrice += p;
                }
                avgPrice /= (double) yearDataPrice.size();

                for (Double k : yearDataKWh) {
                    totalKWh += k;
                }

                for (Double con : yearDataConsumers) {
                    if (con > maxDwellings) maxDwellings = con;
                }

                String[] record = {year, Double.toString(totalKWh), Double.toString(avgPrice), Double.toString(totalGHG), Double.toString(maxDwellings)};
                csvWriterYear.writeNext(record);
            }

            csvWriterYear.close();

        } catch (IOException ex) {
            System.out.println(ex);
        }


    }
}
