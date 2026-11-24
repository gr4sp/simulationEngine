"""N30 (16 Nov 2026): Victorian households on 1 January of each year from the census, for householdsFile.

The model's household register took `domestic_consumers` (source not stated) to 2015 and Victoria in Future 2016
(`household_forecast_victoria`) from 2016, which put a 7 % jump into 2015. `domestic_consumers` equals the census count
of occupied private dwellings in 2006 (1,869,388 against 1,869,384) and is within 0.6 % of it in 2001, but after 2006 it
runs above the census (2011 +4.4 %, 2016 +6.1 %, 2021 +8.1 %); Victoria in Future is 13 % above the census in 2016.

Households = occupied private dwellings counted by the census (ABS QuickStats, Victoria, state code 2):
  2001 1,731,343   2006 1,869,384   2011 1,944,691   2016 2,112,699   2021 2,390,232
Census night is in early August (7 Aug 2001, 8 Aug 2006, 9 Aug 2011, 9 Aug 2016, 10 Aug 2021), taken as year + 0.6;
1 January values are linear between the two census nights around them. Years before 2007 keep `domestic_consumers`
(its 2006 value is the August 2006 census count, filed under 1 January, so the file starts in 2007); after 2021 the model grows the last value at Victoria in Future's rates (LoadData).

Definition: an occupied private dwelling on census night is a household. Dwellings whose residents were all away that
night are counted as unoccupied, so the census count is below the number of households that use electricity over a
year; ERP-based household estimates (Victoria in Future) are higher for that reason and for the census undercount.

Usage: python households_census_vic.py [--out PATH]
"""
import argparse, os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir))
CENSUS = {2001: 1731343, 2006: 1869384, 2011: 1944691, 2016: 2112699, 2021: 2390232}
SOURCE = {2001: "ABS 2001 Census QuickStats, Victoria", 2006: "ABS 2006 Census QuickStats, Victoria",
          2011: "ABS 2016 Census media release (2011 count)", 2016: "ABS 2016 Census: Victoria, media release",
          2021: "ABS 2021 Census QuickStats, Victoria"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "simulationSettings", "households_VIC.csv"))
    a = ap.parse_args()
    t = {y + 0.6: n for y, n in CENSUS.items()}
    keys = sorted(t)
    rows = []
    for year in range(2007, 2022):
        lo = max(k for k in keys if k <= year) if any(k <= year for k in keys) else keys[0]
        hi = min(k for k in keys if k >= year)
        w = (year - lo) / (hi - lo)
        n = round(t[lo] + w * (t[hi] - t[lo]))
        rows.append((year, n, f"linear between census nights {int(lo)} and {int(hi)} ({SOURCE[int(lo)]}; {SOURCE[int(hi)]})"))
    with open(a.out, "w", newline="\n") as fh:
        fh.write("# Victorian households on 1 January: census counts of occupied private dwellings, linear between census nights\n"
                 "# (N30; scripts/data/households_census_vic.py). Read with householdsFile: these years replace the database's;\n"
                 "# earlier years keep domestic_consumers (equal to the census in 2006); later years grow at Victoria in Future's rates.\n")
        fh.write("year,households,source\n")
        for y, n, s in rows:
            fh.write(f'{y},{n},"{s}"\n')
    for y, n, _ in rows:
        print(y, n)


if __name__ == "__main__":
    main()
