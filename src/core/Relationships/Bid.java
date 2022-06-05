package core.Relationships;


import core.Social.Actor;
import core.Technical.Asset;
import core.Technical.Generator;

public class Bid implements java.io.Serializable, Comparable<Bid>{
    public Actor actor;
    public Asset asset;

    public double getDollarMWh() {
        return dollarMWh;
    }

    public double dollarMWh;
    public double capacity;

    public Bid(Actor actor, Asset asset, double dollarMWh, double capacity) {
        this.actor = actor;
        this.asset = asset;
        this.dollarMWh = dollarMWh;
        this.capacity = capacity;
    }

    public Generator getGen(){ return (Generator) this.asset;}

    @Override
    public int compareTo(Bid other){
        return Double.compare(this.dollarMWh, other.dollarMWh);
    }

    public double getBidEF(int year){
        return this.asset.getEmissionsFactor(year);

    }
}
