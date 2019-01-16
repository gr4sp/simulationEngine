package core;

import core.Actor;

public class Household extends Actor implements ConsumptionActor {
    private int numberOfAdults;
    private int numberOfChildren;
    private boolean family;
    private double income; //median weekly income according to ABS census 2016
    private ConsumptionProfile consumptionProfile;


    public Household(int actor, ActorType actorType, String name, GovRole mainRole, BusinessStructure businessType, OwnershipModel ownershipModel, int numberOfAdults, int numberOfChildren, boolean family, double income, String demandType, double consumption) {
        super(actor, actorType, name, mainRole, businessType, ownershipModel);
        this.numberOfAdults = numberOfAdults;
        this.numberOfChildren = numberOfChildren;
        this.family = family;
        this.income = income;
        this.consumptionProfile = new ConsumptionProfile(demandType, consumption);
    }

    public int getNumberOfAdults() {
        return numberOfAdults;
    }

    public void setNumberOfAdults(int numberOfAdults) {
        this.numberOfAdults = numberOfAdults;
    }

    public int getNumberOfChildren() {
        return numberOfChildren;
    }

    public void setNumberOfChildren(int numberOfChildren) {
        this.numberOfChildren = numberOfChildren;
    }

    public boolean isFamily() {
        return family;
    }

    public void setFamily(boolean family) {
        this.family = family;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    @Override
    public double computeConsumption() {
        double consumptionTotal = 0.0;
        double onSiteGeneration = 0.0; //this value should be taken from onsite generation of household as weekly average per year
        if (consumptionOnly == false) {
            consumptionTotal = onSiteGeneration - consumptionProfile.getConsumption();
            return consumptionTotal;
        } else {
            return consumptionProfile.getConsumption();
        }
    }
}

