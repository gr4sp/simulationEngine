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
    model.constants += [Constant('rooftopPV', 7)]
    model.constants += [Constant('priceChangePercentageBattery', 0)]
    model.constants += [Constant('priceChangePercentageCcgt', 0)]
    model.constants += [Constant('nameplateCapacityChangeBattery', 0)]
    model.constants += [Constant('nameplateCapacityChangeCcgt', 0)]
    model.constants += [Constant('annualCpi', 1)]  # percentage
    model.constants += [Constant('annualInflation', 1)]  # percentage
    model.constants += [Constant('consumption', 0)]
    model.constants += [Constant('learningCurve', 5)]  # percentage

    # variation of contribution of networks, retail and other charges in the tariff
    model.constants += [Constant('wholesaleTariffContribution', 28)]

    model.constants += [Constant('energyEfficiency', 0)]
    model.constants += [Constant('solarUptake', 0)]

    model.constants += [Constant('includePublicallyAnnouncedGen', 1)]
    model.constants += [Constant('generationRolloutPeriod', 1)]
    model.constants += [Constant('generatorRetirement', 0)]
    model.constants += [Constant('importPriceFactor', 29)]

    # arenas

    model.constants += [Constant('scheduleMinCapMarketGen', 30)]
    model.constants += [Constant('semiScheduleMinCapMarketGen', 30)]

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

    # The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
    # This could represent a subsidy

    # Uncertainties (06Sep2020 15 significant factors after EET for past behaviour
    # using the union normalised mu* and sigma median values)
    model.uncertainties += [IntegerParameter('priceChangePercentageSolar', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeSolar', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWater', -50, 51)]
    model.uncertainties += [RealParameter('nonScheduleMinCapMarketGen', 0.1, 15)]
    model.uncertainties += [IntegerParameter('technologicalImprovement', 0, 16)]  # percentage
    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20, 56)]  # percentage
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeBrownCoal', -50, 51)]
    model.uncertainties += [IntegerParameter('nonScheduleGenSpotMarket', 8,11)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -50, 51)]
    model.uncertainties += [IntegerParameter('nameplateCapacityChangeWind', -50, 51)]
    model.uncertainties += [IntegerParameter('semiScheduleGenSpotMarket', 8,11)]





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
