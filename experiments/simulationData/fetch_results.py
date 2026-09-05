"""Download GR4SP simulation result archives from Zenodo into this folder.

The EMA-workbench result archives (*.tar.gz) that the analysis notebooks load
are published on Zenodo under the concept DOI 10.5281/zenodo.4667996 and are too
large to commit to git (they are .gitignore'd). This script fetches them into
experiments/simulationData/ so the notebooks under ../notebookGr4sp/ can run.

The deposit's files are spread over two versions and neither holds them all:

    https://zenodo.org/records/8320754    14 scenario archives (~1.3 GB)
    https://zenodo.org/records/22172036   validation ensemble (972 MB) + inputs

This script merges both file lists, so all 15 archives are reachable in one run.
You can cite all versions by using the DOI 10.5281/zenodo.4667996, which always
resolves to the latest one.

Files already present with the expected size are skipped, so re-running resumes
cleanly. By default only the *.tar.gz result archives are fetched; pass --all to
also pull the record's input CSV/XLSX/ZIP files.

    python fetch_results.py            # the *.tar.gz result archives
    python fetch_results.py --all      # every file in the record
    python fetch_results.py --list     # list files without downloading
    python fetch_results.py EETPast    # only files whose name contains "EETPast"
    python fetch_results.py --record 8320754    # read one version on its own

The deposit is versioned. By default this reads the base record and merges in
its newest version, so archives added after publication (such as the validation
ensemble) are picked up without editing this file. The versions read are printed
on every run; pass --record to read one version on its own.

Any non-flag argument is a case-insensitive substring filter on the filename,
so a single scenario can be fetched without downloading the whole 1.3 GB record.
See ../notebookGr4sp/reproducibility/ for which notebook needs which archive.
"""
import json
import os
import sys
import urllib.request

# Base version of the deposit; its newest version is merged in at run time.
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
    """Return (files, label) for the deposit, merged across its versions.

    Zenodo gives every new version its own record id, and a version's API
    response lists only the files that version holds. A new version does not
    inherit the previous one's files, and this deposit's versions do not carry
    the same set: version 8320754 holds the 14 scenario archives, and version
    22172036 holds the validation ensemble and its inputs. Following links.latest
    alone would therefore see one archive where the notebooks need fifteen.

    So the file lists are merged, newest version winning where a filename appears
    in both. Pass --record to read a single version instead.
    """
    record = get_record("https://zenodo.org/api/records/{}".format(record_id))
    merged = {f["key"]: f for f in record.get("files", [])}
    label = str(record.get("id", record_id))
    latest = record.get("links", {}).get("latest") if follow_latest else None
    if latest:
        try:
            newest = get_record(latest)
        except Exception as e:  # offline, rate limited, API change
            print("warning: could not check for a newer version ({})".format(e))
        else:
            if str(newest.get("id")) != str(record.get("id")):
                added = 0
                for f in newest.get("files", []):
                    added += f["key"] not in merged
                    merged[f["key"]] = f
                print("Record {} has a newer version {}: {} more file(s)".format(
                    record.get("id"), newest.get("id"), added))
                label = "{} + {}".format(label, newest.get("id"))
    return list(merged.values()), label


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

    files, label = list_files(record_id, follow_latest)
    targets = [f for f in files if want_all or f["key"].endswith(".tar.gz")]
    if filters:
        targets = [f for f in targets
                   if any(flt in f["key"].lower() for flt in filters)]
    total = sum(f.get("size", 0) for f in targets)
    print("Record {}: {} file(s) selected, {} total".format(
        label, len(targets), human(total)))

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
