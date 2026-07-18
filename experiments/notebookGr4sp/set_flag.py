"""Set a boolean config flag (ANALYSIS_ONLY / onlyOneScenario) in a notebook copy.

Usage: python _set_flag.py <nb_path> <FLAG> <True|False>
Edits only the flag line in the config cell; byte-stable json.dump.
"""
import json
import re
import sys

nb_path, flag, value = sys.argv[1], sys.argv[2], sys.argv[3]
assert value in ('True', 'False')

nb = json.load(open(nb_path, encoding='utf-8'))
hits = 0
for c in nb['cells']:
    if c['cell_type'] != 'code':
        continue
    for j, line in enumerate(c['source']):
        m = re.match(r'^(%s = )(True|False)(.*)$' % re.escape(flag), line)
        if m:
            c['source'][j] = m.group(1) + value + m.group(3)
            hits += 1
    break_outer = False
assert hits == 1, 'expected exactly 1 assignment line for %s, found %d' % (flag, hits)

with open(nb_path, 'w', encoding='utf-8', newline='') as f:
    json.dump(nb, f, indent=1, ensure_ascii=False)
    f.write('\n')
print('%s = %s set in %s' % (flag, value, nb_path))
