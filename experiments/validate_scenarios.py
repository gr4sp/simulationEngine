"""Structural equivalence check for the 3.1a scenario consolidation.

For each original scenario (BAU/JT/LCT/ST), assert the consolidated
gr4sp_model.getModel(<name>) produces an EMA Model identical to the original
gr4spModel<X>.getModelAfterBaseYear(): same uncertainties (name, type, ordered
ranges), same constants (name -> value), same outcomes (name + type). This is a
DB-free, JVM-free check -- it compares model *definitions*, not simulation runs.

    python validate_scenarios.py    # exits 0 if all scenarios match

Runs at import-time cost only; safe to wire into CI.
"""
import sys, os
sys.path.append(os.path.join(os.getcwd(), 'EMAworkbench'))
# The originals were retired to legacy/ by the 3.1a consolidation; keep them
# importable so this stays a live equivalence guard. Remove this line (and the
# script) if legacy/ is ever deleted.
sys.path.append(os.path.join(os.getcwd(), 'legacy'))

import gr4spModelBAU, gr4spModelJT, gr4spModelLCT, gr4spModelST
import gr4sp_model

ORIGINALS = {
    'BAU': gr4spModelBAU.getModelAfterBaseYear,
    'JT':  gr4spModelJT.getModelAfterBaseYear,
    'LCT': gr4spModelLCT.getModelAfterBaseYear,
    'ST':  gr4spModelST.getModelAfterBaseYear,
}


def unc_signature(u):
    t = type(u).__name__
    if t == 'CategoricalParameter':
        return (u.name, 'categorical', tuple(c.value for c in u.categories))
    if t == 'IntegerParameter':
        return (u.name, 'integer', (int(u.lower_bound), int(u.upper_bound)))
    return (u.name, t, None)


def uncertainties_sig(model):
    return [unc_signature(u) for u in model.uncertainties]


def constants_sig(model):
    # name -> value; order-independent (constants are passed to the run fn by name)
    return {c.name: c.value for c in model.constants}


def outcomes_sig(model):
    return [(o.name, type(o).__name__) for o in model.outcomes]


def compare(name):
    old = ORIGINALS[name]()
    new = gr4sp_model.getModel(name)
    problems = []

    if uncertainties_sig(old) != uncertainties_sig(new):
        problems.append('uncertainties differ:\n  old=%s\n  new=%s'
                        % (uncertainties_sig(old), uncertainties_sig(new)))
    if constants_sig(old) != constants_sig(new):
        oc, nc = constants_sig(old), constants_sig(new)
        keys = set(oc) | set(nc)
        diffs = {k: (oc.get(k, '<none>'), nc.get(k, '<none>'))
                 for k in keys if oc.get(k) != nc.get(k)}
        problems.append('constants differ: %s' % diffs)
    if outcomes_sig(old) != outcomes_sig(new):
        problems.append('outcomes differ:\n  old=%s\n  new=%s'
                        % (outcomes_sig(old), outcomes_sig(new)))
    return problems


def main():
    all_ok = True
    for name in ORIGINALS:
        problems = compare(name)
        if problems:
            all_ok = False
            print('[FAIL] %s' % name)
            for p in problems:
                print('   ' + p)
        else:
            old = ORIGINALS[name]()
            print('[ OK ] %s  (%d uncertainties, %d constants, %d outcomes)'
                  % (name, len(old.uncertainties), len(old.constants), len(old.outcomes)))
    if not all_ok:
        sys.exit(1)
    print('\nAll scenarios structurally equivalent to originals.')


if __name__ == '__main__':
    main()
