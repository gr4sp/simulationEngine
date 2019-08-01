package core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ConsumptionProfile {

    private String demandType; //householdA, householdB, householdC, ..., commercialA, commercialB,...,industryA, industryB
    private HashMap<Date, Double> consumption; //monthly consumption in KwH

    public ConsumptionProfile(String demandType, HashMap<Date, Double> consumption) {
        this.demandType = demandType;
        this.consumption = consumption;
    }

    public String getDemandType() {
        return demandType;
    }

    public void setDemandType(String demandType) {
        this.demandType = demandType;
    }

}


