package core;

enum ActorAssetRelationshipType{
    OWN,LEASE,USE;
}

public class ActorAssetRelationship {
    private Actor actor;
    private ActorAssetRelationshipType type; // ownership, lease
    private double percentage; //share of ownership over an asset
    private Asset asset;

    public ActorAssetRelationship(Actor actor, Asset asset, ActorAssetRelationshipType type, double percentage) {
        this.actor = actor;
        this.type = type;
        this.percentage = percentage;
        this.asset = asset;
    }
}
