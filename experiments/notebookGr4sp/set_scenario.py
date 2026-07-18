"""Set SCENARIO in the consolidated IMAP notebook's config cell.

Usage: python set_scenario.py <JT|LCT|ST>
Edits only the SCENARIO line; byte-stable json.dump.
"""
import json, sys, re

scen = sys.argv[1]
p = "gr4sp_energy_vulnerability_IMAP.ipynb"
nb = json.load(open(p, encoding="utf-8"))
code = [c for c in nb["cells"] if c["cell_type"] == "code"]
src = "".join(code[2]["source"])
new = re.sub(r'SCENARIO = "\w+"', 'SCENARIO = "%s"' % scen, src, count=1)
assert new != src and ('SCENARIO = "%s"' % scen) in new
code[2]["source"] = new.splitlines(keepends=True)
with open(p, "w", encoding="utf-8", newline="\n") as f:
    json.dump(nb, f, indent=1, ensure_ascii=False)
    f.write("\n")
print("set SCENARIO =", scen)
