package core;

import sim.util.Double2D;

import java.util.ArrayList;

enum ActorType {
    RETAILER, ELECPROD, NETWORKOP, HOUSEHOLD;
}

enum BusinessStructure {
    INCORPASSOCIATION, PTYLTD, PUBLICCOMP, TRUST, SOLETRADER, LIMITEDPARTNERSHIP, COOPERATIVE, INFORMAL, GRASSROOTS, NOTFORPROF, SUBSIDIARY;
}
//governance role: maker, follower, implementer
enum GovRole {
    RULEFOLLOW, RULEMAKER, RULEIMPLEMENT;
}

enum OwnershipModel {
    SHARES, DONATION_BASED, COMMUNITY_INVEST, COMM_DEV_PARTNER, COMM_COUNCIL_PARTNER, MULTIHOUSEHOLD, INDIVIDUAL;
}

public class Actor implements java.io.Serializable {
    private static final long serialVersionUID = 1;

    private int id; // actor id
    private ActorType actorType; // household, generator, transmission operator, distribution operator, retailer, gov institution, etc.
    private String name; //AGL, Angela, SunCorp, Powershop, AEMO, ACCC, COAG, Origin, AER, AEMO etc.
    private GovRole role; // rule follower, rule implementer, rule maker
    private BusinessStructure businessType; // cooperative, public company, private, etc.
    private OwnershipModel ownershipModel;
    ArrayList<ActorActorRelationship> contracts;
    ArrayList<ActorAssetRelationship> assetRelationships;

    public Actor(int id, ActorType actorType, String name, GovRole role, BusinessStructure businessType, OwnershipModel ownershipModel ) {
        this.id = id;
        this.actorType = actorType;
        this.name = name;
        this.role = role;
        this.businessType = businessType;
        this.ownershipModel = ownershipModel;
        this.contracts = new ArrayList<>();
        this.assetRelationships = new ArrayList<>();
    }

    public void addContract(ActorActorRelationship newContract) {
        this.contracts.add(newContract);
    }

    public void addAssetRelationship(ActorAssetRelationship newAssetRel) {
        this.assetRelationships.add(newAssetRel);
    }

    /* private int minNumPeople; // minimum number of people to set up the organisation
    private int maxNumPeople; //// minimum number of people to set up the organisation
    private int numPeople; //number of people in the organisation
    private boolean liability; //limited (1) or non-limited (0) liability;
    private boolean taXemp; //tax exemptions or concessions;
    private String activity; // part of the supply chain or SPM owned/managed by the organistion: e.g. generator, network operator, ret
*/

}
