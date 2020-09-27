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
    model.uncertainties += [IntegerParameter('importPriceFactor', -60, 15)] #percentage from historic variations observed in OpenNem

# The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
# This could represent a subsidy. A constant variability was assumed before for all the price changes of -30 to 30; and
# for CF of -10 to 10 percent.
# Last update (16/09/2020) A more specific set bounds is defined depending on the technology type.
# Max and min values, both for LCOEs and CFs were found in different sources
# (e.g. https://aemo.com.au/-/media/files/electricity/nem/planning_and_forecasting/inputs-assumptions-methodologies/2019/csiro-gencost2019-20_draftforreview.pdf?la=en).

    model.uncertainties += [IntegerParameter('priceChangePercentageBattery', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageBrownCoal', -10, 60)]
    model.uncertainties += [IntegerParameter('priceChangePercentageOcgt', -10, 10)]
    model.uncertainties += [IntegerParameter('priceChangePercentageCcgt', -30, 30)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWind', -30, 20)]
    model.uncertainties += [IntegerParameter('priceChangePercentageWater', -20, 50)]
    model.uncertainties += [IntegerParameter('priceChangePercentageSolar', -30,30)]

# variation of min and max capacity factors as a percentage of current values
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeBattery', -10, 10)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeBrownCoal', -30, 5)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeOcgt', -20, 20)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeCcgt', -5, 25)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeWind', -10, 20)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeWater', -15, 50)]
    model.uncertainties += [IntegerParameter('nameplaceCapacityChangeSolar', -5, 20)]

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
