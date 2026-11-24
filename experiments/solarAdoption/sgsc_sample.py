"""Sample of household half-hourly loads from the Smart Grid Smart City customer trial (solar step 3, D4b, averaging test).

Source: Ausgrid / Department of Industry, Smart-Grid Smart-City Customer Trial Data (data.gov.au, CC BY 3.0 AU),
Electricity Use Interval Reading (CD_INTERVAL_READING_ALL_NO_QUOTES.csv, 17.7 GB in a 7z archive) and Customer
Household Data. Households in Ausgrid's network (Sydney, Newcastle, Hunter), 2010-2014.

The interval file is streamed through 7-Zip and only these households are kept:
  - every household with a gross solar meter (GROSS_SOLAR_CNT > 0): general supply is then the home's whole use and
    gross generation its whole PV output, so the self-consumed share can be measured directly;
  - a random sample (seed 1) of N households without solar that answered the appliance survey (HAS_AIRCON known).
Output: <dir>/derived/sgsc_sample.csv.gz (customer, time, general_kwh, controlled_kwh, gross_gen_kwh) and
<dir>/derived/sgsc_sample_households.csv (the household attributes of the sample).
Usage: python sgsc_sample.py SGSC_DIR [--n 2000]
"""
import argparse, csv, gzip, os, subprocess
import numpy as np
import pandas as pd

SEVENZIP = r"C:\Program Files\7-Zip\7z.exe"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--n", type=int, default=2000)
    a = ap.parse_args()
    h = pd.read_csv(os.path.join(a.dir, "sgsc-ct_customer-household-data-revised.csv"), low_memory=False)
    solar = h[h.GROSS_SOLAR_CNT > 0]
    others = h[(h.GROSS_SOLAR_CNT == 0) & (h.NET_SOLAR_CNT == 0) & h.HAS_AIRCON.notna()]
    rng = np.random.default_rng(1)
    pick = others.iloc[rng.choice(len(others), size=min(a.n, len(others)), replace=False)]
    keep = pd.concat([solar.assign(group="gross_solar"), pick.assign(group="no_solar_surveyed")])
    ids = set(keep.CUSTOMER_KEY.astype(str))
    out_dir = os.path.join(a.dir, "derived")
    os.makedirs(out_dir, exist_ok=True)
    keep.to_csv(os.path.join(out_dir, "sgsc_sample_households.csv"), index=False)
    p = subprocess.Popen([SEVENZIP, "e", "-so", os.path.join(a.dir, "cdintervalreadingallnoquotes.csv.7z")],
                         stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    n_in = n_out = 0
    seen = set()
    with gzip.open(os.path.join(out_dir, "sgsc_sample.csv.gz"), "wt", newline="") as fo:
        w = csv.writer(fo)
        w.writerow(["customer", "time", "general_kwh", "controlled_kwh", "gross_gen_kwh"])
        lines = (l.decode("ascii", "replace") for l in p.stdout)
        next(lines)
        for line in lines:
            n_in += 1
            cid, rest = line.split(",", 1)
            if cid in ids:
                f = line.rstrip("\r\n").split(",")
                w.writerow([cid, f[1], f[4].strip(), f[5].strip(), f[6].strip()])
                n_out += 1
                seen.add(cid)
            if n_in % 50_000_000 == 0:
                print(f"{n_in/1e6:.0f} M lines read, {n_out/1e6:.1f} M kept, {len(seen)} households", flush=True)
    p.wait()
    print(f"done: {n_in} lines, {n_out} kept, households found {len(seen)} of {len(ids)}")


if __name__ == "__main__":
    main()
