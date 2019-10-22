package core.Social;

import core.Relationships.ActorActorRelationship;
import core.Relationships.ActorAssetRelationship;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Date;







public class
Actor extends SimplePortrayal2D implements java.io.Serializable, Steppable {
    private static final long serialVersionUID = 1;

    private int id; // actor id
    private ActorType actorType; // household, generator, transmission operator, distribution operator, retailer, gov institution, etc.

    private String name; //AGL, Angela, SunCorp, Powershop, AEMO, ACCC, COAG, Origin, AER, AEMO etc.
    private GovRole role; // rule follower, rule implementer, rule maker
    private BusinessStructure businessType; // cooperative, public company, private, etc.
    private OwnershipModel ownershipModel;
    private Date start;
    private Date end;
    private String region; //VIC, NSW, ACT, etc
    private String reg_number; //ABN or any other official id
    private String actor_role;
    private String businessStructure;

    public ArrayList<ActorActorRelationship> contracts;
    public ArrayList<ActorAssetRelationship> assetRelationships;

    public Actor(int id, ActorType actorType, String name, GovRole role, BusinessStructure businessType, OwnershipModel ownershipModel) {
        this.id = id;
        this.actorType = actorType;
        this.name = name;
        this.role = role;
        this.businessType = businessType;
        this.ownershipModel = ownershipModel;
        this.contracts = new ArrayList<>();
        this.assetRelationships = new ArrayList<>();
        this.start = null;
        this.end = null;
    }

    public Actor(int id, String name, java.sql.Date registrationDate, java.sql.Date changeDate, String reg_number, String region, String actor_role, String businessStructure) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.end = end;
        this.reg_number = reg_number;
        this.region = region;
        this.actor_role = actor_role;
        this.businessType = businessType;
        this.contracts = new ArrayList<>();
        this.assetRelationships = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addContract(ActorActorRelationship newContract) {
        this.contracts.add(newContract);
    }

    public void addAssetRelationship(ActorAssetRelationship newAssetRel) {
        this.assetRelationships.add(newAssetRel);
    }

    @Override
    public void step(SimState simState) {

    }

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
        double width = info.draw.width * 60;
        double height = info.draw.height * 60;

        graphics.setColor(Color.blue);


        int x = (int) (info.draw.x - width / 2.0D);
        int y = (int) (info.draw.y - height / 2.0D);
        int w = (int) width;
        int h = (int) height;
        graphics.fillOval(x, y, w, h);
    }

    public boolean hitObject(Object object, DrawInfo2D range) {
        double SLOP = 1.0D;
        double width = range.draw.width * 10;
        double height = range.draw.height * 10;
        Ellipse2D.Double ellipse = new Ellipse2D.Double(range.draw.x - width / 2.0D - 1.0D, range.draw.y - height / 2.0D - 1.0D, width + 2.0D, height + 2.0D);
        return ellipse.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
    }

    /* private int minNumPeople; // minimum number of people to set up the organisation
    private int maxNumPeople; //// minimum number of people to set up the organisation
    private int numPeople; //number of people in the organisation
    private boolean liability; //limited (1) or non-limited (0) liability;
    private boolean taXemp; //tax exemptions or concessions;
    private String activity; // part of the supply chain or SPM owned/managed by the organistion: e.g. generator, network operator, ret
*/

}
