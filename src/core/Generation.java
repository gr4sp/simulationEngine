package core;

import com.lowagie.text.pdf.ArabicLigaturizer;
import sim.util.Bag;

import java.util.ArrayList;
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

    public ArrayList<Generator> filterGens( ArrayList<Generator> gens, String fuelType){
        ArrayList<Generator> filteredGens = new ArrayList<Generator>();

        for(Generator g : gens){
            if(g.getFuelSourceDescriptor().equalsIgnoreCase(fuelType))
                filteredGens.add(g);
        }
        return filteredGens;
    }

    public double emmissionByFuelType(ArrayList<Generator> activeGens, float percentage, String fuelType){

        ArrayList<Generator> filteredGens = filterGens(activeGens, fuelType);
        double emissions = 0;

        //Get total capacity by Fuel Type
        double sumCapacity =0;
        for (Generator g : filteredGens)
            sumCapacity+=g.getMaxCapacity();

        for (Generator g : filteredGens) {
            //Compute Emission Contribution based on Capacity of all generators of a specific fuelType
            //Normalize it over the total capacity of the fuel type
            double contribution = g.getMaxCapacity() / sumCapacity;

            //Weighted by the percentage of fuel type to total generation of system
            emissions += (contribution * g.getEmissionsFactor()) * percentage;

            /*System.out.println("\t Gen "+g.getName()+
                            " Contributes " + ((contribution * g.getEmissionsFactor()) * percentage) +
                    " GHG (t/co2), Capacity "+g.getMaxCapacity() +
                    " with Em Factor " + g.getEmissionsFactor() + " "+fuelType + " percentage of Total Gen: "+percentage*100.0 +"%"
                    );*/
        }
        return emissions;
    }

    public float computeEmissions(Spm spm) {

        //System.out.println("Generators used for " + date);

        float PsolarRooftopPVGWh = solarRooftopPVGWh / totalGWh;
        float PsolarUtilityGWh = solarUtilityGWh / totalGWh;
        float PwindGWh = windGWh / totalGWh;
        float PhydroGWh = hydroGWh / totalGWh;
        float PbatteryGWh = batteryGWh / totalGWh;
        float PgasOcgtGWh = gasOcgtGWh / totalGWh;
        float PgasSteamGWh = gasSteamGWh / totalGWh;
        float PbrownCoalGWh = brownCoalGWh / totalGWh;
        float PimportsGWh = importsGWh / totalGWh;
        float PexportsGWh = exportsGWh / totalGWh;

        float emissions = 0;

        ArrayList<Generator> activeGens = spm.getActiveGens(date);

        // Average emmission factors NSW and SA from "existing Generation" tab Snapshots xls. Includes all generation fuel types.
        double emissionsNSW = 0.78; //Steam turbine - Black Coal, also OCGT - Distillate, CCGT - Natural gas,Cogeneration - Natural gas
        double emissionsSA = 0.84; // Steam turbine - Black Coal, OCGT - Distillate, CCGT - Natural gas, Steam turbine - Natural gas

        if(PsolarRooftopPVGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PsolarRooftopPVGWh,"Solar");

        if(PsolarUtilityGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PsolarUtilityGWh,"Solar");

        if(PwindGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PwindGWh,"Wind");
        if(PhydroGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PhydroGWh,"Water");
        if(PbatteryGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PbatteryGWh,"Battery");
        if(PgasOcgtGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PgasOcgtGWh,"Natural Gas");
        if(PgasSteamGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PgasSteamGWh,"Natural Gas");
        if(PbrownCoalGWh > 0.0)
            emissions += emmissionByFuelType(activeGens,PbrownCoalGWh,"Brown Coal");
        if(PimportsGWh > 0.0)
            emissions += ((emissionsNSW + emissionsSA) / 2) * PimportsGWh;

        //Do not include Exports to consumer emissions
        //if(PexportsGWh > 0.0)
        //    emissions += emmissionByFuelType(activeGens,PexportsGWh,"Brown Coal");

        //System.out.println("Total emissions for " + date + " of "+emissions + " GHG (t/co2)");


        return emissions;
    }
}


