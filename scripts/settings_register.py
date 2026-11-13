"""
Register of every setting in simulationSettings/VIC.yaml and VICfuture.yaml (audit B1, N24, 14 Nov 2026).

For each leaf key: its value, the line, the commit and date that last set that line (git blame), the comment that
explains it (the comment lines directly above, with no blank line between, and the comment at the end of the line), and
a status:
  cited      the comment names a source (an organisation, a report, a paper, a file or script of this repository, a URL);
  explained  the comment explains the value but names no source;
  none       no comment, or one that only keeps an earlier value.
The status is a screen, not a judgement: a cited comment can still cite the wrong thing, and SETTINGS_REVIEW in
ASSUMPTIONS.md records the keys read by hand. Blocks that repeat for each generator type (fuel and technology) inherit
the comment above the block's first line when their own line has none (status then 'block').

Usage: python scripts/settings_register.py [OUT_CSV]   (default settings_register.csv at the repository root)
"""
import csv
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = ["simulationSettings/VIC.yaml", "simulationSettings/VICfuture.yaml"]
SOURCE = re.compile(
    r"https?://|\bAEMO\b|\bABS\b|\bISP\b|IASR|ACIL|\bCEC\b|Clean Energy|\bBoM\b|Lazard|Oakley|\bAEMC\b|\bCER\b|APVI|"
    r"\bAER\b|OpenNEM|OpenElectricity|\bIEA\b|\bESC\b|\bRBA\b|CSIRO|NatHERS|IMAP|Killinger|Hills|BEIS|DWP|census|"
    r"docs/|experiments/|scripts/|README|INPUTS\.md|\.csv\b|\.py\b|\(\d{4}[a-z]?\)|\b(19|20)\d{2} (report|workbook)|"
    r"[A-Z][a-z]+ (et al\.|and [A-Z][a-z]+) \(?\d{4}", re.I)


def blame(path):
    out = subprocess.run(["git", "blame", "--line-porcelain", path], cwd=ROOT, capture_output=True, text=True,
                         encoding="utf-8").stdout.splitlines()
    commits, cur, info = [], None, {}
    for line in out:
        if re.match(r"^[0-9a-f]{40} ", line):
            cur = line.split()[0]
        elif line.startswith("author-time "):
            info[cur] = int(line.split()[1])
        elif line.startswith("\t"):
            commits.append(cur)
    dates = {}
    for c in set(commits):
        if c.startswith("0000000"):
            dates[c] = "uncommitted"
        else:
            dates[c] = subprocess.run(["git", "show", "-s", "--format=%cs", c], cwd=ROOT, capture_output=True,
                                      text=True).stdout.strip()
    return commits, dates


def leaves(path):
    lines = open(os.path.join(ROOT, path), encoding="utf-8").read().splitlines()
    commits, dates = blame(path)
    stack, rows, list_idx = [], [], {}
    block_comment = {}
    for i, raw in enumerate(lines):
        s = raw.split(" #")[0].rstrip() if not raw.lstrip().startswith("#") else ""
        if not s.strip():
            continue
        indent = len(s) - len(s.lstrip())
        body = s.strip()
        item = body.startswith("- ")
        if item:
            body = body[2:]
            while stack and stack[-1][0] >= indent + 1:
                stack.pop()
            ppath = ".".join(k for _, k in stack)
            n = list_idx.get((ppath, indent), -1) + 1
            list_idx[(ppath, indent)] = n
            stack.append((indent + 1, f"[{n}]"))
            indent += 2
        m = re.match(r"^([A-Za-z0-9_ ()/.\-]+?)\s*:(\s+.*|)$", body)
        if not m:
            continue
        key, val = m.group(1).strip(), m.group(2).strip()
        while stack and stack[-1][0] >= indent:
            stack.pop()
        parent = ".".join(k for _, k in stack).replace(".[", "[")
        # comment: contiguous comment lines above, plus the comment at the end of the line
        above, j = [], i - 1
        while j >= 0 and lines[j].lstrip().startswith("#"):
            above.insert(0, lines[j].strip().lstrip("#").strip())
            j -= 1
        inline = raw.split(" #", 1)[1].strip() if " #" in raw and not raw.lstrip().startswith("#") else ""
        note = " ".join(x for x in above + [inline] if x)
        full = f"{parent}.{key}" if parent else key
        if val == "":
            stack.append((indent, key))
            block_comment[full] = note
            continue
        own = note
        # a comment that only keeps an earlier value ("0.49", "option 2: 14.25", "2000-01-01") explains nothing
        if own and not re.sub(r"(option \d+:?|was|[-+0-9.,:;()%/ ]|or)", "", own, flags=re.I).strip():
            own = ""
        status = "cited" if own and SOURCE.search(own) else ("explained" if own else "none")
        if status == "none":
            # inherit the comment of the nearest enclosing block that has one
            p = full
            while "." in p:
                p = p.rsplit(".", 1)[0]
                if block_comment.get(p):
                    own, status = block_comment[p], "block"
                    break
        rows.append({"file": os.path.basename(path), "key": full, "value": val.strip('"'), "line": i + 1,
                     "commit": (commits[i] or "")[:7], "date": dates.get(commits[i], ""), "status": status,
                     "comment": own[:400]})
    return rows


def main(out):
    rows = []
    for f in FILES:
        rows += leaves(f)
    vic = {r["key"]: r["value"] for r in rows if r["file"] == "VIC.yaml"}
    for r in rows:
        r["differs_after_base_year"] = ("" if r["file"] == "VIC.yaml" else
                                        ("new" if r["key"] not in vic else ("yes" if vic[r["key"]] != r["value"] else "no")))
    with open(out, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)
    from collections import Counter
    for f in ("VIC.yaml", "VICfuture.yaml"):
        print(f, dict(Counter(r["status"] for r in rows if r["file"] == f)))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "settings_register.csv"))
