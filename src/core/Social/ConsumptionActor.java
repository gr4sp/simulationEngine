package core.Social;

public interface ConsumptionActor {

     //boolean consumptionOnly = true; // if the actor has on-site generation, then this will be set to false

     double getCurrentConsumption();
     float getCurrentTariff();
     double getCurrentEmissions();
     double computeConsumption(int month);


}

