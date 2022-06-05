from discord_webhook import DiscordWebhook
import time
import random
import numpy as np
from datetime import date
from gr4spModelPathway import getModel
import os
import sys
import pandas as pd
sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

discord_url = ''


np.random.seed(0)
random.seed(0)
start = time.time()

if __name__ == '__main__':
    '''
    Setup EMA Uncertainties, Levers and Outcomes
    '''
    model = getModel()
    #model = getModelAFterBaseYear()

    '''
    Run EMA
    '''

    from EMAworkbench.ema_workbench import (
        SequentialEvaluator, MultiprocessingEvaluator, ema_logging, perform_experiments)
    from EMAworkbench.ema_workbench.em_framework.evaluators import (
        MC, LHS, FAST, FF, PFF, SOBOL, MORRIS)
    from EMAworkbench.ema_workbench.em_framework.samplers import sample_uncertainties, sample_levers

    ema_logging.log_to_stderr(ema_logging.INFO)

    # Load training scenarios
    df = pd.read_csv("scenarios/pathway_nfe_280521.csv")
    n_scenarios = len(df)

    scenarios_designs = list(df.to_records(index=False))
    scenarios = sample_uncertainties(model, n_scenarios)
    scenarios.designs = scenarios_designs

    levers_df = pd.read_csv("levers/pathway_nfe_220521.csv")
    levers_designs = list(levers_df.to_records(index=False))
    levers = sample_levers(model, 10)
    levers.designs = levers_designs
    with MultiprocessingEvaluator(model, n_processes=45) as evaluator:
        # Run from file
        results = evaluator.perform_experiments(
            scenarios=scenarios, policies=levers)

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

    save_results(results, r'./simulationData/nfe_eval_poldefault' +
                 datekey + '.tar.gz')
    experiments.to_csv("nfe_eval_default.csv")

    print(time.time() - start)

    webhook = DiscordWebhook(url=discord_url, content="Finished")
    response = webhook.execute()
