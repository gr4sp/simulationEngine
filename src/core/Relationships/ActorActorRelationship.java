package core.Relationships;

import core.Social.Actor;


public class ActorActorRelationship implements java.io.Serializable{
    private Actor actor1;
    private Actor actor2;
    private ActorActorRelationshipType type; //relationship type: e.g. bill payment, power purchase agreement (specific time limit), legal, etc.

    public ActorActorRelationship(Actor actor1, Actor actor2, ActorActorRelationshipType type) {
        this.actor1 = actor1;
        this.actor2 = actor2;
        this.type = type;
    }
}
