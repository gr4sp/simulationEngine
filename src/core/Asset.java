package core;

public interface Asset {
    double electricityIn();
    double electricityOut();
    double diameter(); // for visualization purposes
    void addAssetRelationship( ActorAssetRelationship newAssetRel);


}
