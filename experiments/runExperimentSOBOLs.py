import os, sys

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

from gr4spModelSOBOL import getModel
from datetime import date

if __name__ == '__main__':
    '''
    Setup EMA Uncertainties, Levers and Outcomes
    '''
    model = getModel()

    '''
    Run EMA
    '''

    from EMAworkbench.ema_workbench import (SequentialEvaluator, MultiprocessingEvaluator, ema_logging, perform_experiments)
    from EMAworkbench.ema_workbench.em_framework.evaluators import (MC, LHS, FAST, FF, PFF, SOBOL, MORRIS)

    ema_logging.log_to_stderr(ema_logging.INFO)

    #######################################################with MultiprocessingEvaluator(model,n_processes=24) as evaluator:
    with MultiprocessingEvaluator(model, n_processes=31) as evaluator:

        ## Generate Variance-based SA Policies, and save them into a file
        #results = evaluator.perform_experiments(scenarios=1024, policies=0, uncertainty_sampling=SOBOL, generate_experiments_file_only=True)

        results = evaluator.perform_experiments(scenarios=1, policies=0, uncertainty_sampling='uncertainties.sobol.object')

        #Testing
        #results = evaluator.perform_experiments(scenarios=0, policies=20)

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
    from EMAworkbench.ema_workbench import save_results

    save_results(results, r'./simulationData/gr4sp_' + datekey + '.tar.gz')
