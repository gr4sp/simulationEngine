# Import module
import jpype

# Enable Java imports
import jpype.imports

import pandas as pd
import numpy as np
from random import randint
import os
import json

'''
Create Java Virtual Machine (jpype.getDefaultJVMPath())
'''

def startJVM():
    with open('settingsExperiments.json') as f:
        settings = json.load(f)

    jvmpath = settings[settings["jvmPath"]]

    gr4spPath = os.getcwd() + "/.."

    gr4spPath = gr4spPath.replace("\\", "/")

    ## Startup Jpype and import the messaging java package
    if jpype.isJVMStarted():
        return

    classpathSeparator = ";"
    if settings["jvmPath"] == "jvmPathUbuntu":
        classpathSeparator = ":"

    classpath = "-Djava.class.path=" \
                "{0}/{2}{1}" \
                "{0}/libraries/bsh-2.0b4.jar{1}{0}/libraries/itext-1.2.jar{1}" \
                "{0}/libraries/j3dcore.jar{1}{0}/libraries/j3dutils.jar{1}" \
                "{0}/libraries/jcommon-1.0.21.jar{1}" \
                "{0}/libraries/jfreechart-1.0.17.jar{1}" \
                "{0}/libraries/jmf.jar{1}" \
                "{0}/libraries/mason.19.jar{1}" \
                "{0}/libraries/portfolio.jar{1}" \
                "{0}/libraries/vecmath.jar{1}" \
                "{0}/libraries/postgresql-42.2.6.jar{1}" \
                "{0}/libraries/opencsv-4.6.jar{1}" \
                "{0}/libraries/yamlbeans-1.13.jar".format(gr4spPath, classpathSeparator, settings["gr4spClasses"])

    jpype.startJVM(jvmpath, classpath, "-Xmx8192M")  # 8GB


def shutdownJVM():
    jpype.shutdownJVM()


def getResults(outputID, experimentId):
    # LOAD CSV
    # Consumption (KWh) per household
    # Avg Tariff (c/KWh) per household
    # Wholesale ($/MWh)
    # GHG Emissions (tCO2-e) per household
    # Number of Domestic Consumers (households)
    # System Production Primary Spot
    # System Production Secondary Spot
    # System Production Off Spot
    # System Production Rooftop PV
    # Number of Active Actors

    with open('settingsExperiments.json') as f:
        settings = json.load(f)

    slash = "\\"
    if settings["jvmPath"] == "jvmPathUbuntu":
        slash = "/"

    gr4spPath = os.getcwd() + slash + ".."

    # csvFileName = '{0}{1}csv{1}BAUVIC{1}BAUVICSimDataMonthlySummary{2}.csv'.format(gr4spPath, slash, outputID)
    # results = pd.read_csv(csvFileName)

    # #Prepare time series
    # emptyArray = np.array([])
    # timesMonth = emptyArray


    csvFileName = '{0}{1}csv{1}VIC{1}VICSimDataYearSummary{2}.csv'.format(gr4spPath, slash, outputID)
    results = pd.read_csv(csvFileName)

    #
    # # Prepare time series
    timesYear = results['Time (Year)'].to_numpy()
    consumptionYear = results['Consumption (KWh) per household'].to_numpy()
    tariffsYear = results[' Avg Tariff (c/KWh) per household'].to_numpy()
    wholesaleYear = results['Wholesale ($/MWh)'].to_numpy()
    ghgYear = results['GHG Emissions (tCO2-e) per household'].to_numpy()
    numConsumersYear = results['Number of Domestic Consumers (households)'].to_numpy()
    primarySpotYear = results['System Production Primary Spot'].to_numpy()
    secondarySpotYear = results['System Production Secondary Spot'].to_numpy()
    offSpotYear = results['System Production Off Spot'].to_numpy()
    renewableContributionYear = results['Percentage Renewable Production'].to_numpy()
    rooftopPVProductionYear = results['System Production Rooftop PV'].to_numpy()
    coalProductionYear = results['System Production Coal'].to_numpy()
    waterProductionYear = results['System Production Water'].to_numpy()
    windProductionYear = results['System Production Wind'].to_numpy()
    gasProductionYear = results['System Production Gas'].to_numpy()
    solarProductionYear = results['System Production Solar'].to_numpy()
    BatteryProductionYear = results['System Production Battery'].to_numpy()
    numActorsYear = results['Number of Active Actors'].to_numpy()
    primaryUnmetDemandMwh = results['Primary Total Unmet Demand (MWh)'].to_numpy()
    primaryUnmetDemandHours = results['Primary Total Unmet Demand (Hours)'].to_numpy()
    primaryUnmetDemandDays = results['Primary Total Unmet Demand (Days)'].to_numpy()
    primaryMaxUnmetDemandMwhPerHour = results['Primary Max Unmet Demand Per Hour (MWh)'].to_numpy()
    secondaryUnmetDemandMwh = results['Secondary Total Unmet Demand (MWh)'].to_numpy()
    secondaryUnmetDemandHours = results['Secondary Total Unmet Demand (Hours)'].to_numpy()
    secondaryUnmetDemandDays = results['Secondary Total Unmet Demand (Days)'].to_numpy()
    secondaryMaxUnmetDemandMwhPerHour = results['Secondary Max Unmet Demand Per Hour (MWh)'].to_numpy()
    seedExperimentCsv = float(experimentId)

    return timesYear, consumptionYear, tariffsYear, wholesaleYear, ghgYear, numConsumersYear, primarySpotYear, \
           secondarySpotYear, offSpotYear, renewableContributionYear, rooftopPVProductionYear, coalProductionYear, \
           waterProductionYear, windProductionYear, gasProductionYear, solarProductionYear, BatteryProductionYear, \
           numActorsYear, primaryUnmetDemandMwh, primaryUnmetDemandHours, primaryUnmetDemandDays, primaryMaxUnmetDemandMwhPerHour,\
           secondaryUnmetDemandMwh, secondaryUnmetDemandHours, secondaryUnmetDemandDays, secondaryMaxUnmetDemandMwhPerHour, seedExperimentCsv



def runGr4sp(experimentId, annualCpi, annualInflation, consumption, energyEfficiency, onsiteGeneration, solarUptake, rooftopPV,
             domesticConsumptionPercentage, includePublicallyAnnouncedGen, generationRolloutPeriod, generatorRetirement, technologicalImprovement,
             learningCurve, priceChangePercentageBattery, priceChangePercentageBrownCoal, priceChangePercentageOcgt,
             priceChangePercentageCcgt, priceChangePercentageWind, priceChangePercentageWater,
             capacityFactorChangeBattery, capacityFactorChangeBrownCoal, capacityFactorChangeOcgt,
             capacityFactorChangeCcgt, capacityFactorChangeWind, capacityFactorChangeWater, wholesaleTariffContribution, scheduleMinCapMarketGen,
             semiScheduleGenSpotMarket, semiScheduleMinCapMarketGen, nonScheduleGenSpotMarket,
             nonScheduleMinCapMarketGen):
    startJVM()

    try:

        import java.lang

        results = None
        outputID = "_seed_{}".format(experimentId)

        #Check if file exists in the CSVs
        with open('settingsExperiments.json') as f:
            settings = json.load(f)
        slash = "\\"
        if settings["jvmPath"] == "jvmPathUbuntu":
            slash = "/"
        gr4spPath = os.getcwd() + slash + ".."
        csvFileName = '{0}{1}csv{1}VIC{1}VICSimDataMonthlySummary{2}.csv'.format(gr4spPath, slash, outputID)

        #If CSV doesn't exist, then run the simulation. This is usefule to resume failed EMA runs
        if os.path.isfile(csvFileName) is False:

            gr4sp = jpype.JClass("core.Gr4spSim")
            # to identify each csv created in the simulation with an unique experiment number, instead of using seed = randint(0, 100000), we use the experiment number
            gr4spObj = gr4sp(experimentId)
            outputID = str(gr4spObj.outputID)
            print(outputID)

            # Set Uncertainties
            gr4spObj.settings.forecast.annualCpi = annualCpi / 100.0
            gr4spObj.settings.policy.annualInflation = annualInflation / 100.0

            gr4spObj.settings.forecast.scenario.consumption = consumption
            gr4spObj.settings.forecast.scenario.energyEfficiency = energyEfficiency
            gr4spObj.settings.forecast.scenario.onsiteGeneration = onsiteGeneration
            gr4spObj.settings.forecast.scenario.solarUptake = solarUptake
            gr4spObj.settings.forecast.rooftopPV = rooftopPV

            gr4spObj.settings.population.domesticConsumptionPercentage = domesticConsumptionPercentage / 100.0

            gr4spObj.settings.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
                includePublicallyAnnouncedGen)
            gr4spObj.settings.forecast.generationRolloutPeriod = generationRolloutPeriod
            gr4spObj.settings.forecast.generatorRetirement = generatorRetirement
            gr4spObj.settings.forecast.technologicalImprovement = technologicalImprovement / 100.0
            gr4spObj.settings.forecast.learningCurve = learningCurve / 100.0

            # LCOEs and CFs variations

            brown_coal_min_price = gr4spObj.settings.getPriceMinMWh('Brown Coal', '') * (
                    100.0 + priceChangePercentageBrownCoal) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Brown Coal', '', brown_coal_min_price)

            brown_coal_max_price = gr4spObj.settings.getPriceMaxMWh('Brown Coal', '') * (
                    100.0 + priceChangePercentageBrownCoal) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Brown Coal', '', brown_coal_max_price)

            battery_min_price = gr4spObj.settings.getPriceMinMWh('Battery', '') * (
                    100.0 + priceChangePercentageBattery) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Battery', '', battery_min_price)

            battery_max_price = gr4spObj.settings.getPriceMaxMWh('Battery', '') * (
                    100.0 + priceChangePercentageBattery) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Battery', '', battery_max_price)

            ocgt_min_price = gr4spObj.settings.getPriceMinMWh('Natural Gas Pipeline Turbine - OCGT', '') * (
                    100.0 + priceChangePercentageOcgt) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Natural Gas Pipeline Turbine - OCGT', '', ocgt_min_price)

            ocgt_max_price = gr4spObj.settings.getPriceMaxMWh('Natural Gas Pipeline Turbine - OCGT', '') * (
                    100.0 + priceChangePercentageOcgt) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Natural Gas Pipeline Turbine - OCGT', '', ocgt_max_price)

            ccgt_min_price = gr4spObj.settings.getPriceMinMWh('Natural Gas Pipeline Turbine - CCGT', '') * (
                    100.0 + priceChangePercentageCcgt) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Natural Gas Pipeline Turbine - CCGT', '', ccgt_min_price)

            ccgt_max_price = gr4spObj.settings.getPriceMaxMWh('Natural Gas Pipeline Turbine - CCGT', '') * (
                    100.0 + priceChangePercentageCcgt) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Natural Gas Pipeline Turbine - CCGT', '', ccgt_max_price)

            wind_min_price = gr4spObj.settings.getPriceMinMWh('Wind', '') * (
                    100.0 + priceChangePercentageWind) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Wind', '', wind_min_price)

            wind_max_price = gr4spObj.settings.getPriceMaxMWh('Wind', '') * (
                    100.0 + priceChangePercentageWind) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Wind', '', wind_max_price)

            water_min_price = gr4spObj.settings.getPriceMinMWh('Water', '') * (
                    100.0 + priceChangePercentageWater) / 100.0;
            gr4spObj.settings.setPriceMinMWh('Water', '', water_min_price)

            water_max_price = gr4spObj.settings.getPriceMaxMWh('Water', '') * (
                    100.0 + priceChangePercentageWater) / 100.0;
            gr4spObj.settings.setPriceMaxMWh('Water', '', water_max_price)

            # Capacity factors

            brown_coal_min_cf = gr4spObj.settings.getMinCapacityFactor('Brown Coal', '') * (
                    100.0 + capacityFactorChangeBrownCoal) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Brown Coal', '', brown_coal_min_cf)

            brown_coal_max_cf = gr4spObj.settings.getMaxCapacityFactor('Brown Coal', '') * (
                    100.0 + capacityFactorChangeBrownCoal) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Brown Coal', '', brown_coal_max_cf)

            battery_min_cf = gr4spObj.settings.getMinCapacityFactor('Battery', '') * (
                    100.0 + capacityFactorChangeBattery) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Battery', '', battery_min_cf)

            battery_max_cf = gr4spObj.settings.getMaxCapacityFactor('Battery', '') * (
                    100.0 + capacityFactorChangeBattery) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Battery', '', battery_max_cf)

            ocgt_min_cf = gr4spObj.settings.getMinCapacityFactor('Natural Gas Pipeline Turbine - OCGT', '') * (
                    100.0 + capacityFactorChangeOcgt) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Natural Gas Pipeline Turbine - OCGT', '', ocgt_min_cf)

            ocgt_max_cf = gr4spObj.settings.getMaxCapacityFactor('Natural Gas Pipeline Turbine - OCGT', '') * (
                    100.0 + capacityFactorChangeOcgt) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Natural Gas Pipeline Turbine - OCGT', '', ocgt_max_cf)

            ccgt_min_cf = gr4spObj.settings.getMinCapacityFactor('Natural Gas Pipeline Turbine - CCGT', '') * (
                    100.0 + capacityFactorChangeCcgt) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Natural Gas Pipeline Turbine - CCGT', '', ccgt_min_cf)

            ccgt_max_cf = gr4spObj.settings.getMaxCapacityFactor('Natural Gas Pipeline Turbine - CCGT', '') * (
                    100.0 + capacityFactorChangeCcgt) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Natural Gas Pipeline Turbine - CCGT', '', ccgt_max_cf)

            wind_min_cf = gr4spObj.settings.getMinCapacityFactor('Wind', '') * (
                    100.0 + capacityFactorChangeWind) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Wind', '', wind_min_cf)

            wind_max_cf = gr4spObj.settings.getMaxCapacityFactor('Wind', '') * (
                    100.0 + capacityFactorChangeWind) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Wind', '', wind_max_cf)

            water_min_cf = gr4spObj.settings.getMinCapacityFactor('Water', '') * (
                    100.0 + capacityFactorChangeWater) / 100.0;
            gr4spObj.settings.setMinCapacityFactor('Water', '', water_min_cf)

            water_max_cf = gr4spObj.settings.getMaxCapacityFactor('Water', '') * (
                    100.0 + capacityFactorChangeWater) / 100.0;
            gr4spObj.settings.setMaxCapacityFactor('Water', '', water_max_cf)

            # tariff components
            gr4spObj.settings.setUsageTariff('wholesaleContribution', (float) (wholesaleTariffContribution / 100.0))

            # arenas
            gr4spObj.settings.setMinCapMarketGen('scheduled', scheduleMinCapMarketGen)
            gr4spObj.settings.setMinCapMarketGen('semiScheduled', semiScheduleMinCapMarketGen)
            gr4spObj.settings.setMinCapMarketGen('nonScheduled', nonScheduleMinCapMarketGen)

            gr4spObj.settings.setSpotMarket('semiScheduled', semiScheduleGenSpotMarket)
            gr4spObj.settings.setSpotMarket('nonScheduled', nonScheduleGenSpotMarket)

            # Run JAVA Simulation
            gr4spObj.runFromPythonEMA()


    except java.lang.Exception as ex:
        print("Exception: " + ex)

    return getResults(outputID, experimentId)

