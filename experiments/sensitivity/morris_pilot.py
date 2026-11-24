"""R3 step 4 (Angela, 11-12 Nov 2026): Morris screening pilot on the repaired model, to compare with the thesis's.

Two designs, as in the thesis (gr4spModelEET.py, 34 uncertainties, EMA MORRIS sampler with 6 levels):
  aby   after base year (default; Angela 12 Nov: the one that matters for future scenarios and pathways, and the design
        of article Table 5). getModelAFterBaseYear -> connector.runGr4spAfterBaseYear: the run follows the base case
        to the base year (2019) and the sampled values apply from then on. mu* from 2019. Thesis result:
        simulationData/eet_aby.csv (ranks by mu*, columns *-M, and by sigma, *-S; blank = not ranked).
  past  hypothetical past. getModel -> connector.runGr4sp: the sampled values apply from the start of the run.
        mu* from 1999. Thesis result: notebookGr4sp/outputs/data/EE_past_mu_star_median.xlsx (normalised mu*).
20 trajectories instead of 200 (35 x 20 = 700 runs), every run with the same random seed (GR4SP_FIXED_SEED), so
differences between runs come from the parameters alone. Settings are the repository's simulationSettings/ (the
repaired base case); classes from build/classes/java/main.

The sample is kept in results/morris_<design>_sample.object, never in experiments/uncertainties.morris.object (the
thesis's sample, which the workbench overwrites when it samples by itself). An existing sample file is reused, and run
CSVs are named _morris<design>_<n> (GR4SP_OUTPUT_TAG), so an interrupted pilot resumes with GR4SP_RESUME=1 without
mixing designs.

  python morris_pilot.py run DESIGN OUT_TAR [TRAJECTORIES=20] [PROCESSES=8]
  python morris_pilot.py compare DESIGN OUT_TAR REPORT_DIR

`compare` reproduces the thesis's analysis (notebookGr4sp/ema_gr4sp_EET_HypoPast.ipynb, cells 14-31: SALib
morris.analyze with 6 levels, mu* year by year, the median over years, min-max normalised per output) and ranks the
parameters. Per output: Spearman rank correlation with the thesis and the overlap of the top five. Decision rule set
before the run: an output agrees when rho >= 0.8 and at least 4 of the top 5 are shared. For aby the thesis gives
ranks only for the parameters it ranked, so rho is taken over those parameters.
"""
import os, sys

EXP = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))
RESULTS = os.path.join(EXP, "sensitivity", "results")
OUTPUTS = ["tariffsYear", "primaryWholesalePriceYear", "GHGYear", "renewableContributionYear", "consumptionYear",
           "coalProductionYear", "gasProductionYear", "windProductionYear", "solarProductionYear"]
FIRST_YEAR = {"aby": 2019, "past": 1999}
# eet_aby.csv column (rank by mu*) and EE_past_mu_star_median.xlsx column for each model output
THESIS_ABY = {"GHGYear": "GHGE-M", "primaryWholesalePriceYear": "Primary Spot Wholesale prices-M",
              "tariffsYear": "Tariffs-M", "renewableContributionYear": "Renewable energy-M"}
THESIS_PAST = {"primaryWholesalePriceYear": "wholesalePriceYear"}
RULE_RHO, RULE_TOP = 0.8, 4


def _setup(design):
    if design not in FIRST_YEAR:
        sys.exit("design must be aby or past")
    os.chdir(EXP)                               # connector.py reads settingsExperiments.json and ../ from here
    sys.path.insert(0, os.path.join(EXP, "EMAworkbench"))
    sys.path.insert(0, EXP)
    os.environ.setdefault("GR4SP_FIXED_SEED", "42")
    os.environ["GR4SP_OUTPUT_TAG"] = "morris" + design
    import gr4spModelEET
    return gr4spModelEET.getModelAFterBaseYear() if design == "aby" else gr4spModelEET.getModel()


def run(design, out_tar, trajectories, processes):
    model = _setup(design)
    import pickle
    from EMAworkbench.ema_workbench import MultiprocessingEvaluator, ema_logging, save_results
    from EMAworkbench.ema_workbench.em_framework.samplers import sample_uncertainties
    from EMAworkbench.ema_workbench.em_framework.evaluators import MORRIS, SAMPLERS
    ema_logging.log_to_stderr(ema_logging.INFO)
    sample = os.path.join(RESULTS, "morris_{}_sample.object".format(design))
    if not os.path.isfile(sample):
        os.makedirs(RESULTS, exist_ok=True)
        with open(sample, "wb") as f:
            pickle.dump(sample_uncertainties([model], trajectories, sampler=SAMPLERS[MORRIS]()), f)
    with MultiprocessingEvaluator(model, n_processes=processes) as evaluator:
        results = evaluator.perform_experiments(scenarios=1, policies=0, uncertainty_sampling=sample)
    save_results(results, out_tar)
    print("saved", out_tar, results[0].shape)


def mu_star_median(results, model, first_year):
    import numpy as np, pandas as pd
    from SALib.analyze import morris
    from EMAworkbench.ema_workbench.em_framework.salib_samplers import get_SALib_problem
    from EMAworkbench.ema_workbench.em_framework.samplers import determine_parameters
    from EMAworkbench.ema_workbench import CategoricalParameter
    experiments, outcomes = results
    problem = get_SALib_problem(determine_parameters(model, "uncertainties", union=False))
    names = problem["names"]
    X = np.column_stack([[float(model.uncertainties[u].index_for_cat(c)) for c in experiments[u]]
                         if isinstance(model.uncertainties[u], CategoricalParameter)
                         else experiments[u].astype(float).values for u in names])
    years = np.asarray(outcomes["TIMEYear"][0], dtype=float)
    cols = [t for t, y in enumerate(years) if y >= first_year]
    out = {}
    for ooi in OUTPUTS:
        if ooi not in outcomes:
            raise KeyError("output {} not in the results".format(ooi))
        Y = outcomes[ooi]
        per_year = [pd.Series(morris.analyze(problem, X, Y[:, t].astype(float), num_levels=6)["mu_star"], index=names)
                    for t in cols]
        med = pd.concat(per_year, axis=1).median(axis=1)
        out[ooi] = (med - med.min()) / (med.max() - med.min()) if med.max() > med.min() else med * 0
    return pd.DataFrame(out)


def compare(design, out_tar, report_dir):
    import pandas as pd
    from scipy.stats import spearmanr
    model = _setup(design)
    from EMAworkbench.ema_workbench import load_results
    os.makedirs(report_dir, exist_ok=True)
    new = mu_star_median(load_results(out_tar), model, FIRST_YEAR[design])
    new.to_csv(os.path.join(report_dir, "morris_{}_mu_star_median.csv".format(design)))
    if design == "aby":
        old = pd.read_csv(os.path.join(EXP, "simulationData", "eet_aby.csv"), index_col=0, encoding="utf-8-sig")
        thesis_cols, source = THESIS_ABY, "simulationData/eet_aby.csv (thesis ranks by mu*)"
    else:
        old = pd.read_excel(os.path.join(EXP, "notebookGr4sp", "outputs", "data", "EE_past_mu_star_median.xlsx"),
                            index_col=0)
        thesis_cols, source = THESIS_PAST, "EE_past_mu_star_median.xlsx (thesis normalised mu*)"
    lines = ["# Morris pilot ({}) on the repaired model against the thesis screening".format(design), "",
             "Normalised median mu* from {}; thesis: {}. Agreement rule: Spearman rho >= {} and >= {} of the top 5 "
             "shared.".format(FIRST_YEAR[design], source, RULE_RHO, RULE_TOP), "",
             "| Output | rho | params compared | top 5 shared | agrees | top 5, thesis | top 5, repaired |",
             "|---|---|---|---|---|---|---|"]
    for ooi in new.columns:
        oc = thesis_cols.get(ooi, ooi) if design == "past" else thesis_cols.get(ooi)
        if oc is None or oc not in old.columns:
            continue
        b = new[ooi]
        if design == "aby":
            ranks = old[oc].dropna()                          # 1 = most influential
            ta = list(ranks.sort_values().index[:5])
            common = ranks.index.intersection(b.index)
            rho = spearmanr(-ranks[common], b[common]).correlation
            n = len(common)
        else:
            a = old[oc].reindex(b.index).fillna(0)
            ta = list(a.sort_values(ascending=False).index[:5])
            rho = spearmanr(a, b).correlation
            n = len(b)
        tb = list(b.sort_values(ascending=False).index[:5])
        shared = len(set(ta) & set(tb))
        ok = rho >= RULE_RHO and shared >= RULE_TOP
        lines.append(f"| {ooi} | {rho:.2f} | {n} | {shared} | {'yes' if ok else 'no'} | {', '.join(ta)} | {', '.join(tb)} |")
    lines += ["", "Parameters with no effect in the repaired model (normalised mu* < 0.01 in every output):", ""]
    dead = new.index[(new.fillna(0) < 0.01).all(axis=1)]
    lines.append(", ".join(dead) if len(dead) else "none")
    text = "\n".join(lines) + "\n"
    open(os.path.join(report_dir, "morris_{}_comparison.md".format(design)), "w", encoding="utf-8").write(text)
    print(text)


if __name__ == "__main__":
    if sys.argv[1] == "run":
        run(sys.argv[2], os.path.abspath(sys.argv[3]), int(sys.argv[4]) if len(sys.argv) > 4 else 20,
            int(sys.argv[5]) if len(sys.argv) > 5 else 8)
    else:
        compare(sys.argv[2], os.path.abspath(sys.argv[3]), os.path.abspath(sys.argv[4]))
