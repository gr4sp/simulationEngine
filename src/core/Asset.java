package core;

public interface Asset {
    double electricityIn();
    double electricityOut();
    void addAssetRelationship( ActorAssetRelationship newAssetRel);


}
