package core.Relationships;

import java.util.Date;

public class Contract {

    //This is how we can access Actors
    //simstate.actor_register.get(sellerId)
    private String tariffName;
    private int sellerId;
    private int buyerId;
    //To retrieve asset we can run in gr4spSim.java: Asset asset = network_register.get(assetIdUsed); or generator_register if previous lookup returns null
    private int assetIdUsed;
    private float pricecKWh;
    private float mothlyServiceFee;
    private Date start;
    private Date end;

    // PPA Min Capacity contracted, If not met, seller have to pay market price to buyer
    private float capacityContracted;

    public Contract(String tariffName, int sellerId, int buyerId, int assetIdUsed, float pricecKWh, float mothlyServiceFee, Date start, Date end, float capacityContracted) {
        this.tariffName = tariffName;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.assetIdUsed = assetIdUsed;
        this.pricecKWh = pricecKWh;
        this.start = start;
        this.end = end;
        this.capacityContracted = capacityContracted;
        this.mothlyServiceFee = mothlyServiceFee;
    }


    public int getSellerId() {
        return sellerId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public int getAssetIdUsed() {
        return assetIdUsed;
    }

    public float getPricecKWh() {
        return pricecKWh;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public float getCapacityContracted() {
        return capacityContracted;
    }

    public String getTariffName() {
        return tariffName;
    }

    public float getMothlyServiceFee() {
        return mothlyServiceFee;
    }
}
