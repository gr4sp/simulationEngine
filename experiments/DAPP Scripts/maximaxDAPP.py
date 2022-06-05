import time
import random
import numpy as np
from datetime import date
from gr4spModelPathway import getModel
import os
import sys
import pandas as pd
import pickle

from discord_webhook import DiscordWebhook

discord_url = ''


sys.path.append(r'{}\EMAworkbench'.format(os.getcwd()))

np.random.seed(0)
random.seed(0)

file_prefix = 'maximax'

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
    from EMAworkbench.ema_workbench.em_framework.optimization import (HyperVolume,
                                                                      EpsilonProgress)
    from EMAworkbench.ema_workbench import (RealParameter, ScalarOutcome, Constant, IntegerParameter,
                                            Model)

    ema_logging.log_to_stderr(ema_logging.INFO)

    # Load training scenarios
    df = pd.read_csv("scenarios/pathway_nfe_280521.csv")
    n_scenarios = 100

    scenarios_designs = list(df.to_records(index=False))
    scenarios = sample_uncertainties(model, n_scenarios)
    scenarios.designs = scenarios_designs[:n_scenarios]

    convergence_metrics = [EpsilonProgress()]

    import functools

    get_min = functools.partial(np.min)
    get_max = functools.partial(np.max)

    MAXIMIZE = ScalarOutcome.MAXIMIZE
    MINIMIZE = ScalarOutcome.MINIMIZE
    robustnes_functions = [ScalarOutcome(f'{file_prefix} ghg', kind=MINIMIZE,
                                         variable_name='ghg', function=get_min),
                           ScalarOutcome(f'{file_prefix} renew', kind=MAXIMIZE,
                                         variable_name='renew', function=get_max),
                           ScalarOutcome(f'{file_prefix} tariff', kind=MINIMIZE,
                                         variable_name='tariff', function=get_min),
                           ScalarOutcome(f'{file_prefix} wholsesale', kind=MINIMIZE,
                                         variable_name='wholsesale', function=get_min),
                           ScalarOutcome(f'{file_prefix} unmet', kind=MINIMIZE,
                                         variable_name='unmet', function=get_min)]

    # with SequentialEvaluator(model) as evaluator:
    curr = 0
    nfe_per_round = 1
    count = 0

    # Restart from max opt
    start_opt = 0
    files = os.listdir('optimisers')
    files = list(filter(lambda a: 'pickle' in a and file_prefix in a, files))
    if not files:
        start_opt = 0
    else:
        checks = list(
            map(lambda a: int(a.split("checkpoint")[1].split(".")[0]), files))
        start_opt = max(checks)

    for _ in range(18):
        count += 1

        last_check = ((count-1) * nfe_per_round) + start_opt

        print("Start round", count)
        start = time.time()

        webhook = DiscordWebhook(
            url=discord_url, content=f"Start round {count}")
        response = webhook.execute()
        if os.path.exists(f'./optimisers/{file_prefix}_ea_checkpoint{last_check}.pickle'):
            with open(f'./optimisers/{file_prefix}_ea_checkpoint{last_check}.pickle', 'rb') as fh:
                restart_optimizer = pickle.load(fh)
            restart_optimizer.problem.function = lambda _: restart_optimizer.problem.function
            print(f"Restart opt from {last_check} nfes")
            webhook = DiscordWebhook(
                url=discord_url, content=f"Restart opt from {last_check} nfes")
            response = webhook.execute()
        else:
            restart_optimizer = None
            print("Start opt")
            webhook = DiscordWebhook(
                url=discord_url, content=f"Start opt")
            response = webhook.execute()

        # with SequentialEvaluator(model) as evaluator:
        # TODO: Test runtimes using differing logging/convergence freqs
        # with SequentialEvaluator(model) as evaluator:
        try:
            with MultiprocessingEvaluator(model, n_processes=44) as evaluator:
                results, convergence, optimiser = evaluator.robust_optimize(robustnes_functions, scenarios=scenarios, nfe=nfe_per_round,
                                                                            epsilons=[0.08, 0.01, 0.2, 0.5, 1], restart_optimizer=restart_optimizer,
                                                                            convergence=convergence_metrics, logging_freq=1)
        except Exception as e:
            print(e)
            webhook = DiscordWebhook(
                url=discord_url, content=f"Stopped on count {count}")
            response = webhook.execute()
            count -= 1
            continue
        '''
        Print Results
        '''

        # experiments, outcomes = results
        # experiments = results
        # print(experiments.shape)
        # print(list(outcomes.keys()))

        '''
        Save Results
        '''

        # Month abbreviation, day and year
        today = date.today()
        datekey = today.strftime("%Y-%b-%d")
        # from EMAworkbench.ema_workbench import save_results

        optimiser.evaluator = None
        optimiser.algorithm.evaluator = None
        optimiser.problem.function = optimiser.problem.function(0)
        optimiser.problem.ema_constraints = None

        with open(f'./optimisers/{file_prefix}_ea_checkpoint{count*nfe_per_round + start_opt}.pickle', 'wb') as fh:
            pickle.dump(optimiser, fh)

        # save_results(results, r'./simulationData/dapp' +
        #              datekey + str(count*100000) + '.tar.gz')
        convergence.to_csv(
            f"optimisers/{file_prefix}_ea_checkpoint{count*nfe_per_round + start_opt}_conv.csv")
        results.to_csv(
            f"optimisers/{file_prefix}_ea_checkpoint{count*nfe_per_round + start_opt}_results.csv")
        print(time.time() - start)
        webhook = DiscordWebhook(
            url=discord_url, content=f"Time elapsed: {time.time() - start}")
        response = webhook.execute()
