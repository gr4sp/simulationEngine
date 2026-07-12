"""Regenerate the reproducibility manifests from the notebooks.

Scans every ../*.ipynb, records which result archives (load_results) and output
files each notebook uses, and writes archive_manifest.csv / notebook_outputs.csv
next to this script. Re-run after editing notebooks; the manually-filled
`published?` / `keep_for_zenodo?` columns must be re-entered afterwards (or
merged back in), so commit those before regenerating.

    python build_manifest.py
"""
import json, re, glob, os, csv
from collections import defaultdict

here = os.path.dirname(os.path.abspath(__file__))
nbdir = os.path.dirname(here)          # experiments/notebookGr4sp
outdir = here

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

for path in sorted(glob.glob(os.path.join(nbdir, "*.ipynb"))):
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
    nb_rows.append({
        "notebook": name,
        "published?": "",
        "active_archives": " | ".join(sorted(active_arch)),
        "commented_archives_count": len(commented_arch),
        "n_savefig": n_savefig,
        "n_data_writes": n_write,
        "output_files": " | ".join(sorted(out_files)[:12]),
        "first_titles": " / ".join(titles[:4]),
    })

arch_rows = []
for fn, d in arch.items():
    dm = date_re.search(fn)
    arch_rows.append({
        "archive": fn,
        "scenario": scenario_of(fn),
        "date": dm.group(1) if dm else "",
        "active_in": " | ".join(sorted(d["active"])) or "(none - only commented)",
        "n_active": len(d["active"]),
        "commented_in_count": len(d["commented"]),
        "keep_for_zenodo?": "no (legacy - commented-only)" if not d["active"] else "",
    })
arch_rows.sort(key=lambda r: (r["scenario"], r["date"]))

with open(os.path.join(outdir, "archive_manifest.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(arch_rows[0].keys()))
    w.writeheader(); w.writerows(arch_rows)
with open(os.path.join(outdir, "notebook_outputs.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(nb_rows[0].keys()))
    w.writeheader(); w.writerows(nb_rows)

print(f"archives: {len(arch_rows)} unique ({sum(1 for r in arch_rows if r['n_active'])} active)")
print(f"notebooks: {len(nb_rows)}")
