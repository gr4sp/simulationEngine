"""Victorian utility capacity path for simulationSettings/capacity_path_VIC.csv (opt-in capacityPathFile setting).

Source: AEMO 2020 ISP Generation Outlook workbooks ("Final 2020 ISP Generation Outlook.zip", kept with the article
data in Sandbox/papers/GR4SPArticle_ERSS/data/). Default case: Central scenario, DP1, the least-cost development path in
the Central scenario (2020 ISP CBA appendix, Appendix 2, p. 26). Sheets Capacity_2 (installed MW by region and
technology) and StorageCapacity_2 (storage MW by depth); capacity on 1 July, financial year ending, FY2022-FY2042
(FY2021 is the workbook's "Existing+Committed in 2021" column).

Rows written per financial year: Wind; Solar (utility, not distributed PV); Battery = shallow + medium + deep storage
(VIC has shallow only), 2 h of storage assumed (ISP shallow storage); Gas Pipeline = CCGT + peaking gas+liquids.
Brown coal and hydro are not written: they keep the database's own dates. The model tops each written fuel up to the
row's capacity from 1 July of (year - 1) with generic units and holds the last year's capacity to the end of the run
(LoadData.addCapacityPath); it never removes capacity.

Usage: python scripts/data/capacity_path_isp2020.py WORKBOOK OUT_CSV [CASE_LABEL]
"""
import sys
import pandas as pd

WORKBOOK, OUT = sys.argv[1], sys.argv[2]
LABEL = sys.argv[3] if len(sys.argv) > 3 else "AEMO 2020 ISP Central DP1"
HOURS = 2.0


def region_table(sheet):
    d = pd.read_excel(WORKBOOK, sheet, header=None)
    head = [str(h) for h in d.iloc[2].tolist()]
    d = d.iloc[3:].copy()
    d.columns = head
    v = d[d["Region"] == "VIC"].set_index("Technology").drop(columns="Region")
    v = v.apply(pd.to_numeric, errors="coerce").dropna(how="all", axis=1)
    v.columns = [2021 if "2021" in c else int(float(c)) for c in v.columns]
    return v


cap = region_table("Capacity_2")
sto = region_table("StorageCapacity_2")
fuels = {
    "Wind": ("Wind Turbine - Onshore", "SS", "Wind", 0.0, cap.loc["Wind"]),
    "Solar": ("Solar PV - Single axis tracking", "SS", "Solar", 0.0, cap.loc["Solar"]),
    "Battery": ("Storage - Battery", "S", "Battery Storage", HOURS,
                sto.loc[["Shallow Storage", "Medium Storage", "Deep Storage"]].sum()),
    "Gas Pipeline": ("Turbine - OCGT", "S", "OCGT", 0.0, cap.loc["CCGT"] + cap.loc["Peaking Gas+Liquids"]),
}
rows = []
for fuel, (tech, dispatch, bucket, hours, series) in fuels.items():
    for year, mw in series.dropna().items():
        rows.append([int(year), fuel, tech, dispatch, bucket, round(float(mw), 1), hours, LABEL])
out = pd.DataFrame(rows, columns=["fy_end", "fuel_type", "technology_type", "dispatch_type", "fuel_bucket_summary",
                                  "capacity_mw", "storage_hours", "source"]).sort_values(["fuel_type", "fy_end"])
with open(OUT, "w", newline="") as f:
    f.write("# Victorian utility capacity path (MW on 1 July of fy_end - 1); built by scripts/data/capacity_path_isp2020.py\n")
    out.to_csv(f, index=False)
print(out.pivot(index="fy_end", columns="fuel_type", values="capacity_mw").to_string())
