package core;

import com.opencsv.CSVWriter;
import core.Relationships.Arena;
import core.Social.EndUserUnit;
import core.Social.Actor;
import core.Technical.Generator;
import core.Technical.Spm;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.TimeSeriesChartGenerator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class SaveData implements Steppable, java.io.Serializable {

    public TimeSeriesChartGenerator consumptionChart;
    public TimeSeriesChartGenerator WholesaleChart;
    public TimeSeriesChartGenerator TariffUsageChart;
    public TimeSeriesChartGenerator ghgChart;
    public TimeSeriesChartGenerator numDomesticConsumersChart;
    public TimeSeriesChartGenerator genCapacityFactorInSpotChart;
    public TimeSeriesChartGenerator genCapacityFactorOffSpotChart;
    public TimeSeriesChartGenerator systemProductionAggChart;
    public TimeSeriesChartGenerator systemProductionInSpotChart;
    public TimeSeriesChartGenerator systemProductionOffSpotChart;
    public TimeSeriesChartGenerator PriceGenAvgChart;
    public TimeSeriesChartGenerator PriceGenMaxChart;
    public TimeSeriesChartGenerator PriceGenMinChart;
    public TimeSeriesChartGenerator numActorsChart;
    public TimeSeriesChartGenerator unmetDemandMwhChart;
    public TimeSeriesChartGenerator unmetDemandHoursChart;
    public TimeSeriesChartGenerator unmetDemandDaysChart;
    public TimeSeriesChartGenerator maxUnmetDemandMwhPerDayChart;


    public ArrayList<XYSeries> consumptionActorSeries;  // the data series that will be added to
    public ArrayList<XYSeries> tariffUsageConsumptionActorSeries;// the data series that will be added to
    public ArrayList<XYSeries> wholesaleSeries;// the data series that will be added to
    public ArrayList<XYSeries> ghgConsumptionActorSeries; // the data series that will be added to
    public ArrayList<XYSeries> ghgConsumptionSpmSeries; // the data series that will be added to
    public ArrayList<XYSeries> numDomesticConsumersSeries;
    public XYSeries numActorsSeries;

    HashMap<Integer, XYSeries> unmetDemandMwhSeries; // Total UnmetDemand MWh per month
    HashMap<Integer, XYSeries> unmetDemandHoursSeries; // Num Hours with UnmetDemand per month
    HashMap<Integer, XYSeries>  unmetDemandDaysSeries; // Num Days with UnmetDemand per month
    HashMap<Integer, XYSeries>  maxUnmetDemandMwhPerDaySeries; // Day with Max UnmetDemand MWh per month


    HashMap<Integer, XYSeries> genCapacityFactorSeries;  // the data series that will be added to
    HashMap<Integer, XYSeries> systemProductionSeries;  // the data series that will be added to
    public HashMap<Integer, XYSeries> PriceGenAvgSeries;  // the data series that will be added to
    HashMap<Integer, XYSeries> PriceGenMaxSeries;  // the data series that will be added to
    HashMap<Integer, XYSeries> PriceGenMinSeries;  // the data series that will be added to


    public SaveData(SimState simState) {
        consumptionActorSeries = new ArrayList<XYSeries>();  // the data series that will be added to
        tariffUsageConsumptionActorSeries = new ArrayList<XYSeries>();// the data series that will be added to
        wholesaleSeries = new ArrayList<XYSeries>();// the data series that will be added to
        ghgConsumptionActorSeries = new ArrayList<XYSeries>(); // the data series that will be added to
        ghgConsumptionSpmSeries = new ArrayList<XYSeries>(); // the data series that will be added to
        numDomesticConsumersSeries = new ArrayList<XYSeries>();

        unmetDemandMwhSeries = new HashMap<>();
        unmetDemandHoursSeries = new HashMap<>();
        unmetDemandDaysSeries = new HashMap<>();
        maxUnmetDemandMwhPerDaySeries = new HashMap<>();

        genCapacityFactorSeries = new HashMap<>();
        systemProductionSeries = new HashMap<>();
        PriceGenAvgSeries = new HashMap<>();
        PriceGenMaxSeries = new HashMap<>();
        PriceGenMinSeries = new HashMap<>();

        initializeCharts(simState);

    }

    public void initializeCharts(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        Font smallf = new Font("serif", Font.PLAIN, 8);
        Font bigf = new Font("serif", Font.PLAIN, 12);
        consumptionChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        consumptionChart.setTitle("Total Households Consumption (MWh) area code: " + data.getAreaCode());
        consumptionChart.setXAxisLabel("Year");
        consumptionChart.setYAxisLabel("MWh");
        consumptionChart.getChart().getLegend().setItemFont(bigf);


        WholesaleChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        WholesaleChart.setTitle("30min Monthly Average Wholesale ($/MWh) area code: " + data.getAreaCode());
        WholesaleChart.setXAxisLabel("Year");
        WholesaleChart.setYAxisLabel("$/MWh");
        WholesaleChart.getChart().getLegend().setItemFont(bigf);

        TariffUsageChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        TariffUsageChart.setTitle("Tariff (c/KWh) area code: " + data.getAreaCode());
        TariffUsageChart.setXAxisLabel("Year");
        TariffUsageChart.setYAxisLabel("c/KWh");
        TariffUsageChart.getChart().getLegend().setItemFont(bigf);

        ghgChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        ghgChart.setTitle("Total Households GHG (tCO2-e) area code: " + data.getAreaCode());
        ghgChart.setXAxisLabel("Year");
        ghgChart.setYAxisLabel("tCO2-e");
        ghgChart.getChart().getLegend().setItemFont(bigf);

        numDomesticConsumersChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        numDomesticConsumersChart.setTitle("Number of Domestic Consumers (households) area code: " + data.getAreaCode());
        numDomesticConsumersChart.setXAxisLabel("Year");
        numDomesticConsumersChart.setYAxisLabel("Num. Households");
        numDomesticConsumersChart.getChart().getLegend().setItemFont(bigf);

        genCapacityFactorInSpotChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        genCapacityFactorInSpotChart.setTitle("Spot Historic Generators Capacity Factors: " + data.getAreaCode());
        genCapacityFactorInSpotChart.setXAxisLabel("Year");
        genCapacityFactorInSpotChart.setYAxisLabel("% Cap. Factor");
        genCapacityFactorInSpotChart.getChart().getLegend().setItemFont(bigf);

        genCapacityFactorOffSpotChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        genCapacityFactorOffSpotChart.setTitle("Off-Spot Historic Generators Capacity Factors: " + data.getAreaCode());
        genCapacityFactorOffSpotChart.setXAxisLabel("Year");
        genCapacityFactorOffSpotChart.setYAxisLabel("% Cap. Factor");
        genCapacityFactorOffSpotChart.getChart().getLegend().setItemFont(smallf);

        systemProductionAggChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        systemProductionAggChart.setTitle("Aggregated System Production: " + data.getAreaCode());
        systemProductionAggChart.setXAxisLabel("Year");
        systemProductionAggChart.setYAxisLabel("MWh");
        systemProductionAggChart.getChart().getLegend().setItemFont(bigf);

        systemProductionInSpotChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        systemProductionInSpotChart.setTitle("In-Spot System Production: " + data.getAreaCode());
        systemProductionInSpotChart.setXAxisLabel("Year");
        systemProductionInSpotChart.setYAxisLabel("MWh");
        systemProductionInSpotChart.getChart().getLegend().setItemFont(bigf);

        systemProductionOffSpotChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        systemProductionOffSpotChart.setTitle("Off-Spot System Production: " + data.getAreaCode());
        systemProductionOffSpotChart.setXAxisLabel("Year");
        systemProductionOffSpotChart.setYAxisLabel("MWh");
        systemProductionOffSpotChart.getChart().getLegend().setItemFont(smallf);


        numActorsChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        numActorsChart.setTitle("Number of Actors " + data.getAreaCode());
        numActorsChart.setXAxisLabel("Year");
        numActorsChart.setYAxisLabel("Num. Actors");
        numActorsChart.getChart().getLegend().setItemFont(bigf);

        PriceGenAvgChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        PriceGenAvgChart.setTitle("Average Price ($/MWh) area code: " + data.getAreaCode());
        PriceGenAvgChart.setXAxisLabel("Year");
        PriceGenAvgChart.setYAxisLabel("$/MWh");
        PriceGenAvgChart.getChart().getLegend().setItemFont(bigf);

        PriceGenMaxChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        PriceGenMaxChart.setTitle("Max Price ($/MWh) area code: " + data.getAreaCode());
        PriceGenMaxChart.setXAxisLabel("Year");
        PriceGenMaxChart.setYAxisLabel("$/MWh");
        PriceGenMaxChart.getChart().getLegend().setItemFont(bigf);

        PriceGenMinChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        PriceGenMinChart.setTitle("Min Price ($/MWh) area code: " + data.getAreaCode());
        PriceGenMinChart.setXAxisLabel("Year");
        PriceGenMinChart.setYAxisLabel("$/MWh");
        PriceGenMinChart.getChart().getLegend().setItemFont(bigf);

        unmetDemandMwhChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        unmetDemandMwhChart.setTitle("Total Unmet Demand per Month (MWh): " + data.getAreaCode());
        unmetDemandMwhChart.setXAxisLabel("Year");
        unmetDemandMwhChart.setYAxisLabel("MWh");
        unmetDemandMwhChart.getChart().getLegend().setItemFont(bigf);

        unmetDemandHoursChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        unmetDemandHoursChart.setTitle("Number of hours with Unmet Demand per Month: " + data.getAreaCode());
        unmetDemandHoursChart.setXAxisLabel("Year");
        unmetDemandHoursChart.setYAxisLabel("Num. Hours");
        unmetDemandHoursChart.getChart().getLegend().setItemFont(bigf);

        unmetDemandDaysChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        unmetDemandDaysChart.setTitle("Number of Days with Unmet Demand per Month: " + data.getAreaCode());
        unmetDemandDaysChart.setXAxisLabel("Year");
        unmetDemandDaysChart.setYAxisLabel("Num. Days");
        unmetDemandDaysChart.getChart().getLegend().setItemFont(bigf);

        maxUnmetDemandMwhPerDayChart = new sim.util.media.chart.TimeSeriesChartGenerator();
        maxUnmetDemandMwhPerDayChart.setTitle("For every month, maximum Unmet Demand within a single hour (MWh): " + data.getAreaCode());
        maxUnmetDemandMwhPerDayChart.setXAxisLabel("Year");
        maxUnmetDemandMwhPerDayChart.setYAxisLabel("MWh");
        maxUnmetDemandMwhPerDayChart.getChart().getLegend().setItemFont(bigf);

    }

    //Only used if the simulation creates a new Consumer Unit
    private void addNewPlottingSeries(SimState simState, int startId, int numNewSeries) {
        Gr4spSim data = (Gr4spSim) simState;

        int id = startId;
        //Add series to store data about consumption, Tariffs, GHG and number or organisations
        for (int i = 0; i < numNewSeries; i++) {
            EndUserUnit h = (EndUserUnit) data.consumptionActors.get(id + i);

            //i + " - #p:" + h.getNumberOfPerson() + " Gas:" + h.isHasGas(),
            XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            consumptionActorSeries.add(seriesConsumption);

            XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            tariffUsageConsumptionActorSeries.add(seriesTariff);

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
        systemProductionAggChart.removeAllSeries();
        systemProductionInSpotChart.removeAllSeries();
        systemProductionOffSpotChart.removeAllSeries();
        genCapacityFactorInSpotChart.removeAllSeries();
        genCapacityFactorOffSpotChart.removeAllSeries();
        consumptionChart.removeAllSeries();
        WholesaleChart.removeAllSeries();
        TariffUsageChart.removeAllSeries();
        ghgChart.removeAllSeries();
        numDomesticConsumersChart.removeAllSeries();
        numActorsChart.removeAllSeries();
        PriceGenAvgChart.removeAllSeries();
        PriceGenMinChart.removeAllSeries();
        PriceGenMaxChart.removeAllSeries();
        unmetDemandMwhChart.removeAllSeries();
        unmetDemandHoursChart.removeAllSeries();
        unmetDemandDaysChart.removeAllSeries();
        maxUnmetDemandMwhPerDayChart.removeAllSeries();

        Gr4spSim data = (Gr4spSim) simState;

        //Add series to store data about consumption, Tariffs and GHG

        XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        consumptionChart.addSeries(seriesConsumption, null);
        consumptionActorSeries.add(seriesConsumption);

        XYSeries seriesWholesale = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        WholesaleChart.addSeries(seriesWholesale, null);
        wholesaleSeries.add(seriesWholesale);

        XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        TariffUsageChart.addSeries(seriesTariff, null);
        tariffUsageConsumptionActorSeries.add(seriesTariff);

        XYSeries seriesGHG = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        ghgChart.addSeries(seriesGHG, null);
        ghgConsumptionActorSeries.add(seriesGHG);

        numActorsSeries = new org.jfree.data.xy.XYSeries(
                "Number of Actors",
                false);
        numActorsChart.addSeries(numActorsSeries, null);


        XYSeries seriesDomesticConsumers = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        numDomesticConsumersChart.addSeries(seriesDomesticConsumers, null);
        numDomesticConsumersSeries.add(seriesDomesticConsumers);


        for (int i = 0; i < data.consumptionActors.size(); i++) {
            EndUserUnit h = (EndUserUnit) data.consumptionActors.get(i);

            //i + " - #p:" + h.getNumberOfPerson() + " Gas:" + h.isHasGas(),
            XYSeries seriesConsumptionD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            consumptionActorSeries.add(seriesConsumptionD);

            XYSeries seriesTariffD = new org.jfree.data.xy.XYSeries(
                    h.getName(),
                    false);
            tariffUsageConsumptionActorSeries.add(seriesTariffD);

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

        /**
         * Add series to hashmap of series of aggregated
         *  0 PrimarySpot
         * -1 SecondaySpot
         * -2 Offspot
         * -3 RooftopPV
         * -4 Coal
         * -5 Water
         * -6 Wind
         * -7 Gas
         * -8 Solar
         * -9 Battery
         */
        if (data.settings.existsMarket("primary")) {
            XYSeries seriesSystemProductionIn = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            systemProductionSeries.put(0, seriesSystemProductionIn);
            systemProductionAggChart.addSeries(seriesSystemProductionIn, null);

            XYSeries unmetDemandMwh = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandMwhSeries.put(0, unmetDemandMwh);
            unmetDemandMwhChart.addSeries(unmetDemandMwh, null);

            XYSeries unmetDemandHours = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandHoursSeries.put(0, unmetDemandHours);
            unmetDemandHoursChart.addSeries(unmetDemandHours, null);

            XYSeries unmetDemandDays = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandDaysSeries.put(0, unmetDemandDays);
            unmetDemandDaysChart.addSeries(unmetDemandDays, null);

            XYSeries maxUnmetDemandMwhPerDay = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            maxUnmetDemandMwhPerDaySeries.put(0, maxUnmetDemandMwhPerDay);
            maxUnmetDemandMwhPerDayChart.addSeries(maxUnmetDemandMwhPerDay, null);

        }

        if (data.settings.existsMarket("secondary")) {
            XYSeries seriesSystemProductionSec = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            systemProductionSeries.put(-1, seriesSystemProductionSec);
            systemProductionAggChart.addSeries(seriesSystemProductionSec, null);

            XYSeries unmetDemandMwh = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandMwhSeries.put(-1, unmetDemandMwh);
            unmetDemandMwhChart.addSeries(unmetDemandMwh, null);

            XYSeries unmetDemandHours = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandHoursSeries.put(-1, unmetDemandHours);
            unmetDemandHoursChart.addSeries(unmetDemandHours, null);

            XYSeries unmetDemandDays = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandDaysSeries.put(-1, unmetDemandDays);
            unmetDemandDaysChart.addSeries(unmetDemandDays, null);

            XYSeries maxUnmetDemandMwhPerDay = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            maxUnmetDemandMwhPerDaySeries.put(-1, maxUnmetDemandMwhPerDay);
            maxUnmetDemandMwhPerDayChart.addSeries(maxUnmetDemandMwhPerDay, null);
        }
        if (data.settings.existsOffMarket()) {

            XYSeries seriesSystemProductionOff = new org.jfree.data.xy.XYSeries(
                    "OffSpot",
                    false);
            systemProductionSeries.put(-2, seriesSystemProductionOff);
            systemProductionAggChart.addSeries(seriesSystemProductionOff, null);
        }

        XYSeries seriesSystemProductionRooftopPV = new org.jfree.data.xy.XYSeries(
                "RooftopPV",
                false);
        systemProductionSeries.put(-3, seriesSystemProductionRooftopPV);
        systemProductionAggChart.addSeries(seriesSystemProductionRooftopPV, null);

        XYSeries seriesSystemProductionCoal = new org.jfree.data.xy.XYSeries(
                "Coal",
                false);
        systemProductionSeries.put(-4, seriesSystemProductionCoal);
        systemProductionAggChart.addSeries(seriesSystemProductionCoal, null);

        XYSeries seriesSystemProductionWater = new org.jfree.data.xy.XYSeries(
                "Water",
                false);
        systemProductionSeries.put(-5, seriesSystemProductionWater);
        systemProductionAggChart.addSeries(seriesSystemProductionWater, null);

        XYSeries seriesSystemProductionWind = new org.jfree.data.xy.XYSeries(
                "Wind",
                false);
        systemProductionSeries.put(-6, seriesSystemProductionWind);
        systemProductionAggChart.addSeries(seriesSystemProductionWind, null);

        XYSeries seriesSystemProductionGas = new org.jfree.data.xy.XYSeries(
                "Gas",
                false);
        systemProductionSeries.put(-7, seriesSystemProductionGas);
        systemProductionAggChart.addSeries(seriesSystemProductionGas, null);

        XYSeries seriesSystemProductionSolar = new org.jfree.data.xy.XYSeries(
                "Solar",
                false);
        systemProductionSeries.put(-8, seriesSystemProductionSolar);
        systemProductionAggChart.addSeries(seriesSystemProductionSolar, null);

        XYSeries seriesSystemProductionBattery = new org.jfree.data.xy.XYSeries(
                "Battery",
                false);
        systemProductionSeries.put(-9, seriesSystemProductionBattery);
        systemProductionAggChart.addSeries(seriesSystemProductionBattery, null);

        /**
         * Price series
         * */

        XYSeries seriesPriceAvgTariff = new org.jfree.data.xy.XYSeries(
                "Tariff_WA_AllMarkets",
                false);
        PriceGenAvgSeries.put(1, seriesPriceAvgTariff);
        PriceGenAvgChart.addSeries(seriesPriceAvgTariff, null);

        if (data.settings.existsMarket("primary")) {

            XYSeries seriesPriceAvgIn = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            PriceGenAvgSeries.put(0, seriesPriceAvgIn);
            PriceGenAvgChart.addSeries(seriesPriceAvgIn, null);
        }

        if (data.settings.existsMarket("secondary")) {
            XYSeries seriesPriceAvgIn = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            PriceGenAvgSeries.put(-1, seriesPriceAvgIn);
            PriceGenAvgChart.addSeries(seriesPriceAvgIn, null);
        }

        if (data.settings.existsOffMarket()){
            XYSeries seriesPriceAvgOff = new org.jfree.data.xy.XYSeries(
                        "OffSpot",
                        false);
            PriceGenAvgSeries.put(-2, seriesPriceAvgOff);
            PriceGenAvgChart.addSeries(seriesPriceAvgOff, null);
        }

        XYSeries seriesPriceAvgRooftopPV = new org.jfree.data.xy.XYSeries(
                "RooftopPV",
                false);
        PriceGenAvgSeries.put(-3, seriesPriceAvgRooftopPV);
        PriceGenAvgChart.addSeries(seriesPriceAvgRooftopPV, null);

        XYSeries seriesPriceAvgCoal = new org.jfree.data.xy.XYSeries(
                "Coal",
                false);
        PriceGenAvgSeries.put(-4, seriesPriceAvgCoal);
        PriceGenAvgChart.addSeries(seriesPriceAvgCoal, null);

        XYSeries seriesPriceAvgWater = new org.jfree.data.xy.XYSeries(
                "Water",
                false);
        PriceGenAvgSeries.put(-5, seriesPriceAvgWater);
        PriceGenAvgChart.addSeries(seriesPriceAvgWater, null);

        XYSeries seriesPriceAvgWind = new org.jfree.data.xy.XYSeries(
                "Wind",
                false);
        PriceGenAvgSeries.put(-6, seriesPriceAvgWind);
        PriceGenAvgChart.addSeries(seriesPriceAvgWind, null);

        XYSeries seriesPriceAvgGas = new org.jfree.data.xy.XYSeries(
                "Gas",
                false);
        PriceGenAvgSeries.put(-7, seriesPriceAvgGas);
        PriceGenAvgChart.addSeries(seriesPriceAvgGas, null);

        XYSeries seriesPriceAvgSolar = new org.jfree.data.xy.XYSeries(
                "Solar",
                false);
        PriceGenAvgSeries.put(-8, seriesPriceAvgSolar);
        PriceGenAvgChart.addSeries(seriesPriceAvgSolar, null);

        XYSeries seriesPriceAvgBattery = new org.jfree.data.xy.XYSeries(
                "Battery",
                false);
        PriceGenAvgSeries.put(-9, seriesPriceAvgBattery);
        PriceGenAvgChart.addSeries(seriesPriceAvgBattery, null);




        for (Integer integer : data.gen_register.keySet()) {
            Vector<Generator> gens = data.gen_register.get(integer);
            for (int i = 0; i < gens.size(); i++) {
                String nameSeries = Integer.toString(gens.elementAt(i).getId()) + gens.elementAt(i).getfuelSourceDescriptor().substring(0, 2);

                //Add series to hashmap of series
                XYSeries seriesGenCapacityFactors = new org.jfree.data.xy.XYSeries(
                        nameSeries,
                        false);
                genCapacityFactorSeries.put(integer, seriesGenCapacityFactors);

                //Add series to hashmap of series
                XYSeries seriesSystemProduction = new org.jfree.data.xy.XYSeries(
                        nameSeries,
                        false);
                systemProductionSeries.put(integer, seriesSystemProduction);

                //Add series to the correct chart
                //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {
                if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) == false){
                    genCapacityFactorOffSpotChart.addSeries(seriesGenCapacityFactors, null);
                    systemProductionOffSpotChart.addSeries(seriesSystemProduction, null);
                } else {
                    genCapacityFactorInSpotChart.addSeries(seriesGenCapacityFactors, null);
                    systemProductionInSpotChart.addSeries(seriesSystemProduction, null);

                }
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
        float floatDate = data.getSimCalendar().get(Calendar.YEAR);

        //normalize month btw [0.0,1.0]
        float month = data.getSimCalendar().get(Calendar.MONTH) / (float) 12.0;
        //sum normalized month
        floatDate += month;
        //Tariff of current year = average wholesale price of the 12 months
        //Tariff changes in december (see EndUser classs),
        // but refers to the tariff that was used all through the last 12 months, hence -1 year
        float floatDateTariff = floatDate - ((float) 11.0 / (float) 12.0);

        // now add the data to the time series
        if (x >= Schedule.EPOCH && x < Schedule.AFTER_SIMULATION) {
            float sumConsumption = 0;
            float averageTariff = 0;
            double sumEmissions = 0;
            int sumDwellings = 0;


            //Save Emissions by each SPM
            int idSPM = 0;
            for (Integer integer : data.spm_register.keySet()) {
                Vector<Spm> spms = data.spm_register.get(integer);
                for (int i = 0; i < spms.size(); i++) {
                    ghgConsumptionSpmSeries.get(idSPM).add(floatDate, spms.get(i).currentEmissions, false);
                    idSPM++;
                }
            }

            //Save Number of active actors
            int numActiveActors = 0;
            for (Integer integer : data.actor_register.keySet()) {
                Actor act = data.actor_register.get(integer);
                // if start of actor is after current date (it started) and has not ended (change date is after current date)
                if (currentDate.after(act.getStart()) && act.getChangeDate().after(currentDate)) {
                    numActiveActors++;
                }
            }
            numActorsSeries.add(floatDate, numActiveActors, false);


            //save data for Spot maket from its start date
            if (data.getStartSpotMarketDate().after(currentDate) == false) {
                //Save Gen Factors Data
                double MWhPrimarySpot = 0.0;
                double MWhSecondarySpot = 0.0;
                double MWhOffSpot = 0.0;
                double MWhRooftopPV = 0.0;
                double MWhCoal = 0.0;
                double MWhWater = 0.0;
                double MWhWind = 0.0;
                double MWhGas = 0.0;
                double MWhSolar = 0.0;
                double MWhBattery = 0.0;

                //Save Price Gen Data
                double PriceAvgRooftopPV = 0.0;
                double PriceAvgCoal = 0.0;
                double PriceAvgWater = 0.0;
                double PriceAvgWind = 0.0;
                double PriceAvgGas = 0.0;
                double PriceAvgSolar = 0.0;
                double PriceAvgBattery = 0.0;

                double numRooftopPV = 0.0;
                double numCoal = 0.0;
                double numWater = 0.0;
                double numWind = 0.0;
                double numGas = 0.0;
                double numSolar = 0.0;
                double numBattery = 0.0;

                for (Integer integer : data.gen_register.keySet()) {
                    Vector<Generator> gens = data.gen_register.get(integer);
                    for (int i = 0; i < gens.size(); i++) {

                        //If time series hasn't been created because it's a new Generator, create the time series
                        if (genCapacityFactorSeries.containsKey(integer) == false) {
                            String nameSeries = Integer.toString(gens.elementAt(i).getId()) + gens.elementAt(i).getfuelSourceDescriptor().substring(0, 2);
                            //Add series to hashmap of series
                            XYSeries seriesGenCapacityFactors = new org.jfree.data.xy.XYSeries(
                                    nameSeries,
                                    false);
                            genCapacityFactorSeries.put(integer, seriesGenCapacityFactors);

                            //Add series to hashmap of series
                            XYSeries seriesSystemProduction = new org.jfree.data.xy.XYSeries(
                                    nameSeries,
                                    false);
                            systemProductionSeries.put(integer, seriesSystemProduction);

                            //Add series to the correct chart
                            //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {
                            if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) == false &&
                                data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"secondary", gens.elementAt(i).getMaxCapacity() ) == false){
                                genCapacityFactorOffSpotChart.addSeries(seriesGenCapacityFactors, null);
                                systemProductionOffSpotChart.addSeries(seriesSystemProduction, null);
                            } else {
                                genCapacityFactorInSpotChart.addSeries(seriesGenCapacityFactors, null);
                                systemProductionInSpotChart.addSeries(seriesSystemProduction, null);

                            }
                        }


                        //Save info in time series
                        genCapacityFactorSeries.get(integer).add(floatDate, gens.get(i).getHistoricCapacityFactor(), false);
                        systemProductionSeries.get(integer).add(floatDate, gens.get(i).getMonthlyGeneratedMWh(), false);

                        if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) ){
                            MWhPrimarySpot += gens.get(i).getMonthlyGeneratedMWh();
                        } else if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"secondary", gens.elementAt(i).getMaxCapacity() ) ) {
                            MWhSecondarySpot += gens.get(i).getMonthlyGeneratedMWh();
                        } else {
                            MWhOffSpot += gens.get(i).getMonthlyGeneratedMWh();
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Solar - Rooftop")) {
                            MWhRooftopPV += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgRooftopPV += gens.get(i).priceMWhLCOE();
                            numRooftopPV ++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Brown Coal")) {
                            MWhCoal += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgCoal += gens.get(i).priceMWhLCOE();
                            numCoal ++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Water")) {
                            MWhWater += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgWater += gens.get(i).priceMWhLCOE();
                            numWater ++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Wind")) {
                            MWhWind += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgWind += gens.get(i).priceMWhLCOE();
                            numWind ++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Natural Gas Pipeline")) {
                            MWhGas += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgGas += gens.get(i).priceMWhLCOE();
                            numGas ++;
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Solar PV - Fixed")) {
                            MWhSolar += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgSolar += gens.get(i).priceMWhLCOE();
                            numSolar ++;
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Battery")) {
                            MWhBattery += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgBattery += gens.get(i).priceMWhLCOE();
                            numBattery ++;
                        }


                    }
                }

                /**
                 * Add series to hashmap of series of aggregated
                 *  1 CombinedTariff (only in PriceGenAvgSeries)
                 *  0 PrimarySpot
                 * -1 SecondarySpot
                 * -2 Offspot
                 * -3 RooftopPV
                 * -4 Coal
                 * -5 Water
                 * -6 Wind
                 * -7 Gas
                 * -8 Solar
                 * -9 Battery
                 */
                systemProductionSeries.get(0).add(floatDate, MWhPrimarySpot, false);
                if (data.settings.existsMarket("secondary"))
                    systemProductionSeries.get(-1).add(floatDate, MWhSecondarySpot, false);
                if(data.settings.existsOffMarket())
                    systemProductionSeries.get(-2).add(floatDate, MWhOffSpot, false);
                systemProductionSeries.get(-3).add(floatDate, MWhRooftopPV, false);
                systemProductionSeries.get(-4).add(floatDate, MWhCoal, false);
                systemProductionSeries.get(-5).add(floatDate, MWhWater, false);
                systemProductionSeries.get(-6).add(floatDate, MWhWind, false);
                systemProductionSeries.get(-7).add(floatDate, MWhGas, false);
                systemProductionSeries.get(-8).add(floatDate, MWhSolar, false);
                systemProductionSeries.get(-9).add(floatDate, MWhBattery, false);

                PriceAvgRooftopPV /= numRooftopPV;
                PriceAvgCoal /= numCoal;
                PriceAvgWater /= numWater;
                PriceAvgWind /= numWind;
                PriceAvgGas /= numGas;
                PriceAvgSolar /= numSolar;
                PriceAvgBattery /= numBattery;


                PriceGenAvgSeries.get(-3).add(floatDate, PriceAvgRooftopPV, false);
                PriceGenAvgSeries.get(-4).add(floatDate, PriceAvgCoal, false);
                PriceGenAvgSeries.get(-5).add(floatDate, PriceAvgWater, false);
                PriceGenAvgSeries.get(-6).add(floatDate, PriceAvgWind, false);
                PriceGenAvgSeries.get(-7).add(floatDate, PriceAvgGas, false);
                PriceGenAvgSeries.get(-8).add(floatDate, PriceAvgSolar, false);
                PriceGenAvgSeries.get(-9).add(floatDate, PriceAvgBattery, false);

            }

            for (int i = 0; i < data.consumptionActors.size(); i++) {

                int id = i + 1;
                //Save data for CSV
                EndUserUnit c = (EndUserUnit) data.consumptionActors.get(i);
                consumptionActorSeries.get(id).add(floatDate, data.consumptionActors.get(i).getCurrentConsumption(), false);
                tariffUsageConsumptionActorSeries.get(id).add(floatDateTariff, data.consumptionActors.get(i).getCurrentTariff(), false);
                ghgConsumptionActorSeries.get(id).add(floatDate, data.consumptionActors.get(i).getCurrentEmissions(), false);
                numDomesticConsumersSeries.get(id).add(floatDate, c.getNumberOfHouseholds(), false);

                //Average/sum data for plot
                sumConsumption += data.consumptionActors.get(i).getCurrentConsumption();
                averageTariff += data.consumptionActors.get(i).getCurrentTariff();
                sumEmissions += data.consumptionActors.get(i).getCurrentEmissions();
                sumDwellings += c.getNumberOfHouseholds();
            }
            averageTariff = averageTariff / data.consumptionActors.size();

            consumptionActorSeries.get(0).add(floatDate, sumConsumption, false);
            tariffUsageConsumptionActorSeries.get(0).add(floatDateTariff, averageTariff, false);
            ghgConsumptionActorSeries.get(0).add(floatDate, sumEmissions, false);
            numDomesticConsumersSeries.get(0).add(floatDate, sumDwellings, false);

            for (Map.Entry<Integer, Arena> entry : data.getArena_register().entrySet()) {
                Arena a = entry.getValue();
                if (a.getType().equalsIgnoreCase("Spot")) {
                    //If Spot Market hasn't started yet, get historic prices
                    if (data.getStartSpotMarketDate().after(currentDate)) {
                        wholesaleSeries.get(0).add(floatDate, 0, false);
                    } else {
                        wholesaleSeries.get(0).add(floatDate, (float) a.getAvgMonthlyPricePrimarySpot(), false);

                        PriceGenAvgSeries.get(1).add(floatDate, (float) a.getTariff(data), false);

                        PriceGenAvgSeries.get(0).add(floatDate, a.getAvgMonthlyPricePrimarySpot(), false);

                        unmetDemandDaysSeries.get(0).add(floatDate, a.getUnmetDemandDays(), false);;
                        unmetDemandHoursSeries.get(0).add(floatDate, a.getUnmetDemandHours(), false);;
                        unmetDemandMwhSeries.get(0).add(floatDate, a.getUnmetDemandMwh(), false);;
                        maxUnmetDemandMwhPerDaySeries.get(0).add(floatDate, a.getMaxUnmetDemandMwhPerHour(), false);;

                        if (data.settings.existsMarket("secondary")) {
                            PriceGenAvgSeries.get(-1).add(floatDate, a.getAvgMonthlyPriceSecondarySpot(), false);

                            unmetDemandDaysSeries.get(-1).add(floatDate, a.getUnmetDemandDaysSecondary(), false);;
                            unmetDemandHoursSeries.get(-1).add(floatDate, a.getUnmetDemandHoursSecondary(), false);;
                            unmetDemandMwhSeries.get(-1).add(floatDate, a.getUnmetDemandMwhSecondary(), false);;
                            maxUnmetDemandMwhPerDaySeries.get(-1).add(floatDate, a.getMaxUnmetDemandMwhPerHourSecondary(), false);;
                        }
                        if(data.settings.existsOffMarket())
                            PriceGenAvgSeries.get(-2).add(floatDate, a.getAvgMonthlyPriceOffSpot(), false);

                    }
                }
            }

            //numDomesticConsumersSeries.get(0).add(year, data.monthly_domestic_consumers_register.get(currentDate), true);


            // we're in the model thread right now, so we shouldn't directly
            // update the consumptionChart/WholesaleChart/GHGChart/etc.  Instead we request an update to occur the next
            // time that control passes back to the Swing event thread.

            consumptionChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            WholesaleChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            TariffUsageChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            ghgChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            numDomesticConsumersChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            genCapacityFactorInSpotChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            genCapacityFactorOffSpotChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            systemProductionOffSpotChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            systemProductionInSpotChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            systemProductionAggChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            PriceGenAvgChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            PriceGenMinChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            PriceGenMaxChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            unmetDemandMwhChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            unmetDemandHoursChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            unmetDemandDaysChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            maxUnmetDemandMwhPerDayChart.updateChartWithin(simState.schedule.getSteps(), 1000);
            numActorsChart.updateChartWithin(simState.schedule.getSteps(), 1000);

        }

        // Update Current simulation date
        data.advanceCurrentSimDate();

        //Add new Consumers according to population growth
        data.generateHouseholds();

        //Finish simulation if endDate is reached
        if (data.getCurrentSimDate().after(data.getEndSimDate()) || data.getCurrentSimDate().equals(data.getEndSimDate())) {
            finalizeCharts(simState);
            if(data.settings.reportGeneration.equals("full")) {
                saveData(simState);
                savePlots(simState);
            }else{
                saveDataLight(simState);
            }


            //Stop Simulation
            data.finish();
        }

    }

    public void finalizeCharts(SimState simState) {
        consumptionChart.update(simState.schedule.getSteps(), true);
        consumptionChart.repaint();
        consumptionChart.stopMovie();

        WholesaleChart.update(simState.schedule.getSteps(), true);
        WholesaleChart.repaint();
        WholesaleChart.stopMovie();

        TariffUsageChart.update(simState.schedule.getSteps(), true);
        TariffUsageChart.repaint();
        TariffUsageChart.stopMovie();

        ghgChart.update(simState.schedule.getSteps(), true);
        ghgChart.repaint();
        ghgChart.stopMovie();

        genCapacityFactorInSpotChart.update(simState.schedule.getSteps(), true);
        genCapacityFactorInSpotChart.repaint();
        genCapacityFactorInSpotChart.stopMovie();

        genCapacityFactorOffSpotChart.update(simState.schedule.getSteps(), true);
        genCapacityFactorOffSpotChart.repaint();
        genCapacityFactorOffSpotChart.stopMovie();

        numDomesticConsumersChart.update(simState.schedule.getSteps(), true);
        numDomesticConsumersChart.repaint();
        numDomesticConsumersChart.stopMovie();

        systemProductionAggChart.update(simState.schedule.getSteps(), true);
        systemProductionAggChart.repaint();
        systemProductionAggChart.stopMovie();

        systemProductionInSpotChart.update(simState.schedule.getSteps(), true);
        systemProductionInSpotChart.repaint();
        systemProductionInSpotChart.stopMovie();

        systemProductionOffSpotChart.update(simState.schedule.getSteps(), true);
        systemProductionOffSpotChart.repaint();
        systemProductionOffSpotChart.stopMovie();

        numActorsChart.update(simState.schedule.getSteps(), true);
        numActorsChart.repaint();
        numActorsChart.stopMovie();

        PriceGenAvgChart.update(simState.schedule.getSteps(), true);
        PriceGenAvgChart.repaint();
        PriceGenAvgChart.stopMovie();

        PriceGenMaxChart.update(simState.schedule.getSteps(), true);
        PriceGenMaxChart.repaint();
        PriceGenMaxChart.stopMovie();

        PriceGenMinChart.update(simState.schedule.getSteps(), true);
        PriceGenMinChart.repaint();
        PriceGenMinChart.stopMovie();

        unmetDemandMwhChart.update(simState.schedule.getSteps(), true);
        unmetDemandMwhChart.repaint();
        unmetDemandMwhChart.stopMovie();

        unmetDemandHoursChart.update(simState.schedule.getSteps(), true);
        unmetDemandHoursChart.repaint();
        unmetDemandHoursChart.stopMovie();

        unmetDemandDaysChart.update(simState.schedule.getSteps(), true);
        unmetDemandDaysChart.repaint();
        unmetDemandDaysChart.stopMovie();

        maxUnmetDemandMwhPerDayChart.update(simState.schedule.getSteps(), true);
        maxUnmetDemandMwhPerDayChart.repaint();
        maxUnmetDemandMwhPerDayChart.stopMovie();

    }

    public void savePlots(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;


        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH_mm_ss");

	String slash = "\\";
	if( System.getProperty("os.name").contains("Windows") == false )
	    slash = "/";
	
        File fc = new File(data.settings.folderOutput+""+slash+"plots"+slash+"HouseholdConsumption" + data.outputID + ".png");
        File ft = new File(data.settings.folderOutput+""+slash+"plots"+slash+"HouseholdTariff" + data.outputID + ".png");
        File fw = new File(data.settings.folderOutput+""+slash+"plots"+slash+"WholesalePrice" + data.outputID + ".png");
        File fg = new File(data.settings.folderOutput+""+slash+"plots"+slash+"HouseholdGHG" + data.outputID + ".png");
        File fgenspot = new File(data.settings.folderOutput+""+slash+"plots"+slash+"GenCapFactorsSpot" + data.outputID + ".png");
        File fgenoffspot = new File(data.settings.folderOutput+""+slash+"plots"+slash+"GenCapFactorsOffSpot" + data.outputID + ".png");

        File fspis = new File(data.settings.folderOutput+""+slash+"plots"+slash+"SystemProductionSpot" + data.outputID + ".png");
        File fspos = new File(data.settings.folderOutput+""+slash+"plots"+slash+"SystemProductionOffSpot" + data.outputID + ".png");
        File fspagg = new File(data.settings.folderOutput+""+slash+"plots"+slash+"SystemProductionAggregated" + data.outputID + ".png");
        File fna = new File(data.settings.folderOutput+""+slash+"plots"+slash+"numActors" + data.outputID + ".png");
        File fpavg = new File(data.settings.folderOutput+""+slash+"plots"+slash+"PriceGenAvg" + data.outputID + ".png");



        File fd = new File(data.settings.folderOutput+""+slash+"plots"+slash+"NumberHouseholds" + data.outputID + ".png");

        File fum = new File(data.settings.folderOutput+""+slash+"plots"+slash+"unmetDemandMwh" + data.outputID + ".png");
        File fuh = new File(data.settings.folderOutput+""+slash+"plots"+slash+"unmetDemandHours" + data.outputID + ".png");
        File fud = new File(data.settings.folderOutput+""+slash+"plots"+slash+"unmetDemandDays" + data.outputID + ".png");
        File fmu = new File(data.settings.folderOutput+""+slash+"plots"+slash+"maxUnmetDemandMwhPerDay" + data.outputID + ".png");

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
                    TariffUsageChart.getChart(),
                    width,
                    height);

            fw.createNewFile();
            ChartUtilities.saveChartAsPNG(fw,
                    WholesaleChart.getChart(),
                    width,
                    height);

            fg.createNewFile();
            ChartUtilities.saveChartAsPNG(fg,
                    ghgChart.getChart(),
                    width,
                    height);

            fgenspot.createNewFile();
            ChartUtilities.saveChartAsPNG(fgenspot,
                    genCapacityFactorInSpotChart.getChart(),
                    width,
                    height);

            fgenoffspot.createNewFile();
            ChartUtilities.saveChartAsPNG(fgenoffspot,
                    genCapacityFactorOffSpotChart.getChart(),
                    width,
                    height);

            fd.createNewFile();
            ChartUtilities.saveChartAsPNG(fd,
                    numDomesticConsumersChart.getChart(),
                    width,
                    height);

            fspagg.createNewFile();
            ChartUtilities.saveChartAsPNG(fspagg,
                    systemProductionAggChart.getChart(),
                    width,
                    height);

            fspis.createNewFile();
            ChartUtilities.saveChartAsPNG(fspis,
                    systemProductionInSpotChart.getChart(),
                    width,
                    height);

            fspos.createNewFile();
            ChartUtilities.saveChartAsPNG(fspos,
                    systemProductionOffSpotChart.getChart(),
                    width,
                    height);

            fna.createNewFile();
            ChartUtilities.saveChartAsPNG(fna,
                    numActorsChart.getChart(),
                    width,
                    height);

            fpavg.createNewFile();
            ChartUtilities.saveChartAsPNG(fpavg,
                    PriceGenAvgChart.getChart(),
                    width,
                    height);

            fum.createNewFile();
            ChartUtilities.saveChartAsPNG(fum,
                    unmetDemandMwhChart.getChart(),
                    width,
                    height);
            fuh.createNewFile();

            ChartUtilities.saveChartAsPNG(fuh,
                    unmetDemandHoursChart.getChart(),
                    width,
                    height);
            fud.createNewFile();

            ChartUtilities.saveChartAsPNG(fud,
                    unmetDemandDaysChart.getChart(),
                    width,
                    height);

            fmu.createNewFile();
            ChartUtilities.saveChartAsPNG(fmu,
                    maxUnmetDemandMwhPerDayChart.getChart(),
                    width,
                    height);

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public void saveData(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        //final String dir = System.getProperty("user.dir");
        //System.out.println("current dir = " + dir);
        String slash = "\\";
        if( System.getProperty("os.name").contains("Windows") == false )
            slash = "/";
	
        String folderName = data.settings.folderOutput+""+slash+"csv"+slash+"" + data.yamlFileName;
        File directory = new File(folderName);
        if (!directory.exists()) {
            directory.mkdir();

        }

        try (

                Writer writerMonthly = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlySummary" + data.outputID + ".csv"));

                CSVWriter csvWriterMonthly = new CSVWriter(writerMonthly,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerYear = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataYearSummary" + data.outputID + ".csv"));

                CSVWriter csvWriterYear = new CSVWriter(writerYear,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);


                    Writer writer = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimData" + data.outputID + ".csv"));

                    CSVWriter csvWriter = new CSVWriter(writer,
                            CSVWriter.DEFAULT_SEPARATOR,
                            CSVWriter.NO_QUOTE_CHARACTER,
                            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                            CSVWriter.DEFAULT_LINE_END);


                    Writer writerGensCapFactorInSpot = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlyGensCapFactorInSpot" + data.outputID + ".csv"));

                    CSVWriter csvWriterGensCapFactorInSpot = new CSVWriter(writerGensCapFactorInSpot,
                            CSVWriter.DEFAULT_SEPARATOR,
                            CSVWriter.NO_QUOTE_CHARACTER,
                            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                            CSVWriter.DEFAULT_LINE_END);


                    Writer writerGensCapFactorOffSpot = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlyGensCapFactorOFFspot" + data.outputID + ".csv"));

                    CSVWriter csvWriterGensCapFactorOffSpot = new CSVWriter(writerGensCapFactorOffSpot,
                            CSVWriter.DEFAULT_SEPARATOR,
                            CSVWriter.NO_QUOTE_CHARACTER,
                            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                            CSVWriter.DEFAULT_LINE_END);

                    Writer writerSystemProductionInSpot = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlySystemProductionInSpot" + data.outputID + ".csv"));

                    CSVWriter csvWriterSystemProductionInSpot = new CSVWriter(writerSystemProductionInSpot,
                            CSVWriter.DEFAULT_SEPARATOR,
                            CSVWriter.NO_QUOTE_CHARACTER,
                            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                            CSVWriter.DEFAULT_LINE_END);


                    Writer writerSystemProductionOffSpot = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlySystemProductionOFFspot" + data.outputID + ".csv"));

                    CSVWriter csvWriterSystemProductionOffSpot = new CSVWriter(writerSystemProductionOffSpot,
                            CSVWriter.DEFAULT_SEPARATOR,
                            CSVWriter.NO_QUOTE_CHARACTER,
                            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                            CSVWriter.DEFAULT_LINE_END);


        ) {


            String[] headerRecord = {"ConsumerUnit", "Time (month)", "Consumption (MWh)", "Tariff (c/KWh)", "Wholesale ($/MWh)", "GHG Emissions (tCO2-e)", "Number of Domestic Consumers (households)"};
            csvWriter.writeNext(headerRecord);

            String[] headerRecordYear = {"Time (Year)", "Consumption (KWh) per household", "Avg Tariff (c/KWh) per household", "Wholesale ($/MWh)", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)", "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot", "System Production Rooftop PV", "Number of Active Actors",
                                        "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)", "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                                        "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)", "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)"};

            csvWriterYear.writeNext(headerRecordYear);

            String[] headerRecordMonthly = {"Time (Month)", "Consumption (KWh) per household", "Avg Tariff (c/KWh) per household", "Wholesale ($/MWh)", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)", "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot", "System Production Rooftop PV", "Number of Active Actors",
                                            "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)", "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                                            "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)", "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)"};

            csvWriterMonthly.writeNext(headerRecordMonthly);

            //Store header Gen cap factors In Spot
            ArrayList<String> headerGenrecordInSpot = new ArrayList<String>();
            ArrayList<String> headerGenrecordOffSpot = new ArrayList<String>();
            headerGenrecordInSpot.add("Date");
            headerGenrecordOffSpot.add("Date");
            TreeMap<Integer, Vector<Generator>> treeMap = new TreeMap<>(data.gen_register);

            for (Integer integer : treeMap.keySet()) {

                Vector<Generator> gens = data.gen_register.get(integer);
                for (int i = 0; i < gens.size(); i++) {
                    String name = gens.elementAt(i).getId() + " - " + gens.elementAt(i).getfuelSourceDescriptor() + " - "
                            + gens.elementAt(i).getName() + " - " + gens.elementAt(i).getMaxCapacity() + " - " + gens.elementAt(i).getDispatchTypeDescriptor() + " - " + gens.elementAt(i).getStart();
                    name = name.replace(",", "-");
                    //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {
                    if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) == false){
                        headerGenrecordOffSpot.add(name);
                    } else {
                        headerGenrecordInSpot.add(name);
                    }
                }
            }
            String[] hgris = headerGenrecordInSpot.toArray(new String[0]);
            csvWriterGensCapFactorInSpot.writeNext(hgris);
            csvWriterSystemProductionInSpot.writeNext(hgris);
            String[] hgros = headerGenrecordOffSpot.toArray(new String[0]);
            csvWriterGensCapFactorOffSpot.writeNext(hgros);
            csvWriterSystemProductionOffSpot.writeNext(hgros);


            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");


            HashMap<String, ArrayList<Double>> datasetGHGsummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPriceSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetWholesaleSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetKWhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetConsumersSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdPrimarySpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdSecondarySpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdOffSpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdRooftopSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandMwhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandHoursSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandDaysSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimarymaxUnmetDemandMwhPerDaySummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandMwhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandHoursSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandDaysSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondarymaxUnmetDemandMwhPerDaySummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetNumActorsSummary = new HashMap<>();



            for (int i = 0; i <= data.consumptionActors.size(); i++) {

                XYSeries cseries = consumptionActorSeries.get(i);
                XYSeries tseries = tariffUsageConsumptionActorSeries.get(i);
                XYSeries wseries = wholesaleSeries.get(0);
                XYSeries gseries = ghgConsumptionActorSeries.get(i);
                XYSeries dseries = numDomesticConsumersSeries.get(i);
                XYSeries spprimaggseries = systemProductionSeries.get(0);
                XYSeries spsecondaggseries = systemProductionSeries.get(-1);
                XYSeries sposaggseries = systemProductionSeries.get(-2);
                XYSeries sproofaggseries = systemProductionSeries.get(-3);
                XYSeries naseries = numActorsSeries;

                XYSeries primaryunmetDemandMwhSeries = unmetDemandMwhSeries.get(0);
                XYSeries secondaryunmetDemandMwhSeries = unmetDemandMwhSeries.get(-1);
                XYSeries primaryunmetDemandHoursSeries = unmetDemandHoursSeries.get(0);
                XYSeries secondaryunmetDemandHoursSeries = unmetDemandHoursSeries.get(-1);
                XYSeries primaryunmetDemandDaysSeries = unmetDemandDaysSeries.get(0);
                XYSeries secondaryunmetDemandDaysSeries = unmetDemandDaysSeries.get(-1);
                XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerDaySeries.get(0);
                XYSeries secondarymaxUnmetDemandMwhPerDaySeries= maxUnmetDemandMwhPerDaySeries.get(-1);


                Calendar c = Calendar.getInstance();

                //First series starts from beginning simulation, the rest follows a consumtionUnit, which has a creationDate
                if (i == 0)
                    c.setTime(data.getStartSimDate());
                else {
                    EndUserUnit consumer = (EndUserUnit) data.consumptionActors.get(i - 1);
                    c.setTime(consumer.creationDate);
                }

                for (int t = 0; t < cseries.getItems().size(); t++) {
                    //for (Object o : series.getItems()) {

                    int shifttimeseries = cseries.getItemCount() - spprimaggseries.getItemCount();


                    XYDataItem citem = (XYDataItem) cseries.getItems().get(t);
                    XYDataItem titem = (XYDataItem) tseries.getItems().get(t);
                    XYDataItem witem = (XYDataItem) wseries.getItems().get(t);
                    XYDataItem gitem = (XYDataItem) gseries.getItems().get(t);
                    XYDataItem ditem = (XYDataItem) dseries.getItems().get(t);
                    XYDataItem spprimaggitem = null;
                    XYDataItem spsecondaggitem = null;
                    XYDataItem sposaggitem = null;
                    XYDataItem sproofaggitem = null;
                    XYDataItem primaryunmetDemandMwhitem = null;
                    XYDataItem primaryunmetDemandHoursitem = null;
                    XYDataItem primaryunmetDemandDaysitem = null;
                    XYDataItem primarymaxUnmetDemandMwhPerDayitem = null;
                    XYDataItem secondaryunmetDemandMwhitem = null;
                    XYDataItem secondaryunmetDemandHoursitem = null;
                    XYDataItem secondaryunmetDemandDaysitem = null;
                    XYDataItem secondarymaxUnmetDemandMwhPerDayitem = null;

                    XYDataItem naitem = (XYDataItem) naseries.getItems().get(t);
                    if (shifttimeseries <= t) {
                        spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);
                        if(spsecondaggseries != null)
                            spsecondaggitem = (XYDataItem) spsecondaggseries.getItems().get(t - shifttimeseries);
                        if(sposaggseries != null)
                            sposaggitem = (XYDataItem) sposaggseries.getItems().get(t - shifttimeseries);
                        sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);

                        primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems().get(t - shifttimeseries);
                        primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems().get(t - shifttimeseries);
                        primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems().get(t - shifttimeseries);
                        primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries.getItems().get(t - shifttimeseries);
                        if( secondaryunmetDemandMwhSeries != null){
                            secondaryunmetDemandMwhitem = (XYDataItem) secondaryunmetDemandMwhSeries.getItems().get(t - shifttimeseries);
                            secondaryunmetDemandHoursitem = (XYDataItem) secondaryunmetDemandHoursSeries.getItems().get(t - shifttimeseries);
                            secondaryunmetDemandDaysitem = (XYDataItem) secondaryunmetDemandDaysSeries.getItems().get(t - shifttimeseries);
                            secondarymaxUnmetDemandMwhPerDayitem = (XYDataItem) secondarymaxUnmetDemandMwhPerDaySeries.getItems().get(t - shifttimeseries);
                        }

                    }

                    double kwh = citem.getYValue() * 1000.0;
                    double tariffPrice = titem.getYValue();
                    double wholesale = witem.getYValue();
                    double emissions = gitem.getYValue();
                    double consumers = ditem.getYValue();
                    double MWhPrimarySpotAgg = 0.0;
                    double MWhSecondarySpotAgg = 0.0;
                    double MWhOffSpotAgg = 0.0;
                    double MWhRoofSpotAgg = 0.0;
                    double numActors = naitem.getYValue();
                    double primaryunmetDemandMwh = 0.0;
                    double primaryunmetDemandHours = 0.0;
                    double primaryunmetDemandDays = 0.0;
                    double primarymaxUnmetDemandMwhPerDay = 0.0;
                    double secondaryunmetDemandMwh = 0.0;
                    double secondaryunmetDemandHours = 0.0;
                    double secondaryunmetDemandDays = 0.0;
                    double secondarymaxUnmetDemandMwhPerDay = 0.0;

                    if (shifttimeseries <= t) {
                        MWhPrimarySpotAgg = spprimaggitem.getYValue();
                        if(spsecondaggitem != null)
                            MWhSecondarySpotAgg = spsecondaggitem.getYValue();
                        if(sposaggitem != null)
                            MWhOffSpotAgg = sposaggitem.getYValue();
                        MWhRoofSpotAgg = sproofaggitem.getYValue();
                        primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                        primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                        primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                        primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();
                        if(secondaryunmetDemandMwhitem != null) {
                            secondaryunmetDemandMwh = secondaryunmetDemandMwhitem.getYValue();
                            secondaryunmetDemandHours = secondaryunmetDemandHoursitem.getYValue();
                            secondaryunmetDemandDays = secondaryunmetDemandDaysitem.getYValue();
                            secondarymaxUnmetDemandMwhPerDay = secondarymaxUnmetDemandMwhPerDayitem.getYValue();
                        }

                    }

                    SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                    //First series starts from beginning simulation, the rest follows a consumtionUnit, which has a creationDate
                    if (i == 0) {
                        String[] record = {dateToString.format(c.getTime()), Double.toString(kwh / consumers), Double.toString(tariffPrice), Double.toString(wholesale), Double.toString(emissions / consumers),
                                Double.toString(consumers), Double.toString(MWhPrimarySpotAgg), Double.toString(MWhSecondarySpotAgg), Double.toString(MWhOffSpotAgg), Double.toString(MWhRoofSpotAgg), Double.toString(numActors),
                                Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours), Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                                Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours), Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay)};
                        csvWriterMonthly.writeNext(record);

                        //Store data about Gen cap factors
                        ArrayList<String> genrecordInSpot = new ArrayList<String>();
                        genrecordInSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> genrecordOffSpot = new ArrayList<String>();
                        genrecordOffSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> sysprodInSpot = new ArrayList<String>();
                        sysprodInSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> sysprodOffSpot = new ArrayList<String>();
                        sysprodOffSpot.add(dateToString.format(c.getTime()));

                        //Writes data to CSV files about in/off spot generation and system production
                        for (Integer integer : treeMap.keySet()) {
                            XYSeries genseries = genCapacityFactorSeries.get(integer);
                            XYSeries sysprodseries = systemProductionSeries.get(integer);

                            int shift = cseries.getItemCount() - genseries.getItemCount();
                            Vector<Generator> gens = data.gen_register.get(integer);

                            //If Generator hasn't started yet, print "-"
                            if (shift > t) {
                                //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {
                                if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) == false){
                                    genrecordOffSpot.add("-");
                                    sysprodOffSpot.add("-");
                                } else {
                                    genrecordInSpot.add("-");
                                    sysprodInSpot.add("-");
                                }
                            } else {
                                //If Generator has started, print its historic cap factor
                                XYDataItem genitem = (XYDataItem) genseries.getItems().get(t - shift);
                                XYDataItem spitem = (XYDataItem) sysprodseries.getItems().get(t - shift);

                                //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {
                                if( data.settings.isMarketPaticipant( gens.elementAt(i).getDispatchTypeDescriptor(),"primary", gens.elementAt(i).getMaxCapacity() ) == false){
                                    genrecordOffSpot.add(Double.toString(genitem.getYValue()));
                                    sysprodOffSpot.add(Double.toString(spitem.getYValue()));

                                } else {
                                    genrecordInSpot.add(Double.toString(genitem.getYValue()));
                                    sysprodInSpot.add(Double.toString(spitem.getYValue()));
                                }

                            }
                        }
                        String[] gris = genrecordInSpot.toArray(new String[0]);
                        csvWriterGensCapFactorInSpot.writeNext(gris);
                        String[] gros = genrecordOffSpot.toArray(new String[0]);
                        csvWriterGensCapFactorOffSpot.writeNext(gros);

                        String[] spis = sysprodInSpot.toArray(new String[0]);
                        csvWriterSystemProductionInSpot.writeNext(spis);
                        String[] spos = sysprodOffSpot.toArray(new String[0]);
                        csvWriterSystemProductionOffSpot.writeNext(spos);

                    } else {
                        String[] record = {Integer.toString(i), dateToString.format(c.getTime()), Double.toString(kwh), Double.toString(tariffPrice), Double.toString(wholesale), Double.toString(emissions), Double.toString(consumers), Double.toString(numActors)};
                        csvWriter.writeNext(record);
                    }


                    //Year summary based on monthly data from the first series with id=0 that aggregates all SPM data
                    if (i == 0) {
                        String year = dateToYear.format(c.getTime());

                        if (!datasetNumActorsSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetNumActorsSummary.put(year, yearData);
                        }

                        datasetNumActorsSummary.get(year).add(numActors);

                        if (!datasetGHGsummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetGHGsummary.put(year, yearData);
                        }

                        datasetGHGsummary.get(year).add(emissions / consumers);

                        if (!datasetPriceSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPriceSummary.put(year, yearData);
                        }

                        datasetPriceSummary.get(year).add(tariffPrice);

                        if (!datasetWholesaleSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetWholesaleSummary.put(year, yearData);
                        }
                        datasetWholesaleSummary.get(year).add(wholesale);


                        if (!datasetKWhSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetKWhSummary.put(year, yearData);
                        }

                        //Divide Consumption to make it into consumption per household
                        datasetKWhSummary.get(year).add(kwh / consumers);

                        if (!datasetConsumersSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetConsumersSummary.put(year, yearData);
                        }

                        datasetConsumersSummary.get(year).add(consumers);

                        if (!datasetSysProdPrimarySpotSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSysProdPrimarySpotSummary.put(year, yearData);
                        }
                        datasetSysProdPrimarySpotSummary.get(year).add(MWhPrimarySpotAgg);

                        if (!datasetSysProdSecondarySpotSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSysProdSecondarySpotSummary.put(year, yearData);
                        }
                        datasetSysProdSecondarySpotSummary.get(year).add(MWhSecondarySpotAgg);

                        if (!datasetSysProdOffSpotSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSysProdOffSpotSummary.put(year, yearData);
                        }
                        datasetSysProdOffSpotSummary.get(year).add(MWhOffSpotAgg);

                        if (!datasetSysProdRooftopSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSysProdRooftopSummary.put(year, yearData);
                        }
                        datasetSysProdRooftopSummary.get(year).add(MWhRoofSpotAgg);

                        if (!datasetPrimaryunmetDemandMwhSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandMwhSummary .put(year, yearData);
                        }
                        datasetPrimaryunmetDemandMwhSummary .get(year).add(primaryunmetDemandMwh);

                        if (!datasetPrimaryunmetDemandHoursSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandHoursSummary .put(year, yearData);
                        }
                        datasetPrimaryunmetDemandHoursSummary .get(year).add(primaryunmetDemandHours);

                        if (!datasetPrimaryunmetDemandDaysSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandDaysSummary .put(year, yearData);
                        }
                        datasetPrimaryunmetDemandDaysSummary .get(year).add(primaryunmetDemandDays);

                        if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimarymaxUnmetDemandMwhPerDaySummary .put(year, yearData);
                        }
                        datasetPrimarymaxUnmetDemandMwhPerDaySummary .get(year).add(primarymaxUnmetDemandMwhPerDay);

                        if (!datasetSecondaryunmetDemandMwhSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandMwhSummary .put(year, yearData);
                        }
                        datasetSecondaryunmetDemandMwhSummary .get(year).add(secondaryunmetDemandMwh);

                        if (!datasetSecondaryunmetDemandHoursSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandHoursSummary .put(year, yearData);
                        }
                        datasetSecondaryunmetDemandHoursSummary .get(year).add(secondaryunmetDemandHours);

                        if (!datasetSecondaryunmetDemandDaysSummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandDaysSummary .put(year, yearData);
                        }
                        datasetSecondaryunmetDemandDaysSummary .get(year).add(secondaryunmetDemandDays);

                        if (!datasetSecondarymaxUnmetDemandMwhPerDaySummary .containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondarymaxUnmetDemandMwhPerDaySummary .put(year, yearData);
                        }
                        datasetSecondarymaxUnmetDemandMwhPerDaySummary .get(year).add(secondarymaxUnmetDemandMwhPerDay);

                    }

                    c.add(Calendar.MONTH, 1);
                }
            }
//            csvWriter.close();
//            csvWriterMonthly.close();
//            csvWriterGensCapFactorInSpot.close();
//            csvWriterGensCapFactorOffSpot.close();
//            csvWriterSystemProductionInSpot.close();
//            csvWriterSystemProductionOffSpot.close();


            //Save Year summary to csv file
            Object[] years = datasetGHGsummary.keySet().toArray();
            Arrays.sort(years, Collections.reverseOrder());
            for (int i = years.length - 1; i >= 0; i--) {
                String year = (String) years[i];
                ArrayList<Double> yearDataGHG = datasetGHGsummary.get(year);
                ArrayList<Double> yearDataPrice = datasetPriceSummary.get(year);
                ArrayList<Double> yearDataWholesale = datasetWholesaleSummary.get(year);
                ArrayList<Double> yearDataKWh = datasetKWhSummary.get(year);
                ArrayList<Double> yearDataConsumers = datasetConsumersSummary.get(year);
                ArrayList<Double> spPrimarySpotAggConsumers = datasetSysProdPrimarySpotSummary.get(year);
                ArrayList<Double> spSecondarySpotAggConsumers = datasetSysProdSecondarySpotSummary.get(year);
                ArrayList<Double> spOffSpotAggConsumers = datasetSysProdOffSpotSummary.get(year);
                ArrayList<Double> spRooftopAggConsumers = datasetSysProdRooftopSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary .get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary .get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary .get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandMwh = datasetSecondaryunmetDemandMwhSummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandHours = datasetSecondaryunmetDemandHoursSummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandDays = datasetSecondaryunmetDemandDaysSummary .get(year);
                ArrayList<Double> yearSecondarymaxUnmetDemandMwhPerDay = datasetSecondarymaxUnmetDemandMwhPerDaySummary .get(year);
                ArrayList<Double> yearDataNumActors = datasetNumActorsSummary.get(year);


                Double totalGHG = 0.0;
                Double avgPrice = 0.0;
                Double avgWholesale = 0.0;
                Double totalKWh = 0.0;
                Double dwellingsLastMonth = 0.0;
                Double spprimspotagg = 0.0;
                Double spsecondspotagg = 0.0;
                Double spoffspotagg = 0.0;
                Double sproofagg = 0.0;
                Double maxNumActors = 0.0;

                Double primaryunmetDemandMwh = 0.0;
                Double primaryunmetDemandHours = 0.0;
                Double primaryunmetDemandDays = 0.0;
                Double primarymaxUnmetDemandMwhPerDay = 0.0;
                Double secondaryunmetDemandMwh = 0.0;
                Double secondaryunmetDemandHours = 0.0;
                Double secondaryunmetDemandDays = 0.0;
                Double secondarymaxUnmetDemandMwhPerDay = 0.0;

                Double sizeData = (double) yearDataGHG.size();

                for (Double d : yearPrimaryunmetDemandMwh) {
                    primaryunmetDemandMwh += d;
                }
                for (Double d : yearPrimaryunmetDemandHours) {
                    primaryunmetDemandHours += d;
                }
                for (Double d : yearPrimaryunmetDemandDays) {
                    primaryunmetDemandDays += d;
                }
                primarymaxUnmetDemandMwhPerDay =  Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);
                for (Double d : yearSecondaryunmetDemandMwh) {
                    secondaryunmetDemandMwh += d;
                }
                for (Double d : yearSecondaryunmetDemandHours) {
                    secondaryunmetDemandHours += d;
                }
                for (Double d : yearSecondaryunmetDemandDays) {
                    secondaryunmetDemandDays += d;
                }
                secondarymaxUnmetDemandMwhPerDay =  Collections.max(yearSecondarymaxUnmetDemandMwhPerDay);

                for (Double ghg : yearDataGHG) {
                    totalGHG += ghg;
                }

                for (Double con : yearDataNumActors) {
                    if (con > maxNumActors) maxNumActors = con;
                }

                for (Double p : yearDataPrice) {
                    avgPrice += p;
                }
                avgPrice /= (double) yearDataPrice.size();

                for (Double w : yearDataWholesale) {
                    avgWholesale += w;
                }
                avgWholesale /= (double) yearDataWholesale.size();

                for (Double k : yearDataKWh) {
                    totalKWh += k;
                }

                // We report number of consumers in a given year based on last monthly data
                dwellingsLastMonth = yearDataConsumers.get( yearDataConsumers.size() - 1 );

                for (Double k : spPrimarySpotAggConsumers) {
                    spprimspotagg += k;
                }

                for (Double k : spSecondarySpotAggConsumers) {
                    spsecondspotagg += k;
                }

                for (Double k : spOffSpotAggConsumers) {
                    spoffspotagg += k;
                }

                for (Double k : spRooftopAggConsumers) {
                    sproofagg += k;
                }

                String[] record = {year, Double.toString(totalKWh), Double.toString(avgPrice), Double.toString(avgWholesale), Double.toString(totalGHG),
                        Double.toString(dwellingsLastMonth), Double.toString(spprimspotagg), Double.toString(spsecondspotagg), Double.toString(spoffspotagg), Double.toString(sproofagg), Double.toString(maxNumActors),
                        Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours), Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours), Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay)};
                csvWriterYear.writeNext(record);
            }

            //csvWriterYear.close();

        } catch (IOException ex) {
            System.out.println(ex);
        }


    }

    public void saveDataLight(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        //final String dir = System.getProperty("user.dir");
        //System.out.println("current dir = " + dir);
        String slash = "\\";
        if( System.getProperty("os.name").contains("Windows") == false )
            slash = "/";

        String folderName = data.settings.folderOutput+""+slash+"csv"+slash+"" + data.yamlFileName;
        File directory = new File(folderName);
        if (!directory.exists()) {
            directory.mkdir();

        }

        try (

                Writer writerMonthly = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataMonthlySummary" + data.outputID + ".csv"));

                CSVWriter csvWriterMonthly = new CSVWriter(writerMonthly,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerYear = Files.newBufferedWriter(Paths.get(folderName + "/" + data.yamlFileName + "SimDataYearSummary" + data.outputID + ".csv"));

                CSVWriter csvWriterYear = new CSVWriter(writerYear,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);




        ) {
            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");

            //Store header Gen Productions
            ArrayList<String> headerYear = new ArrayList<String>( Arrays.asList("Time (Year)", "Consumption (KWh) per household", "Avg Tariff (c/KWh) per household", "Wholesale ($/MWh)", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "Percentage Renewable Production", "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "System Production Coal", "System Production Water", "System Production Wind", "System Production Gas",
                    "System Production Solar", "System Production Battery", "Number of Active Actors", "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)", "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)", "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)"));
            TreeMap<Integer, Vector<Generator>> treeMap = new TreeMap<>(data.gen_register);

            for (Integer integer : treeMap.keySet()) {

                Vector<Generator> gens = data.gen_register.get(integer);
                for (int i = 0; i < gens.size(); i++) {
                    String name = gens.elementAt(i).getId() + " - " + gens.elementAt(i).getfuelSourceDescriptor() + " - "
                            + gens.elementAt(i).getName() + " - " + gens.elementAt(i).getMaxCapacity() + " - " + gens.elementAt(i).getDispatchTypeDescriptor() + " - " + dateToYear.format(gens.elementAt(i).getStart()) + " - " + dateToYear.format(gens.elementAt(i).getEnd());
                    name = name.replace(",", "-");

                    if(gens.elementAt(i).getInPrimaryMarket())
                        name += " - Primary";
                    else if(gens.elementAt(i).getInPrimaryMarket())
                        name += " - Secondary";
                    else
                        name += " - OffSpot";

                    headerYear.add(name);

                }
            }
            String[] hy = headerYear.toArray(new String[0]);


            csvWriterYear.writeNext(hy);

            String[] headerRecordMonthly = {"Time (Month)", "Consumption (KWh) per household", "Avg Tariff (c/KWh) per household", "Wholesale ($/MWh)", "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "Percentage Renewable Production", "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "System Production Coal", "System Production Water", "System Production Wind", "System Production Gas",
                    "System Production Solar", "System Production Battery", "Number of Active Actors", "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)", "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)", "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)"};

            csvWriterMonthly.writeNext(headerRecordMonthly);




            HashMap<String, ArrayList<Double>> datasetGHGsummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPriceSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetWholesaleSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetKWhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetConsumersSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetNumActorsSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdPrimarySpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdSecondarySpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdOffSpotSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdRooftopSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdCoalSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdWaterSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdWindSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdGasSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdSolSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdBatSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdRenewableSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdFossilSummary = new HashMap<>();
            HashMap<String, ArrayList<ArrayList<Double>>> datasetSysGenProdSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandMwhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandHoursSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandDaysSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimarymaxUnmetDemandMwhPerDaySummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandMwhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandHoursSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondaryunmetDemandDaysSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSecondarymaxUnmetDemandMwhPerDaySummary = new HashMap<>();






            XYSeries cseries = consumptionActorSeries.get(0);
            XYSeries tseries = tariffUsageConsumptionActorSeries.get(0);
            XYSeries wseries = wholesaleSeries.get(0);
            XYSeries gseries = ghgConsumptionActorSeries.get(0);
            XYSeries dseries = numDomesticConsumersSeries.get(0);
            XYSeries naseries = numActorsSeries;
            /**
             * Add series to hashmap of series of aggregated
             *  1 CombinedTariff (only in PriceGenAvgSeries)
             *  0 PrimarySpot
             * -1 SecondarySpot
             * -2 Offspot
             * -3 RooftopPV
             * -4 Coal
             * -5 Water
             * -6 Wind
             * -7 Gas
             * -8 Solar
             * -9 Battery
             */
            XYSeries spprimaggseries = systemProductionSeries.get(0);
            XYSeries spsecondaggseries = systemProductionSeries.get(-1);
            XYSeries sposaggseries = systemProductionSeries.get(-2);
            XYSeries sproofaggseries = systemProductionSeries.get(-3);
            XYSeries spcoalaggseries = systemProductionSeries.get(-4);
            XYSeries spwateraggseries = systemProductionSeries.get(-5);
            XYSeries spwindaggseries = systemProductionSeries.get(-6);
            XYSeries spgasaggseries = systemProductionSeries.get(-7);
            XYSeries spsolaggseries = systemProductionSeries.get(-8);
            XYSeries spbataggseries = systemProductionSeries.get(-9);

            XYSeries primaryunmetDemandMwhSeries = unmetDemandMwhSeries.get(0);
            XYSeries secondaryunmetDemandMwhSeries = unmetDemandMwhSeries.get(-1);
            XYSeries primaryunmetDemandHoursSeries = unmetDemandHoursSeries.get(0);
            XYSeries secondaryunmetDemandHoursSeries = unmetDemandHoursSeries.get(-1);
            XYSeries primaryunmetDemandDaysSeries = unmetDemandDaysSeries.get(0);
            XYSeries secondaryunmetDemandDaysSeries = unmetDemandDaysSeries.get(-1);
            XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerDaySeries.get(0);
            XYSeries secondarymaxUnmetDemandMwhPerDaySeries= maxUnmetDemandMwhPerDaySeries.get(-1);


            Calendar c = Calendar.getInstance();

            //First series starts from beginning simulation, the rest follows a consumtionUnit, which has a creationDate

            c.setTime(data.getStartSimDate());


            for (int t = 0; t < cseries.getItems().size(); t++) {
                //for (Object o : series.getItems()) {

                int shifttimeseries = cseries.getItemCount() - spprimaggseries.getItemCount();


                XYDataItem citem = (XYDataItem) cseries.getItems().get(t);
                XYDataItem titem = (XYDataItem) tseries.getItems().get(t);
                XYDataItem witem = (XYDataItem) wseries.getItems().get(t);
                XYDataItem gitem = (XYDataItem) gseries.getItems().get(t);
                XYDataItem ditem = (XYDataItem) dseries.getItems().get(t);

                XYDataItem spprimaggitem = null;
                XYDataItem spsecondaggitem = null;
                XYDataItem sposaggitem = null;
                XYDataItem sproofaggitem = null;
                XYDataItem spcoalaggitem = null;
                XYDataItem spwateraggitem = null;
                XYDataItem spwindaggitem = null;
                XYDataItem spgasaggitem = null;
                XYDataItem spsolaggitem = null;
                XYDataItem spbataggitem = null;

                XYDataItem primaryunmetDemandMwhitem = null;
                XYDataItem primaryunmetDemandHoursitem = null;
                XYDataItem primaryunmetDemandDaysitem = null;
                XYDataItem primarymaxUnmetDemandMwhPerDayitem = null;
                XYDataItem secondaryunmetDemandMwhitem = null;
                XYDataItem secondaryunmetDemandHoursitem = null;
                XYDataItem secondaryunmetDemandDaysitem = null;
                XYDataItem secondarymaxUnmetDemandMwhPerDayitem = null;

                XYDataItem naitem = (XYDataItem) naseries.getItems().get(t);
                if (shifttimeseries <= t) {
                    spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);
                    if(spsecondaggseries != null)
                        spsecondaggitem = (XYDataItem) spsecondaggseries.getItems().get(t - shifttimeseries);
                    if(sposaggseries != null)
                        sposaggitem = (XYDataItem) sposaggseries.getItems().get(t - shifttimeseries);

                    sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);
                    spcoalaggitem = (XYDataItem) spcoalaggseries.getItems().get(t - shifttimeseries);
                    spwateraggitem = (XYDataItem) spwateraggseries.getItems().get(t - shifttimeseries);
                    spwindaggitem = (XYDataItem) spwindaggseries.getItems().get(t - shifttimeseries);
                    spgasaggitem = (XYDataItem) spgasaggseries.getItems().get(t - shifttimeseries);
                    spsolaggitem = (XYDataItem) spsolaggseries.getItems().get(t - shifttimeseries);
                    spbataggitem = (XYDataItem) spbataggseries.getItems().get(t - shifttimeseries);

                    primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems().get(t - shifttimeseries);
                    primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems().get(t - shifttimeseries);
                    primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems().get(t - shifttimeseries);
                    primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries.getItems().get(t - shifttimeseries);
                    if( secondaryunmetDemandMwhSeries != null){
                        secondaryunmetDemandMwhitem = (XYDataItem) secondaryunmetDemandMwhSeries.getItems().get(t - shifttimeseries);
                        secondaryunmetDemandHoursitem = (XYDataItem) secondaryunmetDemandHoursSeries.getItems().get(t - shifttimeseries);
                        secondaryunmetDemandDaysitem = (XYDataItem) secondaryunmetDemandDaysSeries.getItems().get(t - shifttimeseries);
                        secondarymaxUnmetDemandMwhPerDayitem = (XYDataItem) secondarymaxUnmetDemandMwhPerDaySeries.getItems().get(t - shifttimeseries);
                    }
                }

                double kwh = citem.getYValue() * 1000.0;
                double tariffPrice = titem.getYValue();
                double wholesale = witem.getYValue();
                double emissions = gitem.getYValue();
                double consumers = ditem.getYValue();
                double MWhPrimarySpotAgg = 0.0;
                double MWhSecondarySpotAgg = 0.0;
                double MWhOffSpotAgg = 0.0;
                double MWhRoofSpotAgg = 0.0;
                double MWhCoalSpotAgg = 0.0;
                double MWhWaterSpotAgg = 0.0;
                double MWhWindSpotAgg = 0.0;
                double MWhGasSpotAgg = 0.0;
                double MWhSolSpotAgg = 0.0;
                double MWhBatSpotAgg = 0.0;
                double MWhRenewable = 0.0;
                double MWhFossil = 0.0;
                double percentageRenwewable = 0.0;

                double primaryunmetDemandMwh = 0.0;
                double primaryunmetDemandHours = 0.0;
                double primaryunmetDemandDays = 0.0;
                double primarymaxUnmetDemandMwhPerDay = 0.0;
                double secondaryunmetDemandMwh = 0.0;
                double secondaryunmetDemandHours = 0.0;
                double secondaryunmetDemandDays = 0.0;
                double secondarymaxUnmetDemandMwhPerDay = 0.0;

                double numActors = naitem.getYValue();

                if (shifttimeseries <= t) {
                    MWhPrimarySpotAgg = spprimaggitem.getYValue();
                    if(spsecondaggitem != null)
                        MWhSecondarySpotAgg = spsecondaggitem.getYValue();
                    if(sposaggitem != null)
                        MWhOffSpotAgg = sposaggitem.getYValue();
                    MWhRoofSpotAgg = sproofaggitem.getYValue();
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();
                    MWhWindSpotAgg = spwindaggitem.getYValue();
                    MWhGasSpotAgg = spgasaggitem.getYValue();
                    MWhSolSpotAgg = spsolaggitem.getYValue();
                    MWhBatSpotAgg = spbataggitem.getYValue();

                    MWhRenewable = MWhRoofSpotAgg + MWhWaterSpotAgg + MWhWaterSpotAgg + MWhWindSpotAgg + MWhSolSpotAgg + MWhBatSpotAgg;
                    MWhFossil = MWhCoalSpotAgg + MWhGasSpotAgg;
                    percentageRenwewable = MWhRenewable / (MWhRenewable + MWhFossil);

                    primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                    primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                    primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                    primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();
                    if(secondaryunmetDemandMwhitem != null) {
                        secondaryunmetDemandMwh = secondaryunmetDemandMwhitem.getYValue();
                        secondaryunmetDemandHours = secondaryunmetDemandHoursitem.getYValue();
                        secondaryunmetDemandDays = secondaryunmetDemandDaysitem.getYValue();
                        secondarymaxUnmetDemandMwhPerDay = secondarymaxUnmetDemandMwhPerDayitem.getYValue();
                    }
                }

                SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                String[] record = {dateToString.format(c.getTime()), Double.toString(kwh / consumers), Double.toString(tariffPrice), Double.toString(wholesale), Double.toString(emissions / consumers),
                        Double.toString(consumers), Double.toString(percentageRenwewable), Double.toString(MWhPrimarySpotAgg), Double.toString(MWhSecondarySpotAgg), Double.toString(MWhOffSpotAgg), Double.toString(MWhRoofSpotAgg),
                        Double.toString(MWhCoalSpotAgg), Double.toString(MWhWaterSpotAgg), Double.toString(MWhWindSpotAgg), Double.toString(MWhGasSpotAgg),
                        Double.toString(MWhSolSpotAgg), Double.toString(MWhBatSpotAgg), Double.toString(numActors), Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours), Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours), Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay)};
                csvWriterMonthly.writeNext(record);

                /***
                 * Generators Production Data
                 */

                //Store data about Gen prod
                ArrayList<Double> sysGenProd = new ArrayList<Double>();

                //Writes data to CSV files about in/off spot generation and system production
                for (Integer integer : treeMap.keySet()) {
                    XYSeries sysprodseries = systemProductionSeries.get(integer);

                    int shift = cseries.getItemCount() - sysprodseries.getItemCount();

                    //If Generator hasn't started yet, print "-"
                    if (shift > t) {
                        sysGenProd.add(0.0);
                        //if (gens.elementAt(i).getDispatchTypeDescriptor().equals("S") == false || gens.elementAt(i).getMaxCapacity() < 30) {

                    } else {
                        //If Generator has started, print its historic cap factor
                        XYDataItem spitem = (XYDataItem) sysprodseries.getItems().get(t - shift);
                        sysGenProd.add(spitem.getYValue())  ;
                    }
                }


                /**
                 * save data to compute yearly indicators
                 */


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

                datasetPriceSummary.get(year).add(tariffPrice);

                if (!datasetWholesaleSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetWholesaleSummary.put(year, yearData);
                }
                datasetWholesaleSummary.get(year).add(wholesale);


                if (!datasetKWhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetKWhSummary.put(year, yearData);
                }

                //Divide Consumption to make it into consumption per household
                datasetKWhSummary.get(year).add(kwh / consumers);

                if (!datasetConsumersSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetConsumersSummary.put(year, yearData);
                }

                datasetConsumersSummary.get(year).add(consumers);

                if (!datasetNumActorsSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetNumActorsSummary.put(year, yearData);
                }

                datasetNumActorsSummary.get(year).add(numActors);


                if (!datasetSysProdPrimarySpotSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdPrimarySpotSummary.put(year, yearData);
                }
                datasetSysProdPrimarySpotSummary.get(year).add(MWhPrimarySpotAgg);

                if (!datasetSysProdSecondarySpotSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdSecondarySpotSummary.put(year, yearData);
                }
                datasetSysProdSecondarySpotSummary.get(year).add(MWhSecondarySpotAgg);

                if (!datasetSysProdOffSpotSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdOffSpotSummary.put(year, yearData);
                }
                datasetSysProdOffSpotSummary.get(year).add(MWhOffSpotAgg);

                if (!datasetSysProdRooftopSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdRooftopSummary.put(year, yearData);
                }
                datasetSysProdRooftopSummary.get(year).add(MWhRoofSpotAgg);

                if (!datasetSysProdCoalSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdCoalSummary.put(year, yearData);
                }
                datasetSysProdCoalSummary.get(year).add(MWhCoalSpotAgg);

                if (!datasetSysProdWaterSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdWaterSummary.put(year, yearData);
                }
                datasetSysProdWaterSummary.get(year).add(MWhWaterSpotAgg);

                if (!datasetSysProdWindSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdWindSummary.put(year, yearData);
                }
                datasetSysProdWindSummary.get(year).add(MWhWindSpotAgg);

                if (!datasetSysProdGasSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdGasSummary.put(year, yearData);
                }
                datasetSysProdGasSummary.get(year).add(MWhGasSpotAgg);

                if (!datasetSysProdSolSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdSolSummary.put(year, yearData);
                }
                datasetSysProdSolSummary.get(year).add(MWhSolSpotAgg);

                if (!datasetSysProdBatSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdBatSummary.put(year, yearData);
                }
                datasetSysProdBatSummary.get(year).add(MWhBatSpotAgg);

                if (!datasetSysProdRenewableSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdRenewableSummary.put(year, yearData);
                }
                datasetSysProdRenewableSummary.get(year).add(MWhRenewable);

                if (!datasetSysProdFossilSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSysProdFossilSummary.put(year, yearData);
                }
                datasetSysProdFossilSummary.get(year).add(MWhFossil);

                if (!datasetSysGenProdSummary.containsKey(year)) {
                    ArrayList<ArrayList<Double>> yearData = new ArrayList<>();
                    datasetSysGenProdSummary.put(year, yearData);
                }
                datasetSysGenProdSummary.get(year).add(sysGenProd);

                if (!datasetPrimaryunmetDemandMwhSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandMwhSummary .put(year, yearData);
                }
                datasetPrimaryunmetDemandMwhSummary .get(year).add(primaryunmetDemandMwh);

                if (!datasetPrimaryunmetDemandHoursSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandHoursSummary .put(year, yearData);
                }
                datasetPrimaryunmetDemandHoursSummary .get(year).add(primaryunmetDemandHours);

                if (!datasetPrimaryunmetDemandDaysSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandDaysSummary .put(year, yearData);
                }
                datasetPrimaryunmetDemandDaysSummary .get(year).add(primaryunmetDemandDays);

                if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimarymaxUnmetDemandMwhPerDaySummary .put(year, yearData);
                }
                datasetPrimarymaxUnmetDemandMwhPerDaySummary .get(year).add(primarymaxUnmetDemandMwhPerDay);

                if (!datasetSecondaryunmetDemandMwhSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandMwhSummary .put(year, yearData);
                }
                datasetSecondaryunmetDemandMwhSummary .get(year).add(secondaryunmetDemandMwh);

                if (!datasetSecondaryunmetDemandHoursSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandHoursSummary .put(year, yearData);
                }
                datasetSecondaryunmetDemandHoursSummary .get(year).add(secondaryunmetDemandHours);

                if (!datasetSecondaryunmetDemandDaysSummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandDaysSummary .put(year, yearData);
                }
                datasetSecondaryunmetDemandDaysSummary .get(year).add(secondaryunmetDemandDays);

                if (!datasetSecondarymaxUnmetDemandMwhPerDaySummary .containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondarymaxUnmetDemandMwhPerDaySummary .put(year, yearData);
                }
                datasetSecondarymaxUnmetDemandMwhPerDaySummary .get(year).add(secondarymaxUnmetDemandMwhPerDay);

                c.add(Calendar.MONTH, 1);
            }


            //Save Year summary to csv file
            Object[] years = datasetGHGsummary.keySet().toArray();
            Arrays.sort(years, Collections.reverseOrder());
            for (int i = years.length - 1; i >= 0; i--) {
                String year = (String) years[i];
                ArrayList<Double> yearDataGHG = datasetGHGsummary.get(year);
                ArrayList<Double> yearDataPrice = datasetPriceSummary.get(year);
                ArrayList<Double> yearDataWholesale = datasetWholesaleSummary.get(year);
                ArrayList<Double> yearDataKWh = datasetKWhSummary.get(year);
                ArrayList<Double> yearDataConsumers = datasetConsumersSummary.get(year);
                ArrayList<Double> yearDataNumActors = datasetNumActorsSummary.get(year);

                ArrayList<Double> spPrimarySpotAgg = datasetSysProdPrimarySpotSummary.get(year);
                ArrayList<Double> spSecondarySpotAgg = datasetSysProdSecondarySpotSummary.get(year);
                ArrayList<Double> spOffSpotAgg = datasetSysProdOffSpotSummary.get(year);
                ArrayList<Double> spRooftopAgg = datasetSysProdRooftopSummary.get(year);
                ArrayList<Double> spCoalAgg = datasetSysProdCoalSummary.get(year);
                ArrayList<Double> spWaterAgg = datasetSysProdWaterSummary.get(year);
                ArrayList<Double> spWindAgg = datasetSysProdWindSummary.get(year);
                ArrayList<Double> spGasAgg = datasetSysProdGasSummary.get(year);
                ArrayList<Double> spSolAgg = datasetSysProdSolSummary.get(year);
                ArrayList<Double> spBatAgg = datasetSysProdBatSummary.get(year);
                ArrayList<Double> spRenewableAgg = datasetSysProdRenewableSummary.get(year);
                ArrayList<Double> spFossilAgg = datasetSysProdFossilSummary.get(year);
                ArrayList<ArrayList<Double>> spGenProdAgg = datasetSysGenProdSummary.get(year);

                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary .get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary .get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary .get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandMwh = datasetSecondaryunmetDemandMwhSummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandHours = datasetSecondaryunmetDemandHoursSummary .get(year);
                ArrayList<Double> yearSecondaryunmetDemandDays = datasetSecondaryunmetDemandDaysSummary .get(year);
                ArrayList<Double> yearSecondarymaxUnmetDemandMwhPerDay = datasetSecondarymaxUnmetDemandMwhPerDaySummary .get(year);


                Double totalGHG = 0.0;
                Double avgPrice = 0.0;
                Double avgWholesale = 0.0;
                Double totalKWh = 0.0;
                Double maxDwellings = 0.0;
                Double maxNumActors = 0.0;
                Double spprimspotagg = 0.0;
                Double spsecondspotagg = 0.0;
                Double spoffspotagg = 0.0;
                Double sproofagg = 0.0;
                Double spcoalagg = 0.0;
                Double spwateragg = 0.0;
                Double spwindagg = 0.0;
                Double spgasagg = 0.0;
                Double spsolagg = 0.0;
                Double spbatagg = 0.0;
                Double sprenewableagg = 0.0;
                Double spfossilagg = 0.0;
                ArrayList<Double> spgenprodagg = null;

                Double primaryunmetDemandMwh = 0.0;
                Double primaryunmetDemandHours = 0.0;
                Double primaryunmetDemandDays = 0.0;
                Double primarymaxUnmetDemandMwhPerDay = 0.0;
                Double secondaryunmetDemandMwh = 0.0;
                Double secondaryunmetDemandHours = 0.0;
                Double secondaryunmetDemandDays = 0.0;
                Double secondarymaxUnmetDemandMwhPerDay = 0.0;

                Double sizeData = (double) yearDataGHG.size();

                for (Double d : yearPrimaryunmetDemandMwh) {
                    primaryunmetDemandMwh += d;
                }
                for (Double d : yearPrimaryunmetDemandHours) {
                    primaryunmetDemandHours += d;
                }
                for (Double d : yearPrimaryunmetDemandDays) {
                    primaryunmetDemandDays += d;
                }
                primarymaxUnmetDemandMwhPerDay =  Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);
                for (Double d : yearSecondaryunmetDemandMwh) {
                    secondaryunmetDemandMwh += d;
                }
                for (Double d : yearSecondaryunmetDemandHours) {
                    secondaryunmetDemandHours += d;
                }
                for (Double d : yearSecondaryunmetDemandDays) {
                    secondaryunmetDemandDays += d;
                }
                secondarymaxUnmetDemandMwhPerDay =  Collections.max(yearSecondarymaxUnmetDemandMwhPerDay);

                for (Double ghg : yearDataGHG) {
                    totalGHG += ghg;
                }

                for (Double p : yearDataPrice) {
                    avgPrice += p;
                }
                avgPrice /= (double) yearDataPrice.size();

                for (Double w : yearDataWholesale) {
                    avgWholesale += w;
                }
                avgWholesale /= (double) yearDataWholesale.size();

                for (Double k : yearDataKWh) {
                    totalKWh += k;
                }

                for (Double con : yearDataConsumers) {
                    if (con > maxDwellings) maxDwellings = con;
                }

                for (Double con : yearDataNumActors) {
                    if (con > maxNumActors) maxNumActors = con;
                }

                for (Double k : spPrimarySpotAgg) {
                    spprimspotagg += k;
                }

                for (Double k : spSecondarySpotAgg) {
                    spsecondspotagg += k;
                }

                for (Double k : spOffSpotAgg) {
                    spoffspotagg += k;
                }

                for (Double k : spRooftopAgg) {
                    sproofagg += k;
                }

                for (Double k : spCoalAgg) {
                    spcoalagg += k;
                }

                for (Double k : spWaterAgg) {
                    spwateragg += k;
                }

                for (Double k : spWindAgg) {
                    spwindagg += k;
                }

                for (Double k : spGasAgg) {
                    spgasagg += k;
                }

                for (Double k : spSolAgg) {
                    spsolagg += k;
                }

                for (Double k : spBatAgg) {
                    spbatagg += k;
                }

                for (Double k : spRenewableAgg) {
                    sprenewableagg += k;
                }

                for (Double k : spFossilAgg) {
                    spfossilagg += k;
                }

                Double percentageRenewable = sprenewableagg / (sprenewableagg + spfossilagg);
                if(sprenewableagg + spfossilagg <= 0.00000001)
                    percentageRenewable = 0.0;

                for( ArrayList<Double> spg : spGenProdAgg) {
                    if( spgenprodagg == null)
                        spgenprodagg = (ArrayList<Double>) spg.clone();
                    else{
                        for( int j = 0; j < spgenprodagg.size(); j++){
                            spgenprodagg.set(j,  spgenprodagg.get(j) + spg.get(j));
                        }
                    }
                }

                ArrayList<String> record = new ArrayList<String>(Arrays.asList( year, Double.toString(totalKWh), Double.toString(avgPrice), Double.toString(avgWholesale), Double.toString(totalGHG),
                        Double.toString(maxDwellings), Double.toString(percentageRenewable) ,Double.toString(spprimspotagg), Double.toString(spsecondspotagg), Double.toString(spoffspotagg), Double.toString(sproofagg),
                        Double.toString(spcoalagg), Double.toString(spwateragg), Double.toString(spwindagg), Double.toString(spgasagg), Double.toString(spsolagg),
                        Double.toString(spbatagg), Double.toString(maxNumActors), Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours), Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours), Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay) ));

                for( Double spg : spgenprodagg){
                    record.add(Double.toString(spg));
                }

                String[] rec = record.toArray(new String[0]);

                csvWriterYear.writeNext(rec);
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }


    }
}
