package core.Relationships;


import core.Social.Actor;
import core.Technical.Asset;

public class Bid implements java.io.Serializable, Comparable<Bid>{
    public Actor actor;
    public Asset asset;
    public double dollarMWh;
    public double capacity;

    public Bid(Actor actor, Asset asset, double dollarMWh, double capacity) {
        this.actor = actor;
        this.asset = asset;
        this.dollarMWh = dollarMWh;
        this.capacity = capacity;
    }

    @Override
    public int compareTo(Bid other){
        return Double.compare(this.dollarMWh, other.dollarMWh);
    }
}
