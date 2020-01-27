import connector

from ema_workbench import (IntegerParameter, RealParameter, CategoricalParameter, BooleanParameter, ScalarOutcome,
                           TimeSeriesOutcome, Constant, Model)

from datetime import date

if __name__ == '__main__':
    '''
    Setup EMA Uncertainties, Levers and Outcomes
    '''
    model = Model('Gr4sp', function=connector.runGr4sp)

    # specify uncertainties
    # model.uncertainties = [RealParameter('annualCpi', 0.01, 0.05)]
    # model.uncertainties = [RealParameter('annualInflation', 0.01, 0.05)]

    # set levers Forecast.scenario
    # model.levers = [CategoricalParameter('consumption', ['Central', 'Slow change', 'Fast change','Step change', 'High DER']),
    #                 CategoricalParameter('energyEfficiency', ['Central', 'Slow change', 'Step change']),
    #                 CategoricalParameter('onsiteGeneration', ['Central', 'Slow change','Fast change', 'Step change','High DER']),
    #                 CategoricalParameter('solarUptake',['Central', 'Slow change', 'Fast change', 'Step change', 'High DER']),
    #                 BooleanParameter('IncludePublicallyAnnouncedGen'),
    #                 IntegerParameter('generationRolloutPeriod', 0, 10),
    #                 CategoricalParameter('rooftopPV', ['residential', 'business', 'both']),
    #                 IntegerParameter('generatorRetirement', -5, 5),
    #                 RealParameter('technologicalImprovement', 0.0, 0.1),
    #
    #                 ]

    # override some of the defaults of the model
    model.constants = [Constant('c', 0.41)]
    # model.constants += [Constant('consumption', 'Central')]
    # model.constants += [Constant('energyEfficiency', 'Central')]
    # model.constants += [Constant('onsiteGeneration', 'Central')]
    # model.constants += [Constant('rooftopPV', 'both')]

    model.levers = [
        CategoricalParameter('consumption', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [CategoricalParameter('energyEfficiency', ['Central', 'Slow change', 'Step change'])]
    model.levers += [
        CategoricalParameter('onsiteGeneration', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [
        CategoricalParameter('solarUptake', ['Central', 'Slow change', 'Fast change', 'Step change', 'High DER'])]
    model.levers += [CategoricalParameter('rooftopPV', ['residential', 'business', 'both'])]

    # model.levers += [BooleanParameter('IncludePublicallyAnnouncedGen', [True, False])]
    # model.levers += [IntegerParameter('generationRolloutPeriod', [0, 10])]
    # model.levers += [IntegerParameter, ('generatorRetirement', [-5, 5])]
    # model.levers += [RealParameter, ('technologicalImprovement', [0.0, 0.1])]

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

    '''
    Run EMA
    '''

    from ema_workbench import (SequentialEvaluator, MultiprocessingEvaluator, ema_logging, perform_experiments)
    from ema_workbench.em_framework.evaluators import (MC, LHS, FAST, FF, PFF, SOBOL, MORRIS)

    ema_logging.log_to_stderr(ema_logging.INFO)

    with MultiprocessingEvaluator(model) as evaluator:
        results = evaluator.perform_experiments(scenarios=0, policies=50, levers_sampling=MORRIS)

    '''
    Print Results
    '''

    experiments, outcomes = results
    print(experiments.shape)
    print(list(outcomes.keys()))

    '''
    Save Results
    '''

    # Month abbreviation, day and year
    today = date.today()
    datekey = today.strftime("%Y-%b-%d")
    from ema_workbench import save_results

    save_results(results, r'./data/gr4sp_' + datekey + '.tar.gz')
