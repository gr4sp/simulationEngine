package core.Relationships;

import java.util.Date;

public class Contract implements java.io.Serializable{

    private String tariffName;
    private int buyerId;

    private float pricecKWh;
    private Date start;
    private Date end;

    public Contract(String tariffName,  int buyerId,  float pricecKWh, Date start, Date end) {
        this.tariffName = tariffName;
        this.buyerId = buyerId;
        this.pricecKWh = pricecKWh;
        this.start = start;
        this.end = end;
    }


    public int getBuyerId() {
        return buyerId;
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

    public String getTariffName() {
        return tariffName;
    }

}
