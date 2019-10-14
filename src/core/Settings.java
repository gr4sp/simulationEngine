package core;

import core.Policies.EndConsumerTariff;

import java.util.List;
import java.util.Map;

class SimulationDatesSettings{
    public String startDate;
    public String endDate;
    public String startDateSpotMarket;
}

class PopulationSettings{
    public String areaCode;
    public double populationPercentageAreacCode;
    public int maxHouseholdsPerConsumerUnit;
    public double domesticConsumptionPercentage;
}

class PolicySettings{
    public double uptakeRate;
    public EndConsumerTariff endConsumerTariff;
}

class EmissionFactor{
    public int startYear;
    public double minEF;
    public double linRateEF;
    public double expRateEF;
}

class GeneratorSettings{
    public double priceMinMWh;
    public double priceMaxMWh;
    public double priceRateParameterMWh;

    public double minCapacityFactor;
    public double maxCapacityFactor;
    public double maxCapacityFactorSummer;

    public List<EmissionFactor> emissionFactors;


    public EmissionFactor selectEmissionFactor(int startYear){
        int selectedEFidx = 0;


        //Get the emission Factor for the correct startYear
        for(int i = 0; i < emissionFactors.size(); i++){
            if( emissionFactors.get(i).startYear <= startYear ) {
                int nextI = i + 1;
                if( nextI < emissionFactors.size() ){
                    if(emissionFactors.get(nextI).startYear > startYear )
                        return emissionFactors.get(i);
                }else{
                    return emissionFactors.get(i);
                }
            }
        }
        return null;
    }

}

public class Settings {
    //public int ConstantMaxInt;
    public SimulationDatesSettings simulationDates;
    public PopulationSettings population;
    public PolicySettings policy;
    public Map<String, GeneratorSettings> generators;


    //Get the Generator Settings with a composite Key
    private GeneratorSettings getGenSettings(String fuelType, String techType) {
        if(generators.containsKey(fuelType)) {
            return generators.get(fuelType);
        }else{
            for (Map.Entry<String, GeneratorSettings> e : generators.entrySet()) {
                if( e.getKey().contains(fuelType) && e.getKey().contains( techType ) ){
                    return e.getValue();
                }
            }
        }

        return generators.get("Default");
    }
    
    public double getPriceMinMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).priceMinMWh;
    }

    public double getPriceMaxMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).priceMaxMWh;
    }

    public double getPriceRateParameterMWh(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).priceRateParameterMWh;
    }

    public double getMinCapacityFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).minCapacityFactor;
    }

    public double getMaxCapacityFactor(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).maxCapacityFactor;
    }

    public double getMaxCapacityFactorSummer(String fuelType, String techType) {
        return getGenSettings(fuelType, techType).maxCapacityFactorSummer;
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
}
