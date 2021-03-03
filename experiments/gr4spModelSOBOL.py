import sys
import os
# This model apply changes from the present year (currently at 2019) to the future. (It uses VICFuture.yaml)
#From the last iteration of the EET, a set of 21 out of 34 input variables were identified as significant
# variables for the outpurs of interest.

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

import connectorSOBOL

from EMAworkbench.ema_workbench import (IntegerParameter, RealParameter, CategoricalParameter, BooleanParameter,
                                        ScalarOutcome, ArrayOutcome, TimeSeriesOutcome, Constant, Model)


def getModel():
    model = Model('Gr4sp', function=connectorSOBOL.runGr4sp)

    # specify uncertainties
    # model.uncertainties = [RealParameter('annualCpi', 0.01, 0.05)]
    # model.uncertainties += [RealParameter('annualInflation', 0.01, 0.05)]



    # arenas

# set levers
#
#     def category(i):
#         switcher={
#             0: 'Central',
#             1: 'Slow change',
#             2: 'Step change',
#             3: 'Fast change',
#             4: 'High DER',
#             5: 'residential',
#             6:'business',
#             7: 'both',
#             8: 'primary',
#             9: 'secondary',
#             10: 'none'
#         }
#         return switcher.get(i,"invalid category")

    # The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
    # This could represent a subsidy

    # Uncertainties (14Jan2021: EET with 34 parameters, from 1998 to 2050, changes in 1999 and 2019 (forecasts). 24 significant factors
    # using median results of mu* and sigma.
    # specify constants - levers deemed not significant by EET
    model.constants = [Constant('onsiteGeneration', 0)]
    model.constants += [Constant('rooftopPV', 7)]
    model.constants += [Constant('scheduleMinCapMarketGen', 30)]
    model.constants += [Constant('priceChangePercentageSolar', 0)]
    model.constants += [Constant('annualCpi', 2.33)]  # percentage
    model.constants += [Constant('annualInflation', 3.3)]  # percentage
    model.constants += [Constant('energyEfficiency', 0)]
    model.constants += [Constant('nameplateCapacityChangeCcgt', 0)]
    model.constants += [Constant('priceChangePercentageCcgt', 0)]
    model.constants += [Constant('generatorRetirement', 0)]


    model.uncertainties = [IntegerParameter('consumption', 0, 5)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('generationRolloutPeriod', 1, 11)]
    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20, 51)]  # percentage
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('wholesaleTariffContribution', 10, 46)] # ( 11, 45)
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('includePublicallyAnnouncedGen', 0, 2)]
    model.uncertainties += [IntegerParameter('nonScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWind', -50, 51)]
    model.uncertainties += [IntegerParameter('importPriceFactor', -50, 51)] #percentage from historic variations observed in OpenNem
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBattery', -50, 51)]
    model.uncertainties += [IntegerParameter('semiScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeSolar', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageBattery', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWater', -50, 51)]
    model.uncertainties += [IntegerParameter('technologicalImprovement', 0, 16)]  # percentage
    model.uncertainties += [IntegerParameter('solarUptake', 0, 5)]
    model.uncertainties += [IntegerParameter('semiScheduleMinCapMarketGen', 1, 301)]  # divided by 10
    model.uncertainties += [IntegerParameter('nonScheduleMinCapMarketGen', 1, 151)]  # divided by 10
    model.uncertainties += [IntegerParameter('learningCurve', 0, 16)]  # percentage








# specify outcomes
    model.outcomes = [
                        TimeSeriesOutcome('TIMEYear'),
                        TimeSeriesOutcome('consumptionYear'),
                        TimeSeriesOutcome('tariffsYear'),
                        TimeSeriesOutcome('primaryWholesalePriceYear'),
                        TimeSeriesOutcome('GHGYear'),
                        TimeSeriesOutcome('numConsumersYear'),
                        TimeSeriesOutcome('primarySpotProductionYear'),
                        TimeSeriesOutcome('secondarySpotProductionYear'),
                        TimeSeriesOutcome('offSpotProductionYear'),
                        TimeSeriesOutcome('renewableContributionYear'),
                        TimeSeriesOutcome('rooftopPVProductionYear'),
                        TimeSeriesOutcome('coalProductionYear'),
                        TimeSeriesOutcome('waterProductionYear'),
                        TimeSeriesOutcome('windProductionYear'),
                        TimeSeriesOutcome('gasProductionYear'),
                        TimeSeriesOutcome('solarProductionYear'),
                        TimeSeriesOutcome('BatteryProductionYear'),
                        TimeSeriesOutcome('numActorsYear'),
                        TimeSeriesOutcome('primaryUnmetDemandMwh'),
                        TimeSeriesOutcome('primaryUnmetDemandHours'),
                        TimeSeriesOutcome('primaryUnmetDemandDays'),
                        TimeSeriesOutcome('primaryMaxUnmetDemandMwhPerHour'),
                        TimeSeriesOutcome('secondaryUnmetDemandMwh'),
                        TimeSeriesOutcome('secondaryUnmetDemandHours'),
                        TimeSeriesOutcome('secondaryUnmetDemandDays'),
                        TimeSeriesOutcome('secondaryMaxUnmetDemandMwhPerHour'),
                        TimeSeriesOutcome('TIMEMonth'),
                        TimeSeriesOutcome('primaryWholesalePriceMonth'),
                        ScalarOutcome('seedExperimentCsv')
                      ]

    return model

## MODEL with changes only after BASE YEAR

def getModelAFterBaseYear():
    model = Model('Gr4sp', function=connectorSOBOL.runGr4spAfterBaseYear)


    model.constants = [Constant('onsiteGeneration', 0)]
    model.constants += [Constant('technologicalImprovement', 1)]  # percentage
    model.constants += [Constant('priceChangePercentageSolar', 0)]
    model.constants += [Constant('nameplateCapacityChangeCcgt', 0)]
    model.constants += [Constant('priceChangePercentageCcgt', 0)]

#Dec 2 results for AfterBaseYear changes. From 34 inputs, 29 important factors
    model.uncertainties = [IntegerParameter('consumption', 0, 5)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('generationRolloutPeriod', 1, 11)]
    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20, 51)]  # percentage
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('wholesaleTariffContribution', 10, 46)] # ( 11, 45)
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('includePublicallyAnnouncedGen', 0, 2)]
    model.uncertainties += [IntegerParameter('nonScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWind', -50, 51)]
    model.uncertainties += [IntegerParameter('importPriceFactor', -50, 51)] #percentage from historic variations observed in OpenNem
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBattery', -50, 51)]
    model.uncertainties += [IntegerParameter('semiScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeSolar', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageBattery', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWater', -50, 51)]
    model.uncertainties += [IntegerParameter('learningCurve', 0, 16)]  # percentage
    model.uncertainties += [IntegerParameter('solarUptake', 0, 5)]
    model.uncertainties += [IntegerParameter('nonScheduleMinCapMarketGen', 1, 151)] #divided by 10
    model.uncertainties += [IntegerParameter('semiScheduleMinCapMarketGen', 1, 301)] #divided by 10
    model.uncertainties += [IntegerParameter('generatorRetirement', -5, 6)]
    model.uncertainties += [IntegerParameter('annualInflation', 1, 6)]  # percentage
    model.uncertainties += [IntegerParameter('scheduleMinCapMarketGen',1, 301)] #divided by 10
    model.uncertainties += [IntegerParameter('rooftopPV', 5, 8)]
    model.uncertainties += [IntegerParameter('annualCpi', 1, 6)] #percentage
    model.uncertainties += [IntegerParameter('energyEfficiency', 0, 3)]










    model.outcomes = [
                        TimeSeriesOutcome('TIMEYear'),
                        TimeSeriesOutcome('consumptionYear'),
                        TimeSeriesOutcome('tariffsYear'),
                        TimeSeriesOutcome('primaryWholesalePriceYear'),
                        TimeSeriesOutcome('GHGYear'),
                        TimeSeriesOutcome('numConsumersYear'),
                        TimeSeriesOutcome('primarySpotProductionYear'),
                        TimeSeriesOutcome('secondarySpotProductionYear'),
                        TimeSeriesOutcome('offSpotProductionYear'),
                        TimeSeriesOutcome('renewableContributionYear'),
                        TimeSeriesOutcome('rooftopPVProductionYear'),
                        TimeSeriesOutcome('coalProductionYear'),
                        TimeSeriesOutcome('waterProductionYear'),
                        TimeSeriesOutcome('windProductionYear'),
                        TimeSeriesOutcome('gasProductionYear'),
                        TimeSeriesOutcome('solarProductionYear'),
                        TimeSeriesOutcome('BatteryProductionYear'),
                        TimeSeriesOutcome('numActorsYear'),
                        TimeSeriesOutcome('primaryUnmetDemandMwh'),
                        TimeSeriesOutcome('primaryUnmetDemandHours'),
                        TimeSeriesOutcome('primaryUnmetDemandDays'),
                        TimeSeriesOutcome('primaryMaxUnmetDemandMwhPerHour'),
                        TimeSeriesOutcome('secondaryUnmetDemandMwh'),
                        TimeSeriesOutcome('secondaryUnmetDemandHours'),
                        TimeSeriesOutcome('secondaryUnmetDemandDays'),
                        TimeSeriesOutcome('secondaryMaxUnmetDemandMwhPerHour'),
                        TimeSeriesOutcome('TIMEMonth'),
                        TimeSeriesOutcome('primaryWholesalePriceMonth'),
                        ScalarOutcome('seedExperimentCsv')
                      ]

    return model