"""Config-driven EMA model builder (task 3.1a consolidation).

Replaces the per-scenario gr4spModel<X>.py clones with one builder reading
scenarios.yaml. A new scenario is a new block in scenarios.yaml -- no new Python.

    from gr4sp_model import getModel
    model = getModel('JT')
"""
import os
import yaml

import gr4sp_connector

from EMAworkbench.ema_workbench import (IntegerParameter, CategoricalParameter,
                                        ScalarOutcome, TimeSeriesOutcome, Constant, Model)

SCENARIOS_YAML = 'scenarios.yaml'


def load_config(path=SCENARIOS_YAML):
    with open(path) as f:
        return yaml.safe_load(f)


def _build_uncertainty(spec):
    kind = spec['type']
    if kind == 'categorical':
        return CategoricalParameter(spec['name'], list(spec['categories']))
    if kind == 'integer':
        return IntegerParameter(spec['name'], int(spec['lower']), int(spec['upper']))
    raise ValueError('unknown uncertainty type %r for %r' % (kind, spec.get('name')))


def _build_outcomes(outcomes_cfg):
    outcomes = [TimeSeriesOutcome(n) for n in outcomes_cfg['timeseries']]
    outcomes += [ScalarOutcome(n) for n in outcomes_cfg['scalar']]
    return outcomes


def getModel(scenario, path=SCENARIOS_YAML):
    """Build the EMA Model for a named scenario from scenarios.yaml."""
    cfg = load_config(path)
    if scenario not in cfg['scenarios']:
        raise KeyError('unknown scenario %r; known: %s'
                       % (scenario, ', '.join(cfg['scenarios'])))
    sc = cfg['scenarios'][scenario]

    if sc.get('model_type', 'afterBaseYear') == 'afterBaseYear':
        function = gr4sp_connector.runGr4spAfterBaseYear
    else:
        function = gr4sp_connector.runGr4sp

    model = Model('Gr4sp', function=function)
    model.uncertainties = [_build_uncertainty(u) for u in sc['uncertainties']]
    model.constants = [Constant(name, value) for name, value in sc['constants'].items()]
    model.outcomes = _build_outcomes(cfg['outcomes'])
    return model


def run_settings(scenario, path=SCENARIOS_YAML):
    """Operational EMA invocation settings (n_processes, sampling, ...)."""
    return load_config(path)['scenarios'][scenario]['run']
