"""Solar adoption, step 1: cross-sectional targets and household electricity use from the ABS Household Energy
Consumption Survey 2012 (cat. 4670.0, additional tables), saved in
Sandbox/papers/GR4SPArticle_ERSS/data/abs_energy_use_VIC/4670_2012_HECS_additional_tables.xls.

Writes data/hecs2012_targets.csv, long format: table, group, measure, value, unit, rse_flag
  pv_share          households with solar electricity connected to the grid / estimated households (%)
                    by tenure (Table 12A, Australia), economic resources (Table 7A, Australia),
                    Victoria capital city and balance of state (Tables 19B, 19C)
  kwh_week          mean weekly electricity consumption (kWh) by energy sources used in the dwelling and in total,
                    Melbourne and balance of Victoria (Tables 18B, 18C), by tenure (12A), by family composition (16A)
  fit_kwh_week      average weekly electricity related to the feed-in tariff, households with grid PV (kWh)
rse_flag carries the ABS marks: * relative standard error 25-50 %, ** above 50 % (use with caution / not reliable).

These are validation targets (pattern-oriented: who adopts) and benchmarks for consumption by profile; the
calibration uses only the APVI monthly series.

Usage: python build_hecs2012_targets.py XLS
"""
import argparse, csv, os

import xlrd

HERE = os.path.dirname(os.path.abspath(__file__))


def num(v):
    s = str(v).strip()
    flag = "**" if s.startswith("**") else ("*" if s.startswith("*") else "")
    s = s.lstrip("*").replace(",", "")
    try:
        return float(s), flag
    except ValueError:
        return None, flag


def find(sheet, label, unit=None, start=0):
    for r in range(start, sheet.nrows):
        if str(sheet.cell_value(r, 0)).strip().lower().startswith(label.lower()) and \
                (unit is None or str(sheet.cell_value(r, 1)).strip() == unit):
            return r
    raise KeyError((sheet.name, label))


def columns(sheet, header_rows):
    """Column labels: the last non-empty header text above each data column."""
    labs = {}
    for c in range(2, sheet.ncols):
        parts = [str(sheet.cell_value(r, c)).strip() for r in header_rows if str(sheet.cell_value(r, c)).strip()]
        if parts:
            labs[c] = " ".join(parts)
    return labs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("xls")
    ap.add_argument("--out", default=os.path.join(HERE, "data", "hecs2012_targets.csv"))
    a = ap.parse_args()
    b = xlrd.open_workbook(a.xls)
    out = []

    def share(table, header_rows, pv_label="Household has solar electricity connected to grid"):
        s = b.sheet_by_name(table)
        cols = columns(s, header_rows)
        rp, rn = find(s, pv_label, "'000"), find(s, "Estimated number of households", "'000")
        for c, lab in cols.items():
            pv, f1 = num(s.cell_value(rp, c))
            n, _ = num(s.cell_value(rn, c))
            if pv is not None and n:
                out.append((table, lab, "pv_share", round(100 * pv / n, 2), "%", f1))

    def kwh(table, header_rows, area=None):
        s = b.sheet_by_name(table)
        cols = columns(s, header_rows)
        try:  # the consumption block is only in some tables
            r0 = find(s, "Energy source(s) used in dwelling", start=find(s, "Electricity", start=85))
            rows = list(range(r0 + 1, r0 + 9))
        except KeyError:
            rows = []
        for r in rows:
            lab = str(s.cell_value(r, 0)).strip()
            if not lab or str(s.cell_value(r, 1)).strip() != "kWh":
                continue
            for c, g in cols.items():
                v, f = num(s.cell_value(r, c))
                if v is not None:
                    out.append((table, f"{g} | {lab}", "kwh_week", v, "kWh/week", f))
        rf = find(s, "Average electricity amount related to feed-in tariff", "kWh")
        for c, g in cols.items():
            v, f = num(s.cell_value(rf, c))
            if v is not None:
                out.append((table, g, "fit_kwh_week", v, "kWh/week", f))

    share("Table 12A", [6, 7])
    share("Table 7A", [6])
    share("Table 19B", [6])
    share("Table 19C", [6])
    kwh("Table 18B", [6])
    kwh("Table 18C", [6])
    kwh("Table 12A", [6, 7])
    kwh("Table 16A", [7, 8])

    os.makedirs(os.path.dirname(a.out), exist_ok=True)
    with open(a.out, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["table", "group", "measure", "value", "unit", "rse_flag"])
        w.writerows(out)
    for t in out:
        if t[2] == "pv_share" or ("VIC" in t[1] and t[2] == "kwh_week"):
            print(t)


if __name__ == "__main__":
    main()
