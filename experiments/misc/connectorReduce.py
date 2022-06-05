# Import module
import jpype

# Enable Java imports
import jpype.imports

import pandas as pd
import numpy as np
from random import randint
import os
import json
import gc

'''
Create Java Virtual Machine (jpype.getDefaultJVMPath())
'''


def startJVM():
    with open('settingsExperiments.json') as f:
        settings = json.load(f)

    jvmpath = settings[settings["jvmPath"]]

    gr4spPath = os.getcwd() + "/.."

    gr4spPath = gr4spPath.replace("\\", "/")

    # Startup Jpype and import the messaging java package
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
                "{0}/libraries/yamlbeans-1.13.jar".format(
                    gr4spPath, classpathSeparator, settings["gr4spClasses"])

    # jpype.startJVM(jvmpath, classpath, "-Xmx8192M")  # 8GB
    # jpype.startJVM(jvmpath, classpath, "-Xmx1024M")  # 8GB
    jpype.startJVM(jvmpath, classpath)  # 8GB


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

    # csvFileName = '{0}{1}csv{1}VIC{1}NominalSimDataMonthlySummary{2}.csv'.format(gr4spPath, slash, outputID)
    # resultsMonth = pd.read_csv(csvFileName)
    #
    # timesMonth = resultsMonth['Time (Month)'].to_numpy()
    # wholesaleMonth = resultsMonth['Wholesale ($/MWh)'].to_numpy()

    csvFileName = '{0}{1}csv{1}Nominal{1}NominalSimDataYearSummary{2}.csv'.format(
        gr4spPath, slash, outputID)
    results = pd.read_csv(csvFileName)

    #
    # # Prepare time series
    timesYear = results['Time (Year)'].to_numpy()
    consumptionYear = results['Consumption (KWh) per household'].to_numpy()
    tariffsYear = results['Avg Tariff (c/KWh) per household'].to_numpy()
    wholesaleYear = results['Primary Wholesale ($/MWh)'].to_numpy()
    ghgYear = results['GHG Emissions (tCO2-e) per household'].to_numpy()
    numConsumersYear = results['Number of Domestic Consumers (households)'].to_numpy(
    )
    primarySpotYear = results['System Production Primary Spot'].to_numpy()
    secondarySpotYear = results['System Production Secondary Spot'].to_numpy()
    offSpotYear = results['System Production Off Spot'].to_numpy()
    renewableContributionYear = results['Percentage Renewable Production'].to_numpy(
    )
    rooftopPVProductionYear = results['System Production Rooftop PV'].to_numpy(
    )
    coalProductionYear = results['System Production Coal'].to_numpy()
    waterProductionYear = results['System Production Water'].to_numpy()
    windProductionYear = results['System Production Wind'].to_numpy()
    gasProductionYear = results['System Production Gas'].to_numpy()
    solarProductionYear = results['System Production Solar'].to_numpy()
    BatteryProductionYear = results['System Production Battery'].to_numpy()
    numActorsYear = results['Number of Active Actors'].to_numpy()
    primaryUnmetDemandMwh = results['Primary Total Unmet Demand (MWh)'].to_numpy(
    )
    primaryUnmetDemandHours = results['Primary Total Unmet Demand (Hours)'].to_numpy(
    )
    primaryUnmetDemandDays = results['Primary Total Unmet Demand (Days)'].to_numpy(
    )
    primaryMaxUnmetDemandMwhPerHour = results['Primary Max Unmet Demand Per Hour (MWh)'].to_numpy(
    )
    secondaryUnmetDemandMwh = results['Secondary Total Unmet Demand (MWh)'].to_numpy(
    )
    secondaryUnmetDemandHours = results['Secondary Total Unmet Demand (Hours)'].to_numpy(
    )
    secondaryUnmetDemandDays = results['Secondary Total Unmet Demand (Days)'].to_numpy(
    )
    secondaryMaxUnmetDemandMwhPerHour = results['Secondary Max Unmet Demand Per Hour (MWh)'].to_numpy(
    )
    seedExperimentCsv = float(experimentId)

    # shutdownJVM()

    return timesYear, consumptionYear, tariffsYear, wholesaleYear, ghgYear, numConsumersYear, primarySpotYear, \
        secondarySpotYear, offSpotYear, renewableContributionYear, rooftopPVProductionYear, coalProductionYear, \
        waterProductionYear, windProductionYear, gasProductionYear, solarProductionYear, BatteryProductionYear, \
        numActorsYear, primaryUnmetDemandMwh, primaryUnmetDemandHours, primaryUnmetDemandDays, primaryMaxUnmetDemandMwhPerHour, \
        secondaryUnmetDemandMwh, secondaryUnmetDemandHours, secondaryUnmetDemandDays, secondaryMaxUnmetDemandMwhPerHour, seedExperimentCsv


def category(i):
    switcher = {
        0: 'Central',
        1: 'Slow change',
        2: 'Step change',
        3: 'Fast change',
        4: 'High DER',
        5: 'residential',
        6: 'business',
        7: 'both',
        8: 'primary',
        9: 'secondary',
        10: 'none'
    }
    return switcher.get(i, "invalid category")

# LCOEs and CFs variations


def applyPercentageChange(percentageChange):
    return (100.0 + percentageChange) / 100.0


# Changes are applied to both xxx.yaml and xxxfuture.yaml
def runGr4sp(experimentId, annualCpi=2.33, annualInflation=3.3, consumption=0, energyEfficiency=0, onsiteGeneration=0, solarUptake=0, rooftopPV=7,
             domesticConsumptionPercentage=30, includePublicallyAnnouncedGen=0, generationRolloutPeriod=1, generatorRetirement=0, technologicalImprovement=1,
             learningCurve=5, importPriceFactor=29, priceChangePercentageBattery=0, priceChangePercentageBrownCoal=0, priceChangePercentageOcgt=0,
             priceChangePercentageCcgt=0, priceChangePercentageWind=0, priceChangePercentageWater=0, priceChangePercentageSolar=0,
             nameplateCapacityChangeBattery=0, nameplateCapacityChangeBrownCoal=0, nameplateCapacityChangeOcgt=0,
             nameplateCapacityChangeCcgt=0, nameplateCapacityChangeWind=0, nameplateCapacityChangeWater=0, nameplateCapacityChangeSolar=0, wholesaleTariffContribution=28.37,
             scheduleMinCapMarketGen=30, semiScheduleGenSpotMarket=8, semiScheduleMinCapMarketGen=30, nonScheduleGenSpotMarket=10,
             nonScheduleMinCapMarketGen=1):
    print("start", experimentId)
    if not jpype.isJVMStarted():
        print("JVM", experimentId)
        startJVM()
    # else:
    #     print("EHE")
    #     jpype.attachThreadToJVM()

    try:

        import java.lang

        results = None
        outputID = "_seed_{}".format(experimentId)

        # print(f"experimentId: {experimentId}\n, annualCpi: {annualCpi}\n, annualInflation: {annualInflation}\n, consumption: {category(consumption)}\n, consumption{consumption}\n, energyEfficiency: {category(energyEfficiency)}\n, energyEfficiency{energyEfficiency}\n, onsiteGeneration: {category(onsiteGeneration)}\n, onsiteGeneration{onsiteGeneration}\n, solarUptake: {category(solarUptake)}\n, solarUptake{solarUptake}\n, rooftopPV: {category(rooftopPV)}\n, rooftopPV{rooftopPV}\n")
        # print(f"domesticConsumptionPercentage: {domesticConsumptionPercentage}\n, includePublicallyAnnouncedGen: {includePublicallyAnnouncedGen}\n, generationRolloutPeriod: {generationRolloutPeriod}\n, generatorRetirement: {generatorRetirement}\n, technologicalImprovement:{technologicalImprovement}\n")
        # print(f"learningCurve: {learningCurve}\n, importPriceFactor: {importPriceFactor}\n, priceChangePercentageBattery: {priceChangePercentageBattery}\n, priceChangePercentageBrownCoal: {priceChangePercentageBrownCoal}\n, priceChangePercentageOcgt: {priceChangePercentageOcgt}\n")
        # print(f"priceChangePercentageCcgt: {priceChangePercentageCcgt}\n, priceChangePercentageWind: {priceChangePercentageWind}\n, priceChangePercentageWater: {priceChangePercentageWater}\n, priceChangePercentageSolar: {priceChangePercentageSolar}\n")
        # print(
        #     f"nameplateCapacityChangeBattery: {nameplateCapacityChangeBattery}\n, nameplateCapacityChangeBrownCoal: {nameplateCapacityChangeBrownCoal}\n, nameplateCapacityChangeOcgt: {nameplateCapacityChangeOcgt}\n")
        # print(f"nameplateCapacityChangeCcgt: {nameplateCapacityChangeCcgt}\n, nameplateCapacityChangeWind: {nameplateCapacityChangeWind}\n, nameplateCapacityChangeWater: {nameplateCapacityChangeWater}\n, nameplateCapacityChangeSolar: {nameplateCapacityChangeSolar}\n, wholesaleTariffContribution: {wholesaleTariffContribution}\n")
        # print(f"scheduleMinCapMarketGen: {scheduleMinCapMarketGen}\n, semiScheduleGenSpotMarket: {category(semiScheduleGenSpotMarket)}\n, semiScheduleGenSpotMarket{semiScheduleGenSpotMarket}\n, semiScheduleMinCapMarketGen: {semiScheduleMinCapMarketGen}\n, nonScheduleGenSpotMarket: {category(nonScheduleGenSpotMarket)}\n, nonScheduleGenSpotMarket{nonScheduleGenSpotMarket}\n")
        # print(f"nonScheduleMinCapMarketGen: {nonScheduleMinCapMarketGen}\n")

        # print("HERE", nonScheduleGenSpotMarket)
        # Check if file exists in the CSVs
        with open('settingsExperiments.json') as f:
            settings = json.load(f)
        slash = "\\"
        if settings["jvmPath"] == "jvmPathUbuntu":
            slash = "/"
        gr4spPath = os.getcwd() + slash + ".."
        csvFileName = '{0}{1}csv{1}Nominal{1}NominalSimDataMonthlySummary{2}.csv'.format(
            gr4spPath, slash, outputID)

        # If CSV doesn't exist, then run the simulation. This is usefule to resume failed EMA runs
        #################### REMOVE OR TRUE #######################
        if os.path.isfile(csvFileName) is False or True:

            gr4sp = jpype.JClass("core.Gr4spSim")
            # to identify each csv created in the simulation with an unique experiment number, instead of using seed = randint(0, 100000), we use the experiment number
            gr4spObj = gr4sp(experimentId)
            outputID = str(gr4spObj.outputID)
            print(outputID)

            # Set Uncertainties
            gr4spObj.settings.forecast.annualCpi = annualCpi / 100.0
            gr4spObj.settings.policy.annualInflation = annualInflation / 100.0

            gr4spObj.settings.forecast.scenario.consumption = category(
                consumption)
            gr4spObj.settings.forecast.scenario.energyEfficiency = category(
                energyEfficiency)
            gr4spObj.settings.forecast.scenario.onsiteGeneration = category(
                onsiteGeneration)
            gr4spObj.settings.forecast.scenario.solarUptake = category(
                solarUptake)
            gr4spObj.settings.forecast.rooftopPV = category(rooftopPV)

            gr4spObj.settings.population.domesticConsumptionPercentage = domesticConsumptionPercentage / 100.0

            gr4spObj.settings.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
                includePublicallyAnnouncedGen)
            gr4spObj.settings.forecast.generationRolloutPeriod = generationRolloutPeriod
            gr4spObj.settings.forecast.generatorRetirement = generatorRetirement
            gr4spObj.settings.forecast.technologicalImprovement = technologicalImprovement / 100.0
            gr4spObj.settings.forecast.learningCurve = learningCurve / 100.0
            gr4spObj.settings.forecast.importPriceFactor = importPriceFactor / 100.0

            # LCOEs and CFs variations
            brown_coal_base_price = gr4spObj.settings.getBasePriceMWh(
                'Brown Coal', '') * applyPercentageChange(priceChangePercentageBrownCoal)
            gr4spObj.settings.setBasePriceMWh(
                'Brown Coal', '', brown_coal_base_price)

            battery_base_price = gr4spObj.settings.getBasePriceMWh(
                'Battery', '') * applyPercentageChange(priceChangePercentageBattery)
            gr4spObj.settings.setBasePriceMWh(
                'Battery', '', battery_base_price)

            ocgt_base_price = gr4spObj.settings.getBasePriceMWh('Gas Pipeline Turbine - OCGT',
                                                                '') * applyPercentageChange(priceChangePercentageOcgt)
            gr4spObj.settings.setBasePriceMWh(
                'Gas Pipeline Turbine - OCGT', '', ocgt_base_price)

            ccgt_base_price = gr4spObj.settings.getBasePriceMWh('Gas Pipeline Turbine - CCGT',
                                                                '') * applyPercentageChange(priceChangePercentageCcgt)
            gr4spObj.settings.setBasePriceMWh(
                'Gas Pipeline Turbine - CCGT', '', ccgt_base_price)

            wind_base_price = gr4spObj.settings.getBasePriceMWh(
                'Wind', '') * applyPercentageChange(priceChangePercentageWind)
            gr4spObj.settings.setBasePriceMWh('Wind', '', wind_base_price)

            water_base_price = gr4spObj.settings.getBasePriceMWh(
                'Water', '') * applyPercentageChange(priceChangePercentageWater)
            gr4spObj.settings.setBasePriceMWh('Water', '', water_base_price)

            solar_base_price = gr4spObj.settings.getBasePriceMWh(
                'Solar', '') * applyPercentageChange(priceChangePercentageSolar)
            gr4spObj.settings.setBasePriceMWh('Solar', '', solar_base_price)

            # Nameplate Capacity Change
            brown_coal_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeBrownCoal)
            gr4spObj.settings.setNameplateCapacityChange(
                'Brown Coal', '', brown_coal_nameplate_change)

            battery_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeBattery)
            gr4spObj.settings.setNameplateCapacityChange(
                'Battery', '', battery_nameplate_change)

            ocgt_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeOcgt)
            gr4spObj.settings.setNameplateCapacityChange(
                'Gas Pipeline Turbine - OCGT', '', ocgt_nameplate_change)

            ccgt_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeCcgt)
            gr4spObj.settings.setNameplateCapacityChange(
                'Gas Pipeline Turbine - CCGT', '', ccgt_nameplate_change)

            wind_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeWind)
            gr4spObj.settings.setNameplateCapacityChange(
                'Wind', '', wind_nameplate_change)

            water_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeWater)
            gr4spObj.settings.setNameplateCapacityChange(
                'Water', '', water_nameplate_change)

            solar_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeSolar)
            gr4spObj.settings.setNameplateCapacityChange(
                'Solar', '', solar_nameplate_change)

            # tariff components
            gr4spObj.settings.setUsageTariff(
                'wholesaleContribution', (float)(wholesaleTariffContribution / 100.0))

            # arenas
            gr4spObj.settings.setMinCapMarketGen('scheduled',
                                                 (float)(scheduleMinCapMarketGen))
            gr4spObj.settings.setMinCapMarketGen('semiScheduled',
                                                 (float)(semiScheduleMinCapMarketGen))
            gr4spObj.settings.setMinCapMarketGen('nonScheduled',
                                                 (float)(nonScheduleMinCapMarketGen/10))

            gr4spObj.settings.setSpotMarket(
                'semiScheduled', category(semiScheduleGenSpotMarket))
            gr4spObj.settings.setSpotMarket(
                'nonScheduled', category(nonScheduleGenSpotMarket))

            # # UPDATE AFTER BASE YEAR

            # Set Uncertainties
            gr4spObj.settingsAfterBaseYear.forecast.annualCpi = annualCpi / 100.0
            gr4spObj.settingsAfterBaseYear.policy.annualInflation = annualInflation / 100.0

            gr4spObj.settingsAfterBaseYear.forecast.scenario.consumption = category(
                consumption)
            gr4spObj.settingsAfterBaseYear.forecast.scenario.energyEfficiency = category(
                energyEfficiency)
            gr4spObj.settingsAfterBaseYear.forecast.scenario.onsiteGeneration = category(
                onsiteGeneration)
            gr4spObj.settingsAfterBaseYear.forecast.scenario.solarUptake = category(
                solarUptake)
            # print(category(solarUptake), solarUptake, technologicalImprovement)
            gr4spObj.settingsAfterBaseYear.forecast.rooftopPV = category(
                rooftopPV)

            gr4spObj.settingsAfterBaseYear.population.domesticConsumptionPercentage = domesticConsumptionPercentage / 100.0

            gr4spObj.settingsAfterBaseYear.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
                includePublicallyAnnouncedGen)
            gr4spObj.settingsAfterBaseYear.forecast.generationRolloutPeriod = generationRolloutPeriod
            gr4spObj.settingsAfterBaseYear.forecast.generatorRetirement = generatorRetirement
            gr4spObj.settingsAfterBaseYear.forecast.technologicalImprovement = technologicalImprovement / 100.0
            gr4spObj.settingsAfterBaseYear.forecast.learningCurve = learningCurve / 100.0
            gr4spObj.settingsAfterBaseYear.forecast.importPriceFactor = importPriceFactor / 100.0

            # LCOEs and CFs variations
            brown_coal_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Brown Coal',
                                                                                   '') * applyPercentageChange(
                priceChangePercentageBrownCoal)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Brown Coal', '', brown_coal_base_price)

            battery_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Battery', '') * applyPercentageChange(
                priceChangePercentageBattery)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Battery', '', battery_base_price)

            ocgt_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - OCGT',
                                                                             '') * applyPercentageChange(
                priceChangePercentageOcgt)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Gas Pipeline Turbine - OCGT', '', ocgt_base_price)

            ccgt_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - CCGT',
                                                                             '') * applyPercentageChange(
                priceChangePercentageCcgt)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Gas Pipeline Turbine - CCGT', '', ccgt_base_price)

            wind_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Wind', '') * applyPercentageChange(
                priceChangePercentageWind)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Wind', '', wind_base_price)

            water_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Water', '') * applyPercentageChange(
                priceChangePercentageWater)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Water', '', water_base_price)

            solar_base_price = gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Solar', '') * applyPercentageChange(
                priceChangePercentageSolar)
            gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Solar', '', solar_base_price)

            # Nameplate Capacity Change
            brown_coal_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeBrownCoal)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Brown Coal', '', brown_coal_nameplate_change)

            battery_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeBattery)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Battery', '', battery_nameplate_change)

            ocgt_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeOcgt)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - OCGT', '',
                                                                      ocgt_nameplate_change)

            ccgt_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeCcgt)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - CCGT', '',
                                                                      ccgt_nameplate_change)

            wind_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeWind)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Wind', '', wind_nameplate_change)

            water_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeWater)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Water', '', water_nameplate_change)

            solar_nameplate_change = applyPercentageChange(
                nameplateCapacityChangeSolar)
            gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Solar', '', solar_nameplate_change)

            # tariff components
            gr4spObj.settingsAfterBaseYear.setUsageTariff('wholesaleContribution',
                                                          (float)(wholesaleTariffContribution / 100.0))

            # # arenas
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('scheduled',
                                                              (float)(scheduleMinCapMarketGen))
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('semiScheduled',
                                                              (float)(semiScheduleMinCapMarketGen))
            gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('nonScheduled',
                                                              (float)(nonScheduleMinCapMarketGen/10))

            gr4spObj.settingsAfterBaseYear.setSpotMarket(
                'semiScheduled', category(semiScheduleGenSpotMarket))
            gr4spObj.settingsAfterBaseYear.setSpotMarket(
                'nonScheduled', category(nonScheduleGenSpotMarket))

            # Run JAVA Simulation
            gr4spObj.runFromPythonEMA()

            gr4spObj.cleanup()

    except java.lang.Exception as ex:
        print("Exception: " + ex)

    # shutdownJVM()
    del gr4spObj

    gc.collect()

    return getResults(outputID, experimentId)
