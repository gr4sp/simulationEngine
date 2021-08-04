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
    tariffsYear = results['Avg Tariff (c/KWh) per household'].to_numpy()
    wholesaleYear = results['Primary Wholesale ($/MWh)'].to_numpy()
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


# LCOEs and CFs variations
def applyPercentageChange(percentageChange):
    return (100.0 + percentageChange) / 100.0
    # factor = 100.0 + abs(percentageChange) / 100.0
    # if percentageChange < 0.0:
    #     return 1.0 / factor
    # else:
    #     return factor

def runGr4sp(experimentId, annualCpi, annualInflation, consumption, energyEfficiency, onsiteGeneration, solarUptake, rooftopPV,
             domesticConsumptionPercentage, includePublicallyAnnouncedGen, generationRolloutPeriod, generatorRetirement, technologicalImprovement,
             learningCurve, importPriceFactor, priceChangePercentageBattery, priceChangePercentageBrownCoal, priceChangePercentageOcgt,
             priceChangePercentageCcgt, priceChangePercentageWind, priceChangePercentageWater, priceChangePercentageSolar,
             nameplateCapacityChangeBattery, nameplateCapacityChangeBrownCoal, nameplateCapacityChangeOcgt,
             nameplateCapacityChangeCcgt, nameplateCapacityChangeWind, nameplateCapacityChangeWater, nameplateCapacityChangeSolar, wholesaleTariffContribution,
             scheduleMinCapMarketGen, semiScheduleGenSpotMarket, semiScheduleMinCapMarketGen, nonScheduleGenSpotMarket,
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


            # Run JAVA Simulation
            gr4spObj.runFromPythonEMA()


    except java.lang.Exception as ex:
        print("Exception: " + ex)

    return getResults(outputID, experimentId)


