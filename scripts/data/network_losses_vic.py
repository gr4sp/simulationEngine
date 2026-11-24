"""Network losses of the Victorian SPM levels (simulationSettings/network_losses_VIC.csv), 24 Nov 2026.

The SPM recursion (Spm.computeIndicators) scales the emissions of the energy entering each level by 1 / (1 - loss).
The database values (transmission 10 %, distribution 2 %, 400 V 2 %, customer wiring 3 %) are unsourced; this file
replaces them with sourced values per level:

- Distribution (SPM 5): the AER economic benchmarking RIN, variable DQS03 / ED_QOS730_00001 "System losses" (%) of the
  five Victorian DNSPs (CitiPower, Powercor, Jemena, United Energy, AusNet Services), calendar years 2006-2019,
  weighted by each DNSP's total energy delivered (DOPED01, total_gwh in residential_dnsp_VIC.csv). System losses cover
  the whole distribution network, high voltage to the customer's meter. 2006 is used before 2006, 2019 after.
- 400 V (SPM 6): 0, the AER system losses already include the low-voltage network.
- Customer wiring (SPM 1): 0. Household consumption is metered, so losses behind the meter are inside it; the
  comparators (NGA Factors scope 2 + network losses) also stop at the meter.
- Transmission (SPM 7): the AER economic benchmarking RIN of AusNet Services (ET), the Victorian shared transmission
  network, variable TQS03 / ET_QOS3630_00001 "System losses" (%) = (inflows - outflows) / inflows, measured by the
  revenue meters of all terminal stations, inflows from generators, other TNSPs and DNSPs (Basis of Preparation 2014,
  D14 120101, Table 3.6.3). Regulatory years run April to March: the 2006-13 back-cast column N covers April N-1 to
  March N, a response file 'YYYY-YY' covers April YYYY to March YYYY+1. Each value applies from the start of its
  year; 2017-18 has no response, so 2016-17 carries on; the first value is used before.

Usage: python network_losses_vic.py RIN_DIR TNSP_DIR OUT_CSV
  RIN_DIR   the AER DNSP RIN downloads (Sandbox/papers/GR4SPArticle_ERSS/data/aer_dnsp_benchmarking/)
  TNSP_DIR  the AusNet (ET) RIN downloads (Sandbox/papers/GR4SPArticle_ERSS/data/aer_tnsp_benchmarking/)
"""
import argparse, csv, glob, os, re

import openpyxl

HERE = os.path.dirname(os.path.abspath(__file__))
DNSP_CSV = os.path.join(HERE, os.pardir, os.pardir, "simulationSettings", "residential_dnsp_VIC.csv")
CODES = ("DQS03", "ED_QOS730_00001")
T_CODES = ("TQS03", "ET_QOS3630_00001")


def number(v):
    if isinstance(v, (int, float)):
        return float(v)
    if isinstance(v, str) and re.fullmatch(r"\s*[\d.]+\s*%\s*", v):
        return float(v.strip().rstrip("%")) / 100.0
    return None


def system_losses(path, codes=CODES):
    """The system losses row of one RIN response: list of loss fractions, one per year of the file."""
    wb = openpyxl.load_workbook(path, data_only=True, read_only=True)
    for ws in wb.worksheets:
        if "quality" not in ws.title.lower():
            continue
        for row in ws.iter_rows(values_only=True):
            cells = [c for c in row if c is not None]
            at = [i for i, c in enumerate(cells) if isinstance(c, str) and c.strip() in codes]
            if at:  # code(s), "System losses", unit, then the values
                k = max(at) + 3
                return [x for x in (number(c) for c in cells[k:]) if x is not None]
    raise ValueError(f"no system losses row in {path}")


def transmission(tnsp_dir):
    """(from date, loss, source) of AusNet (ET), one per regulatory year (April to March)."""
    out = []
    for path in sorted(glob.glob(os.path.join(tnsp_dir, "*.xls*"))):
        name = os.path.basename(path)
        m = re.search(r"\(ET\) (\d{4})-(\d{2}) - (Economic Benchmarking|Order)", name)
        if not m:
            continue  # category analysis RIN
        first, last = int(m.group(1)), 2000 + int(m.group(2))
        v = system_losses(path, T_CODES)
        if last - first > 1:  # 2006-13 back-cast: regulatory years 2006..2013, year N = April N-1 to March N
            for i, x in enumerate(v):
                out.append((f"{first - 1 + i}-04-01", x, f"AER EB RIN AusNet (ET) TQS03 system losses, regulatory "
                                                          f"year {first + i} (Apr {first - 1 + i} to Mar {first + i}), back-cast"))
        else:
            out.append((f"{first}-04-01", v[0], f"AER EB RIN AusNet (ET) TQS03 system losses, {first}-{last % 100:02d} "
                                                f"(Apr {first} to Mar {last})"))
    out.sort()
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("rin_dir")
    ap.add_argument("tnsp_dir")
    ap.add_argument("out_csv")
    a = ap.parse_args()

    energy = {}
    with open(DNSP_CSV, encoding="utf8") as f:
        rows = [r for r in csv.reader(f) if r and not r[0].startswith("#")]
    head = rows[0]
    for r in rows[1:]:
        d = dict(zip(head, r))
        energy[(d["dnsp"], int(d["year"]))] = float(d["total_gwh"])

    losses = {}
    for path in sorted(glob.glob(os.path.join(a.rin_dir, "*_EB_RIN.xls*"))):
        name = os.path.basename(path)
        dnsp, span = name.split("_")[0], name.split("_")[1]
        first = int(span[:4])
        for i, v in enumerate(system_losses(path)):
            losses[(dnsp, first + i)] = v

    years = sorted({y for _, y in losses})
    out = []
    for y in years:
        keys = [k for k in losses if k[1] == y]
        if len(keys) != 5:
            raise ValueError(f"{y}: {len(keys)} DNSPs")
        w = sum(energy[k] for k in keys)
        loss = sum(losses[k] * energy[k] for k in keys) / w
        out.append((y, loss, " ".join(f"{k[0]} {losses[k]:.4f}" for k in sorted(keys))))

    with open(a.out_csv, "w", newline="\n", encoding="utf8") as f:
        f.write("spm_id,level,from,loss,source\n")
        t = transmission(a.tnsp_dir)
        f.write(f"7,transmission,1990-01-01,{t[0][1]:.4f},{t[0][2]} (earliest; used before)\n")
        for d, loss, src in t[1:]:
            f.write(f"7,transmission,{d},{loss:.4f},{src}\n")
        first_year = out[0][0]
        f.write(f"5,distribution,1990-01-01,{out[0][1]:.4f},AER EB RIN DQS03 system losses {first_year} "
                f"(earliest; used before), energy weighted over the five Victorian DNSPs\n")
        for y, loss, detail in out[1:]:
            f.write(f"5,distribution,{y}-01-01,{loss:.4f},AER EB RIN DQS03 system losses {y}, energy weighted: {detail}\n")
        f.write("6,400V,1990-01-01,0.0,included in the AER distribution system losses (high voltage to the meter)\n")
        f.write("1,customer,1990-01-01,0.0,behind the meter: metered household consumption already includes it\n")
    for y, loss, _ in out:
        print("distribution", y, f"{100 * loss:.2f} %")
    for d, loss, _ in t:
        print("transmission", d, f"{100 * loss:.2f} %")


if __name__ == "__main__":
    main()
