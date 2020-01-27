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


# annualCpi, annualInflation, IncludePublicallyAnnouncedGen, generationRolloutPeriod, generatorRetirement, technologicalImprovement):

def runGr4sp(consumption, energyEfficiency, onsiteGeneration, solarUptake,  rooftopPV , priceMinMWh, priceMaxMWh, c, seed=None):
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
        # gr4spObj.settings.forecast.annualCpi = annualCpi
        # gr4spObj.settings.policy.annualInflation = annualInflation

        # Set Levers
        brownCoal = str('Brown Coal')
        gr4spObj.settings.forecast.scenario.consumption = consumption
        gr4spObj.settings.forecast.scenario.energyEfficiency = energyEfficiency
        gr4spObj.settings.forecast.scenario.onsiteGeneration = onsiteGeneration
        gr4spObj.settings.forecast.scenario.solarUptake = solarUptake
        gr4spObj.settings.forecast.rooftopPV = rooftopPV

        #gr4spObj.settings.generators.BrownCoal.priceMaxMWh = priceMaxMWh

        # gr4spObj.settings.forecast.scenario.IncludePublicallyAnnouncedGen = IncludePublicallyAnnouncedGen
        # gr4spObj.settings.forecast.scenario.generationRolloutPeriod = generationRolloutPeriod
        # gr4spObj.settings.forecast.scenario.generatorRetirement = generatorRetirement
        # gr4spObj.settings.forecast.scenario.technologicalImprovement = technologicalImprovement

        # Run JAVA Simulation
        gr4spObj.runFromPythonEMA()


    except java.lang.Exception as ex:
        print("Exception: " + ex)

    return getResults(outputID)
