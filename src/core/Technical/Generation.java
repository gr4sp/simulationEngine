package core.Technical;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Generation {

    public Date date;
    public Float volumeDollarMWh;
    public Float temperature;

    public Float solarRooftopPVdollarGWh;
    public Float solarRooftopPVGWh;

    public Float solarUtilitydollarGWh;
    public Float solarUtilityGWh;

    public Float windDollarGWh;
    public Float windGWh;

    public Float hydroDollarGWh;
    public Float hydroGWh;

    public Float batteryDollarGWh;
    public Float batteryGWh;

    public Float gasOcgtDollarGWh;
    public Float gasOcgtGWh;

    public Float gasSteamDollarGWh;
    public Float gasSteamGWh;

    public Float brownCoalDollarGWh;
    public Float brownCoalGWh;

    public Float importsDollarGWh;
    public Float importsGWh;

    public Float exportsDollarGWh;
    public Float exportsGWh;

    public Float totalGWh;

    public Generation( Generation g ){
        this.date = g.date;
        this.volumeDollarMWh = g.volumeDollarMWh;
        this.temperature = g.temperature;
        this.solarRooftopPVdollarGWh = g.solarRooftopPVdollarGWh;
        this.solarRooftopPVGWh = g.solarRooftopPVGWh;
        this.solarUtilitydollarGWh = g.solarUtilitydollarGWh;
        this.solarUtilityGWh = g.solarUtilityGWh;
        this.windDollarGWh = g.windDollarGWh;
        this.windGWh = g.windGWh;
        this.hydroDollarGWh = g.hydroDollarGWh;
        this.hydroGWh = g.hydroGWh;
        this.batteryDollarGWh = g.batteryDollarGWh;
        this.batteryGWh = g.batteryGWh;
        this.gasOcgtDollarGWh = g.gasOcgtDollarGWh;
        this.gasOcgtGWh = g.gasOcgtGWh;
        this.gasSteamDollarGWh = g.gasSteamDollarGWh;
        this.gasSteamGWh = g.gasSteamGWh;
        this.brownCoalDollarGWh = g.brownCoalDollarGWh;
        this.brownCoalGWh = g.brownCoalGWh;
        this.importsDollarGWh = g.importsDollarGWh;
        this.importsGWh = g.importsGWh;
        this.exportsDollarGWh = g.exportsDollarGWh;
        this.exportsGWh = g.exportsGWh;
        this.totalGWh = g.totalGWh - this.exportsGWh;
    }
    public Generation(Date date, Float volumeDollarMWh, Float temperature, Float solarRooftopPVdollarGWh, Float solarRooftopPVGWh,
                      Float solarUtilitydollarGWh, Float solarUtilityGWh, Float windDollarGWh, Float windGWh, Float hydroDollarGWh,
                      Float hydroGWh, Float batteryDollarGWh, Float batteryGWh, Float gasOcgtDollarGWh, Float gasOcgtGWh, Float gasSteamDollarGWh,
                      Float gasSteamGWh, Float brownCoalDollarGWh, Float brownCoalGWh, Float importsDollarGWh, Float importsGWh, Float exportsDollarGWh,
                      Float exportsGWh, Float totalGWh) {
        this.date = date;
        this.volumeDollarMWh = volumeDollarMWh;
        this.temperature = temperature;
        this.solarRooftopPVdollarGWh = solarRooftopPVdollarGWh;
        this.solarRooftopPVGWh = solarRooftopPVGWh;
        this.solarUtilitydollarGWh = solarUtilitydollarGWh;
        this.solarUtilityGWh = solarUtilityGWh;
        this.windDollarGWh = windDollarGWh;
        this.windGWh = windGWh;
        this.hydroDollarGWh = hydroDollarGWh;
        this.hydroGWh = hydroGWh;
        this.batteryDollarGWh = batteryDollarGWh;
        this.batteryGWh = batteryGWh;
        this.gasOcgtDollarGWh = gasOcgtDollarGWh;
        this.gasOcgtGWh = gasOcgtGWh;
        this.gasSteamDollarGWh = gasSteamDollarGWh;
        this.gasSteamGWh = gasSteamGWh;
        this.brownCoalDollarGWh = brownCoalDollarGWh;
        this.brownCoalGWh = brownCoalGWh;
        this.importsDollarGWh = importsDollarGWh;
        this.importsGWh = importsGWh;
        this.exportsDollarGWh = exportsDollarGWh;
        this.exportsGWh = exportsGWh;
        this.totalGWh = totalGWh - this.exportsGWh;
    }

    public ArrayList<Generator> filterGens(ArrayList<Generator> gens, String fuelType) {
        ArrayList<Generator> filteredGens = new ArrayList<Generator>();

        for (Generator g : gens) {
            if (g.getFuelSourceDescriptor().equalsIgnoreCase(fuelType))
                filteredGens.add(g);
        }
        return filteredGens;
    }


    public double CDEFuelType(int currentYear, ArrayList<Generator> activeGens, float MWh, String fuelType) {

        ArrayList<Generator> filteredGens = filterGens(activeGens, fuelType);

        //Get total capacity by Fuel Type (sumCapacity)
        //Normalize it over the total capacity of the fuel type (1/sumCi) Ci per fuel type or power plant

        double sumCapacity = 0;
        for (Generator g : filteredGens)
            sumCapacity += g.getMaxCapacity();

        //Compute Emission Factor based on Capacity of all generators of a specific fuelType

        double emissionFactor = 0;
        for (Generator g : filteredGens) {

            //Weighted by the percentage of fuel type to total generation of system (capacity factor). emissionIntensity index of a generator is in tCO2-e/MWh
            // and depends on the emisisonFactor per technology and fueltype. This emission factor is in tCO2-e/MWh and is in the DB
            emissionFactor += (g.getMaxCapacity() * g.getEmissionsFactor(currentYear)) / sumCapacity ;

        }
        return emissionFactor * MWh;
    }


    public float computeGenEmissionIntensity(ArrayList<Generator> activeGens, Date today ) {

        Calendar c = Calendar.getInstance();
        c.setTime(today);
        int currentYear = c.get(Calendar.YEAR);

        // Average emmission factors NSW and SA from "existing Generation" tab Snapshots xls. Includes all generation fuel types.
        double emissionsNSW = 0.78; //Steam turbine - Black Coal, also OCGT - Distillate, CCGT - Natural gas,Cogeneration - Natural gas
        double emissionsSA = 0.84; // Steam turbine - Black Coal, OCGT - Distillate, CCGT - Natural gas, Steam turbine - Natural gas

        //Find out the total Emission Factor of all fuel types combined. Using the
        float CDE = 0;

        if (solarRooftopPVGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, solarRooftopPVGWh * 1000, "Solar");

        if (solarUtilityGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, solarUtilityGWh * 1000, "Solar");

        if (windGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, windGWh * 1000, "Wind");
        if (hydroGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, hydroGWh * 1000, "Water");
        if (batteryGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, batteryGWh * 1000, "Battery");
        if (gasOcgtGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, gasOcgtGWh * 1000, "Natural Gas");
        if (gasSteamGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, gasSteamGWh * 1000, "Natural Gas");
        if (brownCoalGWh > 0.0)
            CDE += CDEFuelType(currentYear, activeGens, brownCoalGWh * 1000, "Brown Coal");

        //If no emissions are accounted, it's because we only use renewables onsite and we are not at an generation SPM
        if(CDE == 0.0) return (float)0.0;

        if (importsGWh > 0.0)
            CDE += ((emissionsNSW + emissionsSA) / 2) * importsGWh * 1000;

        //return emissions intensity in (M?)tCO2-e/MWh
        return CDE / (totalGWh * 1000);
    }
}


