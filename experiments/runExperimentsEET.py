import os, sys

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

from gr4spModelEET import getModel
from datetime import date

if __name__ == '__main__':
    '''
    Setup EMA Uncertainties, Levers and Outcomes
    '''
    model = getModelAFterBaseYear()

    '''
    Run EMA
    '''

    from EMAworkbench.ema_workbench import (SequentialEvaluator, MultiprocessingEvaluator, ema_logging, perform_experiments)
    from EMAworkbench.ema_workbench.em_framework.evaluators import (MC, LHS, FAST, FF, PFF, SOBOL, MORRIS)

    ema_logging.log_to_stderr(ema_logging.INFO)

    with MultiprocessingEvaluator(model,n_processes=79) as evaluator:
        ## EET
        ## Generate EET Policies, and save them into a file
        #results = evaluator.perform_experiments(scenarios=200, policies=0, uncertainty_sampling=MORRIS, generate_experiments_file_only=True)

        # Run from file
        results = evaluator.perform_experiments(scenarios=1, policies=0, uncertainty_sampling='uncertainties.morris.object')

        # Sample using Morris
        #results = evaluator.perform_experiments(scenarios=200, policies=0, uncertainty_sampling=MORRIS) #levers + 1 * policies


        #Testing
        #results = evaluator.perform_experiments(scenarios=0, policies=2)

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

    save_results(results, r'./simulationData/gr4sp_EET' + datekey + '.tar.gz')
