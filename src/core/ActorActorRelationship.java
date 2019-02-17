package core;

enum ActorActorRelationshipType {
    BILLING,OTC,ETF,SPOT,ACCESS_FEE,COMMISSION_FEE,P2P, OWNS, OTHER;  //additional "owns" relationship to represent subsidiaries.
}

public class ActorActorRelationship {
    private Actor actor1;
    private Actor actor2;
    private ActorActorRelationshipType type; //relationship type: e.g. bill payment, power purchase agreement (specific time limit), legal, etc.

    public ActorActorRelationship(Actor actor1, Actor actor2, ActorActorRelationshipType type) {
        this.actor1 = actor1;
        this.actor2 = actor2;
        this.type = type;
    }
}
