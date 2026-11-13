"""
Station emission intensities and capacity factors of Victorian units from the NGER designated generation facility data
(Clean Energy Regulator), 2013-14 to 2018-19, as rows of simulationSettings/generator_parameters_VIC.csv (N25,
14 Nov 2026). Prints the block that goes between the NGER markers of that file.

- emissionFactorTonnesMWh: total scope 1 emissions / electricity production over the years a facility reported
  (NGER's own "emission intensity"). Morwell (Energy Brix) reports the briquette works with the power station, so its
  value is its heat rate (ACIL Tasman 2009, already in the file) x 95 kg CO2-e/GJ, the Latrobe Valley brown coal factor
  of Acil Allen (2016), Emission factors assumptions update for AEMO, s. 3.1.1.
- capacityFactor (off-market units): the method of AEMO's demand forecasts for other non-scheduled generation (AEMO
  2020, Electricity Demand Forecasting Methodology Information Paper, s. 4.1.2): each unit's mean annual output over
  the years with data / (its database capacity x 8,760 h); units without history take the total output of reported
  units of the same class in Victoria over their combined capacity; units embedded in an industrial or hospital load
  (captured by net metering, which AEMO removes from the fleet) get 0.

Inputs: the CER CSVs "Greenhouse and energy information for designated generation facilities" for 2013-14 to 2018-19
(https://cer.gov.au/document/greenhouse-and-energy-information-designated-generation-facilities-<YYYY-YY>), in DIR as
nger<YYYY-YY>.csv. 2013-14 has no state or MWh column: MWh = GJ / 3.6, state from the other years.

Usage: python scripts/data/nger_station_parameters_vic.py DIR
"""
import glob
import re
import sys

import pandas as pd

# model unit (exact database site name; "$" asks the model for an exact match) -> NGER facility, database MW
EMISSIONS = {
    "Loy Yang A Power Station": "Loy Yang Power Station and Mine",
    "Loy Yang B": "Loy Yang B Power Station",
    "Yallourn W$": "Yallourn Power Station",
    "Hazelwood": "Hazelwood Power Station",
    "Angelsea$": "Anglesea Power Station",
    "Newport$": "Newport Power Station",
    "Mortlake$": "Mortlake Power Station",
    "Jeeralang": "Jeeralang Power Station",
    "Somerton$": "Somerton Power Station",
    "Laverton North$": "Laverton North Power Station",
    "Bairnsdale$": "Bairnsdale Power Station",
    "Valley Power$": "Valley Power",
}
MORWELL = ("Morwell Power Station", 15.00 * 0.095)   # heat rate (ACIL 2009) x 95 kg/GJ (Acil Allen 2016)
LANDFILL = {   # class: landfill gas engines
    "Clayton$": ("Clayton LFG Power Station", 12.0),
    "Springvale$": ("Springvale LFG Power Station", 4.2),
    "Brooklyn$": ("Brooklyn LFG Power Station", 0.65 + 1.15 + 1.03),
    "Corio$": ("Corio LFG Power Station", 1.0),
    "Berwick$": ("Berwick LFG Power Station", 0.45 + 4.12),
    "Broadmeadows$": ("Broadmeadows LFG Power Station", 6.18),
    "Hallam Road$": ("Hampton Park Renewable Energy Facility", 8.984),
    "Wyndham Renewable Energy Facility$": ("Wyndham Renewable Energy Facility", 0.78 + 1.123),
    "Mornington Waste Disposal Facility$": ("Mornington Peninsula Renewable Energy Facility", 0.789),
    "Ballarat$": ("Ballarat Renewable Energy Facility", 0.789),
    "Bendigo$": ("Bendigo Renewable Energy Facility", 0.5),
    "Shepparton$": ("Shepparton Renewable Energy Facility", 0.78),
}
# no usable history: the database gives Wollert 1.06 MW before 2020, while NGER's Wollert facility produced 55 GWh a
# year (needs about 7 MW); the two biogas engines are not designated facilities
LANDFILL_CLASS = ["REWASTE WOLLERT$", "Wollert$", "Shepparton Wastewater Treatment Facility$", "Tatura Biomass Generator$"]
GAS_SMALL = {  # class: Origin's CBD trigeneration and small gas engines (matched by owner and size)
    "321 Exhibition Street Trigen$": ("Exhibition Street", 1.2),
    "990 Latrobe St$": ("La Trobe", 0.386),
    "200 Victoria St$": ("Drapac", 0.386),
    "Dandenong PEP$": ("Dandenong", 2.0),
}
GAS_SMALL_CLASS = ["Crown Melbourne$"]
ALL_CLASS = ["CARINA WEST H2E$"]                      # bagasse: no Victorian class, regional average of all
EMBEDDED = {"Longford$": "Esso Longford gas plant's own supply", "Qenos Cogeneration Facility$": "Qenos Altona plant's own supply",
            "Ballarat Base Hospital Plant$": "hospital backup diesel"}


def load(d):
    num = lambda s: pd.to_numeric(s.astype(str).str.replace(",", "").str.strip(), errors="coerce")
    frames = []
    for f in sorted(glob.glob(d + "/nger20*.csv")):
        y = re.search(r"(20\d\d-\d\d)", f).group(1)
        t = pd.read_csv(f, encoding="cp1252")
        t.columns = [re.sub(r"\s+", " ", c).strip() for c in t.columns]
        pick = lambda *ks: next(c for c in t.columns if all(k.lower() in c.lower() for k in ks))
        frames.append(pd.DataFrame({"fy": y, "facility": t[pick("Facility")].astype(str).str.strip(),
                                    "state": t[pick("State")].astype(str).str.strip() if any("State" in c for c in t.columns) else None,
                                    "mwh": num(t[pick("Production", "GJ")]) / 3.6, "scope1": num(t[pick("Scope 1")])}))
    a = pd.concat(frames)
    a["facility"] = a.facility.str.replace("’", "'")
    vic = set(a[a.state == "VIC"].facility)
    return a[a.facility.isin(vic)]


def main(d):
    a = load(d)
    g = a.groupby("facility").agg(mwh=("mwh", "mean"), mwh_sum=("mwh", "sum"), s1=("scope1", "sum"),
                                  years=("fy", lambda s: f"{s.min()} to {s.max()}"), n=("fy", "count"))
    out = []
    for unit, fac in EMISSIONS.items():
        r = g.loc[fac]
        out.append((unit, "emissionFactorTonnesMWh", f"{r.s1 / r.mwh_sum:.3f}",
                    f"NGER {fac}, scope 1 / production, {r.years} ({r.n} years)"))
    out.append((MORWELL[0], "emissionFactorTonnesMWh", f"{MORWELL[1]:.3f}",
                "heat rate 15.00 (ACIL 2009) x 95 kg/GJ (Acil Allen 2016); NGER Energy Brix includes the briquette works"))

    def cf_rows(table, label):
        tot_mwh = tot_mw = 0.0
        for unit, (fac, mw) in table.items():
            r = g.loc[fac]
            cf = r.mwh / (mw * 8760)
            tot_mwh += r.mwh
            tot_mw += mw
            out.append((unit, "capacityFactor", f"{cf:.3f}", f"NGER {fac}, mean {r.mwh:,.0f} MWh/yr {r.years} / {mw:g} MW"))
        return tot_mwh, tot_mw
    lf = cf_rows(LANDFILL, "landfill")
    lf_cf = lf[0] / (lf[1] * 8760)
    for unit in LANDFILL_CLASS:
        out.append((unit, "capacityFactor", f"{lf_cf:.3f}", f"Victorian landfill gas class: NGER output / capacity of {len(LANDFILL)} matched units (AEMO 2020 s. 4.1.2)"))
    gs = cf_rows(GAS_SMALL, "gas")
    gs_cf = gs[0] / (gs[1] * 8760)
    for unit in GAS_SMALL_CLASS:
        out.append((unit, "capacityFactor", f"{gs_cf:.3f}", "Victorian small gas engine class: NGER output / capacity of the 4 matched units (AEMO 2020 s. 4.1.2)"))
    all_cf = (lf[0] + gs[0]) / ((lf[1] + gs[1]) * 8760)
    for unit in ALL_CLASS:
        out.append((unit, "capacityFactor", f"{all_cf:.3f}", "no Victorian class: average of all matched other non-scheduled units (AEMO 2020 s. 4.1.2)"))
    for unit, why in EMBEDDED.items():
        out.append((unit, "capacityFactor", "0", f"embedded: {why}; AEMO 2020 s. 4.1.2 removes units captured by net metering, and the demand record (OpenElectricity) does not include them"))
    print("# --- NGER block: scripts/data/nger_station_parameters_vic.py (N25, 14 Nov 2026); a name ending in $ matches exactly")
    for row in out:
        print(",".join([row[0], row[1], row[2], '"' + row[3] + '"']))
    print("# --- end of NGER block")
    print(f"# class factors: landfill {lf_cf:.3f}, small gas {gs_cf:.3f}, all {all_cf:.3f}", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1])
