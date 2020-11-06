package core.Relationships;

import java.util.Date;

public class Contract implements java.io.Serializable{

    private String tariffName;
    private int buyerId;

    private float pricecKWh;
    private float mothlyServiceFee;
    private Date start;
    private Date end;

    public Contract(String tariffName,  int buyerId,  float pricecKWh, float mothlyServiceFee, Date start, Date end) {
        this.tariffName = tariffName;
        this.buyerId = buyerId;
        this.pricecKWh = pricecKWh;
        this.start = start;
        this.end = end;
        this.mothlyServiceFee = mothlyServiceFee;
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

    public float getMothlyServiceFee() {
        return mothlyServiceFee;
    }
}
