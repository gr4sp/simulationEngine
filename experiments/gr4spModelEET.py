import sys
import os

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

import connector

from EMAworkbench.ema_workbench import (IntegerParameter, RealParameter, CategoricalParameter, BooleanParameter,
                                        ScalarOutcome, TimeSeriesOutcome, Constant, Model)


def getModel():
    model = Model('Gr4sp', function=connector.runGr4sp)

    # specify uncertainties
    # model.uncertainties = [RealParameter('annualCpi', 0.01, 0.05)]
    # model.uncertainties += [RealParameter('annualInflation', 0.01, 0.05)]

    # specify constants - levers deemed not significant by EET



# set levers

    model.levers = [CategoricalParameter('consumption', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [CategoricalParameter('energyEfficiency', ['Central', 'Slow change', 'Step change'])]
    model.levers += [CategoricalParameter('onsiteGeneration', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [CategoricalParameter('solarUptake', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [CategoricalParameter('rooftopPV', ['residential', 'business', 'both'])]

    model.levers += [RealParameter('annualCpi', 0.01, 0.05)]
    model.levers += [RealParameter('annualInflation', 0.01, 0.05)]

    model.levers += [BooleanParameter('includePublicallyAnnouncedGen')]
    model.levers += [IntegerParameter('generationRolloutPeriod', 0, 10)]
    model.levers += [IntegerParameter('generatorRetirement', -5, 5)]
    model.levers += [RealParameter('technologicalImprovement', 0.0, 0.1)]
    model.levers += [RealParameter('learningCurve', 0.0, 0.05)]

# The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
# This could represent a subsidy
    model.levers += [IntegerParameter('priceChangePercentageBattery', -30, 30)]
    model.levers += [IntegerParameter('priceChangePercentageBrownCoal', -30, 30)]
    model.levers += [IntegerParameter('priceChangePercentageOcgt', -30, 30)]
    model.levers += [IntegerParameter('priceChangePercentageCcgt', -30, 30)]
    model.levers += [IntegerParameter('priceChangePercentageWind', -30, 30)]
    model.levers += [IntegerParameter('priceChangePercentageWater', -30, 30)]

# variation of min and max capacity factors as a percentage of current values
    model.levers += [IntegerParameter('capacityFactorChangeBattery', -10, 10)]
    model.levers += [IntegerParameter('capacityFactorChangeBrownCoal', -10, 10)]
    model.levers += [IntegerParameter('capacityFactorChangeOcgt', -10, 10)]
    model.levers += [IntegerParameter('capacityFactorChangeCcgt', -10, 10)]
    model.levers += [IntegerParameter('capacityFactorChangeWind', -10, 10)]
    model.levers += [IntegerParameter('capacityFactorChangeWater', -10, 10)]

# variation of contribution of networks, retail and other charges in the tariff
    model.levers += [IntegerParameter('transmissionUsageChange', -10, 10)]
    model.levers += [IntegerParameter('distributionUsageChange', -10, 10)]
    model.levers += [IntegerParameter('retailUsageChange', -10, 10)]
    model.levers += [IntegerParameter('environmentalCostsChange', -10, 10)]

# arenas
    model.levers += [IntegerParameter('scheduleMinCapMarketGen', 10, 30)]
    model.levers += [CategoricalParameter('semiScheduleGenSpotMarket', ['primary', 'secondary', 'none'])]
    model.levers += [RealParameter('semiScheduleMinCapMarketGen', 0.1, 30)]
    model.levers += [CategoricalParameter('nonScheduleGenSpotMarket', ['primary', 'secondary', 'none'])]
    model.levers += [RealParameter('nonScheduleMinCapMarketGen', 0.1, 15)]

# specify outcomes
    model.outcomes = [TimeSeriesOutcome('TIMEMonth'),
                      TimeSeriesOutcome('consumptionMonth'),
                      TimeSeriesOutcome('tariffsMonth'),
                      TimeSeriesOutcome('wholesalePriceMonth'),
                      TimeSeriesOutcome('GHGMonth'),
                      TimeSeriesOutcome('numConsumersMonth'),
                      TimeSeriesOutcome('primarySpotProductionMonth'),
                      TimeSeriesOutcome('secondarySpotProductionMonth'),
                      TimeSeriesOutcome('offSpotProductionMonth'),
                      TimeSeriesOutcome('rooftopPVProductionMonth'),
                      TimeSeriesOutcome('numActorsMonth'),
                      TimeSeriesOutcome('TIMEYear'),
                      TimeSeriesOutcome('consumptionYear'),
                      TimeSeriesOutcome('tariffsYear'),
                      TimeSeriesOutcome('wholesalePriceYear'),
                      TimeSeriesOutcome('GHGYear'),
                      TimeSeriesOutcome('numConsumersYear'),
                      TimeSeriesOutcome('primarySpotProductionYear'),
                      TimeSeriesOutcome('secondarySpotProductionYear'),
                      TimeSeriesOutcome('offSpotProductionYear'),
                      TimeSeriesOutcome('rooftopPVProductionYear'),
                      TimeSeriesOutcome('numActorsYear')
                      ]

    return model
