"""Monthly Victorian net exports (exports minus imports, MWh) for the netExportsFile setting.

Source: experiments/simulationData/2005_2020_OpenNemDataV1.csv (OpenNEM VIC1, April 2005 to June 2020), where
exports are negative GWh. Before April 2005 there is no flow series in the repository: those months are
constructed as the calendar-month mean of 2006-2008, the first three full years, and flagged in the source column.
Usage: python scripts/data/net_exports_opennem.py   (writes simulationSettings/net_exports_VIC.csv)"""
import os
import pandas as pd

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, os.pardir)
o = pd.read_csv(os.path.join(ROOT, "experiments", "simulationData", "2005_2020_OpenNemDataV1.csv"),
                index_col=0, parse_dates=True)
net = (-o["Exports - GWh"] - o["Imports - GWh"]) * 1000.0
rows = [(t.strftime("%Y-%m"), v, "OpenNEM VIC1 exports minus imports") for t, v in net.items()]
proxy = net[(net.index.year >= 2006) & (net.index.year <= 2008)].groupby(net.index[(net.index.year >= 2006) & (net.index.year <= 2008)].month).mean()
for t in pd.date_range("1998-01-01", "2005-03-01", freq="MS"):
    rows.append((t.strftime("%Y-%m"), proxy[t.month], "constructed: calendar-month mean of 2006-2008 (no flow data before April 2005)"))
out = pd.DataFrame(rows, columns=["month", "netExportsMWh", "source"]).sort_values("month")
out.to_csv(os.path.join(ROOT, "simulationSettings", "net_exports_VIC.csv"), index=False, float_format="%.1f")
print(out.groupby(out.month.str[:4]).netExportsMWh.sum().div(1e3).round(0).to_string())
