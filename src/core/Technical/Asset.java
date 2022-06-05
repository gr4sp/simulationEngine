package core.Technical;

import core.Relationships.ActorAssetRelationship;

public interface Asset {
    double diameter(); // for visualization purposes
    void addAssetRelationship( ActorAssetRelationship newAssetRel);


    double getEmissionsFactor(int currentYear);
}
