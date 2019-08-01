package core;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.util.Double2D;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Date;

enum ActorType {
    //Most categories are taken from current NEM participant categories (see http://www.aemo.com.au/-/media/Files/Electricity/NEM/Participant_Information/Participant-Categories-in-the-NEM.pdf)
    GOVAUTHORITY, ELECPROV, HDCONSUMER, MARKETGENSCHD, MARKETGENONSCHD, REGULATOR, MARKETGENSEMISCHD, OMBUDSMAN, IMPLEMENT, MARKETGENANCILLARY, NONMARKETGENSCHD, STATUTORYAGENCY, BROKER,
    NONMARKETGENNONSCHD, SPECULATOR, NONMARKETGENSEMISCHD, PUBLICSERVANT, SMALLGENAGGREG, OTHER, MARKETLOADANCILLARY, MARKETLOADSCHD, FIRSTTIERSCUST, SECONDTIERCUST, MARKETNETWORKPROV,
    NETWORKSERVSCHD, SYSTEMOPER, DISTRIBSYSOPER, REALLOCATOR, TRADER, INTENTPARTICIPANT, INVESTOR, TBD;
    //non-market units are going to be considered for participants before the NEM, specifically
    // as non scheduled, or NONMARKETGENNONSCHD

    //in deciding the actor type we are going to use cases, and a check on actor-asset relationships and actor-actor relationships
    // //instead of plugin the types into the DB:

    public String getActorType(int actorId) {
        String switchValue = "other";
        switch (switchValue) { //depending on the associated ID of the actor, it will be from one type or the other
            case "ELECPROV":
                System.out.printf("Electricity provider");
                break;
            case "MARKETGENSCHD":
                System.out.printf("Market generator scheduled");
                break;
            default:
                System.out.printf("Not Sure");
        }
        return "NA";
    }


}

enum BusinessStructure {
    INCORPASSOCIATION, PTYLTD, PRIVATELTD, PUBLICCOMP, TRUST, PERSON, SOLETRADER, LIMITEDPARTNERSHIP, COOPERATIVE, INFORMAL,
    GRASSROOTS, NOTFORPROF, STATE, COMMONWEALTH, BODYCORP, OTHER, TBD;
}

//governance role: maker, follower, implementer
enum GovRole {
    RULEFOLLOW, RULEMAKER, RULEIMPLEMENT, OTHER, TBD;
}

enum OwnershipModel {
    SHARES, DONATION_BASED, COMMUNITY_INVEST, COMM_DEV_PARTNER, COMM_COUNCIL_PARTNER,
    MULTIHOUSEHOLD, INDIVIDUAL, STATE, OTHER, TBD;
}

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

    public Actor(int id, ActorType actorType, String name, GovRole role, BusinessStructure businessType, OwnershipModel ownershipModel, Date start, Date end) {
        this.id = id;
        this.actorType = actorType;
        this.name = name;
        this.role = role;
        this.businessType = businessType;
        this.ownershipModel = ownershipModel;
        this.contracts = new ArrayList<>();
        this.assetRelationships = new ArrayList<>();
        this.start = start;
        this.end = end;
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
