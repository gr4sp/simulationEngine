# Import module
import jpype

# Enable Java imports
import jpype.imports
from datetime import datetime
import pandas as pd
import numpy as np
from random import randint
import os
import json
import gc
import sys
import time
from multiprocessing import current_process
# import globals
'''
Create Java Virtual Machine (jpype.getDefaultJVMPath())
'''
from discord_webhook import DiscordWebhook

discord_url = ''


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
    jpype.startJVM(jvmpath, classpath, "-Xmx8192M")  # 8GB


def shutdownJVM():
    jpype.shutdownJVM()


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
             nonScheduleMinCapMarketGen=1, a0=12, a1=12, a2=12, a3=12, a4=12, a5=12, a6=12, a7=12):
    count = 0
    start = time.time()

    if not jpype.isJVMStarted():
        # print("JVM", experimentId)
        startJVM()
    # else:
    #     print("EHE")
    #     jpype.attachThreadToJVM()
    import java.lang
    while True:
        try:

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

                # Set pathway
                from java.util import ArrayList
                from jpype.types import JInt
                pathway = ArrayList([JInt(a0), JInt(a1), JInt(a2), JInt(a3),
                                     JInt(a4), JInt(a5), JInt(a6), JInt(a7)])
                # for i in range(8):
                #     pathway.add(JInt(kwargs[str(i)]))
                gr4spObj.tippingPoint = True
                gr4spObj.policyManager.setQueue(pathway)

                gr4spObj.policyManager.setPathway(pathway)

                # Change output to save DAPP
                gr4spObj.saveDAPP = True

                # Set database to read from

                db_id = (int(current_process().name.split("-")[1]) % 2) + 1
                # db_id = 2

                gr4spObj.db_id = db_id

                # Run JAVA Simulation
                gr4spObj.runFromPythonEMA()
                # gr4spObj.cleanup()
                #del gr4spObj
                # gc.collect()
                # ghg = np.mean(list(gr4spObj.saveData.dappGHGDAPP))
                # renew = np.mean(list(gr4spObj.saveData.dappRenewDAPP))
                # tariff = np.mean(list(gr4spObj.saveData.dappTariffDAPP))
                # wholsesale = np.mean(list(gr4spObj.saveData.dappWholesaleDAPP))
                # unmet = np.mean(list(gr4spObj.saveData.dappUnmetDaysDAPP))

                ghg = np.float64(gr4spObj.ghg)
                renew = np.float64(gr4spObj.renew)
                tariff = np.float64(gr4spObj.tariff)
                wholsesale = np.float64(gr4spObj.whole)
                unmet = np.float64(gr4spObj.unmet)

                # ghg, renew, tariff, wholsesale, unmet = getResults2(
                #     outputID, experimentId)

                # ghg = np.mean(ghg)
                # renew = np.mean(renew)
                # tariff = np.mean(tariff)
                # wholsesale = np.mean(wholsesale)
                # unmet = np.mean(unmet)

                # if tariff != tariff:
                #raise Exception(str(experimentId))
                # msg = x = f"{a}experimentId: {experimentId}\n, annualCpi: {annualCpi}\n, annualInflation: {annualInflation}\n, consumption: {category(consumption)}\n, consumption{consumption}\n, energyEfficiency: {category(energyEfficiency)}\n, energyEfficiency{energyEfficiency}\n, onsiteGeneration: {category(onsiteGeneration)}\n, onsiteGeneration{onsiteGeneration}\n, solarUptake: {category(solarUptake)}\n, solarUptake{solarUptake}\n, rooftopPV: {category(rooftopPV)}\n, rooftopPV{rooftopPV}\n" + f"domesticConsumptionPercentage: {domesticConsumptionPercentage}\n, includePublicallyAnnouncedGen: {includePublicallyAnnouncedGen}\n, generationRolloutPeriod: {generationRolloutPeriod}\n, generatorRetirement: {generatorRetirement}\n, technologicalImprovement:{technologicalImprovement}\n" + f"learningCurve: {learningCurve}\n, importPriceFactor: {importPriceFactor}\n, priceChangePercentageBattery: {priceChangePercentageBattery}\n, priceChangePercentageBrownCoal: {priceChangePercentageBrownCoal}\n, priceChangePercentageOcgt: {priceChangePercentageOcgt}\n" + f"priceChangePercentageCcgt: {priceChangePercentageCcgt}\n, priceChangePercentageWind: {priceChangePercentageWind}\n, priceChangePercentageWater: {priceChangePercentageWater}\n, priceChangePercentageSolar: {priceChangePercentageSolar}\n" + f"nameplateCapacityChangeBattery: {nameplateCapacityChangeBattery}\n, nameplateCapacityChangeBrownCoal: {nameplateCapacityChangeBrownCoal}\n, nameplateCapacityChangeOcgt: {nameplateCapacityChangeOcgt}\n" + f"nameplateCapacityChangeCcgt: {nameplateCapacityChangeCcgt}\n, nameplateCapacityChangeWind: {nameplateCapacityChangeWind}\n, nameplateCapacityChangeWater: {nameplateCapacityChangeWater}\n, nameplateCapacityChangeSolar: {nameplateCapacityChangeSolar}\n, wholesaleTariffContribution: {wholesaleTariffContribution}\n" + f"scheduleMinCapMarketGen: {scheduleMinCapMarketGen}\n, semiScheduleGenSpotMarket: {category(semiScheduleGenSpotMarket)}\n, semiScheduleGenSpotMarket{semiScheduleGenSpotMarket}\n, semiScheduleMinCapMarketGen: {semiScheduleMinCapMarketGen}\n, nonScheduleGenSpotMarket: {category(nonScheduleGenSpotMarket)}\n, nonScheduleGenSpotMarket{nonScheduleGenSpotMarket}\n" + f"nonScheduleMinCapMarketGen: {nonScheduleMinCapMarketGen}\n"
                #webhook = DiscordWebhook(url=discord_url, content=msg)
                #response = webhook.execute()
                # sys.exit()
                # else:
                # print(tariff)

                gr4spObj.cleanup()
                del gr4spObj

                gc.collect()

        except java.lang.Exception as ex:

            del gr4spObj

            gc.collect()
            count += 1
            if count > 10:
                print("10 times", experimentId)
                # webhook = DiscordWebhook(
                #     url=discord_url, content="More than 10")
                # response = webhook.execute()
                return np.float64(5.645205538353292), np.float64(0.27847617061830227), \
                    np.float64(18.64332747909258), np.float64(
                    73.76962489152105), np.float64(54.735849056603776)
            # print(count)
            continue

            # print(datetime.now())
            # print(f"{a0},{a1},{a2},{a3},{a4},{a5},{a6},{a7}, experimentId: {experimentId}\n, annualCpi: {annualCpi}\n, annualInflation: {annualInflation}\n, consumption: {category(consumption)}\n, consumption{consumption}\n, energyEfficiency: {category(energyEfficiency)}\n, energyEfficiency{energyEfficiency}\n, onsiteGeneration: {category(onsiteGeneration)}\n, onsiteGeneration{onsiteGeneration}\n, solarUptake: {category(solarUptake)}\n, solarUptake{solarUptake}\n, rooftopPV: {category(rooftopPV)}\n, rooftopPV{rooftopPV}\n" + f"domesticConsumptionPercentage: {domesticConsumptionPercentage}\n, includePublicallyAnnouncedGen: {includePublicallyAnnouncedGen}\n, generationRolloutPeriod: {generationRolloutPeriod}\n, generatorRetirement: {generatorRetirement}\n, technologicalImprovement:{technologicalImprovement}\n" + f"learningCurve: {learningCurve}\n, importPriceFactor: {importPriceFactor}\n, priceChangePercentageBattery: {priceChangePercentageBattery}\n, priceChangePercentageBrownCoal: {priceChangePercentageBrownCoal}\n, priceChangePercentageOcgt: {priceChangePercentageOcgt}\n" +
            #       f"priceChangePercentageCcgt: {priceChangePercentageCcgt}\n, priceChangePercentageWind: {priceChangePercentageWind}\n, priceChangePercentageWater: {priceChangePercentageWater}\n, priceChangePercentageSolar: {priceChangePercentageSolar}\n" + f"nameplateCapacityChangeBattery: {nameplateCapacityChangeBattery}\n, nameplateCapacityChangeBrownCoal: {nameplateCapacityChangeBrownCoal}\n, nameplateCapacityChangeOcgt: {nameplateCapacityChangeOcgt}\n" + f"nameplateCapacityChangeCcgt: {nameplateCapacityChangeCcgt}\n, nameplateCapacityChangeWind: {nameplateCapacityChangeWind}\n, nameplateCapacityChangeWater: {nameplateCapacityChangeWater}\n, nameplateCapacityChangeSolar: {nameplateCapacityChangeSolar}\n, wholesaleTariffContribution: {wholesaleTariffContribution}\n" + f"scheduleMinCapMarketGen: {scheduleMinCapMarketGen}\n, semiScheduleGenSpotMarket: {category(semiScheduleGenSpotMarket)}\n, semiScheduleGenSpotMarket{semiScheduleGenSpotMarket}\n, semiScheduleMinCapMarketGen: {semiScheduleMinCapMarketGen}\n, nonScheduleGenSpotMarket: {category(nonScheduleGenSpotMarket)}\n, nonScheduleGenSpotMarket{nonScheduleGenSpotMarket}\n" + f"nonScheduleMinCapMarketGen: {nonScheduleMinCapMarketGen}\n")
            # print("Exception seed", outputID, experimentId)
            # print("Exception: " + ex)
            # # webhook = DiscordWebhook(url=discord_url, content="Stopped")
            # # response = webhook.execute()
            # sys.exit(int(experimentId))
            # return np.float64(5.645205538353292), np.float64(0.27847617061830227), \
            #     np.float64(18.64332747909258), np.float64(
            #         73.76962489152105), np.float64(54.735849056603776)
        # print("Success", time.time() - start)

        names = ['ghg', 'renew', 'tariff', 'wholsesale', 'unmet']

        for k, i in enumerate([ghg, renew, tariff, wholsesale, unmet]):
            bad = False
            if i != i:
                bad = True
                print("nan", experimentId, names[k])

            if np.isnan(i):
                bad = True
                print("np.nan", experimentId, names[k])

            if i == 0:
                bad = True
                print("Zero", experimentId, names[k])

            # Return default val for this run
            if bad:
                if k == 0:
                    ghg = np.float64(5.645205538353292)
                if k == 1:
                    renew = np.float64(0.27847617061830227)
                if k == 2:
                    tariff = np.float64(18.64332747909258)
                if k == 3:
                    wholsesale = np.float64(73.76962489152105)
                if k == 4:
                    unmet = np.float64(54.735849056603776)

        # print(ghg, renew, tariff, wholsesale, unmet)
        # if count > 5:
        #     print(experimentId, count)
        return ghg, renew, tariff, wholsesale, unmet
