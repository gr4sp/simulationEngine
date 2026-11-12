"""Realised commissioning slippage of the Victorian project pipeline in the model database (calibration of the
commissioning delay d, R3 forward period).

The database (`generationassets`, DB-2021-8-21) carries AEMO's Generation Information pipeline as it stood between
18 Jul and 12 Nov 2019: it already has the 18 Jul 2019 site renames (Mallee Solar Park -> Mallee Solar Farm) and not
the 12 Nov 2019 ones (Lal Lal -> Yendon / Elaine). Each project's `full_commercial_use_date` is the date expected then.
This script finds what happened to each project in a later Generation Information file. It writes one row per project
with its outcome and, for built projects, the slippage in months. It also compares installed wind and solar with the
AEMO 2020 ISP Central path (capacity_path_VIC.csv) by financial year.

Outcome per project:
  built        In Service in the later file. Date = its Full Commercial Use Date or, when blank, the first change-log
               entry that moved it to In Service (a publication date, so up to a quarter late; flagged).
  in_progress  Committed, Anticipated or In Commissioning; date = the expected Full Commercial Use Date (if any)
  announced    still Publicly Announced
  dropped      marked discontinued or removed in the change log, or no longer listed
  duplicate    the same project twice in the database (excluded)

Sources: AEMO NEM Generation Information, July 2025 (aemo.com.au/-/media/files/electricity/nem/planning_and_forecasting/
generation_information/2025/nem-generation-information-july-2025.xlsx; kept with the article data in
Sandbox/papers/GR4SPArticle_ERSS/data/). The project mapping below is by hand, from site names, the file's
"Site name changes" sheet and its change log.

Usage:
  psql -U postgres -d gr4spdb -A -F'|' -c "select asset_id, site_name, fuel_type, nameplate_capacity_mw, unit_status,
       full_commercial_use_date from generationassets where region='VIC' and unit_status in ('Committed','Committed*',
       'Emerging','In Commissioning','Publically Announced','Publically Announced - cancelled?')" > db_pipeline.csv
  python scripts/data/commissioning_slippage_vic.py GENINFO_XLSX db_pipeline.csv OUT_DIR
"""
import os
import sys
from datetime import date

import openpyxl
import pandas as pd

GI, DB, OUT = sys.argv[1:4]

# asset_id -> (outcome hint, Generation Information site name(s) or None, note)
# A site name maps to all its VIC1 rows; a (name, MW) pair picks the row of that nameplate.
MAP = {
    # Committed, Committed*, Emerging, In Commissioning in the database
    100: ("Mt Gellibrand Wind Farm", ""),
    282: ("Yatpool Solar Farm", "both Yatpool rows in service 21 Jan 2022 (change log)"),
    283: ("Yatpool Solar Farm", "as 282"),
    276: ("Yendon Wind Farm", "Lal Lal Yendon end, renamed 12 Nov 2019"),
    201: ("Murra Warra Wind Farm - stage 1", ""),
    154: ("Crowlands Wind Farm", ""),
    285: ("Gannawarra Energy Storage System", "commissioning from Jan 2019; no in-service date recorded"),
    275: ("Elaine Wind Farm", "Lal Lal Elaine end, renamed 12 Nov 2019"),
    277: ("Bulgana Green Power Hub - BESS", ""),
    293: (("Berrybank Wind Farm", 180.6), "stage 1"),
    280: ("Kiamal Solar Farm - Stage 1", ""),
    281: ("Cohuna Solar Farm", "In Service Jul 2023, back to In Commissioning Oct 2024"),
    297: ("Stockyard Hill Wind Farm", ""),
    301: ("Cherry Tree Wind Farm", ""),
    295: ("Moorabool Wind Farm", ""),
    303: ("Carwarp Solar Farm - Stage I", ""),
    305: ("Dundonnell Wind Farm", ""),
    308: ("Mortlake South Wind Farm", ""),
    278: ("Bulgana Green Power Hub - Wind Farm", ""),
    317: ("Winton Solar Farm", "database site name is the owner (Winton Asset Co.)"),
    331: (None, "Kiamal stage 2 no longer listed (only a Kiamal BESS)"),
    # Publicly Announced in the database
    "Congupna Solar Farm": (None, "no longer listed"),
    "Wangaratta Solar Farm": ("DROP", "removed 15 Apr 2025 (out-of-date non-scheduled project)"),
    "Nowingi Solar Storage - Storage": ("Nowingi Solar Storage - Storage", ""),
    "Murra Warra Solar Farm": (None, "no longer listed"),
    "Prairie": ("Prairie Solar Farm", ""),
    "Nhill Wind Farm": ("Diapur Wind Farm", "renamed Diapur 15 Oct 2021"),
    "Berrybank Wind Farm": (("Berrybank Wind Farm", 109.2), "stage 2"),
    "Mortlake": (("Mortlake", 200), "third OCGT unit"),
    "Glenrowan Sun Farm": ("Glenrowan West Solar Farm", "Glenrowan West Sun Farm in the change log"),
    "Carisbrook Solar Farm": ("DROP", "discontinued 31 Oct 2024"),
    "AusNet Services Deakin University Victoria Geelong PV Plant": ("DROP", "removed 15 Apr 2025"),
    "Naring Solar Farm": (None, "no longer listed"),
    "Shepparton Solar Farm": ("DROP", "discontinued 22 Jul 2022"),
    "Ouyen Solar Farm": (None, "no longer listed"),
    "Glenrowan Solar Farm": ("Glenrowan Solar Farm", ""),
    "Alberton Wind Farm": ("Alberton Wind Farm", ""),
    "Ben More": ("DROP", "discontinued 15 Jun 2022"),
    "Ferguson Wind Farm": ("Ferguson Wind Farm", ""),
    "Girgarre Solar Farm": ("Girgarre Solar Farm", ""),
    "Horsham Solar Farm": ("Horsham Solar Farm", ""),
    "Warracknabeal Wind Farm": ("Warracknabeal Wind Farm", ""),
    "Willatook": ("Willatook", ""),
    "Wunghnu Solar Farm": ("Wunghnu Solar Farm", ""),
    "Mount Fyans": ("Mount Fyans", ""),
    "Murra Warra Wind Farm - stage 2": ("Murra Warra Wind Farm - stage 2", ""),
    "Kerang Solar Farm": (None, "no longer listed (a Kerang BESS and hybrid are)"),
    "Mallee Solar Farm": ("DROP", "discontinued 15 Jun 2022"),
    "Mildura Power Station": ("Mildura Power Station", ""),
    "Tarrone Wind Farm": (None, "0 MW in the database; no longer listed"),
    "Carwarp Solar Farm - Stage II": ("Carwarp Solar Farm - Stage II", ""),
    "Star of The South": ("Star of the South Wind Farm", ""),
    "GV Community Energy": ("DROP", "removed 15 Apr 2025"),
    "Ryan Corner Wind Farm": ("Ryan Corner Wind Farm", ""),
    "Nowingi Solar Storage - Solar": ("Nowingi Solar Storage - Solar", ""),
    "Inverleigh Solar Farm": ("Inverleigh Solar Farm", ""),
    "Golden Plains Wind Farm": ("Golden Plains Wind Farm East", "East stage (762 MW); West (557 MW) committed for Feb 2027"),
    "Port Phillip Heads Tidal Energy Project": (None, "no longer listed"),
    "Tarrone GT": ("Tarrone GT", ""),
    "Hawkesdale Wind Farm": ("Hawkesdale Wind Farm", ""),
    "Woolsthorpe Wind Farm": ("Woolsthorpe Wind Farm", ""),
    "Rifle Butts Wind Farm": ("Rifle Butts Wind Farm", ""),
    "Moira Solar Farm": ("DUP", "renamed Wunghnu Solar Farm 18 Jul 2019; the database has both"),
    "Berrimal Wind Farm": ("DROP", "discontinued 15 Apr 2025"),
    "Darlington Wind Farm": ("Darlington Wind Farm - GPG", "0 MW in the database"),
    "Inverleigh Wind Farm": ("Inverleigh Wind Farm", ""),
    "Hexham": ("Hexham", ""),
    "Inverliegh Wind Farm Storage": (None, "no longer listed"),
    "Penshurst": ("DROP", "discontinued 31 Oct 2022"),
    "Naroghid": ("DROP", "discontinued 15 Apr 2025"),
}


def as_date(x):
    return x.date() if hasattr(x, "date") else None


wb = openpyxl.load_workbook(GI, read_only=True, data_only=True)
rows = list(wb["ExistingGeneration&NewDevs"].iter_rows(values_only=True))
H = {h: i for i, h in enumerate(rows[1])}
gi = [r for r in rows[2:] if r[H["Region"]] == "VIC1"]
log = [r for r in list(wb["Change Log"].iter_rows(values_only=True))[5:] if r[1] == "VIC1"]


def norm(s):
    return " ".join(str(s).split()).lower()


def gi_rows(key):
    name, mw = key if isinstance(key, tuple) else (key, None)
    out = [r for r in gi if norm(r[H["Site Name"]]) == norm(name)]
    if mw is not None:
        out = [r for r in out if abs(float(r[H["Nameplate Capacity (MW)"]]) - mw) < 0.5]
    if not out:
        raise SystemExit("no Generation Information row for %r" % (key,))
    return out, name


def log_in_service(name):
    ds = [as_date(r[0]) for r in log if norm(r[3]) == norm(name) and "in service" in str(r[6]).lower()
          and "from in service" not in str(r[6]).lower()]
    return min(ds) if ds else None


def months(a, b):
    return (b.year - a.year) * 12 + (b.month - a.month) + (b.day - a.day) / 30.4


db = pd.read_csv(DB, sep="|", dtype=str).dropna(subset=["asset_id"])
db = db[db["asset_id"].str.strip().str.isdigit()]
out = []
for _, d in db.iterrows():
    aid, site = int(d["asset_id"]), d["site_name"].strip()
    key, note = MAP.get(aid) or MAP[site]
    exp = date.fromisoformat(d["full_commercial_use_date"])
    rec = dict(asset_id=aid, site_name=site, fuel=d["fuel_type"], mw=float(d["nameplate_capacity_mw"]),
               db_status=d["unit_status"], expected=exp, status_2025="", actual="", date_source="",
               outcome="", slip_months="", note=note)
    if key in ("DROP", None):
        rec["outcome"] = "dropped"
    elif key == "DUP":
        rec["outcome"] = "duplicate"
    else:
        rs, name = gi_rows(key)
        st = rs[0][H["Unit Status"]]
        rec["status_2025"] = st
        dt = max((as_date(r[H["Full Commercial Use Date"]]) for r in rs if as_date(r[H["Full Commercial Use Date"]])),
                 default=None)
        src = "full commercial use date" if dt else ""
        if st == "In Service":
            rec["outcome"] = "built"
            if dt is None:
                dt, src = log_in_service(name), "change log, in service"
        elif st == "Publicly Announced":
            rec["outcome"] = "announced"
        else:
            rec["outcome"] = "in_progress"
        if dt:
            rec["actual"], rec["date_source"] = dt, src
            rec["slip_months"] = round(months(exp, dt), 1)
    out.append(rec)

res = pd.DataFrame(out).sort_values(["db_status", "expected"])
os.makedirs(OUT, exist_ok=True)
res.to_csv(os.path.join(OUT, "commissioning_slippage_VIC.csv"), index=False)

# Installed utility wind and solar on 1 July, from the later file, against the 2020 ISP Central path
path = pd.read_csv(os.path.join(os.path.dirname(__file__), "..", "..", "simulationSettings", "capacity_path_VIC.csv"),
                   comment="#")
lines = []
for fuel, tech in (("Wind", "Wind"), ("Solar", "Solar")):
    ins = [r for r in gi if r[H["Unit Status"]] == "In Service" and str(r[H["Technology Type"]]).startswith(tech)]
    p = path[path["fuel_type"] == fuel].set_index("fy_end")["capacity_mw"]
    for fy in range(2021, 2026):
        day = date(fy - 1, 7, 1)
        cap = 0.0
        for r in ins:
            dt = as_date(r[H["Full Commercial Use Date"]]) or log_in_service(r[H["Site Name"]])
            mw = r[H["Nameplate Capacity (MW)"]]
            if (dt is None or dt <= day) and isinstance(mw, (int, float)) and mw >= 1:
                cap += float(mw)
        reached = p[p <= cap].index.max() if (p <= cap).any() else None
        lines.append(dict(fuel=fuel, fy_end=fy, installed_mw=round(cap), isp2020_path_mw=round(p.get(fy, float("nan"))),
                          path_fy_matching_installed=reached))
pd.DataFrame(lines).to_csv(os.path.join(OUT, "installed_vs_isp2020_VIC.csv"), index=False)
print(res.groupby(["db_status", "outcome"])["mw"].agg(["count", "sum"]).round(0))
