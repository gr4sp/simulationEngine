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

    public ArrayList<XYSeries> consumptionActorSeries;
    public ArrayList<XYSeries> tariffUsageConsumptionActorSeries;
    public ArrayList<XYSeries> wholesaleSeries;
    public ArrayList<XYSeries> ghgConsumptionActorSeries;
    public ArrayList<XYSeries> ghgConsumptionSpmSeries;
    public ArrayList<XYSeries> numDomesticConsumersSeries;
    public XYSeries numActorsSeries;

    HashMap<Integer, XYSeries> unmetDemandMwhSeries;
    HashMap<Integer, XYSeries> unmetDemandHoursSeries;
    HashMap<Integer, XYSeries> unmetDemandDaysSeries;
    HashMap<Integer, XYSeries> maxUnmetDemandMwhPerHourSeries;

    HashMap<Integer, XYSeries> genCapacityFactorSeries;
    HashMap<Integer, XYSeries> systemProductionSeries;
    public HashMap<Integer, XYSeries> PriceGenAvgSeries;
    HashMap<Integer, XYSeries> PriceGenMaxSeries;
    HashMap<Integer, XYSeries> PriceGenMinSeries;

    public ArrayList<Double> dappGHG;
    public ArrayList<Double> dappRenew;
    public ArrayList<Double> dappTariff;
    public ArrayList<Double> dappWholesale;
    public ArrayList<Double> dappUnmetSum;
    public ArrayList<Double> dappUnmetHours;
    public ArrayList<Double> dappUnmetDays;
    public ArrayList<Double> dappUnmetMax;

    public ArrayList<Double> dappGHGDAPP;
    public ArrayList<Double> dappRenewDAPP;
    public ArrayList<Double> dappTariffDAPP;
    public ArrayList<Double> dappWholesaleDAPP;
    public ArrayList<Double> dappUnmetSumDAPP;
    public ArrayList<Double> dappUnmetHoursDAPP;
    public ArrayList<Double> dappUnmetDaysDAPP;
    public ArrayList<Double> dappUnmetMaxDAPP;

    public SaveData(SimState simState) {
        consumptionActorSeries = new ArrayList<XYSeries>();
        tariffUsageConsumptionActorSeries = new ArrayList<XYSeries>();
        wholesaleSeries = new ArrayList<XYSeries>();
        ghgConsumptionActorSeries = new ArrayList<XYSeries>();
        ghgConsumptionSpmSeries = new ArrayList<XYSeries>();
        numDomesticConsumersSeries = new ArrayList<XYSeries>();

        unmetDemandMwhSeries = new HashMap<>();
        unmetDemandHoursSeries = new HashMap<>();
        unmetDemandDaysSeries = new HashMap<>();
        maxUnmetDemandMwhPerHourSeries = new HashMap<>();

        genCapacityFactorSeries = new HashMap<>();
        systemProductionSeries = new HashMap<>();
        PriceGenAvgSeries = new HashMap<>();
        PriceGenMaxSeries = new HashMap<>();
        PriceGenMinSeries = new HashMap<>();

        dappGHG = new ArrayList<>();
        dappRenew = new ArrayList<>();
        dappTariff = new ArrayList<>();
        dappWholesale = new ArrayList<>();
        dappUnmetSum = new ArrayList<>();
        dappUnmetHours = new ArrayList<>();
        dappUnmetDays = new ArrayList<>();
        dappUnmetMax = new ArrayList<>();

        dappGHGDAPP = new ArrayList<>();
        dappRenewDAPP = new ArrayList<>();
        dappTariffDAPP = new ArrayList<>();
        dappWholesaleDAPP = new ArrayList<>();
        dappUnmetSumDAPP = new ArrayList<>();
        dappUnmetHoursDAPP = new ArrayList<>();
        dappUnmetDaysDAPP = new ArrayList<>();
        dappUnmetMaxDAPP = new ArrayList<>();

    }

    private void addNewPlottingSeries(SimState simState, int startId, int numNewSeries) {
        Gr4spSim data = (Gr4spSim) simState;

        int id = startId;

        for (int i = 0; i < numNewSeries; i++) {
            EndUserUnit h = (EndUserUnit) data.consumptionActors.get(id + i);

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
            numDomesticConsumersSeries.add(seriesDomesticConsumers);

            id++;

        }

    }

    public void plotSeries(SimState simState) {

        Gr4spSim data = (Gr4spSim) simState;

        XYSeries seriesConsumption = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);

        consumptionActorSeries.add(seriesConsumption);

        XYSeries seriesWholesale = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        wholesaleSeries.add(seriesWholesale);

        XYSeries seriesTariff = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        tariffUsageConsumptionActorSeries.add(seriesTariff);

        XYSeries seriesGHG = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);
        ghgConsumptionActorSeries.add(seriesGHG);

        numActorsSeries = new org.jfree.data.xy.XYSeries(
                "Number of Actors",
                false);

        XYSeries seriesDomesticConsumers = new org.jfree.data.xy.XYSeries(
                "AllConsumptionUnits",
                false);

        numDomesticConsumersSeries.add(seriesDomesticConsumers);

        for (int i = 0; i < data.consumptionActors.size(); i++) {
            EndUserUnit h = (EndUserUnit) data.consumptionActors.get(i);

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

            numDomesticConsumersSeries.add(seriesDomesticConsumersD);

        }

        for (Integer integer : data.spm_register.keySet()) {
            Vector<Spm> spms = data.spm_register.get(integer);
            for (int i = 0; i < spms.size(); i++) {
                XYSeries seriesGHGs = new org.jfree.data.xy.XYSeries(
                        spms.elementAt(i).getId(),
                        false);
                ghgConsumptionSpmSeries.add(seriesGHGs);

            }
        }

        /**
         * Add series to hashmap of series of aggregated
         * 0 PrimarySpot
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
        if (data.settings.existsMarket("primary") || data.settingsAfterBaseYear.existsMarket("primary")) {
            XYSeries seriesSystemProductionIn = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            systemProductionSeries.put(0, seriesSystemProductionIn);

            XYSeries unmetDemandMwh = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandMwhSeries.put(0, unmetDemandMwh);

            XYSeries unmetDemandHours = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandHoursSeries.put(0, unmetDemandHours);

            XYSeries unmetDemandDays = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            unmetDemandDaysSeries.put(0, unmetDemandDays);

            XYSeries maxUnmetDemandMwhPerDay = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            maxUnmetDemandMwhPerHourSeries.put(0, maxUnmetDemandMwhPerDay);

        }

        if (true || data.settings.existsMarket("secondary") || data.settingsAfterBaseYear.existsMarket("secondary")) {
            XYSeries seriesSystemProductionSec = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            systemProductionSeries.put(-1, seriesSystemProductionSec);

            XYSeries unmetDemandMwh = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandMwhSeries.put(-1, unmetDemandMwh);

            XYSeries unmetDemandHours = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandHoursSeries.put(-1, unmetDemandHours);

            XYSeries unmetDemandDays = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            unmetDemandDaysSeries.put(-1, unmetDemandDays);

            XYSeries maxUnmetDemandMwhPerDay = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            maxUnmetDemandMwhPerHourSeries.put(-1, maxUnmetDemandMwhPerDay);

        }
        if (data.settings.existsOffMarket() || data.settingsAfterBaseYear.existsOffMarket()) {

            XYSeries seriesSystemProductionOff = new org.jfree.data.xy.XYSeries(
                    "OffSpot",
                    false);
            systemProductionSeries.put(-2, seriesSystemProductionOff);

        }

        XYSeries seriesSystemProductionRooftopPV = new org.jfree.data.xy.XYSeries(
                "RooftopPV",
                false);
        systemProductionSeries.put(-3, seriesSystemProductionRooftopPV);

        XYSeries seriesSystemProductionCoal = new org.jfree.data.xy.XYSeries(
                "Coal",
                false);
        systemProductionSeries.put(-4, seriesSystemProductionCoal);

        XYSeries seriesSystemProductionWater = new org.jfree.data.xy.XYSeries(
                "Water",
                false);
        systemProductionSeries.put(-5, seriesSystemProductionWater);

        XYSeries seriesSystemProductionWind = new org.jfree.data.xy.XYSeries(
                "Wind",
                false);
        systemProductionSeries.put(-6, seriesSystemProductionWind);

        XYSeries seriesSystemProductionGas = new org.jfree.data.xy.XYSeries(
                "Gas",
                false);
        systemProductionSeries.put(-7, seriesSystemProductionGas);

        XYSeries seriesSystemProductionSolar = new org.jfree.data.xy.XYSeries(
                "Solar",
                false);
        systemProductionSeries.put(-8, seriesSystemProductionSolar);

        XYSeries seriesSystemProductionBattery = new org.jfree.data.xy.XYSeries(
                "Battery",
                false);
        systemProductionSeries.put(-9, seriesSystemProductionBattery);

        /**
         * Price series
         */

        XYSeries seriesPriceAvgTariff = new org.jfree.data.xy.XYSeries(
                "Tariff_WA_AllMarkets",
                false);
        PriceGenAvgSeries.put(1, seriesPriceAvgTariff);

        if (data.settings.existsMarket("primary") || data.settingsAfterBaseYear.existsMarket("primary")) {

            XYSeries seriesPriceAvgIn = new org.jfree.data.xy.XYSeries(
                    "PrimarySpot",
                    false);
            PriceGenAvgSeries.put(0, seriesPriceAvgIn);

        }

        if (true || data.settings.existsMarket("secondary") || data.settingsAfterBaseYear.existsMarket("secondary")) {
            XYSeries seriesPriceAvgIn = new org.jfree.data.xy.XYSeries(
                    "SecondarySpot",
                    false);
            PriceGenAvgSeries.put(-1, seriesPriceAvgIn);

        }

        if (data.settings.existsOffMarket() || data.settingsAfterBaseYear.existsOffMarket()) {
            XYSeries seriesPriceAvgOff = new org.jfree.data.xy.XYSeries(
                    "OffSpot",
                    false);
            PriceGenAvgSeries.put(-2, seriesPriceAvgOff);

        }

        XYSeries seriesPriceAvgRooftopPV = new org.jfree.data.xy.XYSeries(
                "RooftopPV",
                false);
        PriceGenAvgSeries.put(-3, seriesPriceAvgRooftopPV);

        XYSeries seriesPriceAvgCoal = new org.jfree.data.xy.XYSeries(
                "Coal",
                false);
        PriceGenAvgSeries.put(-4, seriesPriceAvgCoal);

        XYSeries seriesPriceAvgWater = new org.jfree.data.xy.XYSeries(
                "Water",
                false);
        PriceGenAvgSeries.put(-5, seriesPriceAvgWater);

        XYSeries seriesPriceAvgWind = new org.jfree.data.xy.XYSeries(
                "Wind",
                false);
        PriceGenAvgSeries.put(-6, seriesPriceAvgWind);

        XYSeries seriesPriceAvgGas = new org.jfree.data.xy.XYSeries(
                "Gas",
                false);
        PriceGenAvgSeries.put(-7, seriesPriceAvgGas);

        XYSeries seriesPriceAvgSolar = new org.jfree.data.xy.XYSeries(
                "Solar",
                false);
        PriceGenAvgSeries.put(-8, seriesPriceAvgSolar);

        XYSeries seriesPriceAvgBattery = new org.jfree.data.xy.XYSeries(
                "Battery",
                false);
        PriceGenAvgSeries.put(-9, seriesPriceAvgBattery);

        for (Integer integer : data.gen_register.keySet()) {
            Vector<Generator> gens = data.gen_register.get(integer);
            for (int i = 0; i < gens.size(); i++) {
                String nameSeries = Integer.toString(gens.elementAt(i).getId())
                        + gens.elementAt(i).getfuelSourceDescriptor().substring(0, 2);

                XYSeries seriesGenCapacityFactors = new org.jfree.data.xy.XYSeries(
                        nameSeries,
                        false);
                genCapacityFactorSeries.put(integer, seriesGenCapacityFactors);

                XYSeries seriesSystemProduction = new org.jfree.data.xy.XYSeries(
                        nameSeries,
                        false);
                systemProductionSeries.put(integer, seriesSystemProduction);

                if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(), "primary",
                        gens.elementAt(i).getMaxCapacity()) == false) {

                    if (data.settingsAfterBaseYear.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                            "primary", gens.elementAt(i).getMaxCapacity()) == true) {

                    }
                } else {

                    if (data.settingsAfterBaseYear.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                            "primary", gens.elementAt(i).getMaxCapacity()) == false) {

                    }

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

        int startIdForNewSeries = consumptionActorSeries.size() - 1;
        int NewNumConsumers = data.consumptionActors.size() - startIdForNewSeries;
        if (NewNumConsumers > 0) {
            addNewPlottingSeries(simState, startIdForNewSeries, NewNumConsumers);

        }

        double x = simState.schedule.getSteps();
        Date currentDate = data.getSimCalendar().getTime();
        float floatDate = data.getSimCalendar().get(Calendar.YEAR);

        int currentYear = (int) floatDate;

        float month = data.getSimCalendar().get(Calendar.MONTH) / (float) 12.0;

        floatDate += month;

        float floatDateTariff = floatDate - ((float) 11.0 / (float) 12.0);

        if (x >= Schedule.EPOCH && x < Schedule.AFTER_SIMULATION) {
            float sumConsumption = 0;
            float averageTariff = 0;
            double sumEmissions = 0;
            int sumDwellings = 0;

            int idSPM = 0;
            for (Integer integer : data.spm_register.keySet()) {
                Vector<Spm> spms = data.spm_register.get(integer);
                for (int i = 0; i < spms.size(); i++) {
                    ghgConsumptionSpmSeries.get(idSPM).add(floatDate, spms.get(i).currentEmissions, false);
                    idSPM++;
                }
            }

            int numActiveActors = 0;
            for (Integer integer : data.actor_register.keySet()) {
                Actor act = data.actor_register.get(integer);

                if (currentDate.after(act.getStart()) && act.getChangeDate().after(currentDate)) {
                    numActiveActors++;
                }
            }
            numActorsSeries.add(floatDate, numActiveActors, false);

            if (data.getStartSpotMarketDate().after(currentDate) == false) {

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

                        if (genCapacityFactorSeries.containsKey(integer) == false) {
                            String nameSeries = Integer.toString(gens.elementAt(i).getId())
                                    + gens.elementAt(i).getfuelSourceDescriptor().substring(0, 2);

                            XYSeries seriesGenCapacityFactors = new org.jfree.data.xy.XYSeries(
                                    nameSeries,
                                    false);
                            genCapacityFactorSeries.put(integer, seriesGenCapacityFactors);

                            XYSeries seriesSystemProduction = new org.jfree.data.xy.XYSeries(
                                    nameSeries,
                                    false);
                            systemProductionSeries.put(integer, seriesSystemProduction);

                            if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                    "primary", gens.elementAt(i).getMaxCapacity()) == false &&
                                    data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                            "secondary", gens.elementAt(i).getMaxCapacity()) == false) {

                                if (data.settingsAfterBaseYear.isMarketPaticipant(
                                        gens.elementAt(i).getDispatchTypeDescriptor(), "primary",
                                        gens.elementAt(i).getMaxCapacity()) ||
                                        data.settingsAfterBaseYear.isMarketPaticipant(
                                                gens.elementAt(i).getDispatchTypeDescriptor(), "secondary",
                                                gens.elementAt(i).getMaxCapacity())) {

                                }
                            } else {

                                if (data.settingsAfterBaseYear.isMarketPaticipant(
                                        gens.elementAt(i).getDispatchTypeDescriptor(), "primary",
                                        gens.elementAt(i).getMaxCapacity()) == false &&
                                        data.settingsAfterBaseYear.isMarketPaticipant(
                                                gens.elementAt(i).getDispatchTypeDescriptor(), "secondary",
                                                gens.elementAt(i).getMaxCapacity()) == false) {

                                }

                            }
                        }

                        genCapacityFactorSeries.get(integer).add(floatDate, gens.get(i).getHistoricCapacityFactor(),
                                false);
                        systemProductionSeries.get(integer).add(floatDate, gens.get(i).getMonthlyGeneratedMWh(), false);

                        if (currentDate.before(data.getBaseYearForecastDate()))
                            if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                    "primary", gens.elementAt(i).getMaxCapacity())) {
                                MWhPrimarySpot += gens.get(i).getMonthlyGeneratedMWh();
                            } else if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                    "secondary", gens.elementAt(i).getMaxCapacity())) {
                                MWhSecondarySpot += gens.get(i).getMonthlyGeneratedMWh();
                            } else {
                                MWhOffSpot += gens.get(i).getMonthlyGeneratedMWh();
                            }
                        else if (data.settingsAfterBaseYear.isMarketPaticipant(
                                gens.elementAt(i).getDispatchTypeDescriptor(), "primary",
                                gens.elementAt(i).getMaxCapacity())) {
                            MWhPrimarySpot += gens.get(i).getMonthlyGeneratedMWh();
                        } else if (data.settingsAfterBaseYear.isMarketPaticipant(
                                gens.elementAt(i).getDispatchTypeDescriptor(), "secondary",
                                gens.elementAt(i).getMaxCapacity())) {
                            MWhSecondarySpot += gens.get(i).getMonthlyGeneratedMWh();
                        } else {
                            MWhOffSpot += gens.get(i).getMonthlyGeneratedMWh();
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Solar - Rooftop")) {
                            MWhRooftopPV += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgRooftopPV += gens.get(i).priceMWhLCOE(currentYear);
                            numRooftopPV++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Brown Coal")) {
                            MWhCoal += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgCoal += gens.get(i).priceMWhLCOE(currentYear);
                            numCoal++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Water")) {
                            MWhWater += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgWater += gens.get(i).priceMWhLCOE(currentYear);
                            numWater++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Wind")) {
                            MWhWind += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgWind += gens.get(i).priceMWhLCOE(currentYear);
                            numWind++;
                        }

                        if (gens.elementAt(i).getfuelSourceDescriptor().equals("Gas Pipeline")) {
                            MWhGas += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgGas += gens.get(i).priceMWhLCOE(currentYear);
                            numGas++;
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Solar PV - Fixed")) {
                            MWhSolar += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgSolar += gens.get(i).priceMWhLCOE(currentYear);
                            numSolar++;
                        }

                        if (gens.elementAt(i).getTechTypeDescriptor().equals("Battery")) {
                            MWhBattery += gens.get(i).getMonthlyGeneratedMWh();
                            PriceAvgBattery += gens.get(i).priceMWhLCOE(currentYear);
                            numBattery++;
                        }

                    }
                }

                /**
                 * Add series to hashmap of series of aggregated
                 * 1 CombinedTariff (only in PriceGenAvgSeries)
                 * 0 PrimarySpot
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

                if (true || data.settings.existsMarket("secondary")
                        || data.settingsAfterBaseYear.existsMarket("secondary"))
                    systemProductionSeries.get(-1).add(floatDate, MWhSecondarySpot, false);
                if (data.settings.existsOffMarket() || data.settingsAfterBaseYear.existsOffMarket())
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

            } else {
                Double totalgwh_historic = data.getTotal_monthly_consumption_register().get(currentDate) / 1000.0;
                Double totalWater_historic = data.getMonthly_renewable_historic_register().get(currentDate);
                systemProductionSeries.get(-4).add(floatDate, totalgwh_historic - totalWater_historic, false);
                systemProductionSeries.get(-5).add(floatDate, totalWater_historic, false);
            }

            for (int i = 0; i < data.consumptionActors.size(); i++) {

                int id = i + 1;

                EndUserUnit c = (EndUserUnit) data.consumptionActors.get(i);
                consumptionActorSeries.get(id).add(floatDate, data.consumptionActors.get(i).getCurrentConsumption(),
                        false);
                tariffUsageConsumptionActorSeries.get(id).add(floatDateTariff,
                        data.consumptionActors.get(i).getCurrentTariff(), false);
                ghgConsumptionActorSeries.get(id).add(floatDate, data.consumptionActors.get(i).getCurrentEmissions(),
                        false);
                numDomesticConsumersSeries.get(id).add(floatDate, c.getNumberOfHouseholds(), false);

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

                    if (data.getStartSpotMarketDate().after(currentDate)) {
                        wholesaleSeries.get(0).add(floatDate, 0, false);
                    } else {

                        wholesaleSeries.get(0).add(floatDate, (float) a.getAvgMonthlyPricePrimarySpot(), false);

                        PriceGenAvgSeries.get(1).add(floatDate, (float) a.getWholesalePrice(data), false);

                        PriceGenAvgSeries.get(0).add(floatDate, a.getAvgMonthlyPricePrimarySpot(), false);

                        unmetDemandDaysSeries.get(0).add(floatDate, a.getUnmetDemandDays(), false);
                        ;
                        unmetDemandHoursSeries.get(0).add(floatDate, a.getUnmetDemandHours(), false);
                        ;
                        unmetDemandMwhSeries.get(0).add(floatDate, a.getUnmetDemandMwhPrimary(), false);
                        ;
                        maxUnmetDemandMwhPerHourSeries.get(0).add(floatDate, a.getMaxUnmetDemandMwhPerHourPrimary(),
                                false);
                        ;

                        if (true || data.settings.existsMarket("secondary")
                                || data.settingsAfterBaseYear.existsMarket("secondary")) {
                            PriceGenAvgSeries.get(-1).add(floatDate, a.getAvgMonthlyPriceSecondarySpot(), false);

                            unmetDemandDaysSeries.get(-1).add(floatDate, a.getUnmetDemandDaysSecondary(), false);
                            unmetDemandHoursSeries.get(-1).add(floatDate, a.getUnmetDemandHoursSecondary(), false);
                            unmetDemandMwhSeries.get(-1).add(floatDate, a.getUnmetDemandMwhSecondary(), false);
                            maxUnmetDemandMwhPerHourSeries.get(-1).add(floatDate,
                                    a.getMaxUnmetDemandMwhPerHourSecondary(), false);

                        }
                        if (data.settings.existsOffMarket() || data.settingsAfterBaseYear.existsOffMarket())
                            PriceGenAvgSeries.get(-2).add(floatDate, a.getAvgMonthlyPriceOffSpot(), false);

                    }
                }
            }

        }

        data.advanceCurrentSimDate();

        data.generateHouseholds();

        if (data.getCurrentSimDate().after(data.getEndSimDate())
                || data.getCurrentSimDate().equals(data.getEndSimDate())) {

            if (data.settings.reportGeneration.equals("full")) {
                saveData(simState);

            } else {
                if (!data.saveDAPP) {
                    saveDataLight(simState);

                } else {
                    saveDataDAPP(simState);
                    saveDataLight(simState);

                }

            }

            data.finish();
        }

    }

    public void saveData(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        String slash = "\\";
        if (System.getProperty("os.name").contains("Windows") == false)
            slash = "/";

        String folderName = data.settings.folderOutput + "" + slash + "csv" + slash + "" + data.yamlFileName;
        File directory = new File(folderName);
        if (!directory.exists()) {
            directory.mkdir();

        }

        try (

                Writer writerMonthly = Files.newBufferedWriter(Paths
                        .get(folderName + "/" + data.yamlFileName + "SimDataMonthlySummary" + data.outputID + ".csv"));

                CSVWriter csvWriterMonthly = new CSVWriter(writerMonthly,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerYear = Files.newBufferedWriter(Paths
                        .get(folderName + "/" + data.yamlFileName + "SimDataYearSummary" + data.outputID + ".csv"));

                CSVWriter csvWriterYear = new CSVWriter(writerYear,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writer = Files.newBufferedWriter(
                        Paths.get(folderName + "/" + data.yamlFileName + "SimData" + data.outputID + ".csv"));

                CSVWriter csvWriter = new CSVWriter(writer,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerGensCapFactorInSpot = Files.newBufferedWriter(Paths.get(folderName + "/"
                        + data.yamlFileName + "SimDataMonthlyGensCapFactorInSpot" + data.outputID + ".csv"));

                CSVWriter csvWriterGensCapFactorInSpot = new CSVWriter(writerGensCapFactorInSpot,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerGensCapFactorOffSpot = Files.newBufferedWriter(Paths.get(folderName + "/"
                        + data.yamlFileName + "SimDataMonthlyGensCapFactorOFFspot" + data.outputID + ".csv"));

                CSVWriter csvWriterGensCapFactorOffSpot = new CSVWriter(writerGensCapFactorOffSpot,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerSystemProductionInSpot = Files.newBufferedWriter(Paths.get(folderName + "/"
                        + data.yamlFileName + "SimDataMonthlySystemProductionInSpot" + data.outputID + ".csv"));

                CSVWriter csvWriterSystemProductionInSpot = new CSVWriter(writerSystemProductionInSpot,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerSystemProductionOffSpot = Files.newBufferedWriter(Paths.get(folderName + "/"
                        + data.yamlFileName + "SimDataMonthlySystemProductionOFFspot" + data.outputID + ".csv"));

                CSVWriter csvWriterSystemProductionOffSpot = new CSVWriter(writerSystemProductionOffSpot,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

        ) {

            String[] headerRecord = { "ConsumerUnit", "Time (month)", "Consumption (MWh)", "Tariff (c/KWh)",
                    "Primary Wholesale ($/MWh)", "GHG Emissions (tCO2-e)",
                    "Number of Domestic Consumers (households)" };
            csvWriter.writeNext(headerRecord);

            String[] headerRecordYear = { "Time (Year)", "Consumption (KWh) per household",
                    "Avg Tariff (c/KWh) per household", "Primary Wholesale ($/MWh)",
                    "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "Number of Active Actors",
                    "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)",
                    "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)",
                    "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)" };

            csvWriterYear.writeNext(headerRecordYear);

            String[] headerRecordMonthly = { "Time (Month)", "Consumption (KWh) per household",
                    "Avg Tariff (c/KWh) per household", "Primary Wholesale ($/MWh)",
                    "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "System Production Primary Spot", "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "Number of Active Actors",
                    "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)",
                    "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)",
                    "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)" };

            csvWriterMonthly.writeNext(headerRecordMonthly);

            ArrayList<String> headerGenrecordInSpot = new ArrayList<String>();
            ArrayList<String> headerGenrecordOffSpot = new ArrayList<String>();
            headerGenrecordInSpot.add("Date");
            headerGenrecordOffSpot.add("Date");
            TreeMap<Integer, Vector<Generator>> treeMap = new TreeMap<>(data.gen_register);

            for (Integer integer : treeMap.keySet()) {

                Vector<Generator> gens = data.gen_register.get(integer);
                for (int i = 0; i < gens.size(); i++) {
                    String name = gens.elementAt(i).getId() + " - " + gens.elementAt(i).getfuelSourceDescriptor()
                            + " - "
                            + gens.elementAt(i).getName() + " - " + gens.elementAt(i).getMaxCapacity() + " - "
                            + gens.elementAt(i).getDispatchTypeDescriptor() + " - " + gens.elementAt(i).getStart();
                    name = name.replace(",", "-");

                    if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(), "primary",
                            gens.elementAt(i).getMaxCapacity()) == false) {
                        headerGenrecordOffSpot.add(name);
                        if (data.settingsAfterBaseYear.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                "primary", gens.elementAt(i).getMaxCapacity()))
                            headerGenrecordInSpot.add(name);
                    } else {
                        headerGenrecordInSpot.add(name);
                        if (data.settingsAfterBaseYear.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                "primary", gens.elementAt(i).getMaxCapacity()) == false)
                            headerGenrecordOffSpot.add(name);
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
                XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(0);
                XYSeries secondarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(-1);

                Calendar c = Calendar.getInstance();

                if (i == 0)
                    c.setTime(data.getStartSimDate());
                else {
                    EndUserUnit consumer = (EndUserUnit) data.consumptionActors.get(i - 1);
                    c.setTime(consumer.creationDate);
                }

                for (int t = 0; t < cseries.getItems().size(); t++) {

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
                        if ((t - shifttimeseries) < spprimaggseries.getItems().size())
                            spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);
                        if (spsecondaggseries != null)
                            if ((t - shifttimeseries) < spsecondaggseries.getItems().size())
                                spsecondaggitem = (XYDataItem) spsecondaggseries.getItems().get(t - shifttimeseries);
                        if (sposaggseries != null)
                            if ((t - shifttimeseries) < sposaggseries.getItems().size())
                                sposaggitem = (XYDataItem) sposaggseries.getItems().get(t - shifttimeseries);
                        sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);

                        primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems()
                                .get(t - shifttimeseries);
                        primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems()
                                .get(t - shifttimeseries);
                        primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems()
                                .get(t - shifttimeseries);
                        primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries
                                .getItems().get(t - shifttimeseries);
                        if (secondaryunmetDemandMwhSeries != null) {
                            if ((t - shifttimeseries) < secondaryunmetDemandMwhSeries.getItems().size()) {

                                secondaryunmetDemandMwhitem = (XYDataItem) secondaryunmetDemandMwhSeries.getItems()
                                        .get(t - shifttimeseries);
                                secondaryunmetDemandHoursitem = (XYDataItem) secondaryunmetDemandHoursSeries.getItems()
                                        .get(t - shifttimeseries);
                                secondaryunmetDemandDaysitem = (XYDataItem) secondaryunmetDemandDaysSeries.getItems()
                                        .get(t - shifttimeseries);
                                secondarymaxUnmetDemandMwhPerDayitem = (XYDataItem) secondarymaxUnmetDemandMwhPerDaySeries
                                        .getItems().get(t - shifttimeseries);
                            }
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
                        if (spsecondaggitem != null)
                            MWhSecondarySpotAgg = spsecondaggitem.getYValue();
                        if (sposaggitem != null)
                            MWhOffSpotAgg = sposaggitem.getYValue();
                        MWhRoofSpotAgg = sproofaggitem.getYValue();
                        primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                        primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                        primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                        primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();
                        if (secondaryunmetDemandMwhitem != null) {
                            secondaryunmetDemandMwh = secondaryunmetDemandMwhitem.getYValue();
                            secondaryunmetDemandHours = secondaryunmetDemandHoursitem.getYValue();
                            secondaryunmetDemandDays = secondaryunmetDemandDaysitem.getYValue();
                            secondarymaxUnmetDemandMwhPerDay = secondarymaxUnmetDemandMwhPerDayitem.getYValue();
                        }

                    }

                    SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                    if (i == 0) {
                        String[] record = { dateToString.format(c.getTime()), Double.toString(kwh / consumers),
                                Double.toString(tariffPrice), Double.toString(wholesale),
                                Double.toString(emissions / consumers),
                                Double.toString(consumers), Double.toString(MWhPrimarySpotAgg),
                                Double.toString(MWhSecondarySpotAgg), Double.toString(MWhOffSpotAgg),
                                Double.toString(MWhRoofSpotAgg), Double.toString(numActors),
                                Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours),
                                Double.toString(primaryunmetDemandDays),
                                Double.toString(primarymaxUnmetDemandMwhPerDay),
                                Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours),
                                Double.toString(secondaryunmetDemandDays),
                                Double.toString(secondarymaxUnmetDemandMwhPerDay) };
                        csvWriterMonthly.writeNext(record);

                        ArrayList<String> genrecordInSpot = new ArrayList<String>();
                        genrecordInSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> genrecordOffSpot = new ArrayList<String>();
                        genrecordOffSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> sysprodInSpot = new ArrayList<String>();
                        sysprodInSpot.add(dateToString.format(c.getTime()));

                        ArrayList<String> sysprodOffSpot = new ArrayList<String>();
                        sysprodOffSpot.add(dateToString.format(c.getTime()));

                        for (Integer integer : treeMap.keySet()) {
                            XYSeries genseries = genCapacityFactorSeries.get(integer);
                            XYSeries sysprodseries = systemProductionSeries.get(integer);

                            int shift = cseries.getItemCount() - genseries.getItemCount();
                            Vector<Generator> gens = data.gen_register.get(integer);

                            if (shift > t) {

                                if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                        "primary", gens.elementAt(i).getMaxCapacity()) == false) {
                                    genrecordOffSpot.add("-");
                                    sysprodOffSpot.add("-");
                                } else {
                                    genrecordInSpot.add("-");
                                    sysprodInSpot.add("-");
                                }
                            } else {

                                XYDataItem genitem = (XYDataItem) genseries.getItems().get(t - shift);
                                XYDataItem spitem = (XYDataItem) sysprodseries.getItems().get(t - shift);

                                if (data.settings.isMarketPaticipant(gens.elementAt(i).getDispatchTypeDescriptor(),
                                        "primary", gens.elementAt(i).getMaxCapacity()) == false) {
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
                        String[] record = { Integer.toString(i), dateToString.format(c.getTime()), Double.toString(kwh),
                                Double.toString(tariffPrice), Double.toString(wholesale), Double.toString(emissions),
                                Double.toString(consumers), Double.toString(numActors) };
                        csvWriter.writeNext(record);
                    }

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

                        if (!datasetPrimaryunmetDemandMwhSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandMwhSummary.put(year, yearData);
                        }
                        datasetPrimaryunmetDemandMwhSummary.get(year).add(primaryunmetDemandMwh);

                        if (!datasetPrimaryunmetDemandHoursSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandHoursSummary.put(year, yearData);
                        }
                        datasetPrimaryunmetDemandHoursSummary.get(year).add(primaryunmetDemandHours);

                        if (!datasetPrimaryunmetDemandDaysSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimaryunmetDemandDaysSummary.put(year, yearData);
                        }
                        datasetPrimaryunmetDemandDaysSummary.get(year).add(primaryunmetDemandDays);

                        if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetPrimarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                        }
                        datasetPrimarymaxUnmetDemandMwhPerDaySummary.get(year).add(primarymaxUnmetDemandMwhPerDay);

                        if (!datasetSecondaryunmetDemandMwhSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandMwhSummary.put(year, yearData);
                        }
                        datasetSecondaryunmetDemandMwhSummary.get(year).add(secondaryunmetDemandMwh);

                        if (!datasetSecondaryunmetDemandHoursSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandHoursSummary.put(year, yearData);
                        }
                        datasetSecondaryunmetDemandHoursSummary.get(year).add(secondaryunmetDemandHours);

                        if (!datasetSecondaryunmetDemandDaysSummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondaryunmetDemandDaysSummary.put(year, yearData);
                        }
                        datasetSecondaryunmetDemandDaysSummary.get(year).add(secondaryunmetDemandDays);

                        if (!datasetSecondarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                            ArrayList<Double> yearData = new ArrayList<>();
                            datasetSecondarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                        }
                        datasetSecondarymaxUnmetDemandMwhPerDaySummary.get(year).add(secondarymaxUnmetDemandMwhPerDay);

                    }

                    c.add(Calendar.MONTH, 1);
                }
            }

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
                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary
                        .get(year);
                ArrayList<Double> yearSecondaryunmetDemandMwh = datasetSecondaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandHours = datasetSecondaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandDays = datasetSecondaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearSecondarymaxUnmetDemandMwhPerDay = datasetSecondarymaxUnmetDemandMwhPerDaySummary
                        .get(year);
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
                primarymaxUnmetDemandMwhPerDay = Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);
                for (Double d : yearSecondaryunmetDemandMwh) {
                    secondaryunmetDemandMwh += d;
                }
                for (Double d : yearSecondaryunmetDemandHours) {
                    secondaryunmetDemandHours += d;
                }
                for (Double d : yearSecondaryunmetDemandDays) {
                    secondaryunmetDemandDays += d;
                }
                secondarymaxUnmetDemandMwhPerDay = Collections.max(yearSecondarymaxUnmetDemandMwhPerDay);

                for (Double ghg : yearDataGHG) {
                    totalGHG += ghg;
                }

                for (Double con : yearDataNumActors) {
                    if (con > maxNumActors)
                        maxNumActors = con;
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

                dwellingsLastMonth = yearDataConsumers.get(0);

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

                String[] record = { year, Double.toString(totalKWh), Double.toString(avgPrice),
                        Double.toString(avgWholesale), Double.toString(totalGHG),
                        Double.toString(dwellingsLastMonth), Double.toString(spprimspotagg),
                        Double.toString(spsecondspotagg), Double.toString(spoffspotagg), Double.toString(sproofagg),
                        Double.toString(maxNumActors),
                        Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours),
                        Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours),
                        Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay) };
                csvWriterYear.writeNext(record);
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }

    }

    public void saveDataLight(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        String slash = "\\";
        if (System.getProperty("os.name").contains("Windows") == false)
            slash = "/";

        String folderName = "/home/ray/ray_results/TempResults" + slash + "csv" + slash + "" + data.yamlFileName;
        File directory = new File(folderName);
        if (!directory.exists()) {
            directory.mkdir();

        }

        try (

                Writer writerMonthly = Files.newBufferedWriter(Paths
                        .get(folderName + "/" + data.yamlFileName + "SimDataMonthlySummary" + data.outputID + ".csv"));

                CSVWriter csvWriterMonthly = new CSVWriter(writerMonthly,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

                Writer writerYear = Files.newBufferedWriter(Paths
                        .get(folderName + "/" + data.yamlFileName + "SimDataYearSummary" + data.outputID + ".csv"));

                CSVWriter csvWriterYear = new CSVWriter(writerYear,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);

        ) {
            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");

            ArrayList<String> headerYear = new ArrayList<String>(Arrays.asList("Time (Year)",
                    "Consumption (KWh) per household", "Avg Tariff (c/KWh) per household", "Primary Wholesale ($/MWh)",
                    "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "Percentage Renewable Production", "System Production Primary Spot",
                    "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "System Production Coal", "System Production Water",
                    "System Production Wind", "System Production Gas",
                    "System Production Solar", "System Production Battery", "Number of Active Actors",
                    "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)",
                    "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)",
                    "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)"));
            TreeMap<Integer, Vector<Generator>> treeMap = new TreeMap<>(data.gen_register);

            for (Integer integer : treeMap.keySet()) {

                Vector<Generator> gens = data.gen_register.get(integer);
                for (int i = 0; i < gens.size(); i++) {
                    String name = gens.elementAt(i).getId() + " - " + gens.elementAt(i).getfuelSourceDescriptor()
                            + " - "
                            + gens.elementAt(i).getName() + " - " + gens.elementAt(i).getMaxCapacity() + " - "
                            + gens.elementAt(i).getDispatchTypeDescriptor() + " - "
                            + dateToYear.format(gens.elementAt(i).getStart()) + " - "
                            + dateToYear.format(gens.elementAt(i).getEnd());
                    name = name.replace(",", "-");

                    if (gens.elementAt(i).getInPrimaryMarket())
                        name += " - Primary";
                    else if (gens.elementAt(i).getInPrimaryMarket())
                        name += " - Secondary";
                    else
                        name += " - OffSpot";

                    headerYear.add(name);

                }
            }
            String[] hy = headerYear.toArray(new String[0]);

            csvWriterYear.writeNext(hy);

            String[] headerRecordMonthly = { "Time (Month)", "Consumption (KWh) per household",
                    "Avg Tariff (c/KWh) per household", "Primary Wholesale ($/MWh)",
                    "GHG Emissions (tCO2-e) per household", "Number of Domestic Consumers (households)",
                    "Percentage Renewable Production", "System Production Primary Spot",
                    "System Production Secondary Spot", "System Production Off Spot",
                    "System Production Rooftop PV", "System Production Coal", "System Production Water",
                    "System Production Wind", "System Production Gas",
                    "System Production Solar", "System Production Battery", "Number of Active Actors",
                    "Primary Total Unmet Demand (MWh)", "Primary Total Unmet Demand (Hours)",
                    "Primary Total Unmet Demand (Days)", "Primary Max Unmet Demand Per Hour (MWh)",
                    "Secondary Total Unmet Demand (MWh)", "Secondary Total Unmet Demand (Hours)",
                    "Secondary Total Unmet Demand (Days)", "Secondary Max Unmet Demand Per Hour (MWh)" };

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
             * 1 CombinedTariff (only in PriceGenAvgSeries)
             * 0 PrimarySpot
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
            XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(0);
            XYSeries secondarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(-1);

            Calendar c = Calendar.getInstance();

            c.setTime(data.getStartSimDate());

            for (int t = 0; t < cseries.getItems().size(); t++) {

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
                spcoalaggitem = (XYDataItem) spcoalaggseries.getItems().get(t);
                spwateraggitem = (XYDataItem) spwateraggseries.getItems().get(t);

                if (shifttimeseries <= t) {
                    if ((t - shifttimeseries) < spprimaggseries.getItems().size())
                        spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);
                    if (spsecondaggseries != null)
                        if ((t - shifttimeseries) < spsecondaggseries.getItems().size())
                            spsecondaggitem = (XYDataItem) spsecondaggseries.getItems().get(t - shifttimeseries);
                    if (sposaggseries != null)
                        if ((t - shifttimeseries) < sposaggseries.getItems().size())
                            sposaggitem = (XYDataItem) sposaggseries.getItems().get(t - shifttimeseries);

                    sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);
                    spwindaggitem = (XYDataItem) spwindaggseries.getItems().get(t - shifttimeseries);
                    spgasaggitem = (XYDataItem) spgasaggseries.getItems().get(t - shifttimeseries);
                    spsolaggitem = (XYDataItem) spsolaggseries.getItems().get(t - shifttimeseries);
                    spbataggitem = (XYDataItem) spbataggseries.getItems().get(t - shifttimeseries);

                    primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems()
                            .get(t - shifttimeseries);
                    primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries.getItems()
                            .get(t - shifttimeseries);
                    if (secondaryunmetDemandMwhSeries != null) {
                        if ((t - shifttimeseries) < secondaryunmetDemandMwhSeries.getItems().size()) {
                            secondaryunmetDemandMwhitem = (XYDataItem) secondaryunmetDemandMwhSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondaryunmetDemandHoursitem = (XYDataItem) secondaryunmetDemandHoursSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondaryunmetDemandDaysitem = (XYDataItem) secondaryunmetDemandDaysSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondarymaxUnmetDemandMwhPerDayitem = (XYDataItem) secondarymaxUnmetDemandMwhPerDaySeries
                                    .getItems().get(t - shifttimeseries);
                        }
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
                    if (spsecondaggitem != null)
                        MWhSecondarySpotAgg = spsecondaggitem.getYValue();
                    if (sposaggitem != null)
                        MWhOffSpotAgg = sposaggitem.getYValue();
                    MWhRoofSpotAgg = sproofaggitem.getYValue();
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();
                    MWhWindSpotAgg = spwindaggitem.getYValue();
                    MWhGasSpotAgg = spgasaggitem.getYValue();
                    MWhSolSpotAgg = spsolaggitem.getYValue();
                    MWhBatSpotAgg = spbataggitem.getYValue();

                    MWhRenewable = MWhRoofSpotAgg + MWhWaterSpotAgg + MWhWindSpotAgg + MWhSolSpotAgg + MWhBatSpotAgg;
                    MWhFossil = MWhCoalSpotAgg + MWhGasSpotAgg;
                    percentageRenwewable = MWhRenewable / (MWhRenewable + MWhFossil);

                    primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                    primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                    primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                    primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();
                    if (secondaryunmetDemandMwhitem != null) {
                        secondaryunmetDemandMwh = secondaryunmetDemandMwhitem.getYValue();
                        secondaryunmetDemandHours = secondaryunmetDemandHoursitem.getYValue();
                        secondaryunmetDemandDays = secondaryunmetDemandDaysitem.getYValue();
                        secondarymaxUnmetDemandMwhPerDay = secondarymaxUnmetDemandMwhPerDayitem.getYValue();
                    }
                } else {
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();

                    MWhRenewable = MWhWaterSpotAgg;
                    MWhFossil = MWhCoalSpotAgg;
                    percentageRenwewable = MWhRenewable / (MWhRenewable + MWhFossil);
                }

                SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                String[] record = { dateToString.format(c.getTime()), Double.toString(kwh / consumers),
                        Double.toString(tariffPrice), Double.toString(wholesale),
                        Double.toString(emissions / consumers),
                        Double.toString(consumers), Double.toString(percentageRenwewable),
                        Double.toString(MWhPrimarySpotAgg), Double.toString(MWhSecondarySpotAgg),
                        Double.toString(MWhOffSpotAgg), Double.toString(MWhRoofSpotAgg),
                        Double.toString(MWhCoalSpotAgg), Double.toString(MWhWaterSpotAgg),
                        Double.toString(MWhWindSpotAgg), Double.toString(MWhGasSpotAgg),
                        Double.toString(MWhSolSpotAgg), Double.toString(MWhBatSpotAgg), Double.toString(numActors),
                        Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours),
                        Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours),
                        Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay) };
                csvWriterMonthly.writeNext(record);

                /***
                 * Generators Production Data
                 */

                ArrayList<Double> sysGenProd = new ArrayList<Double>();

                for (Integer integer : treeMap.keySet()) {
                    XYSeries sysprodseries = systemProductionSeries.get(integer);

                    int shift = cseries.getItemCount() - sysprodseries.getItemCount();

                    if (shift > t) {
                        sysGenProd.add(0.0);

                    } else {

                        XYDataItem spitem = (XYDataItem) sysprodseries.getItems().get(t - shift);
                        sysGenProd.add(spitem.getYValue());
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

                if (!datasetPrimaryunmetDemandMwhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandMwhSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandMwhSummary.get(year).add(primaryunmetDemandMwh);

                if (!datasetPrimaryunmetDemandHoursSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandHoursSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandHoursSummary.get(year).add(primaryunmetDemandHours);

                if (!datasetPrimaryunmetDemandDaysSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandDaysSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandDaysSummary.get(year).add(primaryunmetDemandDays);

                if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                }
                datasetPrimarymaxUnmetDemandMwhPerDaySummary.get(year).add(primarymaxUnmetDemandMwhPerDay);

                if (!datasetSecondaryunmetDemandMwhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandMwhSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandMwhSummary.get(year).add(secondaryunmetDemandMwh);

                if (!datasetSecondaryunmetDemandHoursSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandHoursSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandHoursSummary.get(year).add(secondaryunmetDemandHours);

                if (!datasetSecondaryunmetDemandDaysSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandDaysSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandDaysSummary.get(year).add(secondaryunmetDemandDays);

                if (!datasetSecondarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                }
                datasetSecondarymaxUnmetDemandMwhPerDaySummary.get(year).add(secondarymaxUnmetDemandMwhPerDay);

                c.add(Calendar.MONTH, 1);
            }

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

                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary
                        .get(year);
                ArrayList<Double> yearSecondaryunmetDemandMwh = datasetSecondaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandHours = datasetSecondaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandDays = datasetSecondaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearSecondarymaxUnmetDemandMwhPerDay = datasetSecondarymaxUnmetDemandMwhPerDaySummary
                        .get(year);

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
                primarymaxUnmetDemandMwhPerDay = Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);
                for (Double d : yearSecondaryunmetDemandMwh) {
                    secondaryunmetDemandMwh += d;
                }
                for (Double d : yearSecondaryunmetDemandHours) {
                    secondaryunmetDemandHours += d;
                }
                for (Double d : yearSecondaryunmetDemandDays) {
                    secondaryunmetDemandDays += d;
                }
                secondarymaxUnmetDemandMwhPerDay = Collections.max(yearSecondarymaxUnmetDemandMwhPerDay);

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

                maxDwellings = yearDataConsumers.get(0);

                for (Double con : yearDataNumActors) {
                    if (con > maxNumActors)
                        maxNumActors = con;
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
                if (sprenewableagg + spfossilagg <= 0.00000001)
                    percentageRenewable = 0.0;

                for (ArrayList<Double> spg : spGenProdAgg) {
                    if (spgenprodagg == null)
                        spgenprodagg = (ArrayList<Double>) spg.clone();
                    else {
                        for (int j = 0; j < spgenprodagg.size(); j++) {
                            spgenprodagg.set(j, spgenprodagg.get(j) + spg.get(j));
                        }
                    }
                }

                ArrayList<String> record = new ArrayList<String>(Arrays.asList(year, Double.toString(totalKWh),
                        Double.toString(avgPrice), Double.toString(avgWholesale), Double.toString(totalGHG),
                        Double.toString(maxDwellings), Double.toString(percentageRenewable),
                        Double.toString(spprimspotagg), Double.toString(spsecondspotagg), Double.toString(spoffspotagg),
                        Double.toString(sproofagg),
                        Double.toString(spcoalagg), Double.toString(spwateragg), Double.toString(spwindagg),
                        Double.toString(spgasagg), Double.toString(spsolagg),
                        Double.toString(spbatagg), Double.toString(maxNumActors),
                        Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours),
                        Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours),
                        Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay)));

                dappGHG.add(totalGHG);
                dappRenew.add(percentageRenewable);
                dappTariff.add(avgPrice);
                dappWholesale.add(avgWholesale);
                dappUnmetSum.add(primaryunmetDemandMwh);
                dappUnmetHours.add(primaryunmetDemandHours);
                dappUnmetDays.add(primaryunmetDemandDays);
                dappUnmetMax.add(primarymaxUnmetDemandMwhPerDay);

                for (Double spg : spgenprodagg) {
                    record.add(Double.toString(spg));
                }

                String[] rec = record.toArray(new String[0]);

                csvWriterYear.writeNext(rec);
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }

    }

    public void saveDataDAPP(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        try {
            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");

            HashMap<String, ArrayList<Double>> datasetGHGsummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPriceSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetWholesaleSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetKWhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetConsumersSummary = new HashMap<>();

            HashMap<String, ArrayList<Double>> datasetSysProdPrimarySpotSummary = new HashMap<>();

            HashMap<String, ArrayList<Double>> datasetSysProdRenewableSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetSysProdFossilSummary = new HashMap<>();
            HashMap<String, ArrayList<ArrayList<Double>>> datasetSysGenProdSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandMwhSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandHoursSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimaryunmetDemandDaysSummary = new HashMap<>();
            HashMap<String, ArrayList<Double>> datasetPrimarymaxUnmetDemandMwhPerDaySummary = new HashMap<>();

            XYSeries cseries = consumptionActorSeries.get(0);
            XYSeries tseries = tariffUsageConsumptionActorSeries.get(0);
            XYSeries wseries = wholesaleSeries.get(0);
            XYSeries gseries = ghgConsumptionActorSeries.get(0);
            XYSeries dseries = numDomesticConsumersSeries.get(0);

            /**
             * Add series to hashmap of series of aggregated
             * 1 CombinedTariff (only in PriceGenAvgSeries)
             * 0 PrimarySpot
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

            XYSeries sproofaggseries = systemProductionSeries.get(-3);
            XYSeries spcoalaggseries = systemProductionSeries.get(-4);
            XYSeries spwateraggseries = systemProductionSeries.get(-5);
            XYSeries spwindaggseries = systemProductionSeries.get(-6);
            XYSeries spgasaggseries = systemProductionSeries.get(-7);
            XYSeries spsolaggseries = systemProductionSeries.get(-8);
            XYSeries spbataggseries = systemProductionSeries.get(-9);

            XYSeries primaryunmetDemandMwhSeries = unmetDemandMwhSeries.get(0);

            XYSeries primaryunmetDemandHoursSeries = unmetDemandHoursSeries.get(0);

            XYSeries primaryunmetDemandDaysSeries = unmetDemandDaysSeries.get(0);

            XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(0);

            Calendar c = Calendar.getInstance();

            c.setTime(data.getStartSimDate());

            for (int t = 0; t < cseries.getItems().size(); t++) {

                int shifttimeseries = cseries.getItemCount() - spprimaggseries.getItemCount();

                XYDataItem citem = (XYDataItem) cseries.getItems().get(t);
                XYDataItem titem = (XYDataItem) tseries.getItems().get(t);
                XYDataItem witem = (XYDataItem) wseries.getItems().get(t);
                XYDataItem gitem = (XYDataItem) gseries.getItems().get(t);
                XYDataItem ditem = (XYDataItem) dseries.getItems().get(t);

                XYDataItem spprimaggitem = null;

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

                spcoalaggitem = (XYDataItem) spcoalaggseries.getItems().get(t);
                spwateraggitem = (XYDataItem) spwateraggseries.getItems().get(t);

                if (shifttimeseries <= t) {
                    if ((t - shifttimeseries) < spprimaggseries.getItems().size())
                        spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);

                    sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);
                    spwindaggitem = (XYDataItem) spwindaggseries.getItems().get(t - shifttimeseries);
                    spgasaggitem = (XYDataItem) spgasaggseries.getItems().get(t - shifttimeseries);
                    spsolaggitem = (XYDataItem) spsolaggseries.getItems().get(t - shifttimeseries);
                    spbataggitem = (XYDataItem) spbataggseries.getItems().get(t - shifttimeseries);

                    primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems()
                            .get(t - shifttimeseries);
                    primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries.getItems()
                            .get(t - shifttimeseries);

                }

                double kwh = citem.getYValue() * 1000.0;
                double tariffPrice = titem.getYValue();
                double wholesale = witem.getYValue();
                double emissions = gitem.getYValue();
                double consumers = ditem.getYValue();
                double MWhPrimarySpotAgg = 0.0;

                double MWhRoofSpotAgg = 0.0;
                double MWhCoalSpotAgg = 0.0;
                double MWhWaterSpotAgg = 0.0;
                double MWhWindSpotAgg = 0.0;
                double MWhGasSpotAgg = 0.0;
                double MWhSolSpotAgg = 0.0;
                double MWhBatSpotAgg = 0.0;
                double MWhRenewable = 0.0;
                double MWhFossil = 0.0;

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

                    MWhRoofSpotAgg = sproofaggitem.getYValue();
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();
                    MWhWindSpotAgg = spwindaggitem.getYValue();
                    MWhGasSpotAgg = spgasaggitem.getYValue();
                    MWhSolSpotAgg = spsolaggitem.getYValue();
                    MWhBatSpotAgg = spbataggitem.getYValue();

                    MWhRenewable = MWhRoofSpotAgg + MWhWaterSpotAgg + MWhWindSpotAgg + MWhSolSpotAgg + MWhBatSpotAgg;
                    MWhFossil = MWhCoalSpotAgg + MWhGasSpotAgg;

                    primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                    primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                    primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                    primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();

                } else {
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();

                    MWhRenewable = MWhWaterSpotAgg;
                    MWhFossil = MWhCoalSpotAgg;

                }

                SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                /***
                 * Generators Production Data
                 */

                ArrayList<Double> sysGenProd = new ArrayList<Double>();

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

                if (!datasetPrimaryunmetDemandMwhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandMwhSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandMwhSummary.get(year).add(primaryunmetDemandMwh);

                if (!datasetPrimaryunmetDemandHoursSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandHoursSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandHoursSummary.get(year).add(primaryunmetDemandHours);

                if (!datasetPrimaryunmetDemandDaysSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandDaysSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandDaysSummary.get(year).add(primaryunmetDemandDays);

                if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                }
                datasetPrimarymaxUnmetDemandMwhPerDaySummary.get(year).add(primarymaxUnmetDemandMwhPerDay);

                c.add(Calendar.MONTH, 1);
            }

            Object[] years = datasetGHGsummary.keySet().toArray();
            Arrays.sort(years, Collections.reverseOrder());
            for (int i = years.length - 1; i >= 0; i--) {
                String year = (String) years[i];
                ArrayList<Double> yearDataGHG = datasetGHGsummary.get(year);
                ArrayList<Double> yearDataPrice = datasetPriceSummary.get(year);
                ArrayList<Double> yearDataWholesale = datasetWholesaleSummary.get(year);

                ArrayList<Double> spPrimarySpotAgg = datasetSysProdPrimarySpotSummary.get(year);

                ArrayList<Double> spRenewableAgg = datasetSysProdRenewableSummary.get(year);
                ArrayList<Double> spFossilAgg = datasetSysProdFossilSummary.get(year);

                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary
                        .get(year);

                Double totalGHG = 0.0;
                Double avgPrice = 0.0;
                Double avgWholesale = 0.0;

                Double sprenewableagg = 0.0;
                Double spfossilagg = 0.0;

                Double primaryunmetDemandMwh = 0.0;
                Double primaryunmetDemandHours = 0.0;
                Double primaryunmetDemandDays = 0.0;
                Double primarymaxUnmetDemandMwhPerDay = 0.0;

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
                primarymaxUnmetDemandMwhPerDay = Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);

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

                for (Double k : spRenewableAgg) {
                    sprenewableagg += k;
                }

                for (Double k : spFossilAgg) {
                    spfossilagg += k;
                }

                Double percentageRenewable = sprenewableagg / (sprenewableagg + spfossilagg);
                if (sprenewableagg + spfossilagg <= 0.00000001)
                    percentageRenewable = 0.0;

                dappGHGDAPP.add(totalGHG);
                dappRenewDAPP.add(percentageRenewable);
                dappTariffDAPP.add(avgPrice);
                dappWholesaleDAPP.add(avgWholesale);
                dappUnmetSumDAPP.add(primaryunmetDemandMwh);
                dappUnmetHoursDAPP.add(primaryunmetDemandHours);
                dappUnmetDaysDAPP.add(primaryunmetDemandDays);
                dappUnmetMaxDAPP.add(primarymaxUnmetDemandMwhPerDay);

            }

        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public double[][] saveDataRL(SimState simState, int currentYear) {

        Gr4spSim data = (Gr4spSim) simState;

        ArrayList<Integer> currYear = new ArrayList<>();

        ArrayList<Double> currTariff = new ArrayList<>();
        ArrayList<Double> currWholesale = new ArrayList<>();
        ArrayList<Double> currGHG = new ArrayList<>();
        ArrayList<Double> currRenew = new ArrayList<>();
        ArrayList<Double> currUnmet = new ArrayList<>();
        ArrayList<Double> currSysPrim = new ArrayList<>();
        ArrayList<Double> currSysSec = new ArrayList<>();
        ArrayList<Double> currSysOff = new ArrayList<>();
        ArrayList<Double> currPrimTotalUnmet = new ArrayList<>();
        ArrayList<Double> currSecTotalUnmet = new ArrayList<>();
        ArrayList<Double> currSecTotalUnmetDays = new ArrayList<>();
        ArrayList<Double> currConsumption = new ArrayList<>();
        ArrayList<Double> currPV = new ArrayList<>();
        ArrayList<Double> currCoal = new ArrayList<>();
        ArrayList<Double> currWater = new ArrayList<>();
        ArrayList<Double> currWind = new ArrayList<>();
        ArrayList<Double> currGas = new ArrayList<>();
        ArrayList<Double> currSolar = new ArrayList<>();
        ArrayList<Double> currBattery = new ArrayList<>();

        try {
            SimpleDateFormat dateToYear = new SimpleDateFormat("yyyy");
            TreeMap<Integer, Vector<Generator>> treeMap = new TreeMap<>(data.gen_register);

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
             * 1 CombinedTariff (only in PriceGenAvgSeries)
             * 0 PrimarySpot
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
            XYSeries primarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(0);
            XYSeries secondarymaxUnmetDemandMwhPerDaySeries = maxUnmetDemandMwhPerHourSeries.get(-1);

            Calendar c = Calendar.getInstance();

            c.setTime(data.getStartSimDate());
            c.set(Calendar.YEAR, currentYear - 4);

            for (int t = ((currentYear - 4) - 1998) * 12; t < cseries.getItems().size(); t++) {

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
                spcoalaggitem = (XYDataItem) spcoalaggseries.getItems().get(t);
                spwateraggitem = (XYDataItem) spwateraggseries.getItems().get(t);

                if (shifttimeseries <= t) {
                    if ((t - shifttimeseries) < spprimaggseries.getItems().size())
                        spprimaggitem = (XYDataItem) spprimaggseries.getItems().get(t - shifttimeseries);
                    if (spsecondaggseries != null)
                        if ((t - shifttimeseries) < spsecondaggseries.getItems().size())
                            spsecondaggitem = (XYDataItem) spsecondaggseries.getItems().get(t - shifttimeseries);
                    if (sposaggseries != null)
                        if ((t - shifttimeseries) < sposaggseries.getItems().size())
                            sposaggitem = (XYDataItem) sposaggseries.getItems().get(t - shifttimeseries);

                    sproofaggitem = (XYDataItem) sproofaggseries.getItems().get(t - shifttimeseries);
                    spwindaggitem = (XYDataItem) spwindaggseries.getItems().get(t - shifttimeseries);
                    spgasaggitem = (XYDataItem) spgasaggseries.getItems().get(t - shifttimeseries);
                    spsolaggitem = (XYDataItem) spsolaggseries.getItems().get(t - shifttimeseries);
                    spbataggitem = (XYDataItem) spbataggseries.getItems().get(t - shifttimeseries);

                    primaryunmetDemandMwhitem = (XYDataItem) primaryunmetDemandMwhSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandHoursitem = (XYDataItem) primaryunmetDemandHoursSeries.getItems()
                            .get(t - shifttimeseries);
                    primaryunmetDemandDaysitem = (XYDataItem) primaryunmetDemandDaysSeries.getItems()
                            .get(t - shifttimeseries);
                    primarymaxUnmetDemandMwhPerDayitem = (XYDataItem) primarymaxUnmetDemandMwhPerDaySeries.getItems()
                            .get(t - shifttimeseries);
                    if (secondaryunmetDemandMwhSeries != null) {
                        if ((t - shifttimeseries) < secondaryunmetDemandMwhSeries.getItems().size()) {
                            secondaryunmetDemandMwhitem = (XYDataItem) secondaryunmetDemandMwhSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondaryunmetDemandHoursitem = (XYDataItem) secondaryunmetDemandHoursSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondaryunmetDemandDaysitem = (XYDataItem) secondaryunmetDemandDaysSeries.getItems()
                                    .get(t - shifttimeseries);
                            secondarymaxUnmetDemandMwhPerDayitem = (XYDataItem) secondarymaxUnmetDemandMwhPerDaySeries
                                    .getItems().get(t - shifttimeseries);
                        }
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
                    if (spsecondaggitem != null)
                        MWhSecondarySpotAgg = spsecondaggitem.getYValue();
                    if (sposaggitem != null)
                        MWhOffSpotAgg = sposaggitem.getYValue();
                    MWhRoofSpotAgg = sproofaggitem.getYValue();
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();
                    MWhWindSpotAgg = spwindaggitem.getYValue();
                    MWhGasSpotAgg = spgasaggitem.getYValue();
                    MWhSolSpotAgg = spsolaggitem.getYValue();
                    MWhBatSpotAgg = spbataggitem.getYValue();

                    MWhRenewable = MWhRoofSpotAgg + MWhWaterSpotAgg + MWhWindSpotAgg + MWhSolSpotAgg + MWhBatSpotAgg;
                    MWhFossil = MWhCoalSpotAgg + MWhGasSpotAgg;
                    percentageRenwewable = MWhRenewable / (MWhRenewable + MWhFossil);

                    primaryunmetDemandMwh = primaryunmetDemandMwhitem.getYValue();
                    primaryunmetDemandHours = primaryunmetDemandHoursitem.getYValue();
                    primaryunmetDemandDays = primaryunmetDemandDaysitem.getYValue();
                    primarymaxUnmetDemandMwhPerDay = primarymaxUnmetDemandMwhPerDayitem.getYValue();
                    if (secondaryunmetDemandMwhitem != null) {
                        secondaryunmetDemandMwh = secondaryunmetDemandMwhitem.getYValue();
                        secondaryunmetDemandHours = secondaryunmetDemandHoursitem.getYValue();
                        secondaryunmetDemandDays = secondaryunmetDemandDaysitem.getYValue();
                        secondarymaxUnmetDemandMwhPerDay = secondarymaxUnmetDemandMwhPerDayitem.getYValue();
                    }
                } else {
                    MWhCoalSpotAgg = spcoalaggitem.getYValue();
                    MWhWaterSpotAgg = spwateraggitem.getYValue();

                    MWhRenewable = MWhWaterSpotAgg;
                    MWhFossil = MWhCoalSpotAgg;
                    percentageRenwewable = MWhRenewable / (MWhRenewable + MWhFossil);
                }

                SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd");

                String[] record = { dateToString.format(c.getTime()), Double.toString(kwh / consumers),
                        Double.toString(tariffPrice), Double.toString(wholesale),
                        Double.toString(emissions / consumers),
                        Double.toString(consumers), Double.toString(percentageRenwewable),
                        Double.toString(MWhPrimarySpotAgg), Double.toString(MWhSecondarySpotAgg),
                        Double.toString(MWhOffSpotAgg), Double.toString(MWhRoofSpotAgg),
                        Double.toString(MWhCoalSpotAgg), Double.toString(MWhWaterSpotAgg),
                        Double.toString(MWhWindSpotAgg), Double.toString(MWhGasSpotAgg),
                        Double.toString(MWhSolSpotAgg), Double.toString(MWhBatSpotAgg), Double.toString(numActors),
                        Double.toString(primaryunmetDemandMwh), Double.toString(primaryunmetDemandHours),
                        Double.toString(primaryunmetDemandDays), Double.toString(primarymaxUnmetDemandMwhPerDay),
                        Double.toString(secondaryunmetDemandMwh), Double.toString(secondaryunmetDemandHours),
                        Double.toString(secondaryunmetDemandDays), Double.toString(secondarymaxUnmetDemandMwhPerDay) };

                /***
                 * Generators Production Data
                 */

                ArrayList<Double> sysGenProd = new ArrayList<Double>();

                for (Integer integer : treeMap.keySet()) {
                    try {
                        XYSeries sysprodseries = systemProductionSeries.get(integer);

                        int shift = cseries.getItemCount() - sysprodseries.getItemCount();

                        if (shift > t) {
                            sysGenProd.add(0.0);

                        } else {

                            XYDataItem spitem = (XYDataItem) sysprodseries.getItems().get(t - shift);
                            sysGenProd.add(spitem.getYValue());
                        }
                    } catch (Exception e) {
                        continue;
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

                if (!datasetPrimaryunmetDemandMwhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandMwhSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandMwhSummary.get(year).add(primaryunmetDemandMwh);

                if (!datasetPrimaryunmetDemandHoursSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandHoursSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandHoursSummary.get(year).add(primaryunmetDemandHours);

                if (!datasetPrimaryunmetDemandDaysSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimaryunmetDemandDaysSummary.put(year, yearData);
                }
                datasetPrimaryunmetDemandDaysSummary.get(year).add(primaryunmetDemandDays);

                if (!datasetPrimarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetPrimarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                }
                datasetPrimarymaxUnmetDemandMwhPerDaySummary.get(year).add(primarymaxUnmetDemandMwhPerDay);

                if (!datasetSecondaryunmetDemandMwhSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandMwhSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandMwhSummary.get(year).add(secondaryunmetDemandMwh);

                if (!datasetSecondaryunmetDemandHoursSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandHoursSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandHoursSummary.get(year).add(secondaryunmetDemandHours);

                if (!datasetSecondaryunmetDemandDaysSummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondaryunmetDemandDaysSummary.put(year, yearData);
                }
                datasetSecondaryunmetDemandDaysSummary.get(year).add(secondaryunmetDemandDays);

                if (!datasetSecondarymaxUnmetDemandMwhPerDaySummary.containsKey(year)) {
                    ArrayList<Double> yearData = new ArrayList<>();
                    datasetSecondarymaxUnmetDemandMwhPerDaySummary.put(year, yearData);
                }
                datasetSecondarymaxUnmetDemandMwhPerDaySummary.get(year).add(secondarymaxUnmetDemandMwhPerDay);

                c.add(Calendar.MONTH, 1);
            }

            for (int i = currentYear - 1; i >= currentYear - 4; i--) {
                String year = String.valueOf(i);

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

                ArrayList<Double> yearPrimaryunmetDemandMwh = datasetPrimaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandHours = datasetPrimaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearPrimaryunmetDemandDays = datasetPrimaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearPrimarymaxUnmetDemandMwhPerDay = datasetPrimarymaxUnmetDemandMwhPerDaySummary
                        .get(year);
                ArrayList<Double> yearSecondaryunmetDemandMwh = datasetSecondaryunmetDemandMwhSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandHours = datasetSecondaryunmetDemandHoursSummary.get(year);
                ArrayList<Double> yearSecondaryunmetDemandDays = datasetSecondaryunmetDemandDaysSummary.get(year);
                ArrayList<Double> yearSecondarymaxUnmetDemandMwhPerDay = datasetSecondarymaxUnmetDemandMwhPerDaySummary
                        .get(year);

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

                for (Double d : yearPrimaryunmetDemandMwh) {
                    primaryunmetDemandMwh += d;
                }
                for (Double d : yearPrimaryunmetDemandHours) {
                    primaryunmetDemandHours += d;
                }
                for (Double d : yearPrimaryunmetDemandDays) {
                    primaryunmetDemandDays += d;
                }
                primarymaxUnmetDemandMwhPerDay = Collections.max(yearPrimarymaxUnmetDemandMwhPerDay);
                for (Double d : yearSecondaryunmetDemandMwh) {
                    secondaryunmetDemandMwh += d;
                }
                for (Double d : yearSecondaryunmetDemandHours) {
                    secondaryunmetDemandHours += d;
                }
                for (Double d : yearSecondaryunmetDemandDays) {
                    secondaryunmetDemandDays += d;
                }
                secondarymaxUnmetDemandMwhPerDay = Collections.max(yearSecondarymaxUnmetDemandMwhPerDay);

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

                maxDwellings = yearDataConsumers.get(0);

                for (Double con : yearDataNumActors) {
                    if (con > maxNumActors)
                        maxNumActors = con;
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
                if (sprenewableagg + spfossilagg <= 0.00000001)
                    percentageRenewable = 0.0;

                for (ArrayList<Double> spg : spGenProdAgg) {
                    if (spgenprodagg == null)
                        spgenprodagg = (ArrayList<Double>) spg.clone();
                    else {
                        for (int j = 0; j < spgenprodagg.size(); j++) {
                            spgenprodagg.set(j, spgenprodagg.get(j) + spg.get(j));
                        }
                    }
                }

                currYear.add(i);
                currGHG.add(totalGHG);
                currRenew.add(percentageRenewable);
                currTariff.add(avgPrice);
                currWholesale.add(avgWholesale);
                currUnmet.add(primaryunmetDemandDays);

                currSysPrim.add(spprimspotagg);
                currSysSec.add(spsecondspotagg);
                currSysOff.add(spoffspotagg);
                currPrimTotalUnmet.add(primaryunmetDemandMwh);
                currSecTotalUnmet.add(secondaryunmetDemandMwh);
                currSecTotalUnmetDays.add(secondaryunmetDemandDays);
                currConsumption.add(totalKWh);
                currPV.add(sproofagg);
                currCoal.add(spcoalagg);
                currWater.add(spwateragg);
                currWind.add(spwindagg);
                currGas.add(spgasagg);
                currSolar.add(spsolagg);
                currBattery.add(spbatagg);

            }

        } catch (Exception ex) {
            System.out.println(ex);
        }

        boolean print = false;
        if (print) {
            System.out.println(currentYear);
            System.out.print("GHG ");
            for (double d : currGHG) {
                System.out.printf("%f ", d);
            }
            System.out.println();

            System.out.print("Renew ");
            for (double d : currRenew) {
                System.out.printf("%f ", d);
            }
            System.out.println();

            System.out.print("Tariff ");
            for (double d : currTariff) {
                System.out.printf("%f ", d);
            }
            System.out.println();

            System.out.print("Wholesale ");
            for (double d : currWholesale) {
                System.out.printf("%f ", d);
            }
            System.out.println();

            System.out.print("Unmet ");
            for (double d : currUnmet) {
                System.out.printf("%f ", d);
            }
            System.out.println();

            System.out.println();
            System.out.println();
        }

        double[] ret = { ghg, renew, tariff, whole, unmet };
        double[][] bigRet = new double[4][20];
        for (int i = 0; i < 4; i++) {
            bigRet[i][0] = currYear.get(i);
            bigRet[i][1] = currGHG.get(i);
            bigRet[i][2] = currRenew.get(i);
            bigRet[i][3] = currTariff.get(i);
            bigRet[i][4] = currWholesale.get(i);
            bigRet[i][5] = currUnmet.get(i);
            bigRet[i][6] = currSysPrim.get(i);
            bigRet[i][7] = currSysSec.get(i);
            bigRet[i][8] = currSysOff.get(i);
            bigRet[i][9] = currPrimTotalUnmet.get(i);
            bigRet[i][10] = currSecTotalUnmet.get(i);
            bigRet[i][11] = currSecTotalUnmetDays.get(i);
            bigRet[i][12] = currConsumption.get(i);
            bigRet[i][13] = currPV.get(i);
            bigRet[i][14] = currCoal.get(i);
            bigRet[i][15] = currWater.get(i);
            bigRet[i][16] = currWind.get(i);
            bigRet[i][17] = currGas.get(i);
            bigRet[i][18] = currSolar.get(i);
            bigRet[i][19] = currBattery.get(i);
        }

        return bigRet;

    }

}
