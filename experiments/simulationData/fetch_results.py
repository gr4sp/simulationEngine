"""Download GR4SP simulation result archives from Zenodo into this folder.

The EMA-workbench result archives (*.tar.gz) that the analysis notebooks load
are published at https://zenodo.org/records/8320754 and are too large to commit
to git (they are .gitignore'd). This script fetches them into
experiments/simulationData/ so the notebooks under ../notebookGr4sp/ can run.

Files already present with the expected size are skipped, so re-running resumes
cleanly. By default only the *.tar.gz result archives are fetched; pass --all to
also pull the record's input CSV/XLSX/ZIP files.

    python fetch_results.py            # the 14 *.tar.gz result archives
    python fetch_results.py --all      # every file in the record
    python fetch_results.py --list     # list files without downloading

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


def list_files():
    req = urllib.request.Request(API, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req) as r:
        record = json.load(r)
    return record.get("files", [])


def download(url, dest, size):
    tmp = dest + ".part"
    done = [0]

    def hook(block, blocksize, total):
        done[0] += blocksize
        pct = min(100, 100 * done[0] / size) if size else 0
        sys.stdout.write("\r    {:5.1f}%  {} / {}".format(
            pct, human(min(done[0], size)), human(size)))
        sys.stdout.flush()

    urllib.request.urlretrieve(url, tmp, reporthook=hook)
    sys.stdout.write("\n")
    os.replace(tmp, dest)


def main():
    want_all = "--all" in sys.argv
    list_only = "--list" in sys.argv

    files = list_files()
    targets = [f for f in files if want_all or f["key"].endswith(".tar.gz")]
    total = sum(f.get("size", 0) for f in targets)
    print("Record {}: {} file(s) selected, {} total".format(
        RECORD, len(targets), human(total)))

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
