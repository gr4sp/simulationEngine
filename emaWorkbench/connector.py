
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
    
def getResults( outputID ):
    
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
    times = results['Time (Month)'].to_numpy()
    consumption = results['Consumption (KWh) per household'].to_numpy()
    tariffs = results[' Avg Tariff (c/KWh) per household'].to_numpy()
    wholesale = results['Wholesale ($/MWh)'].to_numpy()
    ghg = results['GHG Emissions (tCO2-e) per household'].to_numpy()
    numConsumers = results['Number of Domestic Consumers (households)'].to_numpy()
    primarySpot = results['System Production Primary Spot'].to_numpy()
    secondarySpot = results['System Production Secondary Spot'].to_numpy()
    offSpot = results['System Production Off Spot'].to_numpy()
    rooftopPV = results['System Production Rooftop PV'].to_numpy()
    numActors = results['Number of Active Actors'].to_numpy()
    
    return times, consumption, tariffs, wholesale, ghg, numConsumers, primarySpot, \
           secondarySpot, offSpot, rooftopPV, numActors

    
def runGr4sp( annualCpi,l, c, seed=None):
    startJVM()

    try:


        import java.lang

        results = None
        outputID = None

        gr4sp = jpype.JClass("core.Gr4spSim")
        seed = randint(0,100000)
        gr4spObj = gr4sp(seed)
        outputID = str(gr4spObj.outputID)
        print(outputID)
        gr4spObj.settings.forecast.annualCpi = annualCpi
        gr4spObj.runFromPythonEMA()


    except java.lang.Exception as ex:
        print("Exception: "+ex)


    return getResults(outputID)

