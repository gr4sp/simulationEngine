"""Extract the half-hourly load profiles plotted in ACIL Allen (2019), Victorian energy usage profiles, report to the
Essential Services Commission (1 March 2019): Figures 4.1, 4.2, 4.8, 4.9 and 4.10.

The charts are vector drawings (form XObjects): each series is a stroked polyline of 48 points (half-hour periods 1-48,
period 1 = 00:00-00:30) in chart coordinates. The y axis is calibrated from its tick labels: the chart's horizontal
gridlines are drawn at equal steps from the baseline (0), and the step is read from the axis labels that pypdf
extracts with their positions. Values are average daily consumption (kWh) per half-hour, the average annual profile
of the Victorian sample (mostly 2016 meter data).

Output: data/esc2019_profiles.csv (figure, series, period, kwh) and a check plot per figure (data/esc2019_fig*.png).
The legend labels of Figure 4.2 are read from the chart text; the others are given above in drawing order.
Usage: python extract_esc_profiles.py PDF
"""
import argparse, os, re
import numpy as np
import pandas as pd
import pypdf

HERE = os.path.dirname(os.path.abspath(__file__))
# figure -> (page index, legend labels in drawing order, y-axis top label value)
FIGURES = {
    "4.1": (27, ["Non-CL", "CL"]),
    "4.2": (28, None),
    "4.8": (35, ["No PV", "PV"]),
    "4.9": (35, ["No gas", "Gas"]),
    "4.10": (36, ["No pool", "Pool"]),
}
NUM = r"-?\d+(?:\.\d+)?"


def forms(page):
    xo = page["/Resources"].get("/XObject", {})
    for k, v in xo.items():
        o = v.get_object()
        if o.get("/Subtype") == "/Form":
            yield k, o


def parse(stream):
    """Gridline y values (grey strokes spanning the plot) and coloured 48-point polylines, in drawing order."""
    toks = stream.decode("latin1").split()
    lines, grids, cur, colour = [], [], [], None
    i = 0
    while i < len(toks):
        t = toks[i]
        if t == "RG":
            colour = tuple(float(x) for x in toks[i - 3:i])
        elif t == "G":
            colour = (float(toks[i - 1]),) * 3
        elif t == "m":
            if len(cur) > 1:
                lines.append((colour, cur))
            cur = [(float(toks[i - 2]), float(toks[i - 1]))]
        elif t == "l":
            cur.append((float(toks[i - 2]), float(toks[i - 1])))
        elif t in ("S", "s"):
            if len(cur) > 1:
                lines.append((colour, cur))
            cur = []
        i += 1
    series = [(c, pts) for c, pts in lines if len(pts) == 48]
    for c, pts in lines:
        if len(pts) == 2 and abs(pts[0][1] - pts[1][1]) < 1e-6 and abs(pts[1][0] - pts[0][0]) > 300:
            grids.append(pts[0][1])
    return sorted(set(round(g, 3) for g in grids)), series


def texts(stream):
    """(x, y, string) of each text run: Tm position and the TJ glyph strings joined."""
    s = stream.decode("latin1")
    out = []
    for m in re.finditer(r"(" + NUM + r") (" + NUM + r") Tm(.*?)\[(.*?)\] ?TJ", s, re.S):
        txt = "".join(re.findall(r"\((.*?)\)", m.group(4)))
        out.append((float(m.group(1)), float(m.group(2)), txt))
    return out


def y_scale(grids, labels):
    """Linear map chart y -> kWh from the left-axis tick labels: label i sits next to gridline i (baseline first)."""
    ticks = sorted((y, float(t)) for x, y, t in labels if x < 30 and re.fullmatch(NUM, t))
    # each label's text baseline sits 2-4 units below its gridline; the baseline of the plot (0) may not be a gridline
    pairs = []
    for ty, v in ticks:
        near = [g for g in grids if 1.5 < g - ty < 4.0]
        if near:
            pairs.append((near[0], v))
    assert len(pairs) >= len(ticks) - 1 and len(pairs) >= 3, (ticks, grids)
    gy = np.array([g for g, _ in pairs])
    val = np.array([v for _, v in pairs])
    b, a = np.polyfit(gy, val, 1)
    resid = np.abs(a + b * gy - val).max()
    assert resid < 0.005, resid   # gridline coordinates are rounded in the drawing (about 0.001 kWh)
    print("axis fit: max residual %.4f kWh" % resid)
    return lambda y: a + b * np.asarray(y)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf")
    a = ap.parse_args()
    r = pypdf.PdfReader(a.pdf)
    rows = []
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    done_pages = {}
    for fig, (pi, labels) in FIGURES.items():
        page = r.pages[pi]
        charts = done_pages.setdefault(pi, [])
        if not charts:
            for k, f in forms(page):
                g, s = parse(f.get_data())
                if s:
                    charts.append((k, g, s, texts(f.get_data())))
        # page 36 holds two charts (4.8 then 4.9) in drawing order
        idx = 1 if fig == "4.9" else 0
        k, grids, series, txt = charts[idx]
        scale = y_scale(grids, txt)
        if labels is None:   # legend entries of Figure 4.2 (below the plot), left to right = drawing order
            labels = [t for x, y, t in sorted(txt) if y < 30 and ("eople" in t or "erson" in t)]
        assert len(labels) == len(series), (fig, labels, len(series))
        rows_fig = []
        for j, (c, pts) in enumerate(series):
            xs = np.array([p[0] for p in pts])
            assert np.all(np.diff(xs) > 0)
            rows_fig.append((labels[j], c, scale([p[1] for p in pts])))
        rows.append((fig, rows_fig))
    out = []
    for fig, rf in rows:
        for name, c, kwh in rf:
            for p, v in enumerate(kwh, 1):
                out.append({"figure": fig, "series": name, "period": p, "kwh": round(float(v), 4)})
    df = pd.DataFrame(out)
    with open(os.path.join(HERE, "data", "esc2019_profiles.csv"), "w", newline="") as fh:
        fh.write("# ACIL Allen (2019) Victorian energy usage profiles, report to the ESC, Figures 4.1, 4.2, 4.8-4.10: average\n"
                 "# daily consumption (kWh) in each half-hour period (1 = 00:00-00:30), read from the vector charts\n"
                 "# (extract_esc_profiles.py). Victorian sample, mostly 2016 meter data; grid import (PV homes: net of self-use).\n")
        df.to_csv(fh, index=False)
    print(df.groupby(["figure", "series"], sort=False).kwh.agg(["sum", "min", "max"]).round(2).to_string())
    for fig, g in df.groupby("figure", sort=False):
        plt.figure(figsize=(8, 3.5))
        for name, h in g.groupby("series", sort=False):
            plt.plot(h.period, h.kwh, marker=".", label=name)
        plt.xlabel("Half-hour period"); plt.ylabel("Average daily consumption (kWh)")
        plt.title(f"Figure {fig}, re-plotted from the extracted values"); plt.legend(fontsize=7); plt.xticks(range(1, 49, 2))
        plt.grid(alpha=.3); plt.tight_layout()
        plt.savefig(os.path.join(HERE, "data", f"esc2019_fig{fig.replace('.', '_')}.png"), dpi=110); plt.close()


if __name__ == "__main__":
    main()
