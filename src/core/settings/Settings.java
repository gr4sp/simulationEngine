package core.settings;

import core.Policies.EndConsumerTariff;

import java.util.List;
import java.util.Map;


class SimulationDatesSettings implements java.io.Serializable {
    public String startDate;
    public String endDate;
    public String startDateSpotMarket;

}

class PopulationSettings implements java.io.Serializable {
    public String areaCode;
    public double populationPercentageAreacCode;
    public int maxHouseholdsPerConsumerUnit;
    public double domesticConsumptionPercentage;

}

class PolicySettings implements java.io.Serializable {
    public double uptakeRate;
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
    public double priceMinMWh;
    public double priceMaxMWh;

    public double minCapacityFactor;
    public double maxCapacityFactor;
    public double maxCapacityFactorSummer;

    public List<EmissionFactor> emissionFactors;
    public SolarEfficiency solarEfficiency;

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
    public double annualCpi;
    public Boolean includePublicallyAnnouncedGen;
    public String rooftopPV;
    public int generatorRetirement;
    public double technologicalImprovement;
    public double learningCurve;
    public int generationRolloutPeriod;
    public double solarInstallCapacity;

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

    public double getPriceMinMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).priceMinMWh;
    }

    public double getPriceMaxMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).priceMaxMWh;
    }

    public void setPriceMinMWh(String fuelType, String techType, double price) {
        getGenSettings(fuelType, techType).priceMinMWh = price;
    }

    public void setPriceMaxMWh(String fuelType, String techType,  double price) {
        getGenSettings(fuelType, techType).priceMaxMWh = price;
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

    public double getUptakeRate() {
        return policy.uptakeRate;
    }

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

    public double getAnnualCpiForecast() { return forecast.annualCpi; }

    public Boolean getIncludePublicallyAnnouncedGen() { return forecast.includePublicallyAnnouncedGen;  }

    public String getRooftopPVForecast() { return forecast.rooftopPV;  }

    public int getForecastGeneratorRetirement() { return forecast.generatorRetirement; }

    public double getForecastTechnologicalImprovement() { return forecast.technologicalImprovement; }

    public double getLearningCurve() { return forecast.learningCurve; }

    public int getForecastGenerationRolloutPeriod() { return forecast.generationRolloutPeriod; }


    public double getForecastSolarInstallCapacity() { return forecast.solarInstallCapacity; }

}
