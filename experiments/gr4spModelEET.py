import sys
import os

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

import connector

from EMAworkbench.ema_workbench import (IntegerParameter, RealParameter, CategoricalParameter, BooleanParameter,
                                        ScalarOutcome, TimeSeriesOutcome, Constant, Model)


def getModel():
    model = Model('Gr4sp', function=connector.runGr4sp)

# set levers/uncertainties
    model.uncertainties = [CategoricalParameter('consumption', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.uncertainties += [CategoricalParameter('energyEfficiency', ['Central', 'Slow change', 'Step change'])]
    model.uncertainties += [CategoricalParameter('onsiteGeneration', ['Central', 'Slow change', 'Step change'])]
    model.uncertainties += [CategoricalParameter('solarUptake', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.uncertainties += [CategoricalParameter('rooftopPV', ['residential', 'business', 'both'])]

    model.uncertainties += [IntegerParameter('domesticConsumptionPercentage', 20,35)] # percentage

    model.uncertainties += [IntegerParameter('annualCpi', 1, 5)] #percentage
    model.uncertainties += [IntegerParameter('annualInflation', 1, 5)] #percentage


    model.uncertainties += [IntegerParameter('includePublicallyAnnouncedGen', 0, 1)]
    model.uncertainties += [IntegerParameter('generationRolloutPeriod', 0, 10)]
    model.uncertainties += [IntegerParameter('generatorRetirement', -5, 5)]
    model.uncertainties += [IntegerParameter('technologicalImprovement', 0, 10)] #percentage
    model.uncertainties += [IntegerParameter('learningCurve', 0, 10)] #percentage

# The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
# This could represent a subsidy
    model.uncertainties += [IntegerParameter('priceChangePercentageBattery', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageCcgt', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -30, 30)]

# variation of min and max capacity factors as a percentage of current values
    model.uncertainties += [IntegerParameter('capacityFactorChangeBattery', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeBrownCoal', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeOcgt', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeCcgt', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeWind', -10, 10)]
    model.uncertainties += [IntegerParameter('capacityFactorChangeWater', -10, 10)]

# variation of contribution of networks, retail and other charges in the tariff
    model.uncertainties += [IntegerParameter('wholesaleTariffContribution', 11, 45)]

# arenas
    model.uncertainties += [IntegerParameter('scheduleMinCapMarketGen', 10, 30)]
    model.uncertainties += [CategoricalParameter('semiScheduleGenSpotMarket', ['primary', 'secondary', 'none'])]
    model.uncertainties += [RealParameter('semiScheduleMinCapMarketGen', 0.1, 30)]
    model.uncertainties += [CategoricalParameter('nonScheduleGenSpotMarket', ['primary', 'secondary', 'none'])]
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
