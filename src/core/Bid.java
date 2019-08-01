package core;


public class Bid implements Comparable<Bid>{
    public Actor act;
    public float priceMWh;
    public float capacity;

    @Override
    public int compareTo(Bid other){
        return Float.compare(this.priceMWh, other.priceMWh);
    }
}
