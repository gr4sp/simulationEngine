"""Config-driven EMA runner (task 3.1a consolidation).

Replaces the per-scenario runExperiments<X>.py clones. Reads scenarios.yaml,
builds the model, and runs EMA with that scenario's operational settings.

    python run_experiment.py JT              # run scenario JT
    python run_experiment.py LCT --dry-run   # assemble + print the plan, don't run
    python run_experiment.py BAU --processes 4

Must be run from the experiments/ directory (like the original runners).
"""
import os, sys, argparse
sys.path.append(os.path.join(os.getcwd(), 'EMAworkbench'))
from datetime import date

from gr4sp_model import getModel, run_settings


def build_plan(scenario, processes_override=None):
    """Assemble everything needed to run, without executing EMA. Testable offline."""
    model = getModel(scenario)
    rs = run_settings(scenario)
    n_processes = processes_override if processes_override is not None else rs['n_processes']

    from EMAworkbench.ema_workbench.em_framework.evaluators import (
        MC, LHS, FAST, FF, PFF, SOBOL, MORRIS)
    samplers = {'MC': MC, 'LHS': LHS, 'FAST': FAST, 'FF': FF,
                'PFF': PFF, 'SOBOL': SOBOL, 'MORRIS': MORRIS}

    kwargs = {'scenarios': rs['scenarios'], 'policies': rs['policies']}
    if rs.get('uncertainty_file'):
        kwargs['uncertainty_sampling'] = rs['uncertainty_file']
    elif rs.get('sampling'):
        kwargs['uncertainty_sampling'] = samplers[rs['sampling']]
    if rs.get('generate_only'):
        kwargs['generate_experiments_file_only'] = True

    # BAU ran under SequentialEvaluator (deterministic, no sampling); the sampled
    # scenarios ran under MultiprocessingEvaluator. Preserve that split.
    use_sequential = (n_processes <= 1
                      and not rs.get('sampling')
                      and not rs.get('uncertainty_file'))

    datekey = date.today().strftime("%Y-%b-%d")
    outfile = os.path.join('.', 'simulationData',
                           'gr4sp_%s%s.tar.gz' % (scenario, datekey))
    return {
        'model': model, 'kwargs': kwargs, 'n_processes': n_processes,
        'use_sequential': use_sequential, 'outfile': outfile,
    }


def main():
    ap = argparse.ArgumentParser(description='Run a GR4SP EMA scenario from scenarios.yaml')
    ap.add_argument('scenario', help='scenario name (e.g. BAU, JT, LCT, ST)')
    ap.add_argument('--processes', type=int, default=None,
                    help='override n_processes from config')
    ap.add_argument('--dry-run', action='store_true',
                    help='assemble and print the run plan without executing EMA')
    args = ap.parse_args()

    # Make the scenario visible to connector workers (drives BAU's ABY-EE quirk).
    os.environ['GR4SP_SCENARIO'] = args.scenario

    plan = build_plan(args.scenario, args.processes)

    print('Scenario:        %s' % args.scenario)
    print('Evaluator:       %s' % ('SequentialEvaluator' if plan['use_sequential']
                                    else 'MultiprocessingEvaluator(n_processes=%d)' % plan['n_processes']))
    print('perform_experiments kwargs: %s'
          % {k: (v.__name__ if callable(v) else v) for k, v in plan['kwargs'].items()})
    print('Uncertainties:   %s' % [u.name for u in plan['model'].uncertainties])
    print('Output:          %s' % plan['outfile'])

    if args.dry_run:
        print('\n[dry-run] not executing EMA.')
        return

    from EMAworkbench.ema_workbench import (SequentialEvaluator, MultiprocessingEvaluator,
                                            ema_logging, save_results)
    ema_logging.log_to_stderr(ema_logging.INFO)

    if plan['use_sequential']:
        evaluator_cm = SequentialEvaluator(plan['model'])
    else:
        evaluator_cm = MultiprocessingEvaluator(plan['model'], n_processes=plan['n_processes'])

    with evaluator_cm as evaluator:
        results = evaluator.perform_experiments(**plan['kwargs'])

    experiments, outcomes = results
    print(experiments.shape)
    print(list(outcomes.keys()))

    os.makedirs(os.path.dirname(plan['outfile']), exist_ok=True)
    save_results(results, plan['outfile'])
    print('Saved %s' % plan['outfile'])


if __name__ == '__main__':
    main()
