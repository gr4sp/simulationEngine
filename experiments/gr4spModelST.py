import sys
import os

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

import connectorST

from EMAworkbench.ema_workbench import (IntegerParameter, RealParameter, CategoricalParameter, BooleanParameter,
                                        ScalarOutcome, TimeSeriesOutcome, Constant, Model)
#mode with varaible change before base year and future changes using forecast after base year
def getModelAfterBaseYear():
    model = Model('Gr4sp', function=connectorST.runGr4spAfterBaseYear)
    # set uncertainties according to first PRIM and FS resutls
    model.uncertainties += [CategoricalParameter('generatorRetirement', [-5, -2, 0, 1])]
    model.uncertainties += [CategoricalParameter('priceChangePercentageWater', [-45, -20, 0, 10, 35])]
    model.uncertainties += [CategoricalParameter('nameplateCapacityChangeWind', [-20, -10, 0, 35, 50])]
    model.uncertainties += [IntegerParameter('semiScheduleGenSpotMarket', 8, 10)]
    model.uncertainties += [IntegerParameter('nonScheduleGenSpotMarket', 9, 10)] #PRIM's box 6 worst cases (avoiding non-scehdule in markets)
    model.uncertainties += [CategoricalParameter('generationRolloutPeriod', [1, 3, 5, 7, 10])]
    model.uncertainties += [CategoricalParameter('nameplateCapacityChangeBrownCoal', [-30, -20, -10, 0, 20])]
    model.uncertainties += [CategoricalParameter('wholesaleTariffContribution', [10, 20, 30, 40, 50])]  # ( 11, 45) BAU 0.2837



    # set inputs as constants for BAU
    model.constants += [Constant('consumption', 0)]
    model.constants += [Constant('annualCpi', 2.33)] #percentage BAU 2.33
    model.constants += [Constant('annualInflation', 3.3)]  # percentage
    model.constants += [Constant('energyEfficiency', 0)]
    model.constants += [Constant('solarUptake', 0)]
    model.constants += [Constant('onsiteGeneration', 0)]
    model.constants += [Constant('rooftopPV', 7)]
    model.constants += [Constant('includePublicallyAnnouncedGen', 1)]  #PRIM's output include publically announced
    model.constants += [Constant('domesticConsumptionPercentage', 30)] #percentage (15, 35)
    model.constants += [Constant('technologicalImprovement', 1)] #percentage
    model.constants += [Constant('learningCurve', 5)] #percentage
    model.constants += [Constant('importPriceFactor', 29)] #percentage from historic variations observed in OpenNem

# The variation on LCOEs are achieved increasing or decreasing a percentage depending on the type of fuel
# This could represent a subsidy. A constant variability was assumed before for all the price changes of -30 to 30; and
# for CF of -10 to 10 percent.
# Update (28/09/2020) The price change percentage will affect the newly created 'basePrice' variable.
# Max and min values, both for LCOEs and CFs were found in different sources
# (e.g. https://aemo.com.au/-/media/files/electricity/nem/planning_and_forecasting/inputs-assumptions-methodologies/2019/csiro-gencost2019-20_draftforreview.pdf?la=en).
    model.constants += [Constant('priceChangePercentageBrownCoal', 0)]
    model.constants += [Constant('priceChangePercentageBattery', 0)]
    model.constants += [Constant('priceChangePercentageOcgt', 0)]
    model.constants += [Constant('priceChangePercentageCcgt', 0)]
    model.constants += [Constant('priceChangePercentageSolar', 0)]
    model.constants += [Constant('priceChangePercentageWind', 0)]

# variation of nameplate capacity as a percentage of current values
    model.constants += [Constant('nameplateCapacityChangeBattery', 0)]
    model.constants += [Constant('nameplateCapacityChangeOcgt', 0)]
    model.constants += [Constant('nameplateCapacityChangeCcgt', 0)]
    model.constants += [Constant('nameplateCapacityChangeSolar', 0)]
    model.constants += [Constant('nameplateCapacityChangeWater', 0)]


# variation of contribution of networks, retail and other charges in the tariff
    #model.constants += [Constant('wholesaleTariffContribution', 28.37)] # ( 11, 45) BAU 0.2837

# arenas
    model.constants += [Constant('scheduleMinCapMarketGen', 300)]
    model.constants += [Constant('semiScheduleMinCapMarketGen', 300)]
    model.constants += [Constant('nonScheduleMinCapMarketGen', 1)]

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
        ScalarOutcome('seedExperimentCsv')
                      ]

    return model

