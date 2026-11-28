"""R3 step 4 (Angela, 11 Nov 2026): how large must a Saltelli sample be? Subsample the existing 105,000-run
hypothetical-past ensemble and see at which base sample size N the total-order (ST) rankings stop changing.

The archive (gr4sp_SOBOLhypopast2021-Mar-03_includ_wholesale_month.tar.gz; k = 24 uncertainties, N = 2,100, second
order, so 2k + 2 = 50 runs per base sample) stores runs in Saltelli order: the first n blocks of 50 rows are themselves a
valid design with N = n (checked: within a block, row 1 + i differs from row 0 in parameter i only). Its two empty runs
(41357, 41379) fall in block 827, which is dropped from the full sample (N = 2,099).

For each output (wholesale price, GHGE, renewable share, tariff) and each N, SALib sobol.analyze gives ST per year
(1999-2019); the median over years is compared with the full sample's: Spearman rho, the top five, the largest change
in ST, and the mean width of the ST confidence intervals. A sample size is enough when, for every output, the top five
are the same parameters as at full size and the ten most influential keep their order (rho over the full sample's top
ten >= 0.9). Rho over all 24 is reported too; it is dominated by the order of parameters with near-zero effect.

Usage: python saltelli_convergence.py ARCHIVE OUT_DIR
"""
import os, sys, tarfile
import numpy as np, pandas as pd
from scipy.stats import spearmanr

OUTPUTS = {"primaryWholesalePriceYear": "Wholesale price", "GHGYear": "GHGE",
           "renewableContributionYear": "RE share", "tariffsYear": "Tariff"}
SIZES = [64, 128, 256, 512, 800, 1200]
K = 24
BLOCK = 2 * K + 2
BAD_BLOCK = 41357 // BLOCK
assert 41379 // BLOCK == BAD_BLOCK


def read_members(archive, names):
    out = {}
    with tarfile.open(archive, "r:gz") as tar:
        for m in tar:
            stem = m.name[:-4]
            if stem in names:
                out[stem] = pd.read_csv(tar.extractfile(m), header=None if stem not in ("experiments",) else 0)
                if len(out) == len(names):
                    break
    return out


def main():
    from SALib.analyze import sobol
    archive, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    data = read_members(archive, {"experiments", "TIMEYear", *OUTPUTS})
    exp = data["experiments"]
    names = [c for c in exp.columns if c not in ("scenario", "policy", "model")]
    assert len(names) == K, names
    # structure check on the first blocks: row 1 + i differs from row 0 in parameter i only
    X = exp[names].apply(pd.to_numeric, errors="coerce")
    for b in range(3):
        base = X.iloc[b * BLOCK]
        for i in range(K):
            diff = np.where(X.iloc[b * BLOCK + 1 + i].values != base.values)[0]
            assert len(diff) <= 1 and (len(diff) == 0 or diff[0] == i), (b, i, diff)
    years = data["TIMEYear"].iloc[0].astype(int).values
    cols = [j for j, y in enumerate(years) if 1999 <= y <= 2019]
    problem = {"num_vars": K, "names": names, "bounds": [[0, 1]] * K}
    n_full = len(exp) // BLOCK
    keep_full = [b for b in range(n_full) if b != BAD_BLOCK]
    rows = []
    for ooi, label in OUTPUTS.items():
        Y = data[ooi].values.astype(float)
        for n in SIZES + ["full"]:
            blocks = keep_full if n == "full" else keep_full[:n]   # the empty block is skipped at every size
            idx = np.concatenate([np.arange(b * BLOCK, (b + 1) * BLOCK) for b in blocks])
            st, conf = [], []
            for j in cols:
                y = Y[idx, j]
                if np.nanstd(y) == 0:
                    continue
                si = sobol.analyze(problem, y, calc_second_order=True, num_resamples=100, print_to_console=False)
                st.append(si["ST"])
                conf.append(si["ST_conf"])
            rows.append({"output": label, "N": len(blocks), "runs": len(idx),
                         "ST": pd.Series(np.median(st, axis=0), index=names),
                         "conf": float(np.mean(np.median(conf, axis=0)))})
            print(label, len(blocks), flush=True)
    res = []
    for label in OUTPUTS.values():
        r = [x for x in rows if x["output"] == label]
        full = r[-1]["ST"]
        top_full = list(full.sort_values(ascending=False).index[:5])
        for x in r:
            top = list(x["ST"].sort_values(ascending=False).index[:5])
            res.append({"output": label, "N": x["N"], "runs_k24": x["runs"],
                        "runs_k29": x["N"] * (2 * 29 + 2), "rho_vs_full": spearmanr(x["ST"], full).correlation,
                        "rho_top10": spearmanr(x["ST"][full.sort_values(ascending=False).index[:10]],
                                               full.sort_values(ascending=False)[:10]).correlation,
                        "top5_shared": len(set(top) & set(top_full)), "top5_same_order": top == top_full,
                        "max_abs_dST": float((x["ST"] - full).abs().max()), "mean_ST_conf": x["conf"],
                        "top5": ", ".join(top)})
    res = pd.DataFrame(res)
    res.to_csv(os.path.join(out_dir, "saltelli_convergence.csv"), index=False)
    ok = res.groupby("N").apply(lambda g: bool(((g.rho_top10 >= 0.9) & (g.top5_shared == 5)).all()))
    print(res.drop(columns="top5").round(3).to_string(index=False))
    print("\nN enough for every output (same top five, rho over the top ten >= 0.9):", ok.to_dict())


if __name__ == "__main__":
    main()
