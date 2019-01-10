package core;

public class ConsumptionProfile {

    private String demandType; //householdA, householdB, householdC, ..., commercialA, commercialB,...,industryA, industryB
    private double consumption; //weekly consumption in KwH

    public ConsumptionProfile(String demandType, double consumption) {
        this.demandType = demandType;
        this.consumption = consumption;
    }

    public String getDemandType() {
        return demandType;
    }

    public void setDemandType(String demandType) {
        this.demandType = demandType;
    }

    public double getConsumption() {
        return consumption;
    }

    public void setConsumption(double consumption) {
        this.consumption = consumption;
    }
}


