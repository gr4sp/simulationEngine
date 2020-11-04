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

    # specify constants - levers deemed not significant by EET
    model.constants = [Constant('onsiteGeneration', 'Central')]
    model.constants += [Constant('technologicalImprovement', 10)]  # percentage
    model.constants += [Constant('solarUptake', 0)]
    model.constants += [Constant('generatorRetirement', 0)]
    model.constants += [Constant('semiScheduleMinCapMarketGen', 30)]
    model.constants += [Constant('rooftopPV', 7)]
    model.constants += [Constant('nonScheduleMinCapMarketGen', 0.1)]
    model.constants += [Constant('scheduleMinCapMarketGen', 30)]
    model.constants += [Constant('priceChangePercentageSolar', 0)]
    model.constants += [Constant('learningCurve', 5)]  # percentage
    model.constants += [Constant('annualCpi', 1)]  # percentage
    model.constants += [Constant('annualInflation', 1)]  # percentage
    model.constants += [Constant('energyEfficiency', 0)]

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

    # Uncertainties (26Oct2020: EET with 34 parameters, from 1998 to 2050, changes from 2019. 21 significant factors
    # using median results of mu* and sigma.

    model.uncertainties = [IntegerParameter('consumption', 0, 6)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeCcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('generationRolloutPeriod', 1, 11)]
    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20, 56)]  # percentage
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
    model.uncertainties += [IntegerParameter('priceChangePercentageCcgt', -50, 51)]


# specify outcomes
    model.outcomes = [
                        TimeSeriesOutcome('TIMEYear'),
                        TimeSeriesOutcome('consumptionYear'),
                        TimeSeriesOutcome('tariffsYear'),
                        TimeSeriesOutcome('wholesalePriceYear'),
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
                        ScalarOutcome('seedExperimentCsv')
                      ]

    return model
