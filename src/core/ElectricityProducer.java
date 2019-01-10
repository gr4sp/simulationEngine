package core;

public class ElectricityProducer extends Actor {
    private String size; //small, medium, large business. Multinational
   // private ConsumptionProfile consumptionProfile;

    public ElectricityProducer(int actor, String actorType, String name, String main_role,  String organisationType, String size) {
        super(actor, actorType, name, main_role, organisationType);
        this.size = size;
    }

}
