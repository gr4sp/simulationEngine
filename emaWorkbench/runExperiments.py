import connector

from ema_workbench import (RealParameter, ScalarOutcome, TimeSeriesOutcome, Constant, Model)

from datetime import date



if __name__ == '__main__':

    '''
    Setup EMA Uncertainties, Levers and Outcomes
    '''
    model = Model('Gr4sp', function=connector.runGr4sp)

    # specify uncertainties
    model.uncertainties = [RealParameter('annualCpi', 0.01, 0.5)]

    # set levers
    model.levers = [RealParameter("l", 0, 1)]

    # specify outcomes
    model.outcomes = [TimeSeriesOutcome('TIME'),
                      TimeSeriesOutcome('consumption'),
                      TimeSeriesOutcome('tariffs'),
                      TimeSeriesOutcome('wholesalePrice'),
                      TimeSeriesOutcome('GHG'),
                      TimeSeriesOutcome('numConsumers'),
                      TimeSeriesOutcome('primarySpotProduction'),
                      TimeSeriesOutcome('secondarySpotProduction'),
                      TimeSeriesOutcome('offSpotProduction'),
                      TimeSeriesOutcome('rooftopPVProduction'),
                      TimeSeriesOutcome('numActors')]

    # override some of the defaults of the model
    model.constants = [Constant('c', 0.41)]



    '''
    Run EMA
    '''
    from ema_workbench import (SequentialEvaluator, MultiprocessingEvaluator, ema_logging, perform_experiments)
    ema_logging.log_to_stderr(ema_logging.INFO)

    with MultiprocessingEvaluator(model) as evaluator:
        results = evaluator.perform_experiments(scenarios=5, policies=1)


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

    save_results(results, r'./data/gr4sp_'+datekey+'.tar.gz')
    
