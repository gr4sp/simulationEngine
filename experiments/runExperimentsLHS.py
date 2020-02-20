import os, sys

sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

<<<<<<<< HEAD:experiments/runExperimentsLHS.py
from gr4spModelLHS import getModel
========
from gr4spModelSOBOL import getModel
>>>>>>>> a792cb8 (EET and SOBOL files separated):experiments/runExperimentSOBOLs.py
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

<<<<<<<< HEAD:experiments/runExperimentsLHS.py
    with MultiprocessingEvaluator(model,n_processes=1) as evaluator:
========
    with MultiprocessingEvaluator(model,n_processes=24) as evaluator:
>>>>>>>> a792cb8 (EET and SOBOL files separated):experiments/runExperimentSOBOLs.py
        ## Variance-based SA
        #results = evaluator.perform_experiments(scenarios=0, policies=1050, levers_sampling=SOBOL)

        ## Generate Variance-based SA Policies, and save them into a file
<<<<<<<< HEAD:experiments/runExperimentsLHS.py
        #results = evaluator.perform_experiments(scenarios=0, policies=2200, levers_sampling=LHS, generate_policy_file_only=True)

        results = evaluator.perform_experiments(scenarios=0, policies=1, levers_sampling='policies.lhs.object')
========
        #results = evaluator.perform_experiments(scenarios=0, policies=1050, levers_sampling=SOBOL, generate_policy_file_only=True)

        results = evaluator.perform_experiments(scenarios=0, policies=1, levers_sampling='policies.sobol.object')
>>>>>>>> a792cb8 (EET and SOBOL files separated):experiments/runExperimentSOBOLs.py

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

    save_results(results, r'./data/gr4sp_' + datekey + '.tar.gz')
