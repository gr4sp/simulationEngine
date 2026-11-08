package core.settings;

import core.Policies.EndConsumerTariff;

import java.util.List;
import java.util.Map;


class SimulationDatesSettings implements java.io.Serializable {
    public String startDate;
    public String endDate;
    public String startDateSpotMarket;
    // Optional: first date written to the output CSVs. Earlier months still run as spin-up.
    public String reportFrom;

}

class PopulationSettings implements java.io.Serializable {
    public String areaCode;
    public double populationPercentageAreacCode;
    public int maxHouseholdsPerConsumerUnit;
    public double domesticConsumptionPercentage;

}

class PolicySettings implements java.io.Serializable {
    public EndConsumerTariff endConsumerTariff;
    public double annualInflation;

}

class EmissionFactor implements java.io.Serializable  {
    public int startYear;
    public double minEF;
    public double linRateEF;
    public double expRateEF;
}

class ArenaSettings implements java.io.Serializable {
    public String spotMarket;
    public double minCapMarketGen;

}

class SolarEfficiency implements java.io.Serializable {
    //de-rating factor for manufacturing tolerance, dimensionless
    public double fman ;
    //de-rating factor for dirt, dimensionless
    public double fdirt;
    //temperature de-rating factor, dimensionless, ƒtemp = 1 + (γ × (avg temp) y=-.005 * 20
    //y is the temperature coefficient, using example from CEC guidelines as -0.5%/C and average daily temperature of 20C
    //the de-rating factor increases with increasing average daily temperatures.
    public double ftemp;
    //efficiency of the subsystem (cables) between the PV array
    //and the inverter (DC cable loss)
    public double npv_inv;
    //efficiency of the inverter. Typically 0.9
    public double ninv;
    //efficiency of the subsystem (cables) between the inverter and the switchboard (AC cable loss)
    // recommended voltage drop between inverter and main switch shouldn't be greater than 1%
    public double ninv_sb;
    //solar exposure in data base is in MJ/m2 but converted to KWh/m2 when loaded. Capacity is assumed to be in m^2

    public double solarEfficiency;

    public void computeSolarEfficiency(){
        solarEfficiency = fman * fdirt * ftemp  * npv_inv * ninv * ninv_sb;
    }

    public void improveSolarEfficiency( double factor ){
        solarEfficiency += factor;
    }
}

class GeneratorSettings implements java.io.Serializable {
    public double basePriceMWh;
    public double marketPriceCap;
    //Offer price in the spot market at short-run marginal cost, $/MWh. Negative (the default) keeps the
    //LCOE rule, basePriceMWh / historic capacity factor (ROADMAP item 3)
    public double srmcMWh = -1;
    //Storage offer: this factor times the previous month's mean primary spot price. Negative (the default)
    //keeps the LCOE rule, which prices a rarely used battery at basePriceMWh / minCapacityFactor
    public double storageOfferFactor = -1;
    //Share of nameplate capacity offered in each half-hour. Negative (the default) offers nameplate x the capacity
    //factor, so maxCapacityFactor limits power as well as energy. Read from the base settings only, like the capacity factors
    public double availabilityFactor = -1;
    //Energy-limited plant (hydro): at most nameplate x capacity factor x 24 h a day, offered as long as it lasts
    public boolean dailyEnergyLimit = false;
    //Storage that buys low and sells high each day when positive: the share of stored energy returned on discharge.
    //Negative (the default) keeps the storage offer rules above
    public double roundTripEfficiency = -1;
    //Storage duration in hours, used when the database gives no energy capacity
    public double storageHours = 1;
    //Short-run cost offer (a): heat rate x fuel price + variable O&M + emission factor x carbon price, when heatRate > 0.
    //The fuel price comes from fuelPriceFile (year,priceGJ,source in simulationSettings) or the constant fuelPriceGJ
    public double heatRateGJperMWh = -1;
    public double variableOMMWh = 0;
    public String fuelPriceFile;
    public double fuelPriceGJ = 0;
    //Price bands (b): this share of the offered capacity is offered at mustRunPriceMWh (inflexible plant), the rest at the offer
    public double mustRunShare = 0;
    public double mustRunPriceMWh = 0;
    //Energy-limited plant offers its daily energy only in the day's highest-demand half-hours (hydro water value)
    public boolean peakOnly = false;
    //Breakdowns (c): long-run share of time a unit is fully out, and the mean time to repair in hours
    public double forcedOutageRate = 0;
    public double outageHours = 168;

    public double minCapacityFactor;
    public double maxCapacityFactor;
    public double maxCapacityFactorSummer;

    public List<EmissionFactor> emissionFactors;
    public SolarEfficiency solarEfficiency;

    public double nameplateCapacityChange;

    public EmissionFactor selectEmissionFactor(int startYear) {
        int selectedEFidx = 0;


        //Get the emission Factor for the correct startYear
        for (int i = 0; i < emissionFactors.size(); i++) {
            if (emissionFactors.get(i).startYear <= startYear) {
                int nextI = i + 1;
                if (nextI < emissionFactors.size()) {
                    if (emissionFactors.get(nextI).startYear > startYear)
                        return emissionFactors.get(i);
                } else {
                    return emissionFactors.get(i);
                }
            }
        }
        return null;
    }

}

class TariffSettings implements java.io.Serializable  {
    public double fixed;
    public double usage;

}

class ScenarioSetting implements java.io.Serializable {
    public String consumption;
    public String energyEfficiency;
    public String onsiteGeneration;
    public String solarUptake;
}

class ForecastSetting implements java.io.Serializable {
    public ScenarioSetting scenario;
    public int baseYear;
    public String startDemandForecast;
    public double annualCpi;
    public Boolean includePublicallyAnnouncedGen;
    public String rooftopPV;
    public int generatorRetirement;
    public double technologicalImprovement;
    public double learningCurve;
    public int generationRolloutPeriod;
    public double solarInstallCapacity;
    public double importPriceFactor;


}

public class Settings implements java.io.Serializable {
    //public int ConstantMaxInt;
    public String folderOutput;
    public String reportGeneration;
    public String logLevel;
    public Map<String, ArenaSettings> arena;
    public SimulationDatesSettings simulationDates;
    public PopulationSettings population;
    public PolicySettings policy;
    public Map<String, GeneratorSettings> generators;
    public Map<String, TariffSettings> tariffs;
    public ForecastSetting forecast;
    //Optional CSV in simulationSettings with corrections to the generationassets table (see LoadData.readGeneratorCorrections)
    public String generatorCorrections;
    //Optional CSV in simulationSettings with monthly net exports (month,netExportsMWh,source), added to wholesale demand
    public String netExportsFile;
    //Optional CSV (month,slot,deviationMW,source): the half-hourly shape added to each month's mean net exports
    public String netExportsShapeFile;
    //When true (needs netExportsShapeFile, not netExportsFile): the monthly calibration of half-hourly demand to total
    //generation adds the month's difference as a flat MW amount plus the net-export shape, instead of scaling every
    //half-hour by the same factor. Monthly energy is unchanged; exports no longer grow with the demand peak
    public boolean shapeDemandUplift = false;
    //When true, a generator row with several units (no_units > 1, e.g. Murray 1 with 10) breaks down unit by unit,
    //each with the fuel's forcedOutageRate and outageHours, instead of the whole row going out at once
    public boolean outagesPerUnit = false;
    //Optional CSV in simulationSettings with the carbon price by month (month,priceTonne,source)
    public String carbonPriceFile;
    //Strategic withholding (d): when the offered capacity exceeds demand by less than withholdingMargin (a share of
    //demand), each portfolio holding at least withholdingMinPortfolioShare of the offered capacity reprices
    //withholdingShare of its dispatchable bids to withholdingPriceMWh. Negative margin (the default) turns it off.
    //Stations map to portfolios through portfolioFile (name_prefix,portfolio,source); unmapped stations stand alone
    public double withholdingMargin = -1;
    public double withholdingShare = 0;
    public double withholdingPriceMWh = 0;
    public double withholdingMinPortfolioShare = 0;
    public String portfolioFile;
    //Optional CSV (name_prefix,parameter,value,source) of station parameters that override the generator-type settings:
    //heatRateGJperMWh, variableOMMWh, fuelPriceGJ, forcedOutageRate, outageHours, availabilityFactor
    public String generatorParametersFile;
    //Variable O&M and constant fuel prices are in real dollars of costPriceYear; cpiFile (year,index,source) turns them
    //into each year's dollars (nearest year outside its range). Unset: costs are used as given
    public String cpiFile;
    public int costPriceYear = 0;


    /*
     * Tariff
     * */

    //Get the Generator Settings with a composite Key
    private TariffSettings getTariffSettings(String actorType) {
        if (tariffs.containsKey(actorType)) {
            return tariffs.get(actorType);
        } else {
            return null;
        }
    }


    public double getFixedTariff(String tariffComponent) {
        return getTariffSettings(tariffComponent).fixed;
    }

    public double getUsageTariff(String tariffComponent) {
        return getTariffSettings(tariffComponent).usage;
    }

    public void setUsageTariff(String tariffComponent, double tariff) {  getTariffSettings(tariffComponent).usage = tariff;}

    /*
     * Generator
     * */

    public void computeSolarEfficiency(){
        for (Map.Entry<String, GeneratorSettings> e : generators.entrySet()) {
            if( e.getValue().solarEfficiency != null )
                e.getValue().solarEfficiency.computeSolarEfficiency();
        }
    }

    //Get the Generator Settings with a composite Key
    private GeneratorSettings getGenSettings(String fuelType, String techType) {
        if (generators.containsKey(fuelType)) {
            return generators.get(fuelType);
        } else {
            for (Map.Entry<String, GeneratorSettings> e : generators.entrySet()) {
                if (e.getKey().contains(fuelType) && e.getKey().contains(techType)) {
                    return e.getValue();
                }
            }
        }

        return generators.get("Default");
    }

    public void applyInflationToAllGenBasePrice( double inflation ){
        for (Map.Entry<String, GeneratorSettings> e : generators.entrySet()) {
            e.getValue().basePriceMWh *= inflation;
            e.getValue().srmcMWh *= inflation;
        }
    }

    public void improveSolarEfficiency( double factor ) {
        for (Map.Entry<String, GeneratorSettings> e : generators.entrySet()) {
            if( e.getValue().solarEfficiency != null )
                e.getValue().solarEfficiency.improveSolarEfficiency( factor );
        }
    }

    public double getSolarEfficiency(String fuelType, String techType) {
        if(getGenSettings(fuelType, techType).solarEfficiency != null)
            return getGenSettings(fuelType, techType).solarEfficiency.solarEfficiency;
        return 0.0;
    }


    public double getBasePriceMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).basePriceMWh;
    }

    public double getSrmcMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).srmcMWh;
    }

    public double getStorageOfferFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).storageOfferFactor;
    }

    public double getAvailabilityFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).availabilityFactor;
    }

    public boolean getDailyEnergyLimit(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).dailyEnergyLimit;
    }

    public double getRoundTripEfficiency(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).roundTripEfficiency;
    }

    public double getStorageHours(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).storageHours;
    }

    public double getHeatRateGJperMWh(String f, String t) { return getGenSettings(f, t).heatRateGJperMWh; }

    public double getVariableOMMWh(String f, String t) { return getGenSettings(f, t).variableOMMWh; }

    public String getFuelPriceFile(String f, String t) { return getGenSettings(f, t).fuelPriceFile; }

    public double getFuelPriceGJ(String f, String t) { return getGenSettings(f, t).fuelPriceGJ; }

    public double getMustRunShare(String f, String t) { return getGenSettings(f, t).mustRunShare; }

    public double getMustRunPriceMWh(String f, String t) { return getGenSettings(f, t).mustRunPriceMWh; }

    public boolean getPeakOnly(String f, String t) { return getGenSettings(f, t).peakOnly; }

    public double getForcedOutageRate(String f, String t) { return getGenSettings(f, t).forcedOutageRate; }

    public double getOutageHours(String f, String t) { return getGenSettings(f, t).outageHours; }

    //Every fuelPriceFile named in the generator settings
    public java.util.Set<String> getFuelPriceFiles() {
        java.util.Set<String> files = new java.util.HashSet<>();
        for (GeneratorSettings gs : generators.values()) if (gs.fuelPriceFile != null) files.add(gs.fuelPriceFile);
        return files;
    }

    public double getMarketPriceCap(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).marketPriceCap;
    }

    public void setBasePriceMWh(String fuelType, String techType, double price) {
        getGenSettings(fuelType, techType).basePriceMWh = price;
    }

    public void setMarketPriceCap(String fuelType, String techType, double price) {
        getGenSettings(fuelType, techType).marketPriceCap = price;
    }

    public double getNameplateCapacityChange(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).nameplateCapacityChange;
    }

    public void setNameplateCapacityChange(String fuelType, String techType, double factor) {
        getGenSettings(fuelType, techType).nameplateCapacityChange = factor;
    }

    public double getMinCapacityFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).minCapacityFactor;
    }

    public double getMaxCapacityFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).maxCapacityFactor;
    }

    public void setMinCapacityFactor(String fuelType, String techType, double minCapacityFactor) {
        getGenSettings(fuelType, techType).minCapacityFactor = minCapacityFactor;
    }

    public void setMaxCapacityFactor(String fuelType, String techType,  double maxCapacityFactor) {
        getGenSettings(fuelType, techType).maxCapacityFactor = maxCapacityFactor;
    }

    public double getMaxCapacityFactorSummer(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).maxCapacityFactorSummer;
    }

    public void setMaxCapacityFactorSummer(String fuelType, String techType,  double maxCapacityFactorSummer) {
        getGenSettings(fuelType, techType).maxCapacityFactorSummer = maxCapacityFactorSummer;
    }
    public double getMinEF(String fuelType, String techType, int startYear) {

        return getGenSettings(fuelType, techType).selectEmissionFactor(startYear).minEF;
    }

    public double getLinRateEF(String fuelType, String techType, int startYear) {
        return getGenSettings(fuelType, techType).selectEmissionFactor(startYear).linRateEF;
    }

    public double getExpRateEF(String fuelType, String techType, int startYear) {
        return getGenSettings(fuelType, techType).selectEmissionFactor(startYear).expRateEF;

    }

    /**
     * SimulDates
     */
    public String getStartDate() {
        return simulationDates.startDate;
    }

    public String getEndDate() {
        return simulationDates.endDate;
    }

    public String getStartDateSpotMarket() {
        return simulationDates.startDateSpotMarket;
    }

    public String getReportFrom() {
        return simulationDates.reportFrom;
    }

    /**
     * Population
     */

    public String getAreaCode() {
        return population.areaCode;
    }

    public double getPopulationPercentageAreacCode() {
        return population.populationPercentageAreacCode;
    }

    public int getMaxHouseholdsPerConsumerUnit() {
        return population.maxHouseholdsPerConsumerUnit;
    }

    public double getDomesticConsumptionPercentage() {
        return population.domesticConsumptionPercentage;
    }

    /**
     * Policy
     */


    public EndConsumerTariff getEndConsumerTariff() {
        return policy.endConsumerTariff;
    }

    public double getAnnualInflation() {
        return policy.annualInflation;
    }

    /**
     * Arena
     */

    //Get the Generator Settings with a composite Key
    private ArenaSettings getArenaSettings(String dispatchType) {
        if (arena.containsKey(dispatchType)) {
            return arena.get(dispatchType);
        }

        return null;
    }

    public double getMinCapMarketGen(String dispatchType) {
        return getArenaSettings(dispatchType).minCapMarketGen;
    }
    public String getSpotMarket(String dispatchType) {
        return getArenaSettings(dispatchType).spotMarket;
    }

    public void setMinCapMarketGen(String dispatchType, double minCapMarketGen) {
        getArenaSettings(dispatchType).minCapMarketGen = minCapMarketGen;
    }
    public void setSpotMarket(String dispatchType, String marketType) {
         getArenaSettings(dispatchType).spotMarket = marketType;
    }


    public boolean isMarketPaticipant(String dispatchType, String spotMarketName, double capacity ){
        String fullDispatchName = "";
        if(dispatchType.equals("S"))
            fullDispatchName = "scheduled";
        if(dispatchType.equals("SS"))
            fullDispatchName = "semiScheduled";
        if(dispatchType.equals("NS"))
            fullDispatchName = "nonScheduled";

        if(getSpotMarket(fullDispatchName).equals(spotMarketName) && capacity >= getMinCapMarketGen(fullDispatchName) )
            return true;
        else
            return false;
    }

    public boolean existsMarket(String spotMarketName){
        for (ArenaSettings as : arena.values()) {
            if(as.spotMarket.equals(spotMarketName)){
                return true;
            }
        }
        return false;
    }

    public boolean existsOffMarket(){
        for (ArenaSettings as : arena.values()) {
            if(as.spotMarket.equals("none")){
                return true;
            }else if(as.minCapMarketGen > 0){
                return true;
            }
        }

        return false;
    }



    /**
     *  Forecast
     */

    public String getForecastScenarioConsumption() { return forecast.scenario.consumption; }

    public String getForecastScenarioEnergyEfficiency() { return forecast.scenario.energyEfficiency; }

    public String getForecastScenarioOnsiteGeneration() { return forecast.scenario.onsiteGeneration; }

    public String getForecastScenarioSolarUptake() { return forecast.scenario.solarUptake; }

    public int getBaseYearConsumptionForecast() { return forecast.baseYear;  }

    public String getStartDemandForecast() {
        return forecast.startDemandForecast;
    }

    public double getAnnualCpiForecast() { return forecast.annualCpi; }

    public Boolean getIncludePublicallyAnnouncedGen() { return forecast.includePublicallyAnnouncedGen;  }

    public String getRooftopPVForecast() { return forecast.rooftopPV;  }

    public int getForecastGeneratorRetirement() { return forecast.generatorRetirement; }

    public String getGeneratorCorrections() { return generatorCorrections; }

    public String getNetExportsFile() { return netExportsFile; }

    public String getNetExportsShapeFile() { return netExportsShapeFile; }

    public boolean getShapeDemandUplift() { return shapeDemandUplift; }

    public boolean getOutagesPerUnit() { return outagesPerUnit; }

    public String getCarbonPriceFile() { return carbonPriceFile; }

    public String getPortfolioFile() { return portfolioFile; }

    public String getGeneratorParametersFile() { return generatorParametersFile; }

    public String getCpiFile() { return cpiFile; }

    public int getCostPriceYear() { return costPriceYear; }

    public double getWithholdingMargin() { return withholdingMargin; }

    public double getWithholdingShare() { return withholdingShare; }

    public double getWithholdingPriceMWh() { return withholdingPriceMWh; }

    public double getWithholdingMinPortfolioShare() { return withholdingMinPortfolioShare; }

    public double getForecastTechnologicalImprovement() { return forecast.technologicalImprovement; }

    public double getLearningCurve() { return forecast.learningCurve; }

    public int getForecastGenerationRolloutPeriod() { return forecast.generationRolloutPeriod; }

    public double getForecastSolarInstallCapacity() { return forecast.solarInstallCapacity; }

    public double getImportPriceFactor() { return forecast.importPriceFactor; }

}
