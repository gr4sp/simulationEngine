package core;

import java.util.ArrayList;

public class ConsumptionProfile {

    private String demandType; //householdA, householdB, householdC, ..., commercialA, commercialB,...,industryA, industryB
    private ArrayList<Double> consumption; //weekly consumption in KwH

    public ConsumptionProfile(String demandType, ArrayList<Double> consumption) {
        this.demandType = demandType;
        this.consumption = consumption;
    }

    public String getDemandType() {
        return demandType;
    }

    public void setDemandType(String demandType) {
        this.demandType = demandType;
    }

    public ArrayList<Double> getConsumption() {
        return consumption;
    }

    public void setConsumption(ArrayList<Double> consumption) {
        this.consumption = consumption;
    }
}


