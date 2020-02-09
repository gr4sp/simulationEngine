# Import module
import jpype

# Enable Java imports
import jpype.imports

import pandas as pd
from random import randint

'''
Create Java Virtual Machine (jpype.getDefaultJVMPath())
'''


def startJVM():
    jvmpath = "C:\\Program Files\\JetBrains\\IntelliJ IDEA 2019.1.3\\jbr\\bin\\server\\jvm.dll"

    ## Startup Jpype and import the messaging java package
    if jpype.isJVMStarted():
        return

    jpype.startJVM(jvmpath,
                   "-Djava.class.path=C:/Users/angel/Documents/GitHub/gr4sp/src/out/production/gr4sp;C:/Users/angel/Documents/GitHub/gr4sp/libraries/bsh-2.0b4.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/itext-1.2.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/j3dcore.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/j3dutils.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/jcommon-1.0.21.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/jfreechart-1.0.17.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/jmf.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/mason.19.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/portfolio.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/sqlite-jdbc-3.23.1.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/vecmath.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/postgresql-42.2.6.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/opencsv-4.6.jar;C:/Users/angel/Documents/GitHub/gr4sp/libraries/yamlbeans-1.13.jar",
                   "-Xmx2048M"
                   )


def shutdownJVM():
    jpype.shutdownJVM()


def getResults(outputID):
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

    csvFileName = 'C:\\Users\\angel\\Documents\\GitHub\\gr4sp\\csv\\BAUVIC\\BAUVICSimDataMonthlySummary' + outputID + '.csv'
    results = pd.read_csv(csvFileName)

    # Prepare time series
    timesMonth = results['Time (Month)'].to_numpy()
    consumptionMonth = results['Consumption (KWh) per household'].to_numpy()
    tariffsMonth = results[' Avg Tariff (c/KWh) per household'].to_numpy()
    wholesaleMonth = results['Wholesale ($/MWh)'].to_numpy()
    ghgMonth = results['GHG Emissions (tCO2-e) per household'].to_numpy()
    numConsumersMonth = results['Number of Domestic Consumers (households)'].to_numpy()
    primarySpotMonth = results['System Production Primary Spot'].to_numpy()
    secondarySpotMonth = results['System Production Secondary Spot'].to_numpy()
    offSpotMonth = results['System Production Off Spot'].to_numpy()
    rooftopPVMonth = results['System Production Rooftop PV'].to_numpy()
    numActorsMonth = results['Number of Active Actors'].to_numpy()

    csvFileName = 'C:\\Users\\angel\\Documents\\GitHub\\gr4sp\\csv\\BAUVIC\\BAUVICSimDataYearSummary' + outputID + '.csv'
    results = pd.read_csv(csvFileName)

    # Prepare time series
    timesYear = results['Time (Year)'].to_numpy()
    consumptionYear = results['Consumption (KWh) per household'].to_numpy()
    tariffsYear = results[' Avg Tariff (c/KWh) per household'].to_numpy()
    wholesaleYear = results['Wholesale ($/MWh)'].to_numpy()
    ghgYear = results['GHG Emissions (tCO2-e) per household'].to_numpy()
    numConsumersYear = results['Number of Domestic Consumers (households)'].to_numpy()
    primarySpotYear = results['System Production Primary Spot'].to_numpy()
    secondarySpotYear = results['System Production Secondary Spot'].to_numpy()
    offSpotYear = results['System Production Off Spot'].to_numpy()
    rooftopPVYear = results['System Production Rooftop PV'].to_numpy()
    numActorsYear = results['Number of Active Actors'].to_numpy()

    return timesMonth, consumptionMonth, tariffsMonth, wholesaleMonth, ghgMonth, numConsumersMonth, primarySpotMonth, \
           secondarySpotMonth, offSpotMonth, rooftopPVMonth, numActorsMonth, \
           timesYear, consumptionYear, tariffsYear, wholesaleYear, ghgYear, numConsumersYear, primarySpotYear, \
           secondarySpotYear, offSpotYear, rooftopPVYear, numActorsYear


def runGr4sp(annualCpi, annualInflation, consumption, energyEfficiency, onsiteGeneration, solarUptake, rooftopPV,
             includePublicallyAnnouncedGen, generationRolloutPeriod, generatorRetirement, technologicalImprovement,
             learningCurve, priceChangePercentageBattery, priceChangePercentageBrownCoal, priceChangePercentageOcgt,
             priceChangePercentageCcgt, priceChangePercentageWind, priceChangePercentageWater,
             capacityFactorChangeBattery, capacityFactorChangeBrownCoal, capacityFactorChangeOcgt,
             capacityFactorChangeCcgt, capacityFactorChangeWind, capacityFactorChangeWater, transmissionUsageChange,
             distributionUsageChange, retailUsageChange, environmentalCostsChange, scheduleMinCapMarketGen,
             semiScheduleGenSpotMarket, semiScheduleMinCapMarketGen, nonScheduleGenSpotMarket,
             nonScheduleMinCapMarketGen, seed=None):
    startJVM()

    try:

        import java.lang

        results = None
        outputID = None

        gr4sp = jpype.JClass("core.Gr4spSim")
        # to identify each csv created in the simulation with an unique seed number
        seed = randint(0, 100000)
        gr4spObj = gr4sp(seed)
        outputID = str(gr4spObj.outputID)
        print(outputID)

        # Set Uncertainties
        gr4spObj.settings.forecast.annualCpi = annualCpi
        gr4spObj.settings.policy.annualInflation = annualInflation

        # Set Levers

        gr4spObj.settings.forecast.scenario.consumption = consumption
        gr4spObj.settings.forecast.scenario.energyEfficiency = energyEfficiency
        gr4spObj.settings.forecast.scenario.onsiteGeneration = onsiteGeneration
        gr4spObj.settings.forecast.scenario.solarUptake = solarUptake
        gr4spObj.settings.forecast.rooftopPV = rooftopPV

        gr4spObj.settings.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
            includePublicallyAnnouncedGen)
        gr4spObj.settings.forecast.generationRolloutPeriod = generationRolloutPeriod
        gr4spObj.settings.forecast.generatorRetirement = generatorRetirement
        gr4spObj.settings.forecast.technologicalImprovement = technologicalImprovement
        gr4spObj.settings.forecast.learningCurve = learningCurve

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
        transmission_costs_change = gr4spObj.settings.getUsageTariff('transmissionCosts') * (
                100.0 + transmissionUsageChange) / 100.0;
        gr4spObj.settings.setUsageTariff('transmissionCosts', transmission_costs_change)

        distribution_costs_change = gr4spObj.settings.getUsageTariff('distributionCosts') * (
                100.0 + distributionUsageChange) / 100.0;
        gr4spObj.settings.setUsageTariff('distributionCosts', distribution_costs_change)

        retail_costs_change = gr4spObj.settings.getUsageTariff('retailCosts') * (
                100.0 + retailUsageChange) / 100.0;
        gr4spObj.settings.setUsageTariff('retailCosts', retail_costs_change)

        environmental_costs_change = gr4spObj.settings.getUsageTariff('environmentalCosts') * (
                100.0 + environmentalCostsChange) / 100.0;
        gr4spObj.settings.setUsageTariff('environmentalCosts', environmental_costs_change)

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

    return getResults(outputID)
