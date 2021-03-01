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
            gr4spObj.settings.forecast.importPriceFactor = importPriceFactor / 100.0

            # # LCOEs and CFs variations

            brown_coal_base_price = gr4spObj.settings.getBasePriceMWh('Brown Coal', '') * applyPercentageChange(priceChangePercentageBrownCoal)
            gr4spObj.settings.setBasePriceMWh('Brown Coal', '', brown_coal_base_price)

            battery_base_price = gr4spObj.settings.getBasePriceMWh('Battery', '') * applyPercentageChange(priceChangePercentageBattery)
            gr4spObj.settings.setBasePriceMWh('Battery', '', battery_base_price)

            ocgt_base_price = gr4spObj.settings.getBasePriceMWh('Gas Pipeline Turbine - OCGT', '') * applyPercentageChange(priceChangePercentageOcgt)
            gr4spObj.settings.setBasePriceMWh('Gas Pipeline Turbine - OCGT', '', ocgt_base_price)

            ccgt_base_price = gr4spObj.settings.getBasePriceMWh('Gas Pipeline Turbine - CCGT', '') * applyPercentageChange(priceChangePercentageCcgt)
            gr4spObj.settings.setBasePriceMWh('Gas Pipeline Turbine - CCGT', '', ccgt_base_price)

            wind_base_price = gr4spObj.settings.getBasePriceMWh('Wind', '') * applyPercentageChange(priceChangePercentageWind)
            gr4spObj.settings.setBasePriceMWh('Wind', '', wind_base_price)

            water_base_price = gr4spObj.settings.getBasePriceMWh('Water', '') * applyPercentageChange(priceChangePercentageWater)
            gr4spObj.settings.setBasePriceMWh('Water', '', water_base_price)

            solar_base_price = gr4spObj.settings.getBasePriceMWh('Solar', '') * applyPercentageChange(priceChangePercentageSolar)
            gr4spObj.settings.setBasePriceMWh('Solar', '', solar_base_price)

            # Nameplate Capacity Change

            brown_coal_nameplate_change = applyPercentageChange(nameplateCapacityChangeBrownCoal)
            gr4spObj.settings.setNameplateCapacityChange('Brown Coal', '', brown_coal_nameplate_change)

            battery_nameplate_change = applyPercentageChange(nameplateCapacityChangeBattery)
            gr4spObj.settings.setNameplateCapacityChange('Battery', '', battery_nameplate_change)

            ocgt_nameplate_change = applyPercentageChange(nameplateCapacityChangeOcgt)
            gr4spObj.settings.setNameplateCapacityChange('Gas Pipeline Turbine - OCGT', '', ocgt_nameplate_change)

            ccgt_nameplate_change = applyPercentageChange(nameplateCapacityChangeCcgt)
            gr4spObj.settings.setNameplateCapacityChange('Gas Pipeline Turbine - CCGT', '', ccgt_nameplate_change)

            wind_nameplate_change = applyPercentageChange(nameplateCapacityChangeWind)
            gr4spObj.settings.setNameplateCapacityChange('Wind', '', wind_nameplate_change)

            water_nameplate_change = applyPercentageChange(nameplateCapacityChangeWater)
            gr4spObj.settings.setNameplateCapacityChange('Water', '', water_nameplate_change)

            solar_nameplate_change = applyPercentageChange(nameplateCapacityChangeSolar)
            gr4spObj.settings.setNameplateCapacityChange('Solar', '', solar_nameplate_change)



            # tariff components
            gr4spObj.settings.setUsageTariff('wholesaleContribution', (float) (wholesaleTariffContribution / 100.0))

            # arenas
            gr4spObj.settings.setMinCapMarketGen('scheduled', scheduleMinCapMarketGen)
            gr4spObj.settings.setMinCapMarketGen('semiScheduled', semiScheduleMinCapMarketGen)
            gr4spObj.settings.setMinCapMarketGen('nonScheduled', nonScheduleMinCapMarketGen)

            gr4spObj.settings.setSpotMarket('semiScheduled', semiScheduleGenSpotMarket)
            gr4spObj.settings.setSpotMarket('nonScheduled', nonScheduleGenSpotMarket)


            # UPDATE AFTER BASE YEAR SETTINGS

            # Set Uncertainties
            gr4spObj.settingsAfterBaseYear.forecast.annualCpi = annualCpi / 100.0
            gr4spObj.settingsAfterBaseYear.policy.annualInflation = annualInflation / 100.0

            gr4spObj.settingsAfterBaseYear.forecast.scenario.consumption = consumption
            gr4spObj.settingsAfterBaseYear.forecast.scenario.energyEfficiency = energyEfficiency
            gr4spObj.settingsAfterBaseYear.forecast.scenario.onsiteGeneration = onsiteGeneration
            gr4spObj.settingsAfterBaseYear.forecast.scenario.solarUptake = solarUptake
            gr4spObj.settingsAfterBaseYear.forecast.rooftopPV = rooftopPV

            gr4spObj.settingsAfterBaseYear.population.domesticConsumptionPercentage = domesticConsumptionPercentage / 100.0

            gr4spObj.settingsAfterBaseYear.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
                includePublicallyAnnouncedGen)
            gr4spObj.settingsAfterBaseYear.forecast.generationRolloutPeriod = generationRolloutPeriod
            gr4spObj.settingsAfterBaseYear.forecast.generatorRetirement = generatorRetirement
            gr4spObj.settingsAfterBaseYear.forecast.technologicalImprovement = technologicalImprovement / 100.0
            gr4spObj.settingsAfterBaseYear.forecast.learningCurve = learningCurve / 100.0
            gr4spObj.settingsAfterBaseYear.forecast.importPriceFactor = importPriceFactor / 100.0

            # # LCOEs and CFs variations

            brown_coal_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Brown Coal',
                                                                                   '') * applyPercentageChange(
                priceChangePercentageBrownCoal)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Brown Coal', '', brown_coal_base_price)

            battery_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Battery', '') * applyPercentageChange(
                priceChangePercentageBattery)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Battery', '', battery_base_price)

            ocgt_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - OCGT',
                                                                             '') * applyPercentageChange(
                priceChangePercentageOcgt)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Gas Pipeline Turbine - OCGT', '', ocgt_base_price)

            ccgt_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - CCGT',
                                                                             '') * applyPercentageChange(
                priceChangePercentageCcgt)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Gas Pipeline Turbine - CCGT', '', ccgt_base_price)

            wind_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Wind', '') * applyPercentageChange(
                priceChangePercentageWind)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Wind', '', wind_base_price)

            water_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Water', '') * applyPercentageChange(
                priceChangePercentageWater)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Water', '', water_base_price)

            solar_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Solar', '') * applyPercentageChange(
                priceChangePercentageSolar)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh('Solar', '', solar_base_price)

            # Nameplate Capacity Change

            brown_coal_nameplate_change = applyPercentageChange(nameplateCapacityChangeBrownCoal)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Brown Coal', '', brown_coal_nameplate_change)

            battery_nameplate_change = applyPercentageChange(nameplateCapacityChangeBattery)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Battery', '', battery_nameplate_change)

            ocgt_nameplate_change = applyPercentageChange(nameplateCapacityChangeOcgt)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - OCGT', '',
                                                                      ocgt_nameplate_change)

            ccgt_nameplate_change = applyPercentageChange(nameplateCapacityChangeCcgt)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - CCGT', '',
                                                                      ccgt_nameplate_change)

            wind_nameplate_change = applyPercentageChange(nameplateCapacityChangeWind)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Wind', '', wind_nameplate_change)

            water_nameplate_change = applyPercentageChange(nameplateCapacityChangeWater)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Water', '', water_nameplate_change)

            solar_nameplate_change = applyPercentageChange(nameplateCapacityChangeSolar)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Solar', '', solar_nameplate_change)

            # tariff components
            gr4spObj.settingsAfterBaseYear.setUsageTariff('wholesaleContribution',
                                                          (float)(wholesaleTariffContribution / 100.0))

            # arenas
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('scheduled', scheduleMinCapMarketGen)
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('semiScheduled', semiScheduleMinCapMarketGen)
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('nonScheduled', nonScheduleMinCapMarketGen)

            gr4spObj.settingsAfterBaseYear.setSpotMarket('semiScheduled', semiScheduleGenSpotMarket)
            gr4spObj.settingsAfterBaseYear.setSpotMarket('nonScheduled', nonScheduleGenSpotMarket)

            # Run JAVA Simulation
            gr4spObj.runFromPythonEMA()


    except java.lang.Exception as ex:
        print("Exception: " + ex)

    return getResults(outputID, experimentId)
