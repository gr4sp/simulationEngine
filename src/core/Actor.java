package core;

import sim.util.Double2D;

import java.util.ArrayList;

public class Actor implements java.io.Serializable{
    private static final long serialVersionUID = 1;

    private int actor; // actor id
    private String actorType; // household, generator, transmission operator, distribution operator, retailer, gov institution, etc.
    private String name; //AGL, Angela, SunCorp, Powershop, AEMO, ACCC, COAG, Origin, AER, AEMO etc.
    private String main_role; // rule follower, rule implementer, rule maker
    private String organisationType; //household, cooperative, public company, private, etc.
    ArrayList<ActorActorRelationship> contracts;
    ArrayList<ActorAssetRelationship> assetRelationships;

    public Actor(int actor, String actorType, String name, String main_role, String organisationType) {
        this.actor = actor;
        this.actorType = actorType;
        this.name = name;
        this.main_role = main_role;
        this.organisationType = organisationType;
        this.contracts = new ArrayList<>();
        this.assetRelationships = new ArrayList<>();
    }

    public void addContract(ActorActorRelationship newContract){
        this.contracts.add(newContract);
    }

    public void addAssetRelationship( ActorAssetRelationship newAssetRel){
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
