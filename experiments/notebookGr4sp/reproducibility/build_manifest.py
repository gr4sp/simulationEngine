"""Regenerate the reproducibility manifests from the notebooks.

Scans every ../*.ipynb, records which result archives (load_results) and output
files each notebook uses, cross-references them against the archives published
on Zenodo (record 8320754), and writes archive_manifest.csv /
notebook_outputs.csv next to this script.

The *.tar.gz result archives are NOT committed to git (too large); the 14 final
ones live on Zenodo and are pulled in by fetch_results.py. Archives a notebook
loads that are NOT on Zenodo are legacy test runs kept on record here only.

    python build_manifest.py
"""
import json, re, glob, os, csv
from collections import defaultdict

here = os.path.dirname(os.path.abspath(__file__))
nbdir = os.path.dirname(here)          # experiments/notebookGr4sp
outdir = here

# The 14 result archives published at https://zenodo.org/records/8320754 .
# Keep in sync with the record (fetch_results.py enumerates it live).
ZENODO_RECORD = "8320754"
ZENODO_ARCHIVES = {
    "gr4sp_BAU2021-Aug-03.tar.gz", "gr4sp_BAU2021-Aug-22.tar.gz",
    "gr4sp_BAU2021-Aug-29.tar.gz", "gr4sp_BAU2021-Aug-30.tar.gz",
    "gr4sp_EETPast2021-Jan-14.tar.gz", "gr4sp_JT2021-Aug-30.tar.gz",
    "gr4sp_JT2021-Sep-06.tar.gz", "gr4sp_JT2021-Sep-07.tar.gz",
    "gr4sp_LCT2021-Aug-24.tar.gz", "gr4sp_LCT2021-Sep-01.tar.gz",
    "gr4sp_SOBOL2021-Feb-03.tar.gz", "gr4sp_ST2021-Aug-31.tar.gz",
    "gr4sp_ST2021-Sep-01.tar.gz", "gr4sp_ST2021-Sep-04.tar.gz",
}

load_re = re.compile(r"load_results\s*\(\s*r?['\"]([^'\"]+\.tar\.gz)['\"]")
date_re = re.compile(r"(\d{4}-[A-Za-z]{3}-\d{2}|\d{4}-[A-Za-z]{3})")
out_re = re.compile(r"(savefig|to_csv|to_excel|ExcelWriter)\s*\(")
fname_re = re.compile(r"['\"]([^'\"]*\.(?:png|pdf|svg|jpg|xlsx|xls|csv))['\"]")


def is_commented(line):
    return line.lstrip().startswith("#")


def scenario_of(fname):
    m = re.match(r"gr4sp[_-](.+?)(\d{4}-[A-Za-z]{3})", fname)
    if m:
        return m.group(1).strip("_-") or "(none)"
    return "(other)"


arch = defaultdict(lambda: {"active": set(), "commented": set()})
nb_rows = []

# Active notebooks live in notebookGr4sp/; retired ones in notebookGr4sp/legacy/.
paths = (sorted(glob.glob(os.path.join(nbdir, "*.ipynb")))
         + sorted(glob.glob(os.path.join(nbdir, "legacy", "*.ipynb"))))
for path in paths:
    retired = os.path.basename(os.path.dirname(path)) == "legacy"
    name = os.path.basename(path)
    nb = json.load(open(path, encoding="utf-8"))
    active_arch, commented_arch = set(), set()
    titles, out_files = [], set()
    n_savefig = n_write = 0
    for cell in nb.get("cells", []):
        ct = cell.get("cell_type")
        src = cell.get("source", [])
        if ct == "markdown":
            for line in src:
                s = line.strip()
                if s.startswith("#"):
                    t = s.lstrip("#").strip()
                    if t and len(t) < 90:
                        titles.append(t)
        elif ct == "code":
            for line in src:
                for m in load_re.finditer(line):
                    fn = os.path.basename(m.group(1))
                    if is_commented(line):
                        commented_arch.add(fn); arch[fn]["commented"].add(name)
                    else:
                        active_arch.add(fn); arch[fn]["active"].add(name)
                if not is_commented(line):
                    for om in out_re.finditer(line):
                        n_savefig += om.group(1) == "savefig"
                        n_write += om.group(1) != "savefig"
                    for fm in fname_re.finditer(line):
                        if not fm.group(1).endswith(".tar.gz"):
                            out_files.add(os.path.basename(fm.group(1)))

    missing = sorted(a for a in active_arch if a not in ZENODO_ARCHIVES)
    if retired:
        repro = "n/a (retired)"
    elif not active_arch:
        repro = "n/a (no result archive)"
    elif missing:
        repro = "no (needs retired archive)"
    else:
        repro = "yes"
    nb_rows.append({
        "notebook": name,
        "status": "retired" if retired else "active",
        "reproducible_from_zenodo?": repro,
        "active_archives": " | ".join(sorted(active_arch)),
        "archives_not_on_zenodo": " | ".join(missing),
        "commented_archives_count": len(commented_arch),
        "n_savefig": n_savefig,
        "n_data_writes": n_write,
        "output_files": " | ".join(sorted(out_files)[:12]),
        "first_titles": " / ".join(titles[:4]),
    })

arch_rows = []
for fn, d in arch.items():
    dm = date_re.search(fn)
    on_zenodo = fn in ZENODO_ARCHIVES
    if on_zenodo:
        status = "fetched from Zenodo"
    elif d["active"]:
        status = "retire (legacy test, not deposited)"
    else:
        status = "retire (legacy - commented-only)"
    arch_rows.append({
        "archive": fn,
        "scenario": scenario_of(fn),
        "date": dm.group(1) if dm else "",
        "on_zenodo_8320754?": "yes" if on_zenodo else "no",
        "status": status,
        "active_in": " | ".join(sorted(d["active"])) or "(none - only commented)",
        "n_active": len(d["active"]),
        "commented_in_count": len(d["commented"]),
    })
arch_rows.sort(key=lambda r: (r["on_zenodo_8320754?"] == "no", r["scenario"], r["date"]))

with open(os.path.join(outdir, "archive_manifest.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(arch_rows[0].keys()))
    w.writeheader(); w.writerows(arch_rows)
with open(os.path.join(outdir, "notebook_outputs.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(nb_rows[0].keys()))
    w.writeheader(); w.writerows(nb_rows)

n_zen = sum(1 for r in arch_rows if r["on_zenodo_8320754?"] == "yes")
n_repro = sum(1 for r in nb_rows if r["reproducible_from_zenodo?"] == "yes")
print(f"archives: {len(arch_rows)} unique ({n_zen} on Zenodo, {len(arch_rows) - n_zen} retire)")
print(f"notebooks: {len(nb_rows)} ({n_repro} reproducible from Zenodo)")
