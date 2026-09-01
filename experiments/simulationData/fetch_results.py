"""Download GR4SP simulation result archives from Zenodo into this folder.

The EMA-workbench result archives (*.tar.gz) that the analysis notebooks load
are published at https://zenodo.org/records/8320754 and are too large to commit
to git (they are .gitignore'd). This script fetches them into
experiments/simulationData/ so the notebooks under ../notebookGr4sp/ can run.

Files already present with the expected size are skipped, so re-running resumes
cleanly. By default only the *.tar.gz result archives are fetched; pass --all to
also pull the record's input CSV/XLSX/ZIP files.

    python fetch_results.py            # the *.tar.gz result archives
    python fetch_results.py --all      # every file in the record
    python fetch_results.py --list     # list files without downloading
    python fetch_results.py EETPast    # only files whose name contains "EETPast"
    python fetch_results.py --record 8320754   # pin one version instead of the latest

The deposit is versioned. By default this follows the record to its newest
version, so archives added after publication (such as the validation ensemble)
are picked up without editing this file. The resolved version is printed on
every run; pass --record to pin a specific one.

Any non-flag argument is a case-insensitive substring filter on the filename,
so a single scenario can be fetched without downloading the whole 1.3 GB record.
See ../notebookGr4sp/reproducibility/ for which notebook needs which archive.
"""
import json
import os
import sys
import urllib.request

RECORD = "8320754"
API = "https://zenodo.org/api/records/{}".format(RECORD)
HERE = os.path.dirname(os.path.abspath(__file__))


def human(n):
    for unit in ("B", "kB", "MB", "GB"):
        if n < 1024 or unit == "GB":
            return "{:.1f} {}".format(n, unit)
        n /= 1024.0


def get_record(url):
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def list_files(record_id, follow_latest=True):
    """Return (files, resolved record id) for the deposit.

    Zenodo gives every new version its own record id, and an older version's
    API response only lists the files that version held. Follow links.latest so
    a reader gets archives deposited after this script was written.
    """
    record = get_record("https://zenodo.org/api/records/{}".format(record_id))
    latest = record.get("links", {}).get("latest") if follow_latest else None
    if latest:
        try:
            newest = get_record(latest)
        except Exception as e:  # offline, rate limited, API change
            print("warning: could not check for a newer version ({})".format(e))
        else:
            if str(newest.get("id")) != str(record.get("id")):
                print("Record {} superseded by version {}".format(
                    record.get("id"), newest.get("id")))
                record = newest
    return record.get("files", []), record.get("id", record_id)


def download(url, dest, size):
    tmp = dest + ".part"
    state = {"got": 0, "last_pct": -1}

    def hook(block, blocksize, total):
        state["got"] += blocksize
        pct = int(min(100, 100 * state["got"] / size)) if size else 0
        # Only redraw when the whole-number percent changes, so piped/captured
        # output stays compact instead of one line per block.
        if pct != state["last_pct"]:
            state["last_pct"] = pct
            sys.stdout.write("\r    {:3d}%  {} / {}".format(
                pct, human(min(state["got"], size)), human(size)))
            sys.stdout.flush()

    urllib.request.urlretrieve(url, tmp, reporthook=hook)
    sys.stdout.write("\n")
    os.replace(tmp, dest)


def main():
    want_all = "--all" in sys.argv
    list_only = "--list" in sys.argv
    argv = sys.argv[1:]

    record_id = RECORD
    follow_latest = True
    if "--record" in argv:
        i = argv.index("--record")
        if i + 1 >= len(argv):
            sys.exit("--record needs a Zenodo record id")
        record_id = argv[i + 1]
        follow_latest = False
        del argv[i:i + 2]

    filters = [a.lower() for a in argv if not a.startswith("--")]

    files, record_id = list_files(record_id, follow_latest)
    targets = [f for f in files if want_all or f["key"].endswith(".tar.gz")]
    if filters:
        targets = [f for f in targets
                   if any(flt in f["key"].lower() for flt in filters)]
    total = sum(f.get("size", 0) for f in targets)
    print("Record {}: {} file(s) selected, {} total".format(
        record_id, len(targets), human(total)))

    for f in targets:
        key = f["key"]
        size = f.get("size", 0)
        url = f["links"]["self"]
        dest = os.path.join(HERE, key)

        if list_only:
            print("  {:>10}  {}".format(human(size), key))
            continue

        if os.path.exists(dest) and os.path.getsize(dest) == size:
            print("skip  {} ({}, already present)".format(key, human(size)))
            continue

        print("get   {} ({})".format(key, human(size)))
        download(url, dest, size)

    if not list_only:
        print("Done.")


if __name__ == "__main__":
    main()
