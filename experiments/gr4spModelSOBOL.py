import sys
import os

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
    # model.constants += [Constant('rooftopPV', 'residential')]
    model.constants += [Constant('priceChangePercentageBattery', 0)]
    #model.constants += [Constant('priceChangePercentageOcgt', 0)]
    model.constants += [Constant('priceChangePercentageCcgt', 0)]
    model.constants += [Constant('capacityFactorChangeBattery', 0)]
    #model.constants += [Constant('capacityFactorChangeOcgt', 0)]
    model.constants += [Constant('capacityFactorChangeCcgt', 0)]
    model.constants += [Constant('capacityFactorChangeWind', 0)]
    #model.constants += [Constant('scheduleMinCapMarketGen', 30)]


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

    # Uncertainties (4July2020 23 significant factors after EET using the union normalised mu* and sigma max and median values)
    model.uncertainties = [IntegerParameter('annualCpi', 1, 6)] #percentage
    model.uncertainties += [IntegerParameter('annualInflation', 1, 6)] #percentage
    model.uncertainties += [IntegerParameter('consumption', 0, 5)]
    model.uncertainties += [IntegerParameter('learningCurve', 0, 11)] #percentage

    # variation of contribution of networks, retail and other charges in the tariff
    model.uncertainties += [IntegerParameter('wholesaleTariffContribution', 11, 46)]

    model.uncertainties += [IntegerParameter('energyEfficiency', 0,3)]
    model.uncertainties += [IntegerParameter('solarUptake', 0, 5)]
    model.uncertainties += [IntegerParameter('rooftopPV', 5, 8)]

    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20,36)] # percentage

    model.uncertainties += [IntegerParameter('includePublicallyAnnouncedGen', 0, 2)]
    model.uncertainties += [IntegerParameter('generationRolloutPeriod', 0, 11)]
    model.uncertainties += [IntegerParameter('generatorRetirement', -5, 6)]
    model.uncertainties += [IntegerParameter('technologicalImprovement', 0, 11)] #percentage

    # The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
    # This could represent a subsidy
    # model.levers += [IntegerParameter('priceChangePercentageBattery', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -30, 31)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -30, 31)]
    # model.levers += [IntegerParameter('priceChangePercentageCcgt', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -30, 31)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -30, 31)]

    # variation of min and max capacity factors as a percentage of current values
    # model.levers += [IntegerParameter('capacityFactorChangeBattery', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeBrownCoal', -10, 11)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeOcgt', -10, 11)]
    # model.levers += [IntegerParameter('capacityFactorChangeCcgt', -10, 10)]
    # model.levers += [IntegerParameter('capacityFactorChangeWind', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeWater', -10, 11)]



# arenas

    model.uncertainties += [IntegerParameter('scheduleMinCapMarketGen', 10, 31)]
    model.uncertainties += [IntegerParameter('semiScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [RealParameter('semiScheduleMinCapMarketGen', 0.1, 30)]
    model.uncertainties += [IntegerParameter('nonScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [RealParameter('nonScheduleMinCapMarketGen', 0.1, 15)]

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
