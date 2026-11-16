"""Share of small-scale PV (under 100 kW) owned by non-households, by year, from system sizes (solar step 3, A43).

Source: APVI, Market Analyses, "Monthly installations by size category" (Australia; Clean Energy Regulator small-scale
register; downloaded 16 Nov 2026 to Sandbox/papers/GR4SPArticle_ERSS/data/pv_size/): installations per month in the
bands <2.5, 2.5-4.5, 4.5-6.5, 6.5-9.5, 9.5-14, 14-25 and 25-100 kW, 2010-2026.

Rule: systems of 14 kW and more are taken as non-household (businesses, farms, schools); smaller ones as households.
It is a lower bound for businesses (small businesses with small systems are counted as households) and, before 2020,
close to an upper bound for large homes. Capacity per band = installations x the band's middle (1.6, 3.5, 5.5, 8, 11.5,
19, 50 kW); the band capacity agrees with APVI's average installation size within 0-8 % a year. The series is
national: no Victorian split by size is published; it is applied to Victoria as is (assumption). Before 2010 the 2010
share holds; after the register's last full year the last value holds.

Output: simulationSettings/rooftop_nonresidential_share_VIC.csv (year, share_kw, share_installs, source)
Usage: python rooftop_nonresidential_share.py APVI_SIZE_CSV [--out PATH]
"""
import argparse, os
import pandas as pd

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir))
MID = {"<2.5kW": 1.6, "2.5<4.5kW": 3.5, "4.5<6.5kW": 5.5, "6.5<9.5kW": 8, "9.5<14kW": 11.5, "14<25kW": 19, "25<=100kW": 50}
LARGE = ["14<25kW", "25<=100kW"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("apvi_csv")
    ap.add_argument("--out", default=os.path.join(ROOT, "simulationSettings", "rooftop_nonresidential_share_VIC.csv"))
    a = ap.parse_args()
    d = pd.read_csv(a.apvi_csv)
    full = d.groupby("year").month.nunique()
    y = d.groupby("year")[list(MID) + ["installations_in_month"]].sum().loc[full[full == 12].index]
    kw = sum(y[b] * MID[b] for b in MID)
    out = pd.DataFrame({"share_kw": sum(y[b] * MID[b] for b in LARGE) / kw,
                        "share_installs": y[LARGE].sum(axis=1) / y.installations_in_month})
    out = out.loc[:2025].round(4)
    with open(a.out, "w", newline="\n") as fh:
        fh.write("# Share of small-scale PV (under 100 kW) owned by non-households: systems of 14 kW and more, by capacity and by\n"
                 "# number, Australia (APVI monthly installations by size category, CER register). Applied to Victoria; before 2010\n"
                 "# the 2010 value. Built by scripts/data/rooftop_nonresidential_share.py; read with rooftopNonResidentialShareFile.\n")
        out.assign(source="APVI Market Analyses, monthly installations by size category (downloaded 16 Nov 2026)").to_csv(fh, index_label="year")
    print(out.to_string())


if __name__ == "__main__":
    main()
