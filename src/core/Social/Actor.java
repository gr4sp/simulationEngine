package core.Social;

import core.Relationships.ActorAssetRelationship;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.ArrayList;
import java.util.Date;

public class
Actor implements java.io.Serializable, Steppable {
    private static final long serialVersionUID = 1;

    private int id; // actor id
    private String name; //AGL, Angela, SunCorp, Powershop, AEMO, ACCC, COAG, Origin, AER, AEMO etc.
    private Date start;
    private Date changeDate;
    private String region; //VIC, NSW, ACT, etc
    private String reg_number; //ABN or any other official id
    private String actor_role;
    private String businessStructure;

    public ArrayList<ActorAssetRelationship> assetRelationships;

    public Actor(int id, String name) {
        this.id = id;
        this.name = name;
        this.assetRelationships = new ArrayList<>();
        this.start = null;
        this.changeDate = null;
    }

    public Actor(int id, String name, java.sql.Date registrationDate, java.sql.Date changeDate, String reg_number, String region, String actor_role, String businessStructure) {
        this.id = id;
        this.name = name;
        this.start = registrationDate;
        this.changeDate = changeDate;
        this.reg_number = reg_number;
        this.region = region;
        this.actor_role = actor_role;
        this.businessStructure = businessStructure;
        this.assetRelationships = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public Date getStart() {
        return start;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public String getName() {
        return name;
    }

    public void addAssetRelationship(ActorAssetRelationship newAssetRel) {
        this.assetRelationships.add(newAssetRel);
    }

    @Override
    public void step(SimState simState) {

    }


    /* private int minNumPeople; // minimum number of people to set up the organisation
    private int maxNumPeople; //// minimum number of people to set up the organisation
    private int numPeople; //number of people in the organisation
    private boolean liability; //limited (1) or non-limited (0) liability;
    private boolean taXemp; //tax exemptions or concessions;
    private String activity; // part of the supply chain or SPM owned/managed by the organistion: e.g. generator, network operator, ret
*/

}
