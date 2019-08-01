package core;

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
        this.totalGWh = totalGWh;
    }
}


